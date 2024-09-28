package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import org.jetbrains.annotations.NotNull;

public interface OutgoingMessage {

    @NotNull
    ByteArrayDataOutput write();
}
