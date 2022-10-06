package me.neznamy.tab.bridge.shared;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;

public abstract class BridgePlayer {

    private final String name;
    private final UUID uuid;
    private final int protocolVersion;
    private boolean vanished;
    private boolean disguised;
    private boolean invisible;
    private String group = "NONE";

    public BridgePlayer(String name, UUID uuid, int protocolVersion) {
        this.name = name;
        this.uuid = uuid;
        this.protocolVersion = protocolVersion;
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setVanished(boolean vanished) {
        if (this.vanished == vanished) return;
        this.vanished = vanished;
        sendMessage("Vanished", vanished);
    }

    public boolean isVanished() {
        return vanished;
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
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (Object arg : args) {
            writeObject(out, arg);
        }
        sendPluginMessage(out.toByteArray());
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

    public abstract void sendPluginMessage(byte[] message);

    public abstract void sendPacket(Object packet);
}
