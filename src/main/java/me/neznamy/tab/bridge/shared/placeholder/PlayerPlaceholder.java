package me.neznamy.tab.bridge.shared.placeholder;

import me.neznamy.tab.bridge.bukkit.BridgePlayer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        try {
            return function.apply(player);
        } catch (Throwable t) {
            List<Object> args = new ArrayList<>();
            args.add("PlaceholderError");
            args.add("Player placeholder " + identifier + " generated an error when setting for player " + player.getPlayer().getName());
            args.add(t.getStackTrace().length+1);
            args.add(t.getClass().getName() + ": " + t.getMessage());
            args.addAll(Arrays.stream(t.getStackTrace()).map(e -> "\tat " + e.toString()).collect(Collectors.toList()));
            player.sendMessage(args.toArray());
            return "<PlaceholderAPI Error>";
        }
    }

    public String getLastValue(BridgePlayer player) {
        if (!lastValues.containsKey(player)) {
            update(player);
        }
        return lastValues.getOrDefault(player, getIdentifier());
    }
}
