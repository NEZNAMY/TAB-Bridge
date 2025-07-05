package me.neznamy.tab.bridge.bukkit.paper;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import me.neznamy.tab.bridge.bukkit.BukkitPluginMessageListener;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.jetbrains.annotations.NotNull;

/**
 * Override of {@link BukkitPluginMessageListener} for Paper 1.21.7+. It no longer sends plugin messages
 */
@SuppressWarnings("unused") // Used via reflection
public class PaperPluginMessageListener extends BukkitPluginMessageListener {

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onPluginMessageReceived(@NotNull String channel, @NotNull PlayerConnection connection, byte @NotNull [] message) {
        if (!channel.equals(TABBridge.CHANNEL_NAME)) return;
        if (connection instanceof PlayerConfigurationConnection) {
            TABBridge.getInstance().submitTask(
                    () -> TABBridge.getInstance().getDataBridge().processPluginMessage(((PlayerConfigurationConnection) connection).getProfile().getId(), message, false));
        }
    }
}
