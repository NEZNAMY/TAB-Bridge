package me.neznamy.tab.bridge.bukkit.nms;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NMSStorage {

    //instance of this class
    @Nullable @Getter @Setter private static NMSStorage instance;

    //server package, such as "v1_16_R3"
    private final String serverPackage = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    //server minor version such as "16"
    @Getter private final int minorVersion = Integer.parseInt(serverPackage.split("_")[1]);

    @Getter private final boolean is1_19_3Plus = ReflectionUtils.classExists("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
    @Getter private final boolean is1_19_4Plus = is1_19_3Plus && !serverPackage.equals("v1_19_R2");

    //base
    private final Class<?> Packet = getNMSClass("net.minecraft.network.protocol.Packet", "Packet");
    public final Class<?> EnumChatFormat = getNMSClass("net.minecraft.EnumChatFormat", "EnumChatFormat");
    private final Class<?> EntityPlayer = getNMSClass("net.minecraft.server.level.EntityPlayer", "EntityPlayer");
    private final Class<?> Entity = getNMSClass("net.minecraft.world.entity.Entity", "Entity");
    private final Class<?> EntityLiving = getNMSClass("net.minecraft.world.entity.EntityLiving", "EntityLiving");
    private final Class<?> NetworkManager = getNMSClass("net.minecraft.network.NetworkManager", "NetworkManager");
    private final Class<?> PlayerConnection = getNMSClass("net.minecraft.server.network.PlayerConnection", "PlayerConnection");
    public final Constructor<?> newEntityArmorStand = getNMSClass("net.minecraft.world.entity.decoration.EntityArmorStand", "EntityArmorStand")
            .getConstructor(getNMSClass("net.minecraft.world.level.World", "World"), double.class, double.class, double.class);
    public final Field PLAYER_CONNECTION = ReflectionUtils.getFields(EntityPlayer, PlayerConnection).get(0);
    public final Field NETWORK_MANAGER = ReflectionUtils.getFields(PlayerConnection, NetworkManager).get(0);
    public final Field CHANNEL = ReflectionUtils.getFields(NetworkManager, Channel.class).get(0);
    public final Method getHandle = Class.forName("org.bukkit.craftbukkit." + serverPackage + ".entity.CraftPlayer").getMethod("getHandle");
    public final Method sendPacket = ReflectionUtils.getMethods(PlayerConnection, void.class, Packet).get(0);
    public final Method World_getHandle = Class.forName("org.bukkit.craftbukkit." + serverPackage + ".CraftWorld").getMethod("getHandle");
    public final Enum[] EnumChatFormat_values = getEnumValues(EnumChatFormat);

    //chat
    public final Class<?> IChatBaseComponent = getNMSClass("net.minecraft.network.chat.IChatBaseComponent", "IChatBaseComponent");
    public final Method DESERIALIZE = getNMSClass("net.minecraft.network.chat.IChatBaseComponent$ChatSerializer", "IChatBaseComponent$ChatSerializer", "ChatSerializer").getMethod("a", String.class);

    //DataWatcher
    private final Class<?> DataWatcher = getNMSClass("net.minecraft.network.syncher.DataWatcher", "DataWatcher");
    public Class<?> DataWatcherRegistry;
    public final Constructor<?> newDataWatcher = DataWatcher.getConstructors()[0];
    public Constructor<?> newDataWatcherObject;
    public Method DataWatcher_REGISTER;
    public Method DataWatcher_b;
    @Getter private DataWatcherRegistry registry;
    //1.19.3+
    public Method DataWatcher_markDirty;

    //PacketPlayOutSpawnEntityLiving
    public final Class<?> PacketPlayOutSpawnEntityLiving = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving",
            "net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity", "PacketPlayOutSpawnEntityLiving", "Packet24MobSpawn");
    public Constructor<?> newPacketPlayOutSpawnEntityLiving;
    public final Field PacketPlayOutSpawnEntityLiving_ENTITYID = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, int.class).get(0);
    public Field PacketPlayOutSpawnEntityLiving_ENTITYTYPE;
    public final Field PacketPlayOutSpawnEntityLiving_YAW = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, byte.class).get(0);
    public final Field PacketPlayOutSpawnEntityLiving_PITCH = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, byte.class).get(0);
    public Field PacketPlayOutSpawnEntityLiving_UUID;
    public Field PacketPlayOutSpawnEntityLiving_X;
    public Field PacketPlayOutSpawnEntityLiving_Y;
    public Field PacketPlayOutSpawnEntityLiving_Z;
    public Field PacketPlayOutSpawnEntityLiving_DATAWATCHER;

    //PacketPlayOutEntityTeleport
    public final Class<?> PacketPlayOutEntityTeleport = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport", "PacketPlayOutEntityTeleport", "Packet34EntityTeleport");
    public Constructor<?> newPacketPlayOutEntityTeleport;
    public final Field PacketPlayOutEntityTeleport_ENTITYID = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, int.class).get(0);
    public Field PacketPlayOutEntityTeleport_X;
    public Field PacketPlayOutEntityTeleport_Y;
    public Field PacketPlayOutEntityTeleport_Z;
    public final Field PacketPlayOutEntityTeleport_YAW = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, byte.class).get(0);
    public final Field PacketPlayOutEntityTeleport_PITCH = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, byte.class).get(1);

    //other entity packets
    public Object EntityTypes_ARMOR_STAND;

    public final Class<?> PacketPlayOutEntity = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntity", "PacketPlayOutEntity", "Packet30Entity");
    public final Field PacketPlayOutEntity_ENTITYID = ReflectionUtils.getFields(PacketPlayOutEntity, int.class).get(0);

    public final Class<?> PacketPlayOutEntityDestroy = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy", "PacketPlayOutEntityDestroy", "Packet29DestroyEntity");
    public Constructor<?> newPacketPlayOutEntityDestroy;
    public final Field PacketPlayOutEntityDestroy_ENTITIES = ReflectionUtils.setAccessible(PacketPlayOutEntityDestroy.getDeclaredFields()[0]);

    public final Class<?> PacketPlayOutEntityLook = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntity$PacketPlayOutEntityLook", "PacketPlayOutEntity$PacketPlayOutEntityLook", "PacketPlayOutEntityLook", "Packet32EntityLook");

    public final Class<?> PacketPlayOutEntityMetadata = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata", "PacketPlayOutEntityMetadata", "Packet40EntityMetadata");
    public Constructor<?> newPacketPlayOutEntityMetadata;

    public final Class<?> PacketPlayOutNamedEntitySpawn = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn", "PacketPlayOutNamedEntitySpawn", "Packet20NamedEntitySpawn");
    public final Field PacketPlayOutNamedEntitySpawn_ENTITYID = ReflectionUtils.getFields(PacketPlayOutNamedEntitySpawn, int.class).get(0);

    //scoreboard objectives
    private final Class<?> PacketPlayOutScoreboardDisplayObjective = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective", "PacketPlayOutScoreboardDisplayObjective", "Packet208SetScoreboardDisplayObjective");
    private final Class<?> PacketPlayOutScoreboardObjective = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective", "PacketPlayOutScoreboardObjective", "Packet206SetScoreboardObjective");
    private final Class<?> Scoreboard = getNMSClass("net.minecraft.world.scores.Scoreboard", "Scoreboard");
    private final Class<?> PacketPlayOutScoreboardScore = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore", "PacketPlayOutScoreboardScore", "Packet207SetScoreboardScore");
    private final Class<?> ScoreboardObjective = getNMSClass("net.minecraft.world.scores.ScoreboardObjective", "ScoreboardObjective");
    private final Class<?> ScoreboardScore = getNMSClass("net.minecraft.world.scores.ScoreboardScore", "ScoreboardScore");
    private final Class<?> IScoreboardCriteria = getNMSClass("net.minecraft.world.scores.criteria.IScoreboardCriteria", "IScoreboardCriteria");
    public final Class<Enum> EnumScoreboardHealthDisplay = (Class<Enum>) getNMSClass("net.minecraft.world.scores.criteria.IScoreboardCriteria$EnumScoreboardHealthDisplay", "IScoreboardCriteria$EnumScoreboardHealthDisplay", "EnumScoreboardHealthDisplay");
    public final Class<Enum> EnumScoreboardAction = (Class<Enum>) getNMSClass("net.minecraft.server.ScoreboardServer$Action", "ScoreboardServer$Action", "PacketPlayOutScoreboardScore$EnumScoreboardAction", "EnumScoreboardAction");
    public final Class<Enum> EnumNameTagVisibility = (Class<Enum>) getNMSClass("net.minecraft.world.scores.ScoreboardTeamBase$EnumNameTagVisibility", "ScoreboardTeamBase$EnumNameTagVisibility", "EnumNameTagVisibility");
    public final Constructor<?> newScoreboardObjective = ScoreboardObjective.getConstructors()[0];
    public final Constructor<?> newScoreboardScore = ScoreboardScore.getConstructor(Scoreboard, ScoreboardObjective, String.class);
    public final Constructor<?> newPacketPlayOutScoreboardDisplayObjective = PacketPlayOutScoreboardDisplayObjective.getConstructor(int.class, ScoreboardObjective);
    public Constructor<?> newPacketPlayOutScoreboardObjective;
    public Constructor<?> newPacketPlayOutScoreboardScore_1_13;
    public Constructor<?> newPacketPlayOutScoreboardScore_String;
    public Constructor<?> newPacketPlayOutScoreboardScore;
    public final Field PacketPlayOutScoreboardObjective_OBJECTIVENAME = ReflectionUtils.getFields(PacketPlayOutScoreboardObjective, String.class).get(0);
    public Field PacketPlayOutScoreboardObjective_METHOD;
    public final Field IScoreboardCriteria_self = ReflectionUtils.getFields(IScoreboardCriteria, IScoreboardCriteria).get(0);
    public final Field PacketPlayOutScoreboardObjective_RENDERTYPE = ReflectionUtils.getFields(PacketPlayOutScoreboardObjective, EnumScoreboardHealthDisplay).get(0);
    public Field PacketPlayOutScoreboardObjective_DISPLAYNAME;
    public final Method ScoreboardScore_setScore = ReflectionUtils.getMethod(ScoreboardScore, new String[]{"setScore", "b"}, int.class);

    //PacketPlayOutScoreboardTeam
    public final Class<?> PacketPlayOutScoreboardTeam = getNMSClass("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam", "PacketPlayOutScoreboardTeam");
    private final Class<?> ScoreboardTeam = getNMSClass("net.minecraft.world.scores.ScoreboardTeam", "ScoreboardTeam");
    public Class<Enum> EnumTeamPush;
    public final Constructor<?> newScoreboardTeam = ScoreboardTeam.getConstructor(Scoreboard, String.class);
    public Constructor<?> newPacketPlayOutScoreboardTeam;
    public final Method ScoreboardTeam_getPlayerNameSet = ReflectionUtils.getMethods(ScoreboardTeam, Collection.class).get(0);
    public final Method ScoreboardTeam_setNameTagVisibility = ReflectionUtils.getMethod(ScoreboardTeam, new String[]{"setNameTagVisibility", "a"}, EnumNameTagVisibility);
    public Method ScoreboardTeam_setCollisionRule;
    public Method ScoreboardTeam_setPrefix;
    public Method ScoreboardTeam_setSuffix;
    public Method ScoreboardTeam_setColor;
    public final Method ScoreboardTeam_setAllowFriendlyFire = ReflectionUtils.getMethod(ScoreboardTeam, new String[]{"setAllowFriendlyFire", "a"}, boolean.class);
    public final Method ScoreboardTeam_setCanSeeFriendlyInvisibles = ReflectionUtils.getMethod(ScoreboardTeam, new String[]{"setCanSeeFriendlyInvisibles", "b"}, boolean.class);
    public Method PacketPlayOutScoreboardTeam_of;
    public Method PacketPlayOutScoreboardTeam_ofBoolean;
    public Field PacketPlayOutScoreboardTeam_NAME = ReflectionUtils.getFields(PacketPlayOutScoreboardTeam, String.class).get(0);
    public Field PacketPlayOutScoreboardTeam_ACTION = ReflectionUtils.getInstanceFields(PacketPlayOutScoreboardTeam, int.class).get(0);
    public Field PacketPlayOutScoreboardTeam_PLAYERS = ReflectionUtils.getFields(PacketPlayOutScoreboardTeam, Collection.class).get(0);

    public final Object dummyEntity = newEntityArmorStand.newInstance(World_getHandle.invoke(Bukkit.getWorlds().get(0)), 0, 0, 0);
    public final Object emptyScoreboard = Scoreboard.getConstructor().newInstance();

    /**
     * Creates new instance, initializes required NMS classes and fields
     * @throws    ReflectiveOperationException
     *             If any class, field or method fails to load
     */
    public NMSStorage() throws ReflectiveOperationException {
        initializeDataWatcher();
        initializeEntityPackets();
        initializeScoreboardPackets();
        initializeTeamPackets();
    }

    private void initializeDataWatcher() throws ReflectiveOperationException {
        if (minorVersion >= 9) {
            Class<?> DataWatcherObject = getNMSClass("net.minecraft.network.syncher.DataWatcherObject", "DataWatcherObject");
            DataWatcherRegistry = getNMSClass("net.minecraft.network.syncher.DataWatcherRegistry", "DataWatcherRegistry");
            Class<?> DataWatcherSerializer = getNMSClass("net.minecraft.network.syncher.DataWatcherSerializer", "DataWatcherSerializer");
            newDataWatcherObject = DataWatcherObject.getConstructor(int.class, DataWatcherSerializer);
            DataWatcher_REGISTER = ReflectionUtils.getMethod(DataWatcher, new String[]{"register", "a"}, DataWatcherObject, Object.class);
            if (is1_19_3Plus()) {
                DataWatcher_b = DataWatcher.getMethod("b");
                DataWatcher_markDirty = ReflectionUtils.getMethods(DataWatcher, void.class, DataWatcherObject).get(0);
            }
        } else {
            DataWatcher_REGISTER = ReflectionUtils.getMethod(DataWatcher, new String[]{"a"}, int.class, Object.class);
        }
        registry = new DataWatcherRegistry(this);
    }

    private void initializeEntityPackets() throws ReflectiveOperationException {
        if (is1_19_3Plus()) {
            newPacketPlayOutEntityMetadata = PacketPlayOutEntityMetadata.getConstructor(int.class, List.class);
        } else {
            newPacketPlayOutEntityMetadata = PacketPlayOutEntityMetadata.getConstructor(int.class, DataWatcher, boolean.class);
        }
        if (minorVersion >= 17) {
            if (is1_19_3Plus()) {
                newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor(Entity);
            } else {
                newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor(EntityLiving);
            }
            newPacketPlayOutEntityTeleport = PacketPlayOutEntityTeleport.getConstructor(Entity);
        } else {
            newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor();
            newPacketPlayOutEntityTeleport = PacketPlayOutEntityTeleport.getConstructor();
        }
        try {
            newPacketPlayOutEntityDestroy = PacketPlayOutEntityDestroy.getConstructor(int[].class);
        } catch (NoSuchMethodException e) {
            //1.17.0
            newPacketPlayOutEntityDestroy = PacketPlayOutEntityDestroy.getConstructor(int.class);
        }
        if (minorVersion >= 9) {
            PacketPlayOutSpawnEntityLiving_UUID = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, UUID.class).get(0);
            PacketPlayOutEntityTeleport_X = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, double.class).get(0);
            PacketPlayOutEntityTeleport_Y = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, double.class).get(1);
            PacketPlayOutEntityTeleport_Z = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, double.class).get(2);
            if (minorVersion >= 19) {
                PacketPlayOutSpawnEntityLiving_X = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, double.class).get(2);
                PacketPlayOutSpawnEntityLiving_Y = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, double.class).get(3);
                PacketPlayOutSpawnEntityLiving_Z = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, double.class).get(4);
            } else {
                PacketPlayOutSpawnEntityLiving_X = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, double.class).get(0);
                PacketPlayOutSpawnEntityLiving_Y = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, double.class).get(1);
                PacketPlayOutSpawnEntityLiving_Z = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, double.class).get(2);
            }
        } else {
            PacketPlayOutSpawnEntityLiving_X = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, int.class).get(2);
            PacketPlayOutSpawnEntityLiving_Y = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, int.class).get(3);
            PacketPlayOutSpawnEntityLiving_Z = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, int.class).get(4);
            PacketPlayOutEntityTeleport_X = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, int.class).get(1);
            PacketPlayOutEntityTeleport_Y = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, int.class).get(2);
            PacketPlayOutEntityTeleport_Z = ReflectionUtils.getFields(PacketPlayOutEntityTeleport, int.class).get(3);
        }
        if (minorVersion >= 19) {
            (PacketPlayOutSpawnEntityLiving_ENTITYTYPE = PacketPlayOutSpawnEntityLiving.getDeclaredField("e")).setAccessible(true);
            EntityTypes_ARMOR_STAND = Class.forName("net.minecraft.world.entity.EntityTypes").getDeclaredField("d").get(null);
        } else {
            PacketPlayOutSpawnEntityLiving_ENTITYTYPE = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, int.class).get(1);
        }
        if (minorVersion <= 14) {
            PacketPlayOutSpawnEntityLiving_DATAWATCHER = ReflectionUtils.getFields(PacketPlayOutSpawnEntityLiving, DataWatcher).get(0);
        }
    }

    private void initializeScoreboardPackets() throws ReflectiveOperationException {
        List<Field> list = ReflectionUtils.getFields(PacketPlayOutScoreboardObjective, int.class);
        PacketPlayOutScoreboardObjective_METHOD = list.get(list.size()-1);
        if (minorVersion >= 13) {
            newPacketPlayOutScoreboardObjective = PacketPlayOutScoreboardObjective.getConstructor(ScoreboardObjective, int.class);
            newPacketPlayOutScoreboardScore_1_13 = PacketPlayOutScoreboardScore.getConstructor(EnumScoreboardAction, String.class, String.class, int.class);
            PacketPlayOutScoreboardObjective_DISPLAYNAME = ReflectionUtils.getFields(PacketPlayOutScoreboardObjective, IChatBaseComponent).get(0);
        } else {
            newPacketPlayOutScoreboardObjective = PacketPlayOutScoreboardObjective.getConstructor();
            newPacketPlayOutScoreboardScore_String = PacketPlayOutScoreboardScore.getConstructor(String.class);
            PacketPlayOutScoreboardObjective_DISPLAYNAME = ReflectionUtils.getFields(PacketPlayOutScoreboardObjective, String.class).get(1);
            newPacketPlayOutScoreboardScore = PacketPlayOutScoreboardScore.getConstructor(ScoreboardScore);
        }
    }

    private void initializeTeamPackets() throws ReflectiveOperationException {
        if (minorVersion >= 9) {
            EnumTeamPush = (Class<Enum>) getNMSClass("net.minecraft.world.scores.ScoreboardTeamBase$EnumTeamPush", "ScoreboardTeamBase$EnumTeamPush");
            ScoreboardTeam_setCollisionRule = ReflectionUtils.getMethods(ScoreboardTeam, void.class, EnumTeamPush).get(0);
        }
        if (minorVersion >= 13) {
            ScoreboardTeam_setPrefix = ReflectionUtils.getMethod(ScoreboardTeam, new String[]{"setPrefix", "b"}, IChatBaseComponent);
            ScoreboardTeam_setSuffix = ReflectionUtils.getMethod(ScoreboardTeam, new String[]{"setSuffix", "c"}, IChatBaseComponent);
            ScoreboardTeam_setColor = ReflectionUtils.getMethods(ScoreboardTeam, void.class, EnumChatFormat).get(0);
        } else {
            ScoreboardTeam_setPrefix = ReflectionUtils.getMethod(ScoreboardTeam, new String[]{"setPrefix"}, String.class);
            ScoreboardTeam_setSuffix = ReflectionUtils.getMethod(ScoreboardTeam, new String[]{"setSuffix"}, String.class);
        }
        if (minorVersion >= 17) {
            PacketPlayOutScoreboardTeam_of = ReflectionUtils.getMethods(PacketPlayOutScoreboardTeam, PacketPlayOutScoreboardTeam, ScoreboardTeam).get(0);
            PacketPlayOutScoreboardTeam_ofBoolean = ReflectionUtils.getMethods(PacketPlayOutScoreboardTeam, PacketPlayOutScoreboardTeam, ScoreboardTeam, boolean.class).get(0);
        } else {
            newPacketPlayOutScoreboardTeam = PacketPlayOutScoreboardTeam.getConstructor(ScoreboardTeam, int.class);
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

    public void setField(Object obj, Field field, Object value) throws IllegalAccessException {
        field.set(obj, value);
    }

    private Enum[] getEnumValues(Class<?> enumClass) {
        if (enumClass == null) throw new IllegalArgumentException("Class cannot be null");
        if (!enumClass.isEnum()) throw new IllegalArgumentException(enumClass.getName() + " is not an enum class");
        try {
            return (Enum[]) enumClass.getMethod("values").invoke(null);
        } catch (ReflectiveOperationException e) {
            //this should never happen
            return new Enum[0];
        }
    }
}