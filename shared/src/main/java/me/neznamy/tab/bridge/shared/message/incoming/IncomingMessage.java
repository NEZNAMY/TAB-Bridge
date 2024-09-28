package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import org.jetbrains.annotations.NotNull;

public interface IncomingMessage {

    void read(@NotNull ByteArrayDataInput in);

    void process(@NotNull BridgePlayer player);
}
