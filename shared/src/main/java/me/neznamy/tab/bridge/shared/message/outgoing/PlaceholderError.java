package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class PlaceholderError implements OutgoingMessage {

    private String placeholderMessage;
    private Throwable exception;

    @Override
    public void write(@NotNull ByteArrayDataOutput out) {
        out.writeUTF(placeholderMessage);
        out.writeInt(exception.getStackTrace().length+1);
        out.writeUTF(exception.getClass().getName() + ": " + exception.getMessage());
        for (StackTraceElement e : exception.getStackTrace()) {
            out.writeUTF("\tat " + e.toString());
        }
    }
}
