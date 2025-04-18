package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.message.outgoing.HasPermission;
import org.jetbrains.annotations.NotNull;

/**
 * Incoming message for checking if a player has a specific permission.
 */
public class PermissionCheck implements IncomingMessage {

    @NotNull
    private final String permission;

    /**
     * Constructs a new instance and reads data from given input stream.
     *
     * @param   in
     *          Input stream to read data from
     */
    public PermissionCheck(@NonNull ByteArrayDataInput in) {
        this.permission = in.readUTF();
    }

    @Override
    public void process(@NonNull BridgePlayer player) {
        player.sendPluginMessage(new HasPermission(permission, player.hasPermission(permission)));
    }
}