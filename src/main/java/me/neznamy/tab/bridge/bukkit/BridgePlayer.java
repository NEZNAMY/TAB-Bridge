package me.neznamy.tab.bridge.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.entity.Player;

public class BridgePlayer {

    private final Player player;
    private final int protocolVersion;
    private boolean vanished;
    private boolean disguised;
    private boolean invisible;
    private String group = "NONE";

    public BridgePlayer(Player player, int protocolVersion) {
        this.player = player;
        this.protocolVersion = protocolVersion;
    }

    public Player getPlayer() {
        return player;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setVanished(boolean vanished) {
        if (this.vanished == vanished) return;
        this.vanished = vanished;
        sendMessage("Vanished", vanished);
    }

    public void setDisguised(boolean disguised) {
        if (this.disguised == disguised) return;
        this.disguised = disguised;
        sendMessage("Disguised", disguised);
    }

    public boolean isDisguised() {
        return disguised;
    }

    public void setInvisible(boolean invisible) {
        if (this.invisible == invisible) return;
        this.invisible = invisible;
        sendMessage("Invisible", invisible);
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setGroup(String group) {
        if (this.group.equals(group)) return;
        this.group = group;
        sendMessage("Group", group);
    }

    public void sendMessage(Object... args) {
//        System.out.println("Sending message to " + player.getName() + ": " + Arrays.toString(args));
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (Object arg : args) {
            writeObject(out, arg);
        }
        player.sendPluginMessage(BukkitBridge.getInstance(), BukkitBridge.CHANNEL_NAME, out.toByteArray());
    }

    private void writeObject(ByteArrayDataOutput out, Object value) {
        if (value == null) return;
        if (value instanceof String) {
            out.writeUTF((String) value);
        } else if (value instanceof Boolean) {
            out.writeBoolean((boolean) value);
        } else if (value instanceof Integer) {
            out.writeInt((int) value);
        } else throw new IllegalArgumentException("Unhandled message data type " + value.getClass().getName());
    }

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
