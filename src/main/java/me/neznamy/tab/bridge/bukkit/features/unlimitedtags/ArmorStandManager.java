package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import me.neznamy.tab.bridge.bukkit.BukkitBridgePlayer;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class for easy management of armor stands of a player
 */
public class ArmorStandManager {

    //map of registered armor stands
    private final Map<String, ArmorStand> ArmorStands = new LinkedHashMap<>();

    //players in entity tracking range
    private final List<BukkitBridgePlayer> nearbyPlayers = new ArrayList<>();

    //array to iterate over to avoid concurrent modification and slightly boost performance & memory
    private final ArmorStand[] ArmorStandArray;

    private BukkitBridgePlayer[] nearbyPlayerArray = new BukkitBridgePlayer[0];

    private final BukkitBridgePlayer owner;

    public ArmorStandManager(BridgeNameTagX nameTagX, BukkitBridgePlayer owner) {
        this.owner = owner;
        double height = 0;
        for (String line : nameTagX.getDynamicLines()) {
            ArmorStands.put(line, new ArmorStand(owner, height, false));
            height += 0.26;
        }
        for (Map.Entry<String, Double> line : nameTagX.getStaticLines().entrySet()) {
            ArmorStands.put(line.getKey(), new ArmorStand(owner, Double.parseDouble(line.getValue().toString()), true));
        }
        ArmorStandArray = ArmorStands.values().toArray(new ArmorStand[0]);
    }

    public ArmorStand getArmorStand(String name) {
        return ArmorStands.get(name);
    }
    
    public void spawn(BukkitBridgePlayer viewer) {
        if (nearbyPlayers.contains(viewer)) return;
        nearbyPlayers.add(viewer);
        nearbyPlayerArray = nearbyPlayers.toArray(new BukkitBridgePlayer[0]);
        for (ArmorStand a : ArmorStandArray) a.spawn(viewer);
    }
    
    public void sneak(boolean sneaking) {
        for (ArmorStand a : ArmorStandArray) a.sneak(sneaking);
    }
    
    public void teleport() {
        for (ArmorStand a : ArmorStandArray) a.teleport();
    }
    
    public void teleport(BukkitBridgePlayer viewer) {
        for (ArmorStand a : ArmorStandArray) a.teleport(viewer);
    }

    public void move(BukkitBridgePlayer viewer, Location diff) {
        for (ArmorStand a : ArmorStandArray) a.move(viewer, diff);
    }

    public void updateVisibility(boolean force) {
        for (ArmorStand a : ArmorStandArray) a.updateVisibility(force);
    }
    
    public void unregisterPlayer(BukkitBridgePlayer viewer) {
        if (nearbyPlayers.remove(viewer)) nearbyPlayerArray = nearbyPlayers.toArray(new BukkitBridgePlayer[0]);
    }
    
    public void destroy() {
        for (ArmorStand a : ArmorStandArray) a.destroy();
        nearbyPlayers.clear();
        nearbyPlayerArray = new BukkitBridgePlayer[0];
    }
    
    public void destroy(BukkitBridgePlayer viewer) {
        for (ArmorStand a : ArmorStandArray) a.destroy(viewer);
        unregisterPlayer(viewer);
    }

    public BukkitBridgePlayer[] getNearbyPlayers(){
        return nearbyPlayerArray;
    }
    
    public boolean isNearby(BukkitBridgePlayer viewer) {
        return nearbyPlayers.contains(viewer);
    }
    
    public void respawn() {
        for (ArmorStand a : ArmorStandArray) {
            for (BukkitBridgePlayer viewer : nearbyPlayerArray) {
                a.respawn(viewer);
            }
        }
    }

    public void fixArmorStandHeights() {
        double currentY = -0.26;
        for (ArmorStand as : ArmorStandArray) {
            if (as.hasStaticOffset()) continue;
            if (as.getText().length() != 0) {
                currentY += 0.26;
                as.setOffset(currentY);
            }
        }
    }
}