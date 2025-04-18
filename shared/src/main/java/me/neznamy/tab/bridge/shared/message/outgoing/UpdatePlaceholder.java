package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class UpdatePlaceholder implements OutgoingMessage {

    @NonNull
    private String identifier;

    @NonNull
    private String value;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(identifier);
        out.writeUTF(value);
    }
}
