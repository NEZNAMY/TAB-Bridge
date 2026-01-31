package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for updating relational placeholder value.
 */
@AllArgsConstructor
@ToString
public class UpdateRelationalPlaceholder implements OutgoingMessage {

    /** Placeholder identifier */
    @NonNull
    private final String identifier;

    /** Target player involved in relational placeholder */
    @NonNull
    private final String targetPlayer;

    /** New placeholder value */
    @NonNull
    private final String value;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(identifier);
        out.writeUTF(targetPlayer);
        out.writeUTF(value);
    }
}
