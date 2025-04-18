package me.neznamy.tab.bridge.bukkit;

import lombok.NonNull;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@RequiredArgsConstructor
public class BukkitPlatform implements Platform {

    private final boolean placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    private final JavaPlugin plugin;
    private final boolean folia;

    @Override
    public boolean isOnline(@NonNull Object player) {
        return ((Player)player).isOnline();
    }

    @Override
    @NotNull
    public UUID getUniqueId(@NonNull Object player) {
        return ((Player)player).getUniqueId();
    }

    @Override
    public void scheduleSyncRepeatingTask(@NonNull Runnable task, int intervalTicks) {
        if (folia) return; // Do not refresh sync placeholders on folia
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, intervalTicks, intervalTicks);
    }

    @Override
    public void runTask(@NonNull Runnable task) {
        if (folia) return; // Do not initialize sync placeholders
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void cancelTasks() {
        if (folia) return; // No tasks to cancel
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    @NotNull
    public BridgePlayer newPlayer(@NonNull Object player) {
        return new BukkitBridgePlayer((Player) player);
    }

    @Override
    @NotNull
    public Placeholder createPlaceholder(@NonNull String publicIdentifier, @NonNull String privateIdentifier, int refresh) {
        if (!placeholderAPI) {
            if (privateIdentifier.startsWith("%rel_")) {
                return new RelationalPlaceholder(publicIdentifier, -1, (viewer, target) -> "<PlaceholderAPI is not installed>");
            } else {
                return new ServerPlaceholder(publicIdentifier, -1, () -> "<PlaceholderAPI is not installed>");
            }
        }
        if (privateIdentifier.startsWith("%server_")) {
            return new ServerPlaceholder(publicIdentifier, refresh, () -> parseWithNestedPlaceholders(null, privateIdentifier));
        } else if (privateIdentifier.startsWith("%rel_")) {
            return new RelationalPlaceholder(publicIdentifier, refresh, (viewer, target) ->
                    PlaceholderAPI.setRelationalPlaceholders(((BukkitBridgePlayer)viewer).getPlayer(),
                            ((BukkitBridgePlayer)target).getPlayer(), privateIdentifier));
        } else {
            return new PlayerPlaceholder(publicIdentifier, refresh, p ->
                    parseWithNestedPlaceholders(((BukkitBridgePlayer)p).getPlayer(), privateIdentifier));
        }
    }

    @NotNull
    private String parseWithNestedPlaceholders(@Nullable Player player, @NonNull String identifier) {
        String text = identifier;
        String textBefore;
        do {
            textBefore = text;
            text = PlaceholderAPI.setPlaceholders(player, text);
        } while (!textBefore.equals(text));
        return text;
    }
}