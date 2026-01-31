package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for sending permission check result.
 */
@AllArgsConstructor
@ToString
public class HasPermission implements OutgoingMessage {

    /** Permission node */
    @NonNull
    private final String permission;

    /** Permission check result */
    private final boolean result;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(permission);
        out.writeBoolean(result);
    }
}
