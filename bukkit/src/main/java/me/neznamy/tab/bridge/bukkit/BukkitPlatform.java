package me.neznamy.tab.bridge.bukkit;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.Platform;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@RequiredArgsConstructor
public class BukkitPlatform implements Platform {

    private final boolean placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    private final JavaPlugin plugin;
    private final boolean folia;

    @Override
    public boolean isOnline(Object player) {
        return ((Player)player).isOnline();
    }

    @Override
    public UUID getUniqueId(Object player) {
        return ((Player)player).getUniqueId();
    }

    @Override
    public void scheduleSyncRepeatingTask(Runnable task, int intervalTicks) {
        if (folia) return; // Do not refresh sync placeholders on folia
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, intervalTicks, intervalTicks);
    }

    @Override
    public void runTask(Runnable task) {
        if (folia) return; // Do not initialize sync placeholders
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void cancelTasks() {
        if (folia) return; // No tasks to cancel
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public BridgePlayer newPlayer(Object player, int protocolVersion) {
        return new BukkitBridgePlayer((Player) player, protocolVersion);
    }

    @Override
    public Placeholder createPlaceholder(String publicIdentifier, String privateIdentifier, int refresh) {
        Placeholder placeholder;
        if (privateIdentifier.startsWith("%server_")) {
            placeholder = new ServerPlaceholder(publicIdentifier, refresh, () ->
                    placeholderAPI ? parseWithNestedPlaceholders(null, privateIdentifier) : "<PlaceholderAPI is not installed>");
        } else if (privateIdentifier.startsWith("%rel_")) {
            placeholder = new RelationalPlaceholder(publicIdentifier, refresh, (viewer, target) ->
                    placeholderAPI ? PlaceholderAPI.setRelationalPlaceholders(((BukkitBridgePlayer)viewer).getPlayer(),
                            ((BukkitBridgePlayer)target).getPlayer(), privateIdentifier) : "<PlaceholderAPI is not installed>");
        } else {
            placeholder = new PlayerPlaceholder(publicIdentifier, refresh, p ->
                    placeholderAPI ? parseWithNestedPlaceholders(((BukkitBridgePlayer)p).getPlayer(), privateIdentifier) : "<PlaceholderAPI is not installed>");
        }
        return placeholder;
    }

    private String parseWithNestedPlaceholders(Player player, String identifier) {
        String text = identifier;
        String textBefore;
        do {
            textBefore = text;
            text = PlaceholderAPI.setPlaceholders(player, text);
        } while (!textBefore.equals(text));
        return text;
    }
}