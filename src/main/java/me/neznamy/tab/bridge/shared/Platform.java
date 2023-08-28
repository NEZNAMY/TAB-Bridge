package me.neznamy.tab.bridge.shared;

import com.google.common.io.ByteArrayDataInput;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.UUID;

public interface Platform {

    boolean isOnline(Object player);

    UUID getUniqueId(Object player);

    Channel getChannel(Object player);

    void scheduleSyncRepeatingTask(Runnable task, int intervalTicks);

    void runTask(Runnable task);

    void readUnlimitedNametagJoin(BridgePlayer player, ByteArrayDataInput input);

    void readUnlimitedNametagMessage(BridgePlayer player, ByteArrayDataInput input);

    void registerExpansion(@NotNull TabExpansion expansion);

    BridgePlayer newPlayer(Object player, int protocolVersion);

    Placeholder createPlaceholder(String publicIdentifier, String privateIdentifier, int refresh);

    default void inject(Object player, ChannelDuplexHandler handler) {
        Channel channel = getChannel(player);
        if (channel == null) return;
        if (!channel.pipeline().names().contains("packet_handler")) {
            //fake player or waterfall bug
            return;
        }
        uninject(player);
        try {
            channel.pipeline().addBefore("packet_handler", "TAB-Bridge", handler);
        } catch (NoSuchElementException | IllegalArgumentException ignored) {
            // some sort of netty bug
        }
    }

    default void uninject(Object player) {
        Channel channel = getChannel(player);
        if (channel == null) return;
        try {
            if (channel.pipeline().names().contains("TAB-Bridge"))
                channel.pipeline().remove("TAB-Bridge");
        } catch (NoSuchElementException ignored) {
            // some sort of netty bug
        }
    }

    void cancelTasks();

    void sendConsoleMessage(String message);
}
