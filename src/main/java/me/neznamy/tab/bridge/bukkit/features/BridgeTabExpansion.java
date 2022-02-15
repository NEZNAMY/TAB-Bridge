package me.neznamy.tab.bridge.bukkit.features;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.neznamy.tab.bridge.bukkit.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * TAB's expansion for PlaceholderAPI
 */
public class BridgeTabExpansion extends PlaceholderExpansion {

    /** Map holding all placeholder values for all players */
    private final Map<BridgePlayer, Map<String, String>> values = new WeakHashMap<>();

    private final Map<BridgePlayer, Set<String>> sentRequests = new WeakHashMap<>();

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public @NotNull String getAuthor(){
        return "NEZNAMY";
    }

    @Override
    public @NotNull String getIdentifier(){
        return "tab";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0.0-pre1";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier){
        if (player == null) return "";
        BridgePlayer p = BukkitBridge.getInstance().getPlayer(player);
        String value = values.computeIfAbsent(p, pl -> new HashMap<>()).get(identifier);
        if (value == null) {
            if (identifier.startsWith("placeholder_")) {
                String placeholder = "%" + identifier.substring(12) + "%";
                if (!sentRequests.computeIfAbsent(p, pl -> new HashSet<>()).contains(placeholder)){
                    sentRequests.get(p).add(placeholder);
                    p.sendMessage("RegisterPlaceholder", placeholder);
                }
            }
        }
        //TODO forward replacements and add support for %tab_replace_<placeholder>% if anyone requests it
        return value;
    }

    public void setValue(BridgePlayer player, String identifier, String value) {
        values.computeIfAbsent(player, p -> new HashMap<>()).put(identifier, value);
    }
}
