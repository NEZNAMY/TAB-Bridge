package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for notifying about player changing world.
 */
@AllArgsConstructor
@ToString
public class WorldChange implements OutgoingMessage {

    /** New world name */
    @NonNull
    private final String world;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(world);
    }
}
