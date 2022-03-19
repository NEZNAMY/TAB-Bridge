package me.neznamy.tab.bridge.shared.placeholder;

import com.google.common.collect.Iterables;
import me.neznamy.tab.bridge.bukkit.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServerPlaceholder extends Placeholder {

    private String lastValue;
    private final Supplier<String> function;

    public ServerPlaceholder(String identifier, int refresh, Supplier<String> function) {
        super(identifier, refresh);
        this.function = function;
        update();
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
        try {
            return function.get();
        } catch (Throwable t) {
            BridgePlayer first = Iterables.getFirst(BukkitBridge.getInstance().getOnlinePlayers(), null);
            if (first != null) {
                List<Object> args = new ArrayList<>();
                args.add("PlaceholderError");
                args.add("Server placeholder " + identifier + " generated an error");
                args.add(t.getStackTrace().length+1);
                args.add(t.getClass().getName() + ": " + t.getMessage());
                args.addAll(Arrays.stream(t.getStackTrace()).map(e -> "\tat " + e.toString()).collect(Collectors.toList()));
                first.sendMessage(args.toArray());
            }
            return "<PlaceholderAPI Error>";
        }
    }

    public String getLastValue() {
        if (lastValue == null) {
            update();
        }
        return lastValue;
    }
}
