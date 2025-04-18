package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.jetbrains.annotations.NotNull;

/**
 * Incoming message for registering a placeholder.
 */
public class PlaceholderRegister implements IncomingMessage {

    @NotNull
    private final String identifier;
    private final int refresh;

    /**
     * Constructs a new instance and reads data from given input stream.
     *
     * @param   in
     *          Input stream to read data from
     */
    public PlaceholderRegister(@NonNull ByteArrayDataInput in) {
        this.identifier = in.readUTF();
        this.refresh = in.readInt();
    }

    @Override
    public void process(@NonNull BridgePlayer player) {
        TABBridge.getInstance().getDataBridge().registerPlaceholder(player, identifier, refresh);
    }
}
