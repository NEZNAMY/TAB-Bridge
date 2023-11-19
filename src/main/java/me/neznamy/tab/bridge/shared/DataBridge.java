package me.neznamy.tab.bridge.shared;

import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import me.neznamy.tab.bridge.shared.message.incoming.*;
import me.neznamy.tab.bridge.shared.message.outgoing.PlayerJoinResponse;
import me.neznamy.tab.bridge.shared.message.outgoing.UpdatePlaceholder;
import me.neznamy.tab.bridge.shared.message.outgoing.UpdateRelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlaceholderReplacementPattern;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class DataBridge {

    private final Map<Object, List<byte[]>> messageQueue = new WeakHashMap<>();
    @Getter private final Map<Object, List<IncomingMessage>> messageQueue2 = new WeakHashMap<>();
    private final Map<String, Placeholder> asyncPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, Placeholder> syncPlaceholders = new ConcurrentHashMap<>();
    @Getter private Map<String, PlaceholderReplacementPattern> replacements = new HashMap<>();
    private boolean groupForwarding;

    private final Map<String, Supplier<IncomingMessage>> registeredMessages = new HashMap<String, Supplier<IncomingMessage>>() {{
        put("Permission", PermissionCheck::new);
        put("Placeholder", PlaceholderRegister::new);
        put("Expansion", ExpansionPlaceholder::new);
        put("PacketPlayOutScoreboardDisplayObjective", SetDisplayObjective::new);
        put("PacketPlayOutScoreboardObjective", ScoreboardObjective::new);
        put("PacketPlayOutScoreboardScore", ScoreboardScore::new);
        put("PacketPlayOutScoreboardTeam", ScoreboardTeam::new);
        put("NameTagX", NameTagMessage::new);
    }};

    public void startTasks() {
        TABBridge.getInstance().getPlatform().scheduleSyncRepeatingTask(() -> updatePlaceholders(syncPlaceholders.values(), false), 1);
        TABBridge.getInstance().getPlaceholderThread().scheduleAtFixedRate(() -> updatePlaceholders(asyncPlaceholders.values(), false), 50, 50, TimeUnit.MILLISECONDS);
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
    @SuppressWarnings("UnstableApiUsage")
    public void processPluginMessage(Object player, byte[] bytes, boolean retry) {
        if (!TABBridge.getInstance().getPlatform().isOnline(player)) {
            messageQueue.computeIfAbsent(player, p -> new ArrayList<>()).add(bytes);
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();
        if (subChannel.equals("PlayerJoin")) {
            // Read join input
            int protocolVersion = in.readInt();
            groupForwarding = in.readBoolean();
            int placeholderCount = in.readInt();
            for (int i=0; i<placeholderCount; i++) {
                registerPlaceholder(in.readUTF(), in.readInt());
            }
            BridgePlayer bp = TABBridge.getInstance().getPlatform().newPlayer(player, protocolVersion);
            readReplacements(in);
            TABBridge.getInstance().getPlatform().readUnlimitedNametagJoin(bp, in);
            TABBridge.getInstance().addPlayer(bp);

            // Send response
            int gamemode = bp.checkGameMode();
            bp.setGameModeRaw(gamemode);
            bp.sendPluginMessage(new PlayerJoinResponse(
                    bp.getWorld(),
                    groupForwarding ? bp.checkGroup() : null,
                    parsePlaceholders(bp),
                    gamemode
            ));
            for (Placeholder placeholder : asyncPlaceholders.values()) {
                if (placeholder instanceof RelationalPlaceholder) {
                    RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
                    for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                        if (pl.update(viewer, bp)) {
                            viewer.sendPluginMessage(new UpdateRelationalPlaceholder(pl.getIdentifier(), bp.getName(), pl.getLastValue(viewer, bp)));
                        }
                    }
                }
            }
            processQueue(player);
            return;
        }
        BridgePlayer pl = TABBridge.getInstance().getPlayer(TABBridge.getInstance().getPlatform().getUniqueId(player));
        if (pl == null) {
            messageQueue.computeIfAbsent(player, p -> new ArrayList<>()).add(bytes);
            return;
        }
        Supplier<IncomingMessage> supplier = registeredMessages.get(subChannel);
        if (supplier != null) {
            IncomingMessage msg = supplier.get();
            msg.read(in);
            msg.process(pl);
        }

        if (subChannel.equals("Unload") && !retry) {
            TABBridge.getInstance().removePlayer(pl);
        }
    }

    private void readReplacements(ByteArrayDataInput in) {
        int placeholderCount = in.readInt();
        Map<String, PlaceholderReplacementPattern> replacements = new HashMap<>();
        for (int i=0; i<placeholderCount; i++) {
            String placeholder = in.readUTF();
            Map<Object, Object> rules = new HashMap<>();
            int ruleCount = in.readInt();
            for (int j=0; j<ruleCount; j++) {
                rules.put(in.readUTF(), in.readUTF());
            }
            replacements.put(placeholder, new PlaceholderReplacementPattern(placeholder, rules));
        }
        this.replacements = replacements;
    }

    public void processQueue(Object player) {
        List<byte[]> list = new ArrayList<>(messageQueue.computeIfAbsent(player, p -> new ArrayList<>()));
        messageQueue.remove(player);
        list.forEach(msg -> processPluginMessage(player, msg, true));
    }

    public void registerPlaceholder(String identifier, int refresh) {
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
                syncPlaceholders.put(identifier, placeholder);
            } else {
                asyncPlaceholders.put(identifier, placeholder);
            }
        }
        if (identifier.startsWith("%sync:")) {
            TABBridge.getInstance().getPlatform().runTask(() -> updatePlaceholders(Collections.singletonList(syncPlaceholders.get(identifier)), true));
        } else {
            updatePlaceholders(Collections.singletonList(asyncPlaceholders.get(identifier)), true);
        }
    }

    public Map<String, Object> parsePlaceholders(BridgePlayer player) {
        Map<String, Object> outputs = new LinkedHashMap<>();
        List<Placeholder> allPlaceholders = Lists.newArrayList(asyncPlaceholders.values());
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

    private void updatePlaceholders(Collection<Placeholder> placeholders, boolean force) {
        for (Placeholder placeholder : placeholders) {
            if (!placeholder.isInPeriod() && !force) continue;
            if (placeholder instanceof ServerPlaceholder) {
                ServerPlaceholder pl = (ServerPlaceholder) placeholder;
                if (pl.update()) {
                    for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
                        p.sendPluginMessage(new UpdatePlaceholder(pl.getIdentifier(), pl.getLastValue()));
                    }
                }
            }
            if (placeholder instanceof PlayerPlaceholder) {
                PlayerPlaceholder pl = (PlayerPlaceholder) placeholder;
                for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
                    if (pl.update(p)) {
                        p.sendPluginMessage(new UpdatePlaceholder(pl.getIdentifier(), pl.getLastValue(p)));
                    }
                }
            }
            if (placeholder instanceof RelationalPlaceholder) {
                RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
                for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                    for (BridgePlayer target : TABBridge.getInstance().getOnlinePlayers()) {
                        if (pl.update(viewer, target)) {
                            viewer.sendPluginMessage(new UpdateRelationalPlaceholder(pl.getIdentifier(), target.getName(), pl.getLastValue(viewer, target)));
                        }
                    }
                }
            }
        }
    }
}
