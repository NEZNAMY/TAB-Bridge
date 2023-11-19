package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class SetDisguised implements OutgoingMessage {

    private boolean disguised;

    @Override
    @NotNull
    public ByteArrayDataOutput write() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Disguised");
        out.writeBoolean(disguised);
        return out;
    }
}
