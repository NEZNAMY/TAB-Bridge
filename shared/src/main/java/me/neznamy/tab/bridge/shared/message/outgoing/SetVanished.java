package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for setting whether the player is vanished or not.
 */
@AllArgsConstructor
@ToString
public class SetVanished implements OutgoingMessage {

    /** True if the player is vanished, false if not */
    private final boolean vanished;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeBoolean(vanished);
    }
}
