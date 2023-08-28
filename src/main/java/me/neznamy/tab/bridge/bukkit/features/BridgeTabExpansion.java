package me.neznamy.tab.bridge.bukkit.features;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import me.neznamy.tab.bridge.shared.placeholder.PlaceholderReplacementPattern;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TAB's expansion for PlaceholderAPI
 */
public class BridgeTabExpansion extends PlaceholderExpansion implements TabExpansion {

    /** Map holding all placeholder values for all players */
    private final Map<Player, Map<String, String>> values = new WeakHashMap<>();

    private final Map<Player, Set<String>> sentRequests = new WeakHashMap<>();

    private final Pattern placeholderPattern = Pattern.compile("%([^%]*)%");

    @Getter private final String author = "NEZNAMY";
    @Getter private final String identifier = "tab";
    @Getter private final String version = TABBridge.PLUGIN_VERSION;

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
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
        String value = values.computeIfAbsent(player, pl -> new HashMap<>()).get(identifier);
        if (value == null && identifier.startsWith("placeholder_")) {
            String placeholder = "%" + identifier.substring(12) + "%";
            if (!sentRequests.computeIfAbsent(player, pl -> new HashSet<>()).contains(placeholder)){
                BridgePlayer pl = TABBridge.getInstance().getPlayer(player.getUniqueId());
                if (pl != null) {
                    sentRequests.get(player).add(placeholder);
                    pl.sendMessage("RegisterPlaceholder", placeholder);
                }
            }
        }
        return value;
    }

    @Override
    public void setValue(Object player, String identifier, String value) {
        values.computeIfAbsent((Player) player, p -> new HashMap<>()).put(identifier, value);
    }

    public @NotNull List<String> detectPlaceholders(@NotNull String text) {
        if (!text.contains("%")) return Collections.emptyList();
        List<String> placeholders = new ArrayList<>();
        Matcher m = placeholderPattern.matcher(text);
        while (m.find()) {
            placeholders.add(m.group());
        }
        return placeholders;
    }
}
