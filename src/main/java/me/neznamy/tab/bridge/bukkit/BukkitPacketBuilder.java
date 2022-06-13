package me.neznamy.tab.bridge.bukkit;

import me.neznamy.tab.bridge.bukkit.nms.DataWatcher;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.UUID;

public class BukkitPacketBuilder {

	private static final BukkitPacketBuilder instance = new BukkitPacketBuilder();

	//nms storage
	private final NMSStorage nms = NMSStorage.getInstance();

	//entity type ids
	private final EnumMap<EntityType, Integer> entityIds = new EnumMap<>(EntityType.class);

	private Object dummyEntity;

	/**
	 * Constructs new instance
	 */
	public BukkitPacketBuilder() {
        if (nms == null) return;
        if (nms.getMinorVersion() >= 19) {
            entityIds.put(EntityType.ARMOR_STAND, 2);
        } else if (nms.getMinorVersion() >= 13) {
			entityIds.put(EntityType.ARMOR_STAND, 1);
			entityIds.put(EntityType.WITHER, 83);
		} else {
			entityIds.put(EntityType.WITHER, 64);
			if (nms.getMinorVersion() >= 8){
				entityIds.put(EntityType.ARMOR_STAND, 30);
			}
		}
		if (nms.getMinorVersion() >= 8) {
			try {
				dummyEntity = nms.newEntityArmorStand.newInstance(nms.World_getHandle.invoke(Bukkit.getWorlds().get(0)), 0, 0, 0);
			} catch (ReflectiveOperationException e) {
				Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB] Failed to create instance of \"EntityArmorStand\"");
			}
		}
	}

	public static BukkitPacketBuilder getInstance() {
		return instance;
	}

	public Object entityDestroy(int... entities) {
        try {
            try {
                return nms.newPacketPlayOutEntityDestroy.newInstance(new Object[]{entities});
            } catch (IllegalArgumentException e) {
                //1.17.0
                return nms.newPacketPlayOutEntityDestroy.newInstance(entities[0]);
            }
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

	public Object entityMetadata(int entityId, DataWatcher dataWatcher) {
        try {
            return nms.newPacketPlayOutEntityMetadata.newInstance(entityId, dataWatcher.toNMS(), true);
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

	public Object entitySpawn(int entityId, UUID uniqueId, EntityType entityType, Location location, DataWatcher dataWatcher) {
        try {
            Object nmsPacket;
            if (nms.getMinorVersion() >= 17) {
                nmsPacket = nms.newPacketPlayOutSpawnEntityLiving.newInstance(dummyEntity);
            } else {
                nmsPacket = nms.newPacketPlayOutSpawnEntityLiving.newInstance();
            }
            nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_ENTITYID, entityId);
            nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_YAW, (byte)(location.getYaw() * 256.0f / 360.0f));
            nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_PITCH, (byte)(location.getPitch() * 256.0f / 360.0f));
            if (nms.getMinorVersion() <= 14) {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_DATAWATCHER, dataWatcher.toNMS());
            }
            if (nms.getMinorVersion() >= 9) {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_UUID, uniqueId);
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_X, location.getX());
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Y, location.getY());
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Z, location.getZ());
            } else {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_X, floor(location.getX()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Y, floor(location.getY()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Z, floor(location.getZ()*32));
            }
            int id = entityIds.get(entityType);
            if (nms.getMinorVersion() >= 19) {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_ENTITYTYPE, nms.Registry_a.invoke(nms.IRegistry_X, id));
            } else {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_ENTITYTYPE, id);
            }
            return nmsPacket;
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

	public Object entityTeleport(int entityId, Location location) {
        try {
            Object nmsPacket;
            if (nms.getMinorVersion() >= 17) {
                nmsPacket = nms.newPacketPlayOutEntityTeleport.newInstance(dummyEntity);
            } else {
                nmsPacket = nms.newPacketPlayOutEntityTeleport.newInstance();
            }
            nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_ENTITYID, entityId);
            if (nms.getMinorVersion() >= 9) {
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_X, location.getX());
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Y, location.getY());
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Z, location.getZ());
            } else {
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_X, floor(location.getX()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Y, floor(location.getY()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Z, floor(location.getZ()*32));
            }
            nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_YAW, (byte) (location.getYaw()/360*256));
            nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_PITCH, (byte) (location.getPitch()/360*256));
            return nmsPacket;
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

	private int floor(double paramDouble){
		int i = (int)paramDouble;
		return paramDouble < i ? i - 1 : i;
	}
}