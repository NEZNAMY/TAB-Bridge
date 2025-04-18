package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class SetDisguised implements OutgoingMessage {

    private boolean disguised;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeBoolean(disguised);
    }
}
