package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class GroupChange implements OutgoingMessage {

    @NonNull
    private String group;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(group);
    }
}
