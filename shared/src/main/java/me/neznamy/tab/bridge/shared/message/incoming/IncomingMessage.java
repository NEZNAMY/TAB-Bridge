package me.neznamy.tab.bridge.shared.message.incoming;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;

/**
 * Interface representing an incoming message from the proxy.
 */
public interface IncomingMessage {

    /**
     * Processes the incoming message for the specified player.
     *
     * @param   player
     *          The player to process the message for
     */
    void process(@NonNull BridgePlayer player);
}
