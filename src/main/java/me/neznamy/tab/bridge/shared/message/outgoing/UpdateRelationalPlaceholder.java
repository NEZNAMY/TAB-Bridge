package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class UpdateRelationalPlaceholder implements OutgoingMessage {

    private String identifier;
    private String otherPlayer;
    private String value;

    @Override
    @NotNull
    public ByteArrayDataOutput write() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Placeholder");
        out.writeUTF(identifier);
        out.writeUTF(otherPlayer);
        out.writeUTF(value);
        return out;
    }
}
