package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import com.google.common.io.ByteArrayDataInput;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.bridge.bukkit.BukkitBridgePlayer;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BridgeNameTagX {

    private final JavaPlugin plugin;
    @Getter private boolean enabled;

    @Getter private boolean disableOnBoats;
    @Getter private boolean alwaysVisible;
    @Getter private List<String> dynamicLines;
    @Getter private Map<String, Double> staticLines;

    @Getter private final PacketListener packetListener = new PacketListener(this);
    @Getter private final VehicleRefresher vehicleManager = new VehicleRefresher(this);
    private final EventListener eventListener = new EventListener(this);
    private ScheduledFuture<?> visibilityRefreshTask;

    public boolean isPlayerDisabled(BridgePlayer player) {
        return player.unlimitedNametagData.disabled || player.unlimitedNametagData.disabledWithAPI;
    }

    private void spawnArmorStands(BukkitBridgePlayer viewer, BukkitBridgePlayer target) {
        if (viewer.getProtocolVersion() < 47) return;
        if (target == viewer || isPlayerDisabled(target) || target.getPlayer().isDead()) return;
        if (viewer.getPlayer().getWorld() != target.getPlayer().getWorld()) return;
        if (getDistance(viewer, target) <= 48 && viewer.getPlayer().canSee(target.getPlayer()) && !target.isVanished()) {
            target.unlimitedNametagData.armorStandManager.spawn(viewer);
        }
    }

    public void load() {
        Bukkit.getPluginManager().registerEvents(eventListener, plugin);
        visibilityRefreshTask = TABBridge.getInstance().getScheduler().scheduleAtFixedRate(() -> {
            for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
                if (isPlayerDisabled(p) || p.unlimitedNametagData.armorStandManager == null) continue;
                p.unlimitedNametagData.armorStandManager.updateVisibility(false);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void unload() {
        if (!enabled) return;
        HandlerList.unregisterAll(eventListener);
        visibilityRefreshTask.cancel(true);
        for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
            p.unlimitedNametagData.armorStandManager.destroy();
        }
    }

    public void onJoin(BukkitBridgePlayer player, ByteArrayDataInput input) {
        if (NMSStorage.getInstance() == null) return;
        boolean enabled = input.readBoolean();
        if (enabled) {
            disableOnBoats = input.readBoolean();
            alwaysVisible = input.readBoolean();
            if (input.readBoolean()) {
                player.unlimitedNametagData.disabled = true;
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
        player.unlimitedNametagData.armorStandManager = new ArmorStandManager(this, player);
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
            if (all.unlimitedNametagData.armorStandManager != null) all.unlimitedNametagData.armorStandManager.unregisterPlayer(player);
        }
        player.unlimitedNametagData.armorStandManager.destroy();
    }

    public void readMessage(BukkitBridgePlayer receiver, ByteArrayDataInput in) {
        ArmorStandManager asm = receiver.unlimitedNametagData.armorStandManager;
        if (asm == null) return;
        String action = in.readUTF();
        if (action.equals("Preview")) {
            if (in.readBoolean()) {
                receiver.unlimitedNametagData.previewing = true;
                asm.spawn(receiver);
            } else {
                receiver.unlimitedNametagData.previewing = false;
                asm.destroy(receiver);
            }
        }
        if (action.equals("Destroy")) {
            asm.destroy();
        }
        if (action.equals("SetText")) {
            String line = in.readUTF();
            String text = in.readUTF();
            asm.getArmorStand(line).setText(text);
        }
        if (action.equals("Pause")) {
            receiver.unlimitedNametagData.disabledWithAPI = true;
            asm.destroy();
        }
        if (action.equals("Resume")) {
            for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                if (viewer == receiver) continue;
                spawnArmorStands((BukkitBridgePlayer) viewer, receiver);
            }
            receiver.unlimitedNametagData.disabledWithAPI = false;
        }
        if (action.equals("VisibilityView")) {
            receiver.unlimitedNametagData.hiddenNameTagView = !receiver.unlimitedNametagData.hiddenNameTagView;
            for (BridgePlayer all : TABBridge.getInstance().getOnlinePlayers()) {
                all.unlimitedNametagData.armorStandManager.updateVisibility(true);
            }
        }
        if (action.equals("SetEnabled")) {
            boolean enabled = in.readBoolean();
            if (enabled) {
                receiver.unlimitedNametagData.disabled = false;
                for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
                    spawnArmorStands((BukkitBridgePlayer) viewer, receiver);
                }
            } else {
                receiver.unlimitedNametagData.disabled = true;
                receiver.unlimitedNametagData.armorStandManager.destroy();
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

    /**
     * Class storing unlimited nametag data for players.
     */
    public static class PlayerData {

        /** Armor stand manager */
        public ArmorStandManager armorStandManager;

        /** Whether player is previewing armor stands or not */
        public boolean previewing;

        /** Whether armor stands are disabled via API or not */
        public boolean disabledWithAPI;

        /** Whether player is riding a boat or not */
        public boolean onBoat;

        /** Whether this player has hidden nametag view or not */
        public boolean hiddenNameTagView;

        /** Whether player has disabled unlimited nametags or not */
        public boolean disabled;
    }
}
