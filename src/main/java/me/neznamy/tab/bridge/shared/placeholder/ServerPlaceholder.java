package me.neznamy.tab.bridge.shared.placeholder;

import com.google.common.collect.Iterables;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.message.outgoing.PlaceholderError;

import java.util.function.Supplier;

public class ServerPlaceholder extends Placeholder {

    private String lastValue = "<Not initialized yet>";
    private final Supplier<String> function;

    public ServerPlaceholder(String identifier, int refresh, Supplier<String> function) {
        super(identifier, refresh);
        this.function = function;
    }

    public boolean update() {
        String value = request();
        if (!value.equals(lastValue)) {
            lastValue = value;
            return true;
        }
        return false;
    }

    private String request() {
        long time = System.currentTimeMillis();
        try {
            return function.get();
        } catch (Throwable t) {
            BridgePlayer first = Iterables.getFirst(TABBridge.getInstance().getOnlinePlayers(), null);
            if (first != null) {
                first.sendPluginMessage(new PlaceholderError("Server placeholder " + identifier + " generated an error", t));
            }
            return "<PlaceholderAPI Error>";
        } finally {
            long timeDiff = System.currentTimeMillis() - time;
            if (PRINT_WARNS && timeDiff > 50) {
                TABBridge.getInstance().getPlatform().sendConsoleMessage("&c[WARN] Placeholder " + identifier + " took " + timeDiff + "ms to return value");
            }
        }
    }

    public String getLastValue() {
        if (lastValue == null) {
            update();
        }
        return lastValue;
    }
}
