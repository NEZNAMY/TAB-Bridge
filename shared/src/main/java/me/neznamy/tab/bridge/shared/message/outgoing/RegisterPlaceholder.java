package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for registering a placeholder on the proxy when
 * requested by another plugin/mod via tab expansion.
 */
@AllArgsConstructor
@ToString
public class RegisterPlaceholder implements OutgoingMessage {

    /** Placeholder identifier */
    @NonNull
    private final String identifier;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(identifier);
    }
}
