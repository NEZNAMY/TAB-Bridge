package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import com.google.common.io.ByteArrayDataInput;
import lombok.Getter;
import me.neznamy.tab.bridge.bukkit.BukkitBridgePlayer;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BridgeNameTagX implements Listener {

    private final JavaPlugin plugin;
    @Getter private boolean enabled;

    @Getter private boolean disableOnBoats;
    @Getter private boolean alwaysVisible;
    @Getter private List<String> dynamicLines;
    @Getter private Map<String, Double> staticLines;

    private final Set<BridgePlayer> playersDisabledWithAPI = Collections.newSetFromMap(new WeakHashMap<>());
    @Getter private final Set<BridgePlayer> disabledUnlimitedPlayers = Collections.newSetFromMap(new WeakHashMap<>());

    @Getter private final PacketListener packetListener = new PacketListener(this);
    @Getter private final VehicleRefresher vehicleManager = new VehicleRefresher(this);
    private final EventListener eventListener = new EventListener(this);

    private final Map<BridgePlayer, ArmorStandManager> armorStandManagerMap = new WeakHashMap<>();
    private final Set<BridgePlayer> playersPreviewingNameTag = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<BridgePlayer> playersWithHiddenVisibilityView = Collections.newSetFromMap(new WeakHashMap<>());

    private BukkitTask visibilityRefreshTask;

    public BridgeNameTagX(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ArmorStandManager getArmorStandManager(BridgePlayer player) {
        return armorStandManagerMap.get(player);
    }

    public boolean isPlayerDisabled(BridgePlayer player) {
        return disabledUnlimitedPlayers.contains(player) || playersDisabledWithAPI.contains(player);
    }

    public Set<BridgePlayer> getPlayersPreviewingNameTag() {
        return playersPreviewingNameTag;
    }

    private void spawnArmorStands(BukkitBridgePlayer viewer, BukkitBridgePlayer target) {
        if (viewer.getProtocolVersion() < 47) return;
        if (target == viewer || isPlayerDisabled(target)) return;
        if (viewer.getPlayer().getWorld() != target.getPlayer().getWorld()) return;
        if (getDistance(viewer, target) <= 48 && viewer.getPlayer().canSee(target.getPlayer()) && !target.isVanished()) {
            if (viewer.getPlayer().canSee(target.getPlayer())) getArmorStandManager(target).spawn(viewer);
        }
    }

    public void load() {
        Bukkit.getPluginManager().registerEvents(eventListener, plugin);
        visibilityRefreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
                if (isPlayerDisabled(p) || !armorStandManagerMap.containsKey(p)) continue;
                getArmorStandManager(p).updateVisibility(false);
            }
        }, 0, 10);
    }

    public void unload() {
        if (!enabled) return;
        HandlerList.unregisterAll(eventListener);
        visibilityRefreshTask.cancel();
        for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
            getArmorStandManager(p).destroy();
        }
    }

    public void onJoin(BukkitBridgePlayer player, ByteArrayDataInput input) {
        if (NMSStorage.getInstance() == null) return;
        boolean enabled = input.readBoolean();
        if (enabled) {
            disableOnBoats = input.readBoolean();
            alwaysVisible = input.readBoolean();
            if (input.readBoolean()) {
                disabledUnlimitedPlayers.add(player);
            }
            int dynamicLineCount = input.readInt();
            List<String> dynamicLines = new ArrayList<>();
            for (int i=0; i<dynamicLineCount; i++) {
                dynamicLines.add(input.readUTF());
            }
            this.dynamicLines = dynamicLines;
            int staticLineCount = input.readInt();
            Map<String, Double> staticLines = new HashMap<>();
            for (int i=0; i<staticLineCount; i++) {
                staticLines.put(input.readUTF(), input.readDouble());
            }
            this.staticLines = staticLines;
        }
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                load();
            } else {
                unload();
            }
        }
        if (!enabled) return;
        packetListener.onJoin(player);
        vehicleManager.onJoin(player);
        armorStandManagerMap.put(player, new ArmorStandManager(this, player));
        if (isPlayerDisabled(player)) return;
        for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
            if (viewer == player) continue;
            spawnArmorStands((BukkitBridgePlayer) viewer, player);
            spawnArmorStands(player, (BukkitBridgePlayer) viewer);
        }
    }

    public void onQuit(BukkitBridgePlayer player) {
        if (!enabled) return;
        packetListener.onQuit(player);
        vehicleManager.onQuit(player);
        for (BridgePlayer all : TABBridge.getInstance().getOnlinePlayers()) {
            if (getArmorStandManager(all) != null) getArmorStandManager(all).unregisterPlayer(player);
        }
        getArmorStandManager(player).destroy();
        armorStandManagerMap.remove(player);
    }

    public void readMessage(BukkitBridgePlayer receiver, ByteArrayDataInput in) {
        ArmorStandManager asm = getArmorStandManager(receiver);
        if (asm == null) return;
        String action = in.readUTF();
        if (action.equals("Preview")) {
            if (in.readBoolean()) {
                playersPreviewingNameTag.add(receiver);
                asm.spawn(receiver);
            } else {
                playersPreviewingNameTag.remove(receiver);
                asm.destroy(receiver);
            }
        }
        if (action.equals("Destroy")) {
            asm.destroy();
        }
        if (action.equals("SetText")) {
            asm.getArmorStand(in.readUTF()).setText(in.readUTF(), in.readUTF());
        }
        if (action.equals("Pause")) {
            playersDisabledWithAPI.add(receiver);
            asm.destroy();
        }
        if (action.equals("Resume")) {
            for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                if (viewer == receiver) continue;
                spawnArmorStands((BukkitBridgePlayer) viewer, receiver);
            }
            playersDisabledWithAPI.remove(receiver);
        }
        if (action.equals("VisibilityView")) {
            if (playersWithHiddenVisibilityView.contains(receiver)) {
                playersWithHiddenVisibilityView.remove(receiver);
            } else {
                playersWithHiddenVisibilityView.add(receiver);
            }
            for (BridgePlayer all : TABBridge.getInstance().getOnlinePlayers()) {
                getArmorStandManager(all).updateVisibility(true);
            }
        }
        if (action.equals("SetEnabled")) {
            boolean enabled = in.readBoolean();
            if (enabled) {
                disabledUnlimitedPlayers.remove(receiver);
            } else {
                disabledUnlimitedPlayers.add(receiver);
            }
        }
    }

    /**
     * Returns flat distance between two players ignoring Y value
     * @param player1 - first player
     * @param player2 - second player
     * @return flat distance in blocks
     */
    private double getDistance(BukkitBridgePlayer player1, BukkitBridgePlayer player2) {
        Location loc1 = player1.getPlayer().getLocation();
        Location loc2 = player2.getPlayer().getLocation();
        return Math.sqrt(Math.pow(loc1.getX()-loc2.getX(), 2) + Math.pow(loc1.getZ()-loc2.getZ(), 2));
    }

    public boolean hasHiddenNametag(BridgePlayer player) {
        return playersDisabledWithAPI.contains(player);
    }

    public boolean hasHiddenNameTagVisibilityView(BridgePlayer player) {
        return playersWithHiddenVisibilityView.contains(player);
    }
}
