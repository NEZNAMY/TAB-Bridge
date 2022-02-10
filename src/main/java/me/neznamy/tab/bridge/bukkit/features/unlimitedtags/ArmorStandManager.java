package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import me.neznamy.tab.bridge.bukkit.BridgePlayer;

import java.util.*;

/**
 * A helper class for easy management of armor stands of a player
 */
public class ArmorStandManager {

	private final BridgeNameTagX nameTagX;
	//map of registered armor stands
	private final Map<String, ArmorStand> ArmorStands = new LinkedHashMap<>();

	//players in entity tracking range
	private final List<BridgePlayer> nearbyPlayers = new ArrayList<>();

	//array to iterate over to avoid concurrent modification and slightly boost performance & memory
	private final ArmorStand[] ArmorStandArray;

	private BridgePlayer[] nearbyPlayerArray = new BridgePlayer[0];

	public ArmorStandManager(BridgeNameTagX nameTagX, BridgePlayer owner) {
		this.nameTagX = nameTagX;
		double height = 0;
		for (String line : nameTagX.getDynamicLines()) {
			ArmorStands.put(line, new ArmorStand(owner, height, false));
			height += nameTagX.getSpaceBetweenLines();
		}
		for (Map.Entry<String, Double> line : nameTagX.getStaticLines().entrySet()) {
			ArmorStands.put(line.getKey(), new ArmorStand(owner, Double.parseDouble(line.getValue().toString()), true));
		}
		ArmorStandArray = ArmorStands.values().toArray(new ArmorStand[0]);
	}

    public ArmorStand getArmorStand(String name) {
        return ArmorStands.get(name);
    }
	
	public void spawn(BridgePlayer viewer) {
		if (nearbyPlayers.contains(viewer)) return;
		nearbyPlayers.add(viewer);
		nearbyPlayerArray = nearbyPlayers.toArray(new BridgePlayer[0]);
		for (ArmorStand a : ArmorStandArray) a.spawn(viewer);
	}
	
	public void sneak(boolean sneaking) {
		for (ArmorStand a : ArmorStandArray) a.sneak(sneaking);
	}
	
	public void teleport() {
		for (ArmorStand a : ArmorStandArray) a.teleport();
	}
	
	public void teleport(BridgePlayer viewer) {
		for (ArmorStand a : ArmorStandArray) a.teleport(viewer);
	}

	public void updateVisibility(boolean force) {
		for (ArmorStand a : ArmorStandArray) a.updateVisibility(force);
	}
	
	public void unregisterPlayer(BridgePlayer viewer) {
		if (nearbyPlayers.remove(viewer)) nearbyPlayerArray = nearbyPlayers.toArray(new BridgePlayer[0]);
	}
	
	public void destroy() {
		for (ArmorStand a : ArmorStandArray) a.destroy();
		nearbyPlayers.clear();
		nearbyPlayerArray = new BridgePlayer[0];
	}
	
	public void destroy(BridgePlayer viewer) {
		for (ArmorStand a : ArmorStandArray) a.destroy(viewer);
		unregisterPlayer(viewer);
	}
	
	public boolean hasArmorStandWithID(int entityId) {
		for (ArmorStand a : ArmorStandArray) {
			if (a.getEntityId() == entityId) {
				return true;
			}
		}
		return false;
	}

	public BridgePlayer[] getNearbyPlayers(){
		return nearbyPlayerArray;
	}
	
	public boolean isNearby(BridgePlayer viewer) {
		return nearbyPlayers.contains(viewer);
	}
	
	public void respawn() {
		for (ArmorStand a : ArmorStandArray) {
			for (BridgePlayer viewer : nearbyPlayerArray) {
				a.respawn(viewer);
			}
		}
	}

	public void fixArmorStandHeights() {
		double currentY = -nameTagX.getSpaceBetweenLines();
		for (ArmorStand as : ArmorStandArray) {
			if (as.hasStaticOffset()) continue;
			if (as.getText().length() != 0) {
				currentY += nameTagX.getSpaceBetweenLines();
				as.setOffset(currentY);
			}
		}
	}
}