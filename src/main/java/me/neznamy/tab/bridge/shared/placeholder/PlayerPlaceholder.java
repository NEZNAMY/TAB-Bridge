package me.neznamy.tab.bridge.shared.placeholder;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

public class PlayerPlaceholder extends Placeholder {

    private final Map<Object, String> lastValues = new WeakHashMap<>();
    private final Function<Object, String> function;

    public PlayerPlaceholder(String identifier, int refresh, Function<Object, String> function) {
        super(identifier, refresh);
        this.function = function;
    }

    public boolean update(Object player) {
        String value = function.apply(player);
        if (!lastValues.getOrDefault(player, getIdentifier()).equals(value)) {
            lastValues.put(player, value);
            return true;
        }
        return false;
    }

    public String getLastValue(Object player) {
        return lastValues.getOrDefault(player, getIdentifier());
    }
}
