package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for setting whether the player is disguised or not.
 */
@AllArgsConstructor
@ToString
public class SetDisguised implements OutgoingMessage {

    /** True if the player is disguised, false if not */
    private final boolean disguised;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeBoolean(disguised);
    }
}
