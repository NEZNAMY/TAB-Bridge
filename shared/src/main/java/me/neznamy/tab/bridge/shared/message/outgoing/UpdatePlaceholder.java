package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for updating placeholder value when it changed.
 */
@AllArgsConstructor
@ToString
public class UpdatePlaceholder implements OutgoingMessage {

    /** Placeholder identifier */
    @NonNull
    private final String identifier;

    /** New placeholder value */
    @NonNull
    private final String value;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(identifier);
        out.writeUTF(value);
    }
}
