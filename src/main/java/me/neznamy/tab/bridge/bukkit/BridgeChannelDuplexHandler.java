package me.neznamy.tab.bridge.bukkit;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class BridgeChannelDuplexHandler extends ChannelDuplexHandler {

    private final Player player;
    private BukkitBridgePlayer bridgePlayer;

    @Override
    public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) {
        try {
            if (bridgePlayer == null) {
                bridgePlayer = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(player.getUniqueId());
            }
            if (bridgePlayer != null && NMSStorage.getInstance() != null) {
                if (BukkitBridge.getInstance().nametagx.isEnabled()) {
                    BukkitBridge.getInstance().nametagx.getPacketListener().onPacketSend(bridgePlayer, packet);
                }
                if (BukkitScoreboard.isAvailable()) {
                    bridgePlayer.getScoreboard().onPacketSend(packet);
                }
            }
            super.write(context, packet, channelPromise);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}