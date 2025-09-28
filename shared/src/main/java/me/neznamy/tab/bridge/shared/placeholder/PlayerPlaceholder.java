package me.neznamy.tab.bridge.shared.placeholder;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.message.outgoing.PlaceholderError;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * A placeholder that may return a different value for each player.
 */
public class PlayerPlaceholder extends Placeholder {

    /** Last known values for each player */
    @NotNull
    private final Map<BridgePlayer, String> lastValues = Collections.synchronizedMap(new WeakHashMap<>());

    /** Placeholder apply function */
    @NotNull
    private final Function<BridgePlayer, String> function;

    /**
     * Constructs new instance with given parameters.
     *
     * @param   identifier
     *          Placeholder identifier
     * @param   refresh
     *          Placeholder refresh interval
     * @param   function
     *          Placeholder apply function
     */
    public PlayerPlaceholder(@NonNull String identifier, int refresh, @NonNull Function<BridgePlayer, String> function) {
        super(identifier, refresh);
        this.function = function;
    }

    /**
     * Updates the placeholder for given player. Returns {@code true} if value changed since
     * last time, {@code false} if not.
     *
     * @param   player
     *          Player to update the placeholder for
     * @return  {@code true} if value changed, {@code false} if not
     */
    public boolean update(@NonNull BridgePlayer player) {
        String value = request(player);
        if (!lastValues.getOrDefault(player, identifier).equals(value)) {
            lastValues.put(player, value);
            return true;
        }
        return false;
    }

    /**
     * Requests new value for the player and returns it. If the call threw an error, it is forwarded
     * to the proxy and {@code <PlaceholderAPI Error>} is returned.
     *
     * @param   player
     *          Player to request new value for.
     * @return  New value for the player
     */
    @NotNull
    private String request(@NonNull BridgePlayer player) {
        try {
            String result = function.apply(player);
            return Objects.requireNonNull(result, "Placeholder " + identifier + " returned null for player " + player.getName());
        } catch (Throwable t) {
            player.sendPluginMessage(new PlaceholderError("Player placeholder " + identifier + " generated an error when setting for player " + player.getName(), t));
            return "<PlaceholderAPI Error>";
        }
    }

    /**
     * Returns last known value for the player. If not initialized yet, it is calculated using
     * provided apply function and then returned.
     *
     * @param   player
     *          Player to get last value for
     * @return  Last known value of the placeholder for player
     */
    @NotNull
    public String getLastValue(@NonNull BridgePlayer player) {
        return lastValues.computeIfAbsent(player, this::request);
    }
}
