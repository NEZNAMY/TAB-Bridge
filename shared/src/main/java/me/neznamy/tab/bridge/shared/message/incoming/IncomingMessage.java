package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;

public interface IncomingMessage {

    void read(@NonNull ByteArrayDataInput in);

    void process(@NonNull BridgePlayer player);
}
