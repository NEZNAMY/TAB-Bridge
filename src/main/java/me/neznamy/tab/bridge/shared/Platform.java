package me.neznamy.tab.bridge.shared;

import com.google.common.io.ByteArrayDataInput;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.UUID;

public interface Platform {

    boolean isInvisible(BridgePlayer player);

    boolean isVanished(BridgePlayer player);

    boolean isDisguised(BridgePlayer player);

    String getGroup(BridgePlayer player);

    boolean isOnline(Object player);

    UUID getUniqueId(Object player);

    Channel getChannel(Object player);

    void runTaskTimerAsynchronously(Runnable task, int intervalTicks);

    void scheduleSyncRepeatingTask(Runnable task, int intervalTicks);

    void runTask(Runnable task);

    void readUnlimitedNametagJoin(BridgePlayer player, ByteArrayDataInput input);

    void readUnlimitedNametagMessage(BridgePlayer player, ByteArrayDataInput input);

    void sendScoreboardDisplayObjective(BridgePlayer player, int slot, String objective);

    void sendScoreboardObjective(BridgePlayer player, String objective, int action, String displayName,
                                 String displayComponent, int renderType);

    void sendScoreboardScore(BridgePlayer player, String objective, int action, String playerName, int score);

    void sendScoreboardTeam(BridgePlayer player, String name, int action, Collection<String> players, String prefix, String prefixComponent,
                                 String suffix, String suffixComponent, int options, String visibility,
                                 String collision, int color);

    void setExpansionValue(Object player, String identifier, String value);

    boolean hasPermission(Object player, String permission);

    void registerExpansion();

    String getWorld(Object player);

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
        }
    }

    default void uninject(Object player) {
        Channel channel = getChannel(player);
        if (channel == null) return;
        try {
            if (channel.pipeline().names().contains("TAB-Bridge"))
                channel.pipeline().remove("TAB-Bridge");
        } catch (NoSuchElementException ignored) {
        }
    }
}
