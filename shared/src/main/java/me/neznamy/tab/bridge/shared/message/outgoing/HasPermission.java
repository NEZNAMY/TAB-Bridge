package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class HasPermission implements OutgoingMessage {

    @NonNull
    private String permission;
    private boolean result;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(permission);
        out.writeBoolean(result);
    }
}
