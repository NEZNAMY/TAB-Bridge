package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.jetbrains.annotations.NotNull;

public class ExpansionPlaceholder implements IncomingMessage {

    private String identifier;
    private String value;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        identifier = in.readUTF();
        value = in.readUTF();
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        if (TABBridge.getInstance().getExpansion() != null) {
            TABBridge.getInstance().getExpansion().setValue(player, identifier, value);
        }
    }
}