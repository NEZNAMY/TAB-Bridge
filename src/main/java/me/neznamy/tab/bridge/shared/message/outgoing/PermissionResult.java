package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class PermissionResult implements OutgoingMessage {

    private String permission;
    private boolean result;

    @Override
    @NotNull
    public ByteArrayDataOutput write() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Permission");
        out.writeUTF(permission);
        out.writeBoolean(result);
        return out;
    }
}
