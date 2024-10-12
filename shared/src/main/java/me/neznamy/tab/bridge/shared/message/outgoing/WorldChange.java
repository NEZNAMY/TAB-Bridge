package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class WorldChange implements OutgoingMessage {

    private String world;

    @Override
    public void write(@NotNull ByteArrayDataOutput out) {
        out.writeUTF(world);
    }
}
