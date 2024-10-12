package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class HasPermission implements OutgoingMessage {

    private String permission;
    private boolean result;

    @Override
    public void write(@NotNull ByteArrayDataOutput out) {
        out.writeUTF(permission);
        out.writeBoolean(result);
    }
}
