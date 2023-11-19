package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.jetbrains.annotations.NotNull;

public class NameTagMessage implements IncomingMessage {

    private ByteArrayDataInput in;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        this.in = in;
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        TABBridge.getInstance().getPlatform().readUnlimitedNametagMessage(player, in);
    }
}
