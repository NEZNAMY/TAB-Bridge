package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.message.outgoing.HasPermission;

public class PermissionCheck implements IncomingMessage {

    private String permission;

    @Override
    public void read(@NonNull ByteArrayDataInput in) {
        permission = in.readUTF();
    }

    @Override
    public void process(@NonNull BridgePlayer player) {
        player.sendPluginMessage(new HasPermission(permission, player.hasPermission(permission)));
    }
}