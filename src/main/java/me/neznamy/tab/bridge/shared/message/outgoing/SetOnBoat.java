package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class SetOnBoat implements OutgoingMessage {

    private boolean onBoat;

    @Override
    @NotNull
    public ByteArrayDataOutput write() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Boat");
        out.writeBoolean(onBoat);
        return out;
    }
}
