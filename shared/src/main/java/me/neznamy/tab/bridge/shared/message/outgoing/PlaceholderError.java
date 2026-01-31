package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Outgoing message for sending placeholder error details when a placeholder
 * threw an error when parsing.
 */
@AllArgsConstructor
@ToString
public class PlaceholderError implements OutgoingMessage {

    /** Message containing placeholder's identifier and type */
    @NonNull
    private final String placeholderMessage;

    /** Exception thrown by the placeholder */
    @NonNull
    private final Throwable exception;

    @Override
    public void write(@NonNull ByteArrayDataOutput out) {
        out.writeUTF(placeholderMessage);
        out.writeInt(exception.getStackTrace().length+1);
        out.writeUTF(exception.getClass().getName() + ": " + exception.getMessage());
        for (StackTraceElement e : exception.getStackTrace()) {
            out.writeUTF("\tat " + e.toString());
        }
    }
}
