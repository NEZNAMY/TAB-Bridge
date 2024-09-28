package me.neznamy.tab.bridge.shared.placeholder;

import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.message.outgoing.PlaceholderError;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

/**
 * A placeholder that takes 2 players as input.
 */
public class RelationalPlaceholder extends Placeholder {

    /** Last known placeholder values */
    private final Map<BridgePlayer, Map<BridgePlayer, String>> lastValues = new WeakHashMap<>();

    /** Placeholder apply function */
    private final BiFunction<BridgePlayer, BridgePlayer, String> function;

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
    public RelationalPlaceholder(@NotNull String identifier, int refresh, @NotNull BiFunction<BridgePlayer, BridgePlayer, String> function) {
        super(identifier, refresh);
        this.function = function;
    }

    /**
     * Updates the placeholder for given players. Returns {@code true} if value changed since
     * last time, {@code false} if not.
     *
     * @param   viewer
     *          Player looking at the placeholder value
     * @param   target
     *          Player the value is displayed on
     * @return  {@code true} if value changed, {@code false} if not
     */
    public boolean update(@NotNull BridgePlayer viewer, @NotNull BridgePlayer target) {
        String value = request(viewer, target);
        if (!lastValues.computeIfAbsent(viewer, v -> new WeakHashMap<>()).getOrDefault(target, getIdentifier()).equals(value)) {
            lastValues.get(viewer).put(target, value);
            return true;
        }
        return false;
    }

    /**
     * Requests new value for the players and returns it. If the call threw an error, it is forwarded
     * to the proxy and {@code <PlaceholderAPI Error>} is returned.
     *
     * @param   viewer
     *          Player looking at the placeholder value
     * @param   target
     *          Player the value is displayed on
     * @return  New value for the players
     */
    @NotNull
    private String request(@NotNull BridgePlayer viewer, @NotNull BridgePlayer target) {
        try {
            return function.apply(viewer, target);
        } catch (Throwable t) {
            viewer.sendPluginMessage(new PlaceholderError("Relational placeholder " + identifier +
                    " generated an error when setting for viewer " + viewer.getName() + " and target " + target.getName(), t));
            return "<PlaceholderAPI Error>";
        }
    }

    /**
     * Returns last known value for players. If not initialized yet, it is calculated using
     * provided apply function and then returned.
     *
     * @param   viewer
     *          Player looking at the placeholder value
     * @param   target
     *          Player the value is displayed on
     * @return  Last known value of the placeholder for players
     */
    @NotNull
    public String getLastValue(@NotNull BridgePlayer viewer, @NotNull BridgePlayer target) {
        return lastValues.computeIfAbsent(viewer, v -> new WeakHashMap<>()).computeIfAbsent(target, t -> request(viewer, target));
    }
}
