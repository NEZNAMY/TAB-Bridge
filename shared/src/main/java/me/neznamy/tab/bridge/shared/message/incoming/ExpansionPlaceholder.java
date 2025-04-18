package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;

public class ExpansionPlaceholder implements IncomingMessage {

    private String identifier;
    private String value;

    @Override
    public void read(@NonNull ByteArrayDataInput in) {
        identifier = in.readUTF();
        value = in.readUTF();
    }

    @Override
    public void process(@NonNull BridgePlayer player) {
        if (TABBridge.getInstance().getExpansion() != null) {
            TABBridge.getInstance().getExpansion().setValue(player, identifier, value);
        }
    }
}