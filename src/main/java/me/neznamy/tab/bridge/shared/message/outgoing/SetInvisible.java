package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class SetInvisible implements OutgoingMessage {

    private boolean invisible;

    @Override
    @NotNull
    public ByteArrayDataOutput write() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Invisible");
        out.writeBoolean(invisible);
        return out;
    }
}
