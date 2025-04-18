package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class SetInvisible implements OutgoingMessage {

    private boolean invisible;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeBoolean(invisible);
    }
}
