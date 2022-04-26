package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.bukkit.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BridgeNameTagX implements Listener {

    private boolean enabled;

    private boolean markerFor18x;
    private boolean disableOnBoats;
    private double spaceBetweenLines;
    private boolean alwaysVisible;
    private List<String> dynamicLines;
    private Map<String, Double> staticLines;

    private final Set<BridgePlayer> playersDisabledWithAPI = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<BridgePlayer> disabledUnlimitedPlayers = Collections.newSetFromMap(new WeakHashMap<>());
    private String[] disabledUnlimitedWorldsArray;
    private boolean unlimitedWorldWhitelistMode;

    private final PacketListener packetListener = new PacketListener(this);
    private final VehicleRefresher vehicleManager = new VehicleRefresher(this);
    private final EventListener eventListener = new EventListener(this);

    private final Map<BridgePlayer, ArmorStandManager> armorStandManagerMap = new WeakHashMap<>();
    private final Set<BridgePlayer> playersPreviewingNameTag = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<BridgePlayer> playersWithHiddenVisibilityView = Collections.newSetFromMap(new WeakHashMap<>());

    private BukkitTask visibilityRefreshTask;

    public BridgeNameTagX() {
        Bukkit.getPluginManager().registerEvents(this, BukkitBridge.getInstance());
    }

    public ArmorStandManager getArmorStandManager(BridgePlayer player) {
        return armorStandManagerMap.get(player);
    }

    public boolean isPlayerDisabled(BridgePlayer player) {
        return disabledUnlimitedPlayers.contains(player) || playersDisabledWithAPI.contains(player);
    }

    public double getSpaceBetweenLines() {
        return spaceBetweenLines;
    }

    public List<String> getDynamicLines() {
        return dynamicLines;
    }

    public Map<String, Double> getStaticLines() {
        return staticLines;
    }

    public boolean isAlwaysVisible() {
        return alwaysVisible;
    }

    public Set<BridgePlayer> getPlayersPreviewingNameTag() {
        return playersPreviewingNameTag;
    }

    public boolean isUnlimitedDisabled(String world) {
        boolean contains = contains(disabledUnlimitedWorldsArray, world);
        if (unlimitedWorldWhitelistMode) contains = !contains;
        return contains;
    }

    private boolean contains(String[] list, String element) {
        if (element == null) return false;
        for (String s : list) {
            if (s.endsWith("*")) {
                if (element.toLowerCase().startsWith(s.substring(0, s.length()-1).toLowerCase())) return true;
            } else {
                if (element.equalsIgnoreCase(s)) return true;
            }
        }
        return false;
    }

    public Set<BridgePlayer> getDisabledUnlimitedPlayers() {
        return disabledUnlimitedPlayers;
    }

    private void spawnArmorStands(BridgePlayer viewer, BridgePlayer target) {
        if (target == viewer || isPlayerDisabled(target)) return;
        if (viewer.getPlayer().getWorld() != target.getPlayer().getWorld()) return;
        if (getDistance(viewer, target) <= 48) {
            if (viewer.getPlayer().canSee(target.getPlayer())) getArmorStandManager(target).spawn(viewer);
        }
    }

    public void load() {
        Bukkit.getPluginManager().registerEvents(eventListener, BukkitBridge.getInstance());
        visibilityRefreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(BukkitBridge.getInstance(), () -> {
            for (BridgePlayer p : BukkitBridge.getInstance().getOnlinePlayers()) {
                if (isPlayerDisabled(p) || !armorStandManagerMap.containsKey(p)) continue;
                getArmorStandManager(p).updateVisibility(false);
            }
        }, 0, 10);
    }

    public void unload() {
        HandlerList.unregisterAll(eventListener);
        visibilityRefreshTask.cancel();
        for (BridgePlayer p : BukkitBridge.getInstance().getOnlinePlayers()) {
            getArmorStandManager(p).destroy();
        }
    }

    public void onJoin(BridgePlayer player) {
        if (!enabled) return;
        packetListener.onJoin(player);
        vehicleManager.onJoin(player);
        if (isUnlimitedDisabled(player.getPlayer().getWorld().getName()))
            disabledUnlimitedPlayers.add(player);
        armorStandManagerMap.put(player, new ArmorStandManager(this, player));
        if (isPlayerDisabled(player)) return;
        for (BridgePlayer viewer : BukkitBridge.getInstance().getOnlinePlayers()) {
            if (viewer == player) continue;
            spawnArmorStands(viewer, player);
            spawnArmorStands(player, viewer);
        }
    }

    public void onQuit(BridgePlayer player) {
        if (!enabled) return;
        packetListener.onQuit(player);
        vehicleManager.onQuit(player);
        for (BridgePlayer all : BukkitBridge.getInstance().getOnlinePlayers()) {
            if (getArmorStandManager(all) != null) getArmorStandManager(all).unregisterPlayer(player);
        }
        getArmorStandManager(player).destroy();
        armorStandManagerMap.remove(player);
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }

    public boolean isDisableOnBoats() {
        return disableOnBoats;
    }

    public VehicleRefresher getVehicleManager() {
        return vehicleManager;
    }

    public boolean isMarkerFor18x() {
        return markerFor18x;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void readJoinInput(ByteArrayDataInput input) {
        if (NMSStorage.getInstance() == null) return;
        boolean enabled = input.readBoolean();
        if (enabled) {
            markerFor18x = input.readBoolean();
            spaceBetweenLines = input.readDouble();
            disableOnBoats = input.readBoolean();
            alwaysVisible = input.readBoolean();
            int disabledWouldCount = input.readInt();
            List<String> worlds = new ArrayList<>();
            for (int i=0; i<disabledWouldCount; i++) {
                worlds.add(input.readUTF());
            }
            disabledUnlimitedWorldsArray = worlds.toArray(new String[0]);
            unlimitedWorldWhitelistMode = worlds.contains("WHITELIST");
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
    }

    public void readMessage(BridgePlayer receiver, ByteArrayDataInput in) {
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
            for (BridgePlayer viewer : BukkitBridge.getInstance().getOnlinePlayers()) {
                if (viewer == receiver) continue;
                spawnArmorStands(viewer, receiver);
            }
            playersDisabledWithAPI.remove(receiver);
        }
        if (action.equals("VisibilityView")) {
            if (playersWithHiddenVisibilityView.contains(receiver)) {
                playersWithHiddenVisibilityView.remove(receiver);
            } else {
                playersWithHiddenVisibilityView.add(receiver);
            }
            for (BridgePlayer all : BukkitBridge.getInstance().getOnlinePlayers()) {
                getArmorStandManager(all).updateVisibility(true);
            }
        }
    }

    /**
     * Returns flat distance between two players ignoring Y value
     * @param player1 - first player
     * @param player2 - second player
     * @return flat distance in blocks
     */
    private double getDistance(BridgePlayer player1, BridgePlayer player2) {
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
