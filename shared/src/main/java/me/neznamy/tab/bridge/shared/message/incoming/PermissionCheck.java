package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.message.outgoing.PermissionResult;
import org.jetbrains.annotations.NotNull;

public class PermissionCheck implements IncomingMessage {

    private String permission;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        permission = in.readUTF();
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        player.sendPluginMessage(new PermissionResult(permission, player.hasPermission(permission)));
    }
}