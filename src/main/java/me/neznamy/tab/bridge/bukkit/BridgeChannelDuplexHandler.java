package me.neznamy.tab.bridge.bukkit;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
public class BridgeChannelDuplexHandler extends ChannelDuplexHandler {

    private final Player player;
    private String lastConsoleMessage;

    @Override
    public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) {
        try {
            BukkitBridgePlayer p = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(player.getUniqueId());
            if (p != null && NMSStorage.getInstance() != null) {
                if (BukkitBridge.getInstance().nametagx.isEnabled()) {
                    BukkitBridge.getInstance().nametagx.getPacketListener().onPacketSend(p, packet);
                }
                if (BukkitScoreboard.isAvailable() && BukkitScoreboard.teamPacketData.TeamPacketClass.isInstance(packet)) {
                    modifyPlayers(packet);
                }
            }
            super.write(context, packet, channelPromise);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyPlayers(@NonNull Object packetPlayOutScoreboardTeam) throws ReflectiveOperationException {
        BridgePlayer bp = TABBridge.getInstance().getPlayer(player.getUniqueId());
        if (bp == null) return;
        BukkitScoreboard scoreboard = (BukkitScoreboard) bp.getScoreboard();
        NMSStorage nms = NMSStorage.getInstance();
        if (nms == null) return;
        int action = BukkitScoreboard.teamPacketData.TeamPacket_ACTION.getInt(packetPlayOutScoreboardTeam);
        if (action == 1 || action == 2 || action == 4) return;
        Collection<String> players = (Collection<String>) BukkitScoreboard.teamPacketData.TeamPacket_PLAYERS.get(packetPlayOutScoreboardTeam);
        String teamName = (String) BukkitScoreboard.teamPacketData.TeamPacket_NAME.get(packetPlayOutScoreboardTeam);
        if (players == null) return;
        //creating a new list to prevent NoSuchFieldException in minecraft packet encoder when a player is removed
        Collection<String> newList = new ArrayList<>();
        for (String entry : players) {
            String expectedTeam = scoreboard.getExpectedTeams().get(entry);
            if (expectedTeam != null && !expectedTeam.equals(teamName)) {
                String msg = "[TAB-Bridge] Blocked attempt to add player " + entry + " into team " + teamName +
                        " (expected team: " + expectedTeam + ")";
                if (!msg.equals(lastConsoleMessage)) {
                    lastConsoleMessage = msg;
                    //Bukkit.getConsoleSender().sendMessage(msg);
                }
            } else {
                newList.add(entry);
            }
        }
        BukkitScoreboard.teamPacketData.TeamPacket_PLAYERS.set(packetPlayOutScoreboardTeam, newList);
    }
}