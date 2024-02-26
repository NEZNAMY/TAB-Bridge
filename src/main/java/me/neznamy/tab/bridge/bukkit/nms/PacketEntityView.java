package me.neznamy.tab.bridge.bukkit.nms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.bukkit.BukkitBridgePlayer;
import me.neznamy.tab.bridge.shared.util.QuadFunction;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PacketEntityView {

    /** PacketPlayOutEntityDestroy */
    private static Class<?> EntityDestroyClass;
    private static Constructor<?> newEntityDestroy;
    private static Field EntityDestroy_Entities;

    /** PacketPlayOutEntityMetadata */
    private static Constructor<?> newEntityMetadata;

    /** PacketPlayOutEntityTeleport */
    private static Class<?> EntityTeleportClass;
    private static Constructor<?> newEntityTeleport;
    private static Field EntityTeleport_EntityId;
    private static Field EntityTeleport_X;
    private static Field EntityTeleport_Y;
    private static Field EntityTeleport_Z;

    /** PacketPlayOutSpawnEntityLiving */
    private static Class<?> SpawnEntityClass;
    private static Constructor<?> newSpawnEntity;

    /** 1.17+ */
    private static Object Vec3D_Empty;
    private static Object EntityTypes_ARMOR_STAND;

    /** 1.16.5- */
    private static Field SpawnEntity_EntityId;
    private static Field SpawnEntity_EntityType;
    private static Field SpawnEntity_UUID;
    private static Field SpawnEntity_X;
    private static Field SpawnEntity_Y;
    private static Field SpawnEntity_Z;
    private static Field SpawnEntity_DataWatcher;
    private static final EnumMap<EntityType, Integer> entityIds = new EnumMap<>(EntityType.class);

    /** Other entity packets */
    private static Class<?> PacketPlayOutEntity;
    private static Field PacketPlayOutEntity_ENTITYID;
    private static Field PacketPlayOutEntity_X;
    private static Field PacketPlayOutEntity_Y;
    private static Field PacketPlayOutEntity_Z;
    private static Class<?> PacketPlayOutEntityLook;
    private static QuadFunction<Integer, Long, Long, Long, Object> newMovePacket;
    private static Class<?> PacketPlayOutNamedEntitySpawn;
    private static Field PacketPlayOutNamedEntitySpawn_ENTITYID;

    private static Object dummyEntity;

    @Getter
    private static boolean available;

    /** Player this view belongs to */
    private final BukkitBridgePlayer player;

    /**
     * Loads all required classes and fields and throws Exception if something went wrong
     *
     * @throws  ReflectiveOperationException
     *          If something fails
     */
    public static void load() throws ReflectiveOperationException {
        loadEntityMetadata();
        loadEntityDestroy();
        loadEntityTeleport();
        loadEntityMove();
        loadEntitySpawn();
        available = true;
    }

    private static void loadEntityMetadata() throws ReflectiveOperationException {
        // Class
        Class<?> entityMetadataClass = getClass("network.protocol.game.ClientboundSetEntityDataPacket",
                "network.protocol.game.PacketPlayOutEntityMetadata", "PacketPlayOutEntityMetadata", "Packet40EntityMetadata");

        // Constructor
        if (BukkitBridge.is1_19_3Plus()) {
            newEntityMetadata = entityMetadataClass.getConstructor(int.class, List.class);
        } else {
            newEntityMetadata = entityMetadataClass.getConstructor(int.class, DataWatcher.DataWatcherClass, boolean.class);
        }
    }

    private static void loadEntityDestroy() throws ReflectiveOperationException {
        // Class
        EntityDestroyClass = getClass("network.protocol.game.ClientboundRemoveEntitiesPacket",
                "network.protocol.game.PacketPlayOutEntityDestroy", "PacketPlayOutEntityDestroy", "Packet29DestroyEntity");

        // Constructor
        try {
            newEntityDestroy = EntityDestroyClass.getConstructor(int[].class);
        } catch (NoSuchMethodException e) {
            //1.17.0
            newEntityDestroy = EntityDestroyClass.getConstructor(int.class);
        }

        // Field
        EntityDestroy_Entities = ReflectionUtils.getOnlyField(EntityDestroyClass);
    }

    private static void loadEntityTeleport() throws ReflectiveOperationException {
        // Class
        EntityTeleportClass = getClass("network.protocol.game.ClientboundTeleportEntityPacket",
                "network.protocol.game.PacketPlayOutEntityTeleport", "PacketPlayOutEntityTeleport", "Packet34EntityTeleport");

        // Constructor
        if (BukkitBridge.getMinorVersion() >= 17) {
            newEntityTeleport = EntityTeleportClass.getConstructor(getClass("world.entity.Entity"));
        } else {
            newEntityTeleport = EntityTeleportClass.getConstructor();
        }

        // Fields
        EntityTeleport_EntityId = ReflectionUtils.getFields(EntityTeleportClass, int.class).get(0);
        if (BukkitBridge.getMinorVersion() >= 9) {
            EntityTeleport_X = ReflectionUtils.getFields(EntityTeleportClass, double.class).get(0);
            EntityTeleport_Y = ReflectionUtils.getFields(EntityTeleportClass, double.class).get(1);
            EntityTeleport_Z = ReflectionUtils.getFields(EntityTeleportClass, double.class).get(2);
        } else {
            EntityTeleport_X = ReflectionUtils.getFields(EntityTeleportClass, int.class).get(1);
            EntityTeleport_Y = ReflectionUtils.getFields(EntityTeleportClass, int.class).get(2);
            EntityTeleport_Z = ReflectionUtils.getFields(EntityTeleportClass, int.class).get(3);
        }

        // Dummy armor stand for constructor
        if (BukkitBridge.getMinorVersion() >= 17) {
            Class<?> world = getClass("world.level.Level", "world.level.World", "World");
            Class<?> entityArmorStand = getClass("world.entity.decoration.ArmorStand",
                    "world.entity.decoration.EntityArmorStand", "EntityArmorStand");
            Constructor<?> newEntityArmorStand = entityArmorStand.getConstructor(world, double.class, double.class, double.class);
            Method World_getHandle = Class.forName("org.bukkit.craftbukkit." + BukkitBridge.getServerPackage() + ".CraftWorld").getMethod("getHandle");
            dummyEntity = newEntityArmorStand.newInstance(World_getHandle.invoke(Bukkit.getWorlds().get(0)), 0, 0, 0);
        }
    }

    private static void loadEntityMove() throws ReflectiveOperationException {
        // Classes
        PacketPlayOutEntity = getClass("network.protocol.game.ClientboundMoveEntityPacket",
                "network.protocol.game.PacketPlayOutEntity", "PacketPlayOutEntity", "Packet30Entity");
        PacketPlayOutEntityLook = getClass("network.protocol.game.ClientboundMoveEntityPacket$Rot",
                "network.protocol.game.PacketPlayOutEntity$PacketPlayOutEntityLook", "PacketPlayOutEntity$PacketPlayOutEntityLook",
                "PacketPlayOutEntityLook", "Packet32EntityLook");
        Class<?> packetPlayOutRelEntityMove = BukkitReflection.getClass("network.protocol.game.ClientboundMoveEntityPacket$Pos",
                "network.protocol.game.PacketPlayOutEntity$PacketPlayOutRelEntityMove", "PacketPlayOutEntity$PacketPlayOutRelEntityMove",
                "PacketPlayOutRelEntityMove", "Packet31RelEntityMove");

        // Fields
        PacketPlayOutEntity_ENTITYID = ReflectionUtils.getFields(PacketPlayOutEntity, int.class).get(0);

        if (BukkitReflection.getMinorVersion() >= 14) {
            List<Field> fields = ReflectionUtils.getFields(PacketPlayOutEntity, short.class);
            PacketPlayOutEntity_X = fields.get(0);
            PacketPlayOutEntity_Y = fields.get(1);
            PacketPlayOutEntity_Z = fields.get(2);
            Constructor<?> constructor = packetPlayOutRelEntityMove.getConstructor(int.class, short.class, short.class, short.class, boolean.class);
            newMovePacket = (entityId, x, y, z) -> constructor.newInstance(entityId, x.shortValue(), y.shortValue(), z.shortValue(), false);
        } else if (BukkitReflection.getMinorVersion() >= 9) {
            List<Field> fields = ReflectionUtils.getFields(PacketPlayOutEntity, int.class);
            PacketPlayOutEntity_X = fields.get(1);
            PacketPlayOutEntity_Y = fields.get(2);
            PacketPlayOutEntity_Z = fields.get(3);
            Constructor<?> constructor = packetPlayOutRelEntityMove.getConstructor(int.class, long.class, long.class, long.class, boolean.class);
            newMovePacket = (entityId, x, y, z) -> constructor.newInstance(entityId, x, y, z, false);
        } else {
            List<Field> fields = ReflectionUtils.getFields(PacketPlayOutEntity, byte.class);
            PacketPlayOutEntity_X = fields.get(0);
            PacketPlayOutEntity_Y = fields.get(1);
            PacketPlayOutEntity_Z = fields.get(2);
            Constructor<?> constructor = packetPlayOutRelEntityMove.getConstructor(int.class, byte.class, byte.class, byte.class, boolean.class);
            newMovePacket = (entityId, x, y, z) -> constructor.newInstance(entityId, x.byteValue(), y.byteValue(), z.byteValue(), false);
        }
    }

    private static void loadEntitySpawn() throws ReflectiveOperationException {
        SpawnEntityClass = getClass("network.protocol.game.ClientboundAddEntityPacket",
                "network.protocol.game.PacketPlayOutSpawnEntity", "PacketPlayOutSpawnEntityLiving", "Packet24MobSpawn");
        SpawnEntity_EntityId = ReflectionUtils.getFields(SpawnEntityClass, int.class).get(0);
        if (BukkitBridge.getMinorVersion() >= 17) {
            Class<?> Vec3D = getClass("world.phys.Vec3", "world.phys.Vec3D");
            Vec3D_Empty = ReflectionUtils.getOnlyField(Vec3D, Vec3D).get(null);
            Class<?> EntityTypes = getClass("world.entity.EntityType", "world.entity.EntityTypes");
            if (BukkitBridge.getMinorVersion() >= 19) {
                EntityTypes_ARMOR_STAND = ReflectionUtils.getField(EntityTypes, "ARMOR_STAND", "d").get(null);
            } else {
                EntityTypes_ARMOR_STAND = ReflectionUtils.getField(EntityTypes, "ARMOR_STAND", "c", "f_20529_").get(null); // Mohist 1.18.2
            }
            if (BukkitBridge.getMinorVersion() >= 19) {
                newSpawnEntity = SpawnEntityClass.getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, EntityTypes, int.class, Vec3D, double.class);
            } else {
                newSpawnEntity = SpawnEntityClass.getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, EntityTypes, int.class, Vec3D);
            }
        } else {
            newSpawnEntity = SpawnEntityClass.getConstructor();
            if (BukkitBridge.getMinorVersion() >= 9) {
                SpawnEntity_UUID = ReflectionUtils.getOnlyField(SpawnEntityClass, UUID.class);
                SpawnEntity_X = ReflectionUtils.getFields(SpawnEntityClass, double.class).get(0);
                SpawnEntity_Y = ReflectionUtils.getFields(SpawnEntityClass, double.class).get(1);
                SpawnEntity_Z = ReflectionUtils.getFields(SpawnEntityClass, double.class).get(2);
            } else {
                SpawnEntity_X = ReflectionUtils.getFields(SpawnEntityClass, int.class).get(2);
                SpawnEntity_Y = ReflectionUtils.getFields(SpawnEntityClass, int.class).get(3);
                SpawnEntity_Z = ReflectionUtils.getFields(SpawnEntityClass, int.class).get(4);
            }
            SpawnEntity_EntityType = ReflectionUtils.getFields(SpawnEntityClass, int.class).get(1);
            if (BukkitBridge.getMinorVersion() <= 14) {
                SpawnEntity_DataWatcher = ReflectionUtils.getOnlyField(SpawnEntityClass, DataWatcher.DataWatcherClass);
            }
        }
        if (BukkitBridge.getMinorVersion() >= 13) {
            entityIds.put(EntityType.ARMOR_STAND, 1);
        } else {
            entityIds.put(EntityType.ARMOR_STAND, 30);
        }
        if (!BukkitBridge.is1_20_2Plus()) {
            PacketPlayOutNamedEntitySpawn = getClass("network.protocol.game.ClientboundAddPlayerPacket",
                    "network.protocol.game.PacketPlayOutNamedEntitySpawn", "PacketPlayOutNamedEntitySpawn", "Packet20NamedEntitySpawn");
            PacketPlayOutNamedEntitySpawn_ENTITYID = ReflectionUtils.getFields(PacketPlayOutNamedEntitySpawn, int.class).get(0);
        }
    }

    private static int floor(double paramDouble) {
        int i = (int)paramDouble;
        return paramDouble < i ? i - 1 : i;
    }

    @SneakyThrows
    public void spawnEntity(int entityId, @NotNull UUID id, @NotNull Object entityType, @NotNull Location l, @NotNull DataWatcher data) {
        int minorVersion = BukkitBridge.getMinorVersion();
        if (minorVersion >= 19) {
            player.sendPacket(newSpawnEntity.newInstance(entityId, id, l.getX(), l.getY(), l.getZ(), 0, 0, EntityTypes_ARMOR_STAND, 0, Vec3D_Empty, 0d));
        } else if (minorVersion >= 17) {
            player.sendPacket(newSpawnEntity.newInstance(entityId, id, l.getX(), l.getY(), l.getZ(), 0, 0, EntityTypes_ARMOR_STAND, 0, Vec3D_Empty));
        } else {
            Object nmsPacket = newSpawnEntity.newInstance();
            SpawnEntity_EntityId.set(nmsPacket, entityId);
            if (minorVersion <= 14) {
                SpawnEntity_DataWatcher.set(nmsPacket, data.toNMS());
            }
            if (minorVersion >= 9) {
                SpawnEntity_UUID.set(nmsPacket, id);
                SpawnEntity_X.set(nmsPacket, l.getX());
                SpawnEntity_Y.set(nmsPacket, l.getY());
                SpawnEntity_Z.set(nmsPacket, l.getZ());
            } else {
                SpawnEntity_X.set(nmsPacket, floor(l.getX()*32));
                SpawnEntity_Y.set(nmsPacket, floor(l.getY()*32));
                SpawnEntity_Z.set(nmsPacket, floor(l.getZ()*32));
            }
            SpawnEntity_EntityType.set(nmsPacket, entityIds.get((EntityType) entityType));
            player.sendPacket(nmsPacket);
        }
        if (BukkitBridge.getMinorVersion() >= 15) {
            updateEntityMetadata(entityId, data);
        }
    }

    @SneakyThrows
    public void updateEntityMetadata(int entityId, @NotNull DataWatcher data) {
        if (newEntityMetadata.getParameterCount() == 2) {
            //1.19.3+
            player.sendPacket(newEntityMetadata.newInstance(entityId, DataWatcher.DataWatcher_packDirty.invoke(data.toNMS())));
        } else {
            player.sendPacket(newEntityMetadata.newInstance(entityId, data.toNMS(), true));
        }
    }

    @SneakyThrows
    public void teleportEntity(int entityId, @NotNull Location location) {
        Object nmsPacket;
        if (BukkitBridge.getMinorVersion() >= 17) {
            nmsPacket = newEntityTeleport.newInstance(dummyEntity);
        } else {
            nmsPacket = newEntityTeleport.newInstance();
        }
        EntityTeleport_EntityId.set(nmsPacket, entityId);
        if (BukkitBridge.getMinorVersion() >= 9) {
            EntityTeleport_X.set(nmsPacket, location.getX());
            EntityTeleport_Y.set(nmsPacket, location.getY());
            EntityTeleport_Z.set(nmsPacket, location.getZ());
        } else {
            EntityTeleport_X.set(nmsPacket, floor(location.getX()*32));
            EntityTeleport_Y.set(nmsPacket, floor(location.getY()*32));
            EntityTeleport_Z.set(nmsPacket, floor(location.getZ()*32));
        }
        player.sendPacket(nmsPacket);
    }

    @SneakyThrows
    public void destroyEntities(int... entities) {
        if (newEntityDestroy.getParameterTypes()[0] != int.class) {
            player.sendPacket(newEntityDestroy.newInstance(new Object[]{entities}));
        } else {
            //1.17.0 Mojank
            for (int entity : entities) {
                player.sendPacket(newEntityDestroy.newInstance(entity));
            }
        }
    }

    public boolean isDestroyPacket(@NotNull Object packet) {
        return EntityDestroyClass.isInstance(packet);
    }

    public boolean isTeleportPacket(@NotNull Object packet) {
        return EntityTeleportClass.isInstance(packet);
    }

    public boolean isNamedEntitySpawnPacket(@NotNull Object packet) {
        if (BukkitBridge.is1_20_2Plus()) {
            return SpawnEntityClass.isInstance(packet);
        } else {
            return PacketPlayOutNamedEntitySpawn.isInstance(packet);
        }
    }

    public boolean isMovePacket(@NotNull Object packet) {
        return PacketPlayOutEntity.isInstance(packet);
    }

    public boolean isLookPacket(@NotNull Object packet) {
        return PacketPlayOutEntityLook.isInstance(packet);
    }

    @SneakyThrows
    public int getTeleportEntityId(@NotNull Object teleportPacket) {
        return EntityTeleport_EntityId.getInt(teleportPacket);
    }

    @SneakyThrows
    public int getMoveEntityId(@NotNull Object movePacket) {
        return PacketPlayOutEntity_ENTITYID.getInt(movePacket);
    }

    @SneakyThrows
    public int getSpawnedPlayer(@NotNull Object playerSpawnPacket) {
        if (BukkitBridge.is1_20_2Plus()) {
            return SpawnEntity_EntityId.getInt(playerSpawnPacket);
        } else {
            return PacketPlayOutNamedEntitySpawn_ENTITYID.getInt(playerSpawnPacket);
        }
    }

    @SneakyThrows
    public int[] getDestroyedEntities(@NotNull Object destroyPacket) {
        Object entities = PacketEntityView.EntityDestroy_Entities.get(destroyPacket);
        if (BukkitBridge.getMinorVersion() >= 17) {
            if (entities instanceof List) {
                return ((List<Integer>)entities).stream().mapToInt(i -> i).toArray();
            } else {
                //1.17.0
                return new int[]{(int) entities};
            }
        } else {
            return (int[]) entities;
        }
    }

    @SneakyThrows
    @NotNull
    public Location getMoveDiff(@NotNull Object movePacket) {
        return new Location(null,
                ((Number)PacketPlayOutEntity_X.get(movePacket)).intValue(),
                ((Number)PacketPlayOutEntity_Y.get(movePacket)).intValue(),
                ((Number)PacketPlayOutEntity_Z.get(movePacket)).intValue()
        );
    }

    @SneakyThrows
    public void moveEntity(int entityId, @NotNull Location moveDiff) {
        player.sendPacket(newMovePacket.apply(
                entityId, (long) moveDiff.getX(), (long) moveDiff.getY(), (long) moveDiff.getZ()));
    }

    public static Class<?> getClass(@NotNull String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                if (BukkitBridge.getMinorVersion() >= 17) {
                    return PacketEntityView.class.getClassLoader().loadClass("net.minecraft." + name);
                } else {
                    return PacketEntityView.class.getClassLoader().loadClass("net.minecraft.server." + BukkitBridge.getServerPackage() + "." + name);
                }
            } catch (ClassNotFoundException | NullPointerException e) {
                // Wrong class name
            }
        }
        throw new ClassNotFoundException("No class found with potential names " + Arrays.toString(names));
    }
}