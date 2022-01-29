package me.neznamy.tab.bridge.shared.placeholder;

import java.util.function.Supplier;

public class ServerPlaceholder extends Placeholder {

    private String lastValue;
    private final Supplier<String> function;

    public ServerPlaceholder(String identifier, int refresh, Supplier<String> function) {
        super(identifier, refresh);
        this.function = function;
        update();
    }

    public boolean update() {
        String value = function.get();
        if (!value.equals(lastValue)) {
            lastValue = value;
            return true;
        }
        return false;
    }

    public String getLastValue() {
        return lastValue == null ? getIdentifier() : lastValue;
    }
}
