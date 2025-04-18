package me.neznamy.tab.bridge.shared;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.bridge.shared.message.outgoing.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public abstract class BridgePlayer {

    private final String name;
    private final UUID uniqueId;
    private boolean vanished;
    private boolean disguised;
    private boolean invisible;
    private int gameMode;
    private String group = "NONE";

    public void setVanished(boolean vanished) {
        if (this.vanished == vanished) return;
        this.vanished = vanished;
        sendPluginMessage(new SetVanished(vanished));
    }

    public void setDisguised(boolean disguised) {
        if (this.disguised == disguised) return;
        this.disguised = disguised;
        sendPluginMessage(new SetDisguised(disguised));
    }

    public void setInvisible(boolean invisible) {
        if (this.invisible == invisible) return;
        this.invisible = invisible;
        sendPluginMessage(new SetInvisible(invisible));
    }

    public void setGroup(@NonNull String group) {
        if (this.group.equals(group)) return;
        this.group = group;
        sendPluginMessage(new GroupChange(group));
    }

    public void setGameMode(int gameMode) {
        if (this.gameMode == gameMode) return;
        this.gameMode = gameMode;
        sendPluginMessage(new UpdateGameMode(gameMode));
    }

    public void setGameModeRaw(int gameMode) {
        this.gameMode = gameMode;
    }

    public void sendPluginMessage(@NonNull OutgoingMessage message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(OutgoingMessage.PACKET_IDS.get(message.getClass()));
        message.write(out);
        sendPluginMessage(out.toByteArray());
    }

    public abstract void sendPluginMessage(byte[] message);

    @NotNull
    public abstract String getWorld();

    public abstract boolean hasPermission(@NonNull String permission);

    public abstract boolean checkInvisibility();

    public abstract boolean checkVanish();

    public abstract boolean checkDisguised();

    @NotNull
    public abstract String checkGroup();

    public abstract int checkGameMode();

    @NotNull
    public abstract Object getPlayer();
}
