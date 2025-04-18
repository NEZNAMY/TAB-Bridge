package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.jetbrains.annotations.NotNull;

/**
 * Incoming message for setting a placeholder value.
 */
public class ExpansionPlaceholder implements IncomingMessage {

    @NotNull
    private final String identifier;

    @NotNull
    private final String value;

    /**
     * Constructs a new instance and reads data from given input stream.
     *
     * @param   in
     *          Input stream to read data from
     */
    public ExpansionPlaceholder(@NonNull ByteArrayDataInput in) {
        this.identifier = in.readUTF();
        this.value = in.readUTF();
    }

    @Override
    public void process(@NonNull BridgePlayer player) {
        if (TABBridge.getInstance().getExpansion() != null) {
            TABBridge.getInstance().getExpansion().setValue(player, identifier, value);
        }
    }
}