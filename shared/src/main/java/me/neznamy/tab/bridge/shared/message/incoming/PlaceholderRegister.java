package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;

public class PlaceholderRegister implements IncomingMessage {

    private String identifier;
    private int refresh;

    @Override
    public void read(@NonNull ByteArrayDataInput in) {
        identifier = in.readUTF();
        refresh = in.readInt();
    }

    @Override
    public void process(@NonNull BridgePlayer player) {
        TABBridge.getInstance().getDataBridge().registerPlaceholder(player, identifier, refresh);
    }
}
