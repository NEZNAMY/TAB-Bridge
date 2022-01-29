package me.neznamy.tab.bridge.shared.placeholder;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

public class RelationalPlaceholder extends Placeholder {

    private final Map<Object, Map<Object, String>> lastValues = new WeakHashMap<>();
    private final BiFunction<Object, Object, String> function;

    public RelationalPlaceholder(String identifier, int refresh, BiFunction<Object, Object, String> function) {
        super(identifier, refresh);
        this.function = function;
    }

    public boolean update(Object viewer, Object target) {
        String value = function.apply(viewer, target);
        if (!lastValues.computeIfAbsent(viewer, v -> new WeakHashMap<>()).getOrDefault(target, getIdentifier()).equals(value)) {
            lastValues.get(viewer).put(target, value);
            return true;
        }
        return false;
    }

    public String getLastValue(Object viewer, Object target) {
        return lastValues.computeIfAbsent(viewer, v -> new WeakHashMap<>()).getOrDefault(target, getIdentifier());
    }
}
