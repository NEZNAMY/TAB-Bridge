package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import me.neznamy.tab.bridge.bukkit.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleRefresher {

	//map of players currently in a vehicle
	private final WeakHashMap<BridgePlayer, Entity> playersInVehicle = new WeakHashMap<>();
	
	//map of vehicles carrying players
	private final Map<Integer, List<Entity>> vehicles = new ConcurrentHashMap<>();
	
	//set of players currently on boats
	private final Set<BridgePlayer> playersOnBoats = Collections.newSetFromMap(new WeakHashMap<>());

	private final WeakHashMap<BridgePlayer, String> playerVehicles = new WeakHashMap<>();
	
	private final BridgeNameTagX feature;

	public VehicleRefresher(BridgeNameTagX feature) {
		this.feature = feature;
		Bukkit.getScheduler().runTaskTimerAsynchronously(BukkitBridge.getInstance(), () -> {
			for (BridgePlayer inVehicle : playersInVehicle.keySet()) {
				ArmorStandManager asm = feature.getArmorStandManager(inVehicle);
				if (asm != null) asm.teleport();
			}
			for (BridgePlayer p : BukkitBridge.getInstance().getOnlinePlayers()) {
				if (feature.getPlayersPreviewingNameTag().contains(p)) {
					feature.getArmorStandManager(p).teleport(p);
				}
				String vehicle = String.valueOf(p.getPlayer().getVehicle());
				if (playerVehicles.getOrDefault(p, "null").equals(vehicle)) {
					playerVehicles.put(p, vehicle);
					refresh(p);
				}
			}
		}, 0, 1);
	}

	public void onJoin(BridgePlayer connectedPlayer) {
		Entity vehicle = connectedPlayer.getPlayer().getVehicle();
		if (vehicle != null) {
			vehicles.put(vehicle.getEntityId(), getPassengers(vehicle));
			playersInVehicle.put(connectedPlayer, vehicle);
			if (feature.isDisableOnBoats() && vehicle.getType() == EntityType.BOAT) {
				playersOnBoats.add(connectedPlayer);
			}
		}
	}

	public void onQuit(BridgePlayer disconnectedPlayer) {
		if (playersInVehicle.containsKey(disconnectedPlayer)) vehicles.remove(playersInVehicle.get(disconnectedPlayer).getEntityId());
		for (List<Entity> entities : vehicles.values()) {
			entities.remove(disconnectedPlayer.getPlayer());
		}
	}

	public void refresh(BridgePlayer p) {
		if (feature.isPlayerDisabled(p)) return;
		Entity vehicle = p.getPlayer().getVehicle();
		if (playersInVehicle.containsKey(p) && vehicle == null) {
			//vehicle exit
			vehicles.remove(playersInVehicle.get(p).getEntityId());
			feature.getArmorStandManager(p).teleport();
			playersInVehicle.remove(p);
			if (feature.isDisableOnBoats() && playersOnBoats.contains(p)) {
				playersOnBoats.remove(p);
				p.sendMessage("Boat", false);
			}
		}
		if (!playersInVehicle.containsKey(p) && vehicle != null) {
			//vehicle enter
			vehicles.put(vehicle.getEntityId(), getPassengers(vehicle));
			feature.getArmorStandManager(p).respawn(); //making teleport instant instead of showing teleport animation
			playersInVehicle.put(p, vehicle);
			if (feature.isDisableOnBoats() && vehicle.getType() == EntityType.BOAT) {
				playersOnBoats.add(p);
				p.sendMessage("Boat", true);
			}
		}
	}

	public boolean isOnBoat(BridgePlayer p) {
		return playersOnBoats.contains(p);
	}
	
	public Map<Integer, List<Entity>> getVehicles() {
		return vehicles;
	}
	
	/**
	 * Returns list of all passengers on specified vehicle
	 * @param vehicle - vehicle to check passengers of
	 * @return list of passengers
	 */
	@SuppressWarnings("deprecation")
	public List<Entity> getPassengers(Entity vehicle){
		if (NMSStorage.getInstance().getMinorVersion() >= 11) {
			return vehicle.getPassengers();
		} else {
			if (vehicle.getPassenger() != null) {
				return Collections.singletonList(vehicle.getPassenger());
			} else {
				return new ArrayList<>();
			}
		}
	}
}