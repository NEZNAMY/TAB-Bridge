package me.neznamy.tab.bridge.shared;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public abstract class BridgePlayer {

    @Getter private final String name;
    @Getter private final UUID uniqueId;
    @Getter private final int protocolVersion;
    @Getter private boolean vanished;
    @Getter private boolean disguised;
    @Getter private boolean invisible;
    @Getter private int gameMode;
    private String group = "NONE";

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

    public void setInvisible(boolean invisible) {
        if (this.invisible == invisible) return;
        this.invisible = invisible;
        sendMessage("Invisible", invisible);
    }

    public void setGroup(String group) {
        if (this.group.equals(group)) return;
        this.group = group;
        sendMessage("Group", group);
    }

    public void setGameMode(int gameMode) {
        if (this.gameMode == gameMode) return;
        this.gameMode = gameMode;
        sendMessage("UpdateGameMode", gameMode);
    }

    public void setGameModeRaw(int gameMode) {
        this.gameMode = gameMode;
    }

    @SuppressWarnings("UnstableApiUsage")
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

    public abstract Scoreboard getScoreboard();

    public abstract String getWorld();

    public abstract boolean hasPermission(String permission);

    public abstract boolean checkInvisibility();

    public abstract boolean checkVanish();

    public abstract boolean checkDisguised();

    public abstract String checkGroup();

    public abstract int checkGameMode();
}
