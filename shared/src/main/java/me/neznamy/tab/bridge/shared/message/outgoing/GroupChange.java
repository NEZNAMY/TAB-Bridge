package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for notifying about permission group change.
 */
@AllArgsConstructor
@ToString
public class GroupChange implements OutgoingMessage {

    /** New permission group */
    @NonNull
    private final String group;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(group);
    }
}
