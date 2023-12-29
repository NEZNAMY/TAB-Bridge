package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.jetbrains.annotations.NotNull;

public class PlaceholderRegister implements IncomingMessage {

    private String identifier;
    private int refresh;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        identifier = in.readUTF();
        refresh = in.readInt();
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        TABBridge.getInstance().getDataBridge().registerPlaceholder(player, identifier, refresh);
    }
}
