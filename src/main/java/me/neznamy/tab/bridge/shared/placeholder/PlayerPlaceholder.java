package me.neznamy.tab.bridge.shared.placeholder;

import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.message.outgoing.PlaceholderError;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

public class PlayerPlaceholder extends Placeholder {

    private final Map<BridgePlayer, String> lastValues = new WeakHashMap<>();
    private final Function<BridgePlayer, String> function;

    public PlayerPlaceholder(String identifier, int refresh, Function<BridgePlayer, String> function) {
        super(identifier, refresh);
        this.function = function;
    }

    public boolean update(BridgePlayer player) {
        String value = request(player);
        if (!lastValues.getOrDefault(player, getIdentifier()).equals(value)) {
            lastValues.put(player, value);
            return true;
        }
        return false;
    }

    private String request(BridgePlayer player) {
        long time = System.currentTimeMillis();
        try {
            return function.apply(player);
        } catch (Throwable t) {
            player.sendPluginMessage(new PlaceholderError("Player placeholder " + identifier + " generated an error when setting for player " + player.getName(), t));
            return "<PlaceholderAPI Error>";
        } finally {
            long timeDiff = System.currentTimeMillis() - time;
            if (PRINT_WARNS && timeDiff > 50) {
                TABBridge.getInstance().getPlatform().sendConsoleMessage("&c[WARN] Placeholder " + identifier + " took " + timeDiff + "ms to return value for " + player.getName());
            }
        }
    }

    public String getLastValue(BridgePlayer player) {
        if (!lastValues.containsKey(player)) {
            update(player);
        }
        return lastValues.getOrDefault(player, getIdentifier());
    }
}
