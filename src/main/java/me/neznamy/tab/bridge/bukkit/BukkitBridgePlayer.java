package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.entity.Player;

public class BukkitBridgePlayer extends BridgePlayer {

    @Getter private final Player player;

    public BukkitBridgePlayer(Player player, int protocolVersion) {
        super(player.getName(), player.getUniqueId(), protocolVersion);
        this.player = player;
    }

    @Override
    public void sendPluginMessage(byte[] message) {
        player.sendPluginMessage(BukkitBridge.getInstance(), TABBridge.CHANNEL_NAME, message);
    }

    @Override
    public void sendPacket(Object packet) {
        if (packet == null) return;
        try {
            Object handle = NMSStorage.getInstance().getHandle.invoke(player);
            Object playerConnection = NMSStorage.getInstance().PLAYER_CONNECTION.get(handle);
            NMSStorage.getInstance().sendPacket.invoke(playerConnection, packet);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
