package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for setting whether the player is invisible or not.
 */
@AllArgsConstructor
@ToString
public class SetInvisible implements OutgoingMessage {

    /** True if the player is invisible, false if not */
    private final boolean invisible;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeBoolean(invisible);
    }
}
