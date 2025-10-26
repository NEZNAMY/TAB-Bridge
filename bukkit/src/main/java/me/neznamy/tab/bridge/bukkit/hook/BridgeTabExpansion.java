package me.neznamy.tab.bridge.bukkit.hook;

import lombok.Getter;
import lombok.NonNull;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import me.neznamy.tab.bridge.shared.message.outgoing.RegisterPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.PlaceholderReplacementPattern;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TAB's expansion for PlaceholderAPI
 */
public class BridgeTabExpansion extends PlaceholderExpansion implements TabExpansion {

    /** Map holding all placeholder values for all players */
    private final Map<BridgePlayer, Map<String, String>> values = Collections.synchronizedMap(new WeakHashMap<>());

    private final Map<BridgePlayer, Set<String>> sentRequests = new WeakHashMap<>();

    @Getter private final String author = "NEZNAMY";
    @Getter private final String identifier = "tab";
    @Getter private final String version = TABBridge.PLUGIN_VERSION;

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public synchronized String onPlaceholderRequest(@Nullable Player player, @NonNull String identifier) {
        if (player == null) return "";
        if (identifier.startsWith("replace_")) {
            String text = "%" + identifier.substring(8) + "%";
            String textBefore;
            do {
                textBefore = text;
                for (String placeholder : detectPlaceholders(text)) {
                    PlaceholderReplacementPattern pattern = TABBridge.getInstance().getDataBridge().getReplacements().get(placeholder);
                    if (pattern != null) text = text.replace(placeholder, pattern.findReplacement(PlaceholderAPI.setPlaceholders(player, placeholder)));
                }
            } while (!textBefore.equals(text));
            return text;
        }
        BridgePlayer bridgePlayer = TABBridge.getInstance().getPlayer(player.getUniqueId());
        if (bridgePlayer == null) {
            return "<Player is not loaded yet>";
        }
        String value = values.computeIfAbsent(bridgePlayer, pl -> new ConcurrentHashMap<>()).get(identifier);
        if (value == null && identifier.startsWith("placeholder_")) {
            String placeholder = "%" + identifier.substring(12) + "%";
            if (!sentRequests.computeIfAbsent(bridgePlayer, pl -> new HashSet<>()).contains(placeholder)){
                sentRequests.get(bridgePlayer).add(placeholder);
                bridgePlayer.sendPluginMessage(new RegisterPlaceholder(placeholder));
            }
        }
        return value;
    }

    @Override
    public synchronized void setValue(@NonNull BridgePlayer player, @NonNull String identifier, @NonNull String value) {
        values.computeIfAbsent(player, p -> new ConcurrentHashMap<>()).put(identifier, value);
    }
}
