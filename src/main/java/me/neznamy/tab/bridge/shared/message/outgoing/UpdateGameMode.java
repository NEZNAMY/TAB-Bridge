package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public class UpdateGameMode implements OutgoingMessage {

    private int gameMode;

    @Override
    @NotNull
    public ByteArrayDataOutput write() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdateGameMode");
        out.writeInt(gameMode);
        return out;
    }
}
