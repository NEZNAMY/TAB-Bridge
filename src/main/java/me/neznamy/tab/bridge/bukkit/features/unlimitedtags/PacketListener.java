package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import me.neznamy.tab.bridge.bukkit.BukkitBridgePlayer;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.Location;
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

    public void onJoin(BukkitBridgePlayer connectedPlayer) {
        entityIdMap.put(connectedPlayer.getPlayer().getEntityId(), connectedPlayer);
    }

    public void onQuit(BukkitBridgePlayer disconnectedPlayer) {
        entityIdMap.remove(disconnectedPlayer.getPlayer().getEntityId());
    }

    public void onPacketSend(BukkitBridgePlayer receiver, Object packet) {
        if (nms == null) return;
        if (receiver.getProtocolVersion() < 47) return;
        if (nameTagX.isPlayerDisabled(receiver) || nameTagX.getDisabledUnlimitedPlayers().contains(receiver)) return;
        if (receiver.getEntityView().isMovePacket(packet) && !receiver.getEntityView().isLookPacket(packet)) { //ignoring head rotation only packets
            onEntityMove(receiver, receiver.getEntityView().getMoveEntityId(packet), receiver.getEntityView().getMoveDiff(packet));
        } else if (receiver.getEntityView().isTeleportPacket(packet)) {
            onEntityTeleport(receiver, receiver.getEntityView().getTeleportEntityId(packet));
        } else if (receiver.getEntityView().isNamedEntitySpawnPacket(packet)) {
            onEntitySpawn(receiver, receiver.getEntityView().getSpawnedPlayer(packet));
        } else if (receiver.getEntityView().isDestroyPacket(packet)) {
            onEntityDestroy(receiver, receiver.getEntityView().getDestroyedEntities(packet));
        }
    }

    private void onEntityMove(BukkitBridgePlayer receiver, int entityId, Location diff) {
        BridgePlayer pl = entityIdMap.get(entityId);
        List<Entity> vehicleList;
        if (pl != null) {
            //player moved
            if (nameTagX.isPlayerDisabled(pl)) return;
            TABBridge.getInstance().submitTask(() -> {
                ArmorStandManager asm = nameTagX.getArmorStandManager(pl);
                if (asm != null) asm.move(receiver, diff);
            });
        } else if ((vehicleList = nameTagX.getVehicleManager().getVehicles().get(entityId)) != null){
            //a vehicle carrying something moved
            for (Entity entity : vehicleList) {
                BridgePlayer passenger = entityIdMap.get(entity.getEntityId());
                if (passenger != null && nameTagX.getArmorStandManager(passenger) != null) {
                    TABBridge.getInstance().submitTask(() -> nameTagX.getArmorStandManager(passenger).move(receiver, diff));
                }
            }
        }
    }

    /**
     * Processes entity teleport packet
     * @param receiver - packet receiver
     * @param entityId - entity that teleported
     */
    private void onEntityTeleport(BukkitBridgePlayer receiver, int entityId) {
        BridgePlayer pl = entityIdMap.get(entityId);
        List<Entity> vehicleList;
        if (pl != null) {
            //player moved
            if (nameTagX.isPlayerDisabled(pl)) return;
            TABBridge.getInstance().submitTask(() -> {
                ArmorStandManager asm = nameTagX.getArmorStandManager(pl);
                if (asm != null) asm.teleport(receiver);
            });
        } else if ((vehicleList = nameTagX.getVehicleManager().getVehicles().get(entityId)) != null){
            //a vehicle carrying something moved
            for (Entity entity : vehicleList) {
                BridgePlayer passenger = entityIdMap.get(entity.getEntityId());
                if (passenger != null && nameTagX.getArmorStandManager(passenger) != null) {
                    TABBridge.getInstance().submitTask(() -> nameTagX.getArmorStandManager(passenger).teleport(receiver));
                }
            }
        }
    }
    
    private void onEntitySpawn(BukkitBridgePlayer receiver, int entityId) {
        BridgePlayer spawnedPlayer = entityIdMap.get(entityId);
        if (spawnedPlayer != null && !nameTagX.isPlayerDisabled(spawnedPlayer)) {
            TABBridge.getInstance().submitTask(() -> nameTagX.getArmorStandManager(spawnedPlayer).spawn(receiver));
        }
    }

    private void onEntityDestroy(BukkitBridgePlayer receiver, int... entities) {
        for (int entity : entities) {
            BridgePlayer deSpawnedPlayer = entityIdMap.get(entity);
            if (deSpawnedPlayer != null && !nameTagX.isPlayerDisabled(deSpawnedPlayer))
                TABBridge.getInstance().submitTask(() -> nameTagX.getArmorStandManager(deSpawnedPlayer).destroy(receiver));
        }
    }
}