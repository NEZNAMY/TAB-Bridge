package me.neznamy.tab.bridge.bukkit.nms;

import io.netty.channel.Channel;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import org.bukkit.Bukkit;

import java.lang.reflect.*;
import java.util.*;

public class NMSStorage {

    //instance of this class
    private static NMSStorage instance;

    //server package, such as "v1_16_R3"
    private final String serverPackage = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    //server minor version such as "16"
    private final int minorVersion = Integer.parseInt(serverPackage.split("_")[1]);

    //base
    private final Class<?> Packet = getNMSClass("net.minecraft.network.protocol.Packet", "Packet");
    private final Class<?> EntityPlayer = getNMSClass("net.minecraft.server.level.EntityPlayer", "EntityPlayer");
    private final Class<?> Entity = getNMSClass("net.minecraft.world.entity.Entity", "Entity");
    private final Class<?> EntityLiving = getNMSClass("net.minecraft.world.entity.EntityLiving", "EntityLiving");
    private final Class<?> PlayerConnection = getNMSClass("net.minecraft.server.network.PlayerConnection", "PlayerConnection");
    public Constructor<?> newEntityArmorStand;
    public final Field PLAYER_CONNECTION = getFields(EntityPlayer, PlayerConnection).get(0);
    public Field NETWORK_MANAGER;
    public Field CHANNEL;
    public final Method getHandle = Class.forName("org.bukkit.craftbukkit." + serverPackage + ".entity.CraftPlayer").getMethod("getHandle");
    public final Method sendPacket = getMethods(PlayerConnection, void.class, Packet).get(0);
    public final Method World_getHandle = Class.forName("org.bukkit.craftbukkit." + serverPackage + ".CraftWorld").getMethod("getHandle");

    //chat
    public Class<?> IChatBaseComponent;
    public Method DESERIALIZE;

    //DataWatcher
    private final Class<?> DataWatcher = getNMSClass("net.minecraft.network.syncher.DataWatcher", "DataWatcher");
    private final Class<?> DataWatcherItem = getNMSClass("net.minecraft.network.syncher.DataWatcher$Item", "DataWatcher$Item", "DataWatcher$WatchableObject", "WatchableObject");
    public Class<?> DataWatcherRegistry;
    public final Constructor<?> newDataWatcher = DataWatcher.getConstructors()[0];
    public Constructor<?> newDataWatcherObject;
    public Field DataWatcherItem_TYPE;
    public final Field DataWatcherItem_VALUE = getFields(DataWatcherItem, Object.class).get(0);
    public Field DataWatcherObject_SLOT;
    public Field DataWatcherObject_SERIALIZER;
    public Method DataWatcher_REGISTER;
    private DataWatcherRegistry registry;

    //PacketPlayOutSpawnEntityLiving
    public final Class<?> PacketPlayOutSpawnEntityLiving = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving",
            "net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity", "PacketPlayOutSpawnEntityLiving", "Packet24MobSpawn");
    public Constructor<?> newPacketPlayOutSpawnEntityLiving;
    public final Field PacketPlayOutSpawnEntityLiving_ENTITYID = getFields(PacketPlayOutSpawnEntityLiving, int.class).get(0);
    public Field PacketPlayOutSpawnEntityLiving_ENTITYTYPE;
    public final Field PacketPlayOutSpawnEntityLiving_YAW = getFields(PacketPlayOutSpawnEntityLiving, byte.class).get(0);
    public final Field PacketPlayOutSpawnEntityLiving_PITCH = getFields(PacketPlayOutSpawnEntityLiving, byte.class).get(0);
    public Field PacketPlayOutSpawnEntityLiving_UUID;
    public Field PacketPlayOutSpawnEntityLiving_X;
    public Field PacketPlayOutSpawnEntityLiving_Y;
    public Field PacketPlayOutSpawnEntityLiving_Z;
    public Field PacketPlayOutSpawnEntityLiving_DATAWATCHER;
    public Method Registry_a;
    public Object IRegistry_X;

    //PacketPlayOutEntityTeleport
    public final Class<?> PacketPlayOutEntityTeleport = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport", "PacketPlayOutEntityTeleport", "Packet34EntityTeleport");
    public Constructor<?> newPacketPlayOutEntityTeleport;
    public final Field PacketPlayOutEntityTeleport_ENTITYID = getFields(PacketPlayOutEntityTeleport, int.class).get(0);
    public Field PacketPlayOutEntityTeleport_X;
    public Field PacketPlayOutEntityTeleport_Y;
    public Field PacketPlayOutEntityTeleport_Z;
    public final Field PacketPlayOutEntityTeleport_YAW = getFields(PacketPlayOutEntityTeleport, byte.class).get(0);
    public final Field PacketPlayOutEntityTeleport_PITCH = getFields(PacketPlayOutEntityTeleport, byte.class).get(1);

    //other entity packets
    public final Class<?> PacketPlayInUseEntity = getNMSClass("net.minecraft.network.protocol.game.PacketPlayInUseEntity", "PacketPlayInUseEntity", "Packet7UseEntity");
    public Class<?> PacketPlayInUseEntity$d;
    public Field PacketPlayInUseEntity_ENTITY;
    public Field PacketPlayInUseEntity_ACTION;

    public final Class<?> PacketPlayOutEntity = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntity", "PacketPlayOutEntity", "Packet30Entity");
    public final Field PacketPlayOutEntity_ENTITYID = getFields(PacketPlayOutEntity, int.class).get(0);

    public final Class<?> PacketPlayOutEntityDestroy = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy", "PacketPlayOutEntityDestroy", "Packet29DestroyEntity");
    public Constructor<?> newPacketPlayOutEntityDestroy;
    public final Field PacketPlayOutEntityDestroy_ENTITIES = setAccessible(PacketPlayOutEntityDestroy.getDeclaredFields()[0]);

    public final Class<?> PacketPlayOutEntityLook = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntity$PacketPlayOutEntityLook", "PacketPlayOutEntity$PacketPlayOutEntityLook", "PacketPlayOutEntityLook", "Packet32EntityLook");

    public final Class<?> PacketPlayOutEntityMetadata = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata", "PacketPlayOutEntityMetadata", "Packet40EntityMetadata");
    public final Constructor<?> newPacketPlayOutEntityMetadata = PacketPlayOutEntityMetadata.getConstructor(int.class, DataWatcher, boolean.class);
    public final Field PacketPlayOutEntityMetadata_LIST = getFields(PacketPlayOutEntityMetadata, List.class).get(0);

    public final Class<?> PacketPlayOutNamedEntitySpawn = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn", "PacketPlayOutNamedEntitySpawn", "Packet20NamedEntitySpawn");
    public final Field PacketPlayOutNamedEntitySpawn_ENTITYID = getFields(PacketPlayOutNamedEntitySpawn, int.class).get(0);

    /**
     * Creates new instance, initializes required NMS classes and fields
     * @throws	ReflectiveOperationException
     * 			If any class, field or method fails to load
     */
    public NMSStorage() throws ReflectiveOperationException {
        Class<?> NetworkManager = getNMSClass("net.minecraft.network.NetworkManager", "NetworkManager");
        if (minorVersion >= 7) {
            NETWORK_MANAGER = getFields(PlayerConnection, NetworkManager).get(0);
            IChatBaseComponent = getNMSClass("net.minecraft.network.chat.IChatBaseComponent", "IChatBaseComponent");
            DESERIALIZE = getMethod(IChatBaseComponent.getDeclaredClasses()[0], "a", String.class);
        }
        if (minorVersion >= 8) {
            CHANNEL = getFields(NetworkManager, Channel.class).get(0);
            newEntityArmorStand = getNMSClass("net.minecraft.world.entity.decoration.EntityArmorStand", "EntityArmorStand")
                    .getConstructor(getNMSClass("net.minecraft.world.level.World", "World"), double.class, double.class, double.class);
        }
        initializeDataWatcher();
        initializeEntityPackets();
    }

    /**
     * Sets new instance
     * @param instance - new instance
     */
    public static void setInstance(NMSStorage instance) {
        NMSStorage.instance = instance;
    }

    /**
     * Returns instance
     * @return instance
     */
    public static NMSStorage getInstance() {
        return instance;
    }

    private void initializeDataWatcher() throws ReflectiveOperationException {
        if (minorVersion >= 9) {
            Class<?> DataWatcherObject = getNMSClass("net.minecraft.network.syncher.DataWatcherObject", "DataWatcherObject");
            DataWatcherRegistry = getNMSClass("net.minecraft.network.syncher.DataWatcherRegistry", "DataWatcherRegistry");
            Class<?> DataWatcherSerializer = getNMSClass("net.minecraft.network.syncher.DataWatcherSerializer", "DataWatcherSerializer");
            newDataWatcherObject = DataWatcherObject.getConstructor(int.class, DataWatcherSerializer);
            DataWatcherItem_TYPE = getFields(DataWatcherItem, DataWatcherObject).get(0);
            DataWatcherObject_SLOT = getFields(DataWatcherObject, int.class).get(0);
            DataWatcherObject_SERIALIZER = getFields(DataWatcherObject, DataWatcherSerializer).get(0);
            DataWatcher_REGISTER = getMethod(DataWatcher, new String[]{"register", "method_12784", "a"}, DataWatcherObject, Object.class);
        } else {
            DataWatcherItem_TYPE = getFields(DataWatcherItem, int.class).get(1);
            DataWatcher_REGISTER = getMethod(DataWatcher, new String[]{"a", "func_75682_a"}, int.class, Object.class);
        }
        registry = new DataWatcherRegistry(this);
    }

    private void initializeEntityPackets() throws ReflectiveOperationException {
        if (minorVersion >= 17) {
            newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor(EntityLiving);
            newPacketPlayOutEntityTeleport = PacketPlayOutEntityTeleport.getConstructor(Entity);
        } else {
            newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor();
            newPacketPlayOutEntityTeleport = PacketPlayOutEntityTeleport.getConstructor();
        }
        if (minorVersion >= 17) {
            PacketPlayInUseEntity$d = Class.forName("net.minecraft.network.protocol.game.PacketPlayInUseEntity$d");
        }
        try {
            newPacketPlayOutEntityDestroy = PacketPlayOutEntityDestroy.getConstructor(int[].class);
        } catch (NoSuchMethodException e) {
            //1.17.0
            newPacketPlayOutEntityDestroy = PacketPlayOutEntityDestroy.getConstructor(int.class);
        }
        if (minorVersion >= 7) {
            Class<?> EnumEntityUseAction = getNMSClass("net.minecraft.network.protocol.game.PacketPlayInUseEntity$EnumEntityUseAction", "PacketPlayInUseEntity$EnumEntityUseAction", "EnumEntityUseAction", "net.minecraft.class_2824$class_5906");
            PacketPlayInUseEntity_ENTITY = getFields(PacketPlayInUseEntity, int.class).get(0);
            PacketPlayInUseEntity_ACTION = getFields(PacketPlayInUseEntity, EnumEntityUseAction).get(0);
        }
        if (minorVersion >= 9) {
            PacketPlayOutSpawnEntityLiving_UUID = getFields(PacketPlayOutSpawnEntityLiving, UUID.class).get(0);
            PacketPlayOutEntityTeleport_X = getFields(PacketPlayOutEntityTeleport, double.class).get(0);
            PacketPlayOutEntityTeleport_Y = getFields(PacketPlayOutEntityTeleport, double.class).get(1);
            PacketPlayOutEntityTeleport_Z = getFields(PacketPlayOutEntityTeleport, double.class).get(2);
            if (minorVersion >= 19) {
                PacketPlayOutSpawnEntityLiving_X = getFields(PacketPlayOutSpawnEntityLiving, double.class).get(2);
                PacketPlayOutSpawnEntityLiving_Y = getFields(PacketPlayOutSpawnEntityLiving, double.class).get(3);
                PacketPlayOutSpawnEntityLiving_Z = getFields(PacketPlayOutSpawnEntityLiving, double.class).get(4);
                Registry_a = Class.forName("net.minecraft.core.Registry").getMethod("a", int.class);
                IRegistry_X = Class.forName("net.minecraft.core.IRegistry").getDeclaredField("X").get(null);
            } else {
                PacketPlayOutSpawnEntityLiving_X = getFields(PacketPlayOutSpawnEntityLiving, double.class).get(0);
                PacketPlayOutSpawnEntityLiving_Y = getFields(PacketPlayOutSpawnEntityLiving, double.class).get(1);
                PacketPlayOutSpawnEntityLiving_Z = getFields(PacketPlayOutSpawnEntityLiving, double.class).get(2);
            }
        } else {
            PacketPlayOutSpawnEntityLiving_X = getFields(PacketPlayOutSpawnEntityLiving, int.class).get(2);
            PacketPlayOutSpawnEntityLiving_Y = getFields(PacketPlayOutSpawnEntityLiving, int.class).get(3);
            PacketPlayOutSpawnEntityLiving_Z = getFields(PacketPlayOutSpawnEntityLiving, int.class).get(4);
            PacketPlayOutEntityTeleport_X = getFields(PacketPlayOutEntityTeleport, int.class).get(1);
            PacketPlayOutEntityTeleport_Y = getFields(PacketPlayOutEntityTeleport, int.class).get(2);
            PacketPlayOutEntityTeleport_Z = getFields(PacketPlayOutEntityTeleport, int.class).get(3);
        }
        if (minorVersion >= 19) {
            PacketPlayOutSpawnEntityLiving_ENTITYTYPE = getField(PacketPlayOutSpawnEntityLiving, "e");
        } else {
            PacketPlayOutSpawnEntityLiving_ENTITYTYPE = getFields(PacketPlayOutSpawnEntityLiving, int.class).get(1);
        }
        if (minorVersion <= 14) {
            PacketPlayOutSpawnEntityLiving_DATAWATCHER = getFields(PacketPlayOutSpawnEntityLiving, DataWatcher).get(0);
        }
    }

    /**
     * Returns class with given potential names in same order
     * @param names - possible class names
     * @return class for specified name(s)
     * @throws ClassNotFoundException if class does not exist
     */
    private Class<?> getNMSClass(String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                return minorVersion >= 17 ? Class.forName(name) : getLegacyClass(name);
            } catch (ClassNotFoundException e) {
                //not the first class name in array
            }
        }
        throw new ClassNotFoundException("No class found with possible names " + Arrays.toString(names));
    }

    /**
     * Returns class from given name
     * @param name - class name
     * @return class from given name
     * @throws ClassNotFoundException if class was not found
     */
    private Class<?> getLegacyClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.server." + serverPackage + "." + name);
        } catch (ClassNotFoundException | NullPointerException e) {
            try {
                //modded server?
                Class<?> clazz = BukkitBridge.class.getClassLoader().loadClass("net.minecraft.server." + serverPackage + "." + name);
                if (clazz != null) return clazz;
                throw new ClassNotFoundException(name);
            } catch (ClassNotFoundException | NullPointerException e1) {
                //maybe fabric?
                return Class.forName(name);
            }
        }
    }

    /**
     * Returns method with specified possible names and parameters. Throws exception if no such method was found
     * @param clazz - class to get method from
     * @param names - possible method names
     * @param parameterTypes - parameter types of the method
     * @return method with specified name and parameters
     * @throws NoSuchMethodException if no such method exists
     */
    private Method getMethod(Class<?> clazz, String[] names, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (String name : names) {
            try {
                return getMethod(clazz, name, parameterTypes);
            } catch (NoSuchMethodException e) {
                //not the first method in array
            }
        }
        List<String> list = new ArrayList<>();
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() != parameterTypes.length) continue;
            Class<?>[] types = m.getParameterTypes();
            boolean valid = true;
            for (int i=0; i<types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    valid = false;
                    break;
                }
            }
            if (valid) list.add(m.getName());
        }
        throw new NoSuchMethodException("No method found with possible names " + Arrays.toString(names) + " with parameters " +
                Arrays.toString(parameterTypes) + " in class " + clazz.getName() + ". Methods with matching parameters: " + list);
    }

    private Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        List<Method> list = new ArrayList<>();
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != parameterTypes.length) continue;
            Class<?>[] types = m.getParameterTypes();
            boolean valid = true;
            for (int i=0; i<types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    valid = false;
                    break;
                }
            }
            if (valid) list.add(m);
        }
        if (!list.isEmpty()) return list.get(0);
        throw new NoSuchMethodException("No method found with name " + name + " in class " + clazz.getName() + " with parameters " + Arrays.toString(parameterTypes));
    }

    private List<Method> getMethods(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes){
        List<Method> list = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getReturnType() != returnType || m.getParameterCount() != parameterTypes.length || !Modifier.isPublic(m.getModifiers())) continue;
            Class<?>[] types = m.getParameterTypes();
            boolean valid = true;
            for (int i=0; i<types.length; i++) {
                if (types[i] != parameterTypes[i]) {
                    valid = false;
                    break;
                }
            }
            if (valid) list.add(m);
        }
        return list;
    }

    /**
     * Returns all fields of class with defined class type
     * @param clazz - class to check fields of
     * @param type - field type to check for
     * @return list of all fields with specified class type
     */
    private List<Field> getFields(Class<?> clazz, Class<?> type){
        if (clazz == null) throw new IllegalArgumentException("Source class cannot be null");
        List<Field> list = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type) {
                list.add(setAccessible(field));
            }
        }
        return list;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setField(Object obj, Field field, Object value) throws IllegalAccessException {
        field.set(obj, value);
    }

    public DataWatcherRegistry getDataWatcherRegistry() {
        return registry;
    }

    public <T extends AccessibleObject> T setAccessible(T o) {
        o.setAccessible(true);
        return o;
    }

    private Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(name) || (f.getName().split("_").length == 3 && f.getName().split("_")[2].equals(name))) {
                return setAccessible(f);
            }
        }
        throw new NoSuchFieldException("Field \"" + name + "\" was not found in class " + clazz.getName());
    }
}