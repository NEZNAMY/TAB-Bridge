package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class GroupChange implements OutgoingMessage {

    private String group;

    @Override
    public void write(@NotNull ByteArrayDataOutput out) {
        out.writeUTF(group);
    }
}
