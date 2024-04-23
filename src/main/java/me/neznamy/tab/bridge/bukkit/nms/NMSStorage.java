package me.neznamy.tab.bridge.bukkit.nms;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.bukkit.BukkitScoreboard;
import me.neznamy.tab.bridge.shared.util.FunctionWithException;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class NMSStorage {

    //instance of this class
    @Nullable @Getter @Setter private static NMSStorage instance;

    private final Class<?> Packet = getNMSClass("net.minecraft.network.protocol.Packet", "Packet");
    private final Class<?> EntityPlayer = getNMSClass("net.minecraft.server.level.EntityPlayer", "EntityPlayer");
    private final Class<?> NetworkManager = getNMSClass("net.minecraft.network.NetworkManager", "NetworkManager");
    private final Class<?> PlayerConnection = getNMSClass("net.minecraft.server.network.PlayerConnection", "PlayerConnection");
    public final Field PLAYER_CONNECTION = ReflectionUtils.getFields(EntityPlayer, PlayerConnection).get(0);
    public final Field NETWORK_MANAGER = ReflectionUtils.getFields(BukkitBridge.is1_20_2Plus() ? PlayerConnection.getSuperclass() : PlayerConnection, NetworkManager).get(0);
    public final Field CHANNEL = ReflectionUtils.getFields(NetworkManager, Channel.class).get(0);
    public final Method getHandle = Class.forName("org.bukkit.craftbukkit." + BukkitBridge.getServerPackage() + ".entity.CraftPlayer").getMethod("getHandle");
    public final Method sendPacket = ReflectionUtils.getMethods(PlayerConnection, void.class, Packet).get(0);
    public final FunctionWithException<String, Object> deserialize = getDeserializeFunction();

    /**
     * Creates new instance, initializes required NMS classes and fields
     * @throws    ReflectiveOperationException
     *             If any class, field or method fails to load
     */
    public NMSStorage() throws ReflectiveOperationException {
        DataWatcher.load(this);
        PacketEntityView.load();
        if (!BukkitScoreboard.isAvailable())
            Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB-Bridge] Failed to initialize Scoreboard fields due to " +
                    "a compatibility issue, plugin functionality will be limited.");
    }

    private FunctionWithException<String, Object> getDeserializeFunction() throws ReflectiveOperationException {
        Class<?> ChatSerializer = BukkitReflection.getClass("network.chat.Component$Serializer",
                "network.chat.IChatBaseComponent$ChatSerializer", "IChatBaseComponent$ChatSerializer", "ChatSerializer");
        try {
            // 1.20.5+
            Class<?> HolderLookup$Provider = BukkitReflection.getClass("core.HolderLookup$Provider", "core.HolderLookup$a");
            Method fromJson = first(ReflectionUtils.getMethods(ChatSerializer, Object.class, String.class, HolderLookup$Provider));
            Object emptyProvider = ReflectionUtils.getOnlyMethod(HolderLookup$Provider, HolderLookup$Provider, Stream.class).invoke(null, Stream.empty());
            return string -> fromJson.invoke(null, string, emptyProvider);
        } catch (ReflectiveOperationException e) {
            // 1.20.4-
            Method fromJson = first(ReflectionUtils.getMethods(ChatSerializer, Object.class, String.class));
            return string -> fromJson.invoke(null, string);
        }
    }

    @NotNull
    private Method first(@NotNull List<Method> methods) throws NoSuchMethodException {
        if (methods.isEmpty()) throw new NoSuchMethodException("Json deserialize method not found");
        return methods.get(0);
    }

    /**
     * Returns class with given potential names in same order
     * @param names - possible class names
     * @return class for specified name(s)
     * @throws ClassNotFoundException if class does not exist
     */
    public Class<?> getNMSClass(String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                return BukkitBridge.getMinorVersion() >= 17 ? Class.forName(name) : getLegacyClass(name);
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
    public Class<?> getLegacyClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.server." + BukkitBridge.getServerPackage() + "." + name);
        } catch (ClassNotFoundException | NullPointerException e) {
            try {
                //modded server?
                Class<?> clazz = BukkitBridge.class.getClassLoader().loadClass("net.minecraft.server." + BukkitBridge.getServerPackage() + "." + name);
                if (clazz != null) return clazz;
                throw new ClassNotFoundException(name);
            } catch (ClassNotFoundException | NullPointerException e1) {
                //maybe fabric?
                return Class.forName(name);
            }
        }
    }
}