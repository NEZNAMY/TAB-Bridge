package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class UpdateGameMode implements OutgoingMessage {

    private int gameMode;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeInt(gameMode);
    }
}
