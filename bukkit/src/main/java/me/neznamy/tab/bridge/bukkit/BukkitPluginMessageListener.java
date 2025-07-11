package me.neznamy.tab.bridge.bukkit;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Listener for plugin messages sent to this plugin.
 * It processes messages received on the TAB-Bridge channel.
 */
public class BukkitPluginMessageListener implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(@NonNull String channel, @NonNull Player player, byte[] bytes) {
        if (!channel.equals(TABBridge.CHANNEL_NAME)) return;
        TABBridge.getInstance().submitTask(
                () -> TABBridge.getInstance().getDataBridge().processPluginMessage(player, player.getUniqueId(), bytes, false));
    }
}
