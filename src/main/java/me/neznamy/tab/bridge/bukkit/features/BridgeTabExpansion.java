package me.neznamy.tab.bridge.bukkit.features;

import lombok.Getter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * TAB's expansion for PlaceholderAPI
 */
public class BridgeTabExpansion extends PlaceholderExpansion implements TabExpansion {

    /** Map holding all placeholder values for all players */
    private final Map<Player, Map<String, String>> values = new WeakHashMap<>();

    private final Map<Player, Set<String>> sentRequests = new WeakHashMap<>();

    @Getter private final String author = "NEZNAMY";
    @Getter private final String identifier = "tab";
    @Getter private final String version = TABBridge.PLUGIN_VERSION;

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier){
        if (player == null) return "";
        String value = values.computeIfAbsent(player, pl -> new HashMap<>()).get(identifier);
        if (value == null) {
            if (identifier.startsWith("placeholder_")) {
                String placeholder = "%" + identifier.substring(12) + "%";
                if (!sentRequests.computeIfAbsent(player, pl -> new HashSet<>()).contains(placeholder)){
                    BridgePlayer pl = TABBridge.getInstance().getPlayer(player.getUniqueId());
                    if (pl != null) {
                        sentRequests.get(player).add(placeholder);
                        pl.sendMessage("RegisterPlaceholder", placeholder);
                    }
                }
            }
        }
        //TODO forward replacements and add support for %tab_replace_<placeholder>% if anyone requests it
        return value;
    }

    @Override
    public void setValue(Object player, String identifier, String value) {
        values.computeIfAbsent((Player) player, p -> new HashMap<>()).put(identifier, value);
    }
}
