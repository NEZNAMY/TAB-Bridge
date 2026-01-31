package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for updating player's game mode.
 */
@AllArgsConstructor
@ToString
public class UpdateGameMode implements OutgoingMessage {

    /** New game mode */
    private final int gameMode;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeInt(gameMode);
    }
}
