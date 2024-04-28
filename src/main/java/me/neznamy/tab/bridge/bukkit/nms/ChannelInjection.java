package me.neznamy.tab.bridge.bukkit.nms;

import io.netty.channel.Channel;
import lombok.SneakyThrows;
import me.neznamy.tab.bridge.shared.util.FunctionWithException;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Pipeline injection for Bukkit 1.8+.
 */
public class ChannelInjection {

    /** Function for getting player's channel */
    private final FunctionWithException<Player, Channel> getChannel;

    /**
     * Constructs new instance and attempts to load required classes, fields and methods and marks class as available.
     * If something fails, throws exception.
     *
     * @throws  ReflectiveOperationException
     *          If something fails
     */
    public ChannelInjection() throws ReflectiveOperationException {
        Class<?> NetworkManager = BukkitReflection.getClass("network.Connection", "network.NetworkManager", "NetworkManager");
        Class<?> PlayerConnection = BukkitReflection.getClass("server.network.ServerGamePacketListenerImpl",
                "server.network.PlayerConnection", "PlayerConnection");
        Class<?> EntityPlayer = BukkitReflection.getClass("server.level.ServerPlayer", "server.level.EntityPlayer", "EntityPlayer");
        Method getHandle = BukkitReflection.getBukkitClass("entity.CraftPlayer").getMethod("getHandle");
        Field PLAYER_CONNECTION = ReflectionUtils.getOnlyField(EntityPlayer, PlayerConnection);
        Field NETWORK_MANAGER;
        if (BukkitReflection.is1_20_2Plus()) {
            NETWORK_MANAGER = ReflectionUtils.getOnlyField(PlayerConnection.getSuperclass(), NetworkManager);
        } else {
            NETWORK_MANAGER = ReflectionUtils.getOnlyField(PlayerConnection, NetworkManager);
        }
        Field CHANNEL = ReflectionUtils.getOnlyField(NetworkManager, Channel.class);
        getChannel = player -> (Channel) CHANNEL.get(NETWORK_MANAGER.get(PLAYER_CONNECTION.get(getHandle.invoke(player))));
    }

    @SneakyThrows
    public Channel getChannel(@NotNull Player player) {
        return getChannel.apply(player);
    }
}