package me.neznamy.tab.bridge.shared.features;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;

/**
 * This is an interface representing TAB's PlaceholderAPI expansion.
 */
public interface TabExpansion {

    /**
     * Registers the expansion.
     *
     * @return  {@code true} if the expansion was unregistered successfully, {@code false} otherwise
     */
    boolean unregister();

    /**
     * Sets the value of specified placeholder for the specified player.
     *
     * @param   player
     *          Player to set the value for
     * @param   identifier
     *          TAB placeholder
     * @param   value
     *          Value of the placeholder
     */
    void setValue(@NonNull BridgePlayer player, @NonNull String identifier, @NonNull String value);
}
