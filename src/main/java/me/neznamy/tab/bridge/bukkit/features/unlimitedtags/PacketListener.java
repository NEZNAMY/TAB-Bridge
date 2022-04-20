package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import me.neznamy.tab.bridge.bukkit.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The packet listening part for securing proper functionality of armor stands
 * Bukkit events are too unreliable and delayed/ahead which causes de-sync
 */
public class PacketListener {

	//main feature
	private final BridgeNameTagX nameTagX;

	//player data by entityId, used for better performance
	private final Map<Integer, BridgePlayer> entityIdMap = new ConcurrentHashMap<>();
	
	//nms storage
	private final NMSStorage nms = NMSStorage.getInstance();

	public PacketListener(BridgeNameTagX nameTagX) {
		this.nameTagX = nameTagX;
	}

	public void onJoin(BridgePlayer connectedPlayer) {
		entityIdMap.put(connectedPlayer.getPlayer().getEntityId(), connectedPlayer);
	}

	public void onQuit(BridgePlayer disconnectedPlayer) {
		entityIdMap.remove(disconnectedPlayer.getPlayer().getEntityId());
	}

	public void onPacketReceive(BridgePlayer sender, Object packet) throws ReflectiveOperationException {
		if (nms == null) return;
		if (nms.PacketPlayInUseEntity.isInstance(packet)) {
			int entityId = nms.PacketPlayInUseEntity_ENTITY.getInt(packet);
			BridgePlayer attacked = null;
			for (BridgePlayer all : BukkitBridge.getInstance().getOnlinePlayers()) {
				if (nameTagX.getArmorStandManager(sender).hasArmorStandWithID(entityId)) {
					attacked = all;
					break;
				}
			}
			if (attacked != null && attacked != sender) {
				nms.setField(packet, nms.PacketPlayInUseEntity_ENTITY, attacked.getPlayer().getEntityId());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void onPacketSend(BridgePlayer receiver, Object packet) throws ReflectiveOperationException {
		if (nms == null) return;
		if (nameTagX.isPlayerDisabled(receiver) || nameTagX.getDisabledUnlimitedPlayers().contains(receiver)) return;
		if (nms.PacketPlayOutEntity.isInstance(packet) && !nms.PacketPlayOutEntityLook.isInstance(packet)) {
			onEntityMove(receiver, nms.PacketPlayOutEntity_ENTITYID.getInt(packet));
		} else if (nms.PacketPlayOutEntityTeleport.isInstance(packet)) {
			onEntityMove(receiver, nms.PacketPlayOutEntityTeleport_ENTITYID.getInt(packet));
		} else if (nms.PacketPlayOutNamedEntitySpawn.isInstance(packet)) {
			onEntitySpawn(receiver, nms.PacketPlayOutNamedEntitySpawn_ENTITYID.getInt(packet));
		} else if (nms.PacketPlayOutEntityDestroy.isInstance(packet)) {
			if (nms.getMinorVersion() >= 17) {
				Object entities = nms.PacketPlayOutEntityDestroy_ENTITIES.get(packet);
				if (entities instanceof List) {
					onEntityDestroy(receiver, (List<Integer>) entities);
				} else {
					//1.17.0
					onEntityDestroy(receiver, (int) entities);
				}
			} else {
				onEntityDestroy(receiver, (int[]) nms.PacketPlayOutEntityDestroy_ENTITIES.get(packet));
			}
		}
	}

	/**
	 * Processes entity move packet
	 * @param receiver - packet receiver
	 * @param entityId - entity that moved
	 */
	private void onEntityMove(BridgePlayer receiver, int entityId) {
		BridgePlayer pl = entityIdMap.get(entityId);
		List<Entity> vehicleList;
		if (pl != null) {
			//player moved
			if (nameTagX.isPlayerDisabled(pl)) return;
			BukkitBridge.getInstance().getDataBridge().submitTask(() -> {
				ArmorStandManager asm = nameTagX.getArmorStandManager(pl);
				if (asm != null) asm.teleport(receiver);
			});
		} else if ((vehicleList = nameTagX.getVehicleManager().getVehicles().get(entityId)) != null){
			//a vehicle carrying something moved
			for (Entity entity : vehicleList) {
				BridgePlayer passenger = entityIdMap.get(entity.getEntityId());
				if (passenger != null && nameTagX.getArmorStandManager(passenger) != null) {
					BukkitBridge.getInstance().getDataBridge().submitTask(() -> nameTagX.getArmorStandManager(passenger).teleport(receiver));
				}
			}
		}
	}
	
	private void onEntitySpawn(BridgePlayer receiver, int entityId) {
		BridgePlayer spawnedPlayer = entityIdMap.get(entityId);
		if (spawnedPlayer != null && !nameTagX.isPlayerDisabled(spawnedPlayer)) {
			BukkitBridge.getInstance().getDataBridge().submitTask(() -> nameTagX.getArmorStandManager(spawnedPlayer).spawn(receiver));
		}
	}

	private void onEntityDestroy(BridgePlayer receiver, List<Integer> entities) {
		for (int entity : entities) {
			onEntityDestroy(receiver, entity);
		}
	}
	
	private void onEntityDestroy(BridgePlayer receiver, int... entities) {
		for (int entity : entities) {
			onEntityDestroy(receiver, entity);
		}
	}
	
	private void onEntityDestroy(BridgePlayer receiver, int entity) {
		BridgePlayer deSpawnedPlayer = entityIdMap.get(entity);
		if (deSpawnedPlayer != null && !nameTagX.isPlayerDisabled(deSpawnedPlayer))
			BukkitBridge.getInstance().getDataBridge().submitTask(() -> nameTagX.getArmorStandManager(deSpawnedPlayer).destroy(receiver));
	}
}