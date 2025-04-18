package me.neznamy.tab.bridge.shared.placeholder;

import lombok.Getter;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.message.outgoing.PlaceholderError;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A placeholder that returns the same value for all players.
 */
public class ServerPlaceholder extends Placeholder {

    /** Last known value of the placeholder */
    @Getter
    @NotNull
    private String lastValue;

    /** Function for retrieving new value */
    @NotNull
    private final Supplier<String> function;

    /**
     * Constructs new instance with given parameters and initializes the value using provided function.
     *
     * @param   identifier
     *          Placeholder identifier
     * @param   refresh
     *          Placeholder refresh interval
     * @param   function
     *          Placeholder apply function
     */
    public ServerPlaceholder(@NonNull String identifier, int refresh, @NonNull Supplier<String> function) {
        super(identifier, refresh);
        this.function = function;
        lastValue = request();
    }

    /**
     * Updates the placeholder. Returns {@code true} if value changed since
     * last time, {@code false} if not.
     *
     * @return  {@code true} if value changed, {@code false} if not
     */
    public boolean update() {
        String value = request();
        if (!lastValue.equals(value)) {
            lastValue = value;
            return true;
        }
        return false;
    }

    /**
     * Requests new value and returns it. If the call threw an error, it is forwarded
     * to the proxy and {@code <PlaceholderAPI Error>} is returned.
     *
     * @return  New value
     */
    @NotNull
    private String request() {
        try {
            return function.get();
        } catch (Throwable t) {
            BridgePlayer[] players = TABBridge.getInstance().getOnlinePlayers();
            if (players.length > 0) {
                players[0].sendPluginMessage(new PlaceholderError("Server placeholder " + identifier + " generated an error", t));
            }
            return "<PlaceholderAPI Error>";
        }
    }
}
