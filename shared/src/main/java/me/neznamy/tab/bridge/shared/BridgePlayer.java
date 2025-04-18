package me.neznamy.tab.bridge.shared;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.bridge.shared.message.outgoing.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Abstract class representing a player connected to the proxy.
 */
@RequiredArgsConstructor
@Getter
public abstract class BridgePlayer {

    /** Player's name */
    @NotNull
    private final String name;

    /** Player's UUID */
    private final UUID uniqueId;

    /** Flag tracking whether this player is vanished or not */
    private boolean vanished;

    /** Flag tracking whether this player is disguised or not */
    private boolean disguised;

    /** Flag tracking whether this player is invisible or not */
    private boolean invisible;

    /** Player's gamemode */
    private int gameMode;

    /** Player's primary permission group */
    private String group = "NONE";

    /**
     * Updates vanish status to give value. If it changed, sends plugin message to the proxy.
     *
     * @param   vanished
     *          New vanish status
     */
    public void setVanished(boolean vanished) {
        if (this.vanished == vanished) return;
        this.vanished = vanished;
        sendPluginMessage(new SetVanished(vanished));
    }

    /**
     * Updates disguise status to give value. If it changed, sends plugin message to the proxy.
     *
     * @param   disguised
     *          New disguise status
     */
    public void setDisguised(boolean disguised) {
        if (this.disguised == disguised) return;
        this.disguised = disguised;
        sendPluginMessage(new SetDisguised(disguised));
    }

    /**
     * Updates invisibility status to give value. If it changed, sends plugin message to the proxy.
     *
     * @param   invisible
     *          New invisibility status
     */
    public void setInvisible(boolean invisible) {
        if (this.invisible == invisible) return;
        this.invisible = invisible;
        sendPluginMessage(new SetInvisible(invisible));
    }

    /**
     * Updates group to give value. If it changed, sends plugin message to the proxy.
     *
     * @param   group
     *          New group
     */
    public void setGroup(@NonNull String group) {
        if (this.group.equals(group)) return;
        this.group = group;
        sendPluginMessage(new GroupChange(group));
    }

    /**
     * Updates gamemode to give value. If it changed, sends plugin message to the proxy.
     *
     * @param   gameMode
     *          New gamemode
     */
    public void setGameMode(int gameMode) {
        if (this.gameMode == gameMode) return;
        this.gameMode = gameMode;
        sendPluginMessage(new UpdateGameMode(gameMode));
    }

    /**
     * Raw update to the gamemode. This is used for the initial value when the player joins.
     *
     * @param   gameMode
     *          New gamemode
     */
    public void setGameModeRaw(int gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * Sends a plugin message to the proxy. The message is serialized and sent as a byte array using
     * {@link #sendPluginMessage(byte[])} method.
     *
     * @param   message
     *          Message to send
     */
    @SuppressWarnings("UnstableApiUsage")
    public void sendPluginMessage(@NonNull OutgoingMessage message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(OutgoingMessage.PACKET_IDS.get(message.getClass()));
        message.write(out);
        sendPluginMessage(out.toByteArray());
    }

    /**
     * Sends plugin message using the platform's API.
     *
     * @param   message
     *          Message to send
     */
    public abstract void sendPluginMessage(byte[] message);

    /**
     * Returns the name of the world the player is in.
     *
     * @return  Name of the world the player is in
     */
    @NotNull
    public abstract String getWorld();

    /**
     * Performs a permission check and returns whether the player has the given permission.
     *
     * @param   permission
     *          Permission to check
     * @return  {@code true} if the player has the permission, {@code false} otherwise
     */
    public abstract boolean hasPermission(@NonNull String permission);

    /**
     * Checks if the player is invisible.
     *
     * @return  {@code true} if the player is invisible, {@code false} otherwise
     */
    public abstract boolean checkInvisibility();

    /**
     * Checks if the player is vanished.
     *
     * @return  {@code true} if the player is vanished, {@code false} otherwise
     */
    public abstract boolean checkVanish();

    /**
     * Checks if the player is disguised.
     *
     * @return  {@code true} if the player is disguised, {@code false} otherwise
     */
    public abstract boolean checkDisguised();

    /**
     * Retrieves the player's primary permission group from permission plugin.
     *
     * @return  Retrieved permission group
     */
    @NotNull
    public abstract String checkGroup();

    /**
     * Checks the player's gamemode.
     *
     * @return  Player's gamemode
     */
    public abstract int checkGameMode();

    /**
     * Returns the platform-specific player object.
     *
     * @return  Platform-specific player object
     */
    @NotNull
    public abstract Object getPlayer();
}
