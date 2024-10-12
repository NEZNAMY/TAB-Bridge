package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class UpdatePlaceholder implements OutgoingMessage {

    private String identifier;
    private String value;

    @Override
    public void write(@NotNull ByteArrayDataOutput out) {
        out.writeUTF(identifier);
        out.writeUTF(value);
    }
}
