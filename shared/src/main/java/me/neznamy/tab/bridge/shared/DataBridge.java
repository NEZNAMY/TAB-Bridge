package me.neznamy.tab.bridge.shared;

import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.message.incoming.*;
import me.neznamy.tab.bridge.shared.message.outgoing.PlayerJoinResponse;
import me.neznamy.tab.bridge.shared.message.outgoing.UpdatePlaceholder;
import me.neznamy.tab.bridge.shared.message.outgoing.UpdateRelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class DataBridge {

    private final Map<UUID, List<byte[]>> messageQueue = new WeakHashMap<>();
    private final Map<String, Placeholder> asyncPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, Placeholder> syncPlaceholders = new ConcurrentHashMap<>();
    private Placeholder[] syncPlaceholderArray = new Placeholder[0];
    private Placeholder[] asyncPlaceholderArray = new Placeholder[0];
    @Getter private Map<String, PlaceholderReplacementPattern> replacements = new HashMap<>();
    private boolean groupForwarding;
    private long refreshCounterSync;
    private long refreshCounterAsync;

    private final Map<String, Function<ByteArrayDataInput, IncomingMessage>> registeredMessages = new HashMap<String, Function<ByteArrayDataInput, IncomingMessage>>() {{
        put("Permission", PermissionCheck::new);
        put("Placeholder", PlaceholderRegister::new);
        put("Expansion", ExpansionPlaceholder::new);
    }};

    public void startTasks() {
        TABBridge.getInstance().getPlatform().scheduleSyncRepeatingTask(() ->
                updatePlaceholders(syncPlaceholderArray, refreshCounterSync += 50), 1);
        TABBridge.getInstance().getPlaceholderThread().scheduleAtFixedRate(() -> {
            try {
                updatePlaceholders(asyncPlaceholderArray, refreshCounterAsync += 50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 50, 50, TimeUnit.MILLISECONDS);
        TABBridge.getInstance().getScheduler().scheduleAtFixedRate(() -> {
            for (BridgePlayer player : TABBridge.getInstance().getOnlinePlayers()) {
                player.setVanished(player.checkVanish());
                player.setDisguised(player.checkDisguised());
                player.setInvisible(player.checkInvisibility());
                if (groupForwarding) {
                    player.setGroup(player.checkGroup());
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
        TABBridge.getInstance().getScheduler().scheduleAtFixedRate(() -> {
            for (BridgePlayer player : TABBridge.getInstance().getOnlinePlayers()) {
                player.setGameMode(player.checkGameMode());
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void processPluginMessage(@NonNull UUID uuid, byte[] bytes, boolean retry) {
        Object player = TABBridge.getInstance().getPlatform().getPlayer(uuid);
        if (player == null) {
            messageQueue.computeIfAbsent(uuid, p -> new ArrayList<>()).add(bytes);
            return;
        }
        processPluginMessage(player, uuid, bytes, retry);
    }

    public void processPluginMessage(@NonNull Object player, @NonNull UUID uuid, byte[] bytes, boolean retry) {
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();
        if (subChannel.equals("PlayerJoin")) {
            // Read join input
            PlayerJoin join = new PlayerJoin(in);
            BridgePlayer bp = TABBridge.getInstance().getPlatform().newPlayer(player);
            TABBridge.getInstance().addPlayer(bp);
            for (Map.Entry<String, Integer> entry : join.getPlaceholders().entrySet()) {
                registerPlaceholder(bp, entry.getKey(), entry.getValue());
            }
            this.replacements = join.getReplacements();

            // Send response
            int gamemode = bp.checkGameMode();
            bp.setGameModeRaw(gamemode);
            bp.sendPluginMessage(new PlayerJoinResponse(
                    bp.getWorld(),
                    groupForwarding ? bp.checkGroup() : null,
                    parsePlaceholders(bp),
                    gamemode
            ));
            for (Placeholder placeholder : asyncPlaceholderArray) {
                if (placeholder instanceof RelationalPlaceholder) {
                    RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
                    for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                        if (pl.update(viewer, bp)) {
                            viewer.sendPluginMessage(new UpdateRelationalPlaceholder(pl.getIdentifier(), bp.getName(), pl.getLastValue(viewer, bp)));
                        }
                    }
                }
            }
            processQueue(player, uuid);
            return;
        }
        BridgePlayer pl = TABBridge.getInstance().getPlayer(uuid);
        if (pl == null) {
            messageQueue.computeIfAbsent(uuid, p -> new ArrayList<>()).add(bytes);
            return;
        }
        Function<ByteArrayDataInput, IncomingMessage> function = registeredMessages.get(subChannel);
        if (function != null) {
            function.apply(in).process(pl);
        }

        if (subChannel.equals("Unload") && !retry) {
            TABBridge.getInstance().removePlayer(pl);
        }
    }

    public void processQueue(@NonNull Object player, @NonNull UUID uuid) {
        List<byte[]> list = messageQueue.remove(uuid);
        if (list != null) list.forEach(msg -> processPluginMessage(player, uuid, msg, true));
    }

    public void registerPlaceholder(@NonNull BridgePlayer player, @NonNull String identifier, int refresh) {
        if (syncPlaceholders.containsKey(identifier)) {
            syncPlaceholders.get(identifier).setRefresh(refresh);
        } else if (asyncPlaceholders.containsKey(identifier)) {
            asyncPlaceholders.get(identifier).setRefresh(refresh);
        } else {
            boolean sync = false;
            String finalIdentifier; //forwarded identifier without sync: prefix
            if (identifier.startsWith("%sync:")) {
                finalIdentifier = "%" + identifier.substring(6);
                sync = true;
            } else {
                finalIdentifier = identifier;
            }
            Placeholder placeholder = TABBridge.getInstance().getPlatform().createPlaceholder(identifier, finalIdentifier, refresh);
            if (sync) {
                addSyncPlaceholder(placeholder);
            } else {
                addAsyncPlaceholder(placeholder);
            }
        }
        if (identifier.startsWith("%sync:")) {
            TABBridge.getInstance().getPlatform().runTask(() ->
                    sendInitialValues(syncPlaceholders.get(identifier), player));
        } else {
            sendInitialValues(asyncPlaceholders.get(identifier), player);
        }
    }

    public void addSyncPlaceholder(@NonNull Placeholder placeholder) {
        syncPlaceholders.put(placeholder.getIdentifier(), placeholder);
        syncPlaceholderArray = syncPlaceholders.values().toArray(new Placeholder[0]);
    }

    public void addAsyncPlaceholder(@NonNull Placeholder placeholder) {
        asyncPlaceholders.put(placeholder.getIdentifier(), placeholder);
        asyncPlaceholderArray = asyncPlaceholders.values().toArray(new Placeholder[0]);
    }

    @NotNull
    public Map<String, Object> parsePlaceholders(@NonNull BridgePlayer player) {
        Map<String, Object> outputs = new LinkedHashMap<>();
        List<Placeholder> allPlaceholders = Lists.newArrayList(asyncPlaceholderArray);
        allPlaceholders.addAll(syncPlaceholders.values());
        for (Placeholder placeholder : allPlaceholders) {
            if (placeholder instanceof ServerPlaceholder) {
                outputs.put(placeholder.getIdentifier(), ((ServerPlaceholder) placeholder).getLastValue());
            }
            if (placeholder instanceof PlayerPlaceholder) {
                outputs.put(placeholder.getIdentifier(), ((PlayerPlaceholder) placeholder).getLastValue(player));
            }
            if (placeholder instanceof RelationalPlaceholder) {
                Map<String, String> relMap = (Map<String, String>) outputs.computeIfAbsent(placeholder.getIdentifier(), p -> new HashMap<>());
                for (BridgePlayer target : TABBridge.getInstance().getOnlinePlayers()) {
                    relMap.put(target.getName(), ((RelationalPlaceholder)placeholder).getLastValue(player, target));
                }
            }
        }
        return outputs;
    }

    private void updatePlaceholders(@NonNull Placeholder[] placeholders, long counter) {
        for (Placeholder placeholder : placeholders) {
            if (placeholder.getRefresh() == -1) continue;
            if (counter % placeholder.getRefresh() != 0) continue;

            if (placeholder instanceof ServerPlaceholder) {
                ServerPlaceholder pl = (ServerPlaceholder) placeholder;
                if (pl.update()) {
                    UpdatePlaceholder msg = new UpdatePlaceholder(pl.getIdentifier(), pl.getLastValue());
                    for (BridgePlayer player : TABBridge.getInstance().getOnlinePlayers()) {
                        player.sendPluginMessage(msg);
                    }
                }
                continue;
            }
            for (BridgePlayer player : TABBridge.getInstance().getOnlinePlayers()) {
                if (placeholder instanceof PlayerPlaceholder) {
                    PlayerPlaceholder pl = (PlayerPlaceholder) placeholder;
                    if (pl.update(player)) {
                        player.sendPluginMessage(new UpdatePlaceholder(pl.getIdentifier(), pl.getLastValue(player)));
                    }
                } else if (placeholder instanceof RelationalPlaceholder) {
                    RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
                    for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                        if (pl.update(viewer, player)) {
                            viewer.sendPluginMessage(new UpdateRelationalPlaceholder(pl.getIdentifier(), player.getName(), pl.getLastValue(viewer, player)));
                        }
                    }
                }
            }
        }
    }

    private void sendInitialValues(@NonNull Placeholder placeholder, @NonNull BridgePlayer player) {
        if (placeholder instanceof ServerPlaceholder) {
            ServerPlaceholder pl = (ServerPlaceholder) placeholder;
            player.sendPluginMessage(new UpdatePlaceholder(pl.getIdentifier(), pl.getLastValue()));
        }
        if (placeholder instanceof PlayerPlaceholder) {
            PlayerPlaceholder pl = (PlayerPlaceholder) placeholder;
            player.sendPluginMessage(new UpdatePlaceholder(pl.getIdentifier(), pl.getLastValue(player)));
        }
        if (placeholder instanceof RelationalPlaceholder) {
            RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
            for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                viewer.sendPluginMessage(new UpdateRelationalPlaceholder(pl.getIdentifier(), player.getName(), pl.getLastValue(viewer, player)));
            }
        }
    }
}
