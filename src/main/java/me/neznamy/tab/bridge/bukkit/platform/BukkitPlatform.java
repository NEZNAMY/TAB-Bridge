package me.neznamy.tab.bridge.bukkit.platform;

import com.google.common.io.ByteArrayDataInput;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.bukkit.BukkitBridgePlayer;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.Platform;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@RequiredArgsConstructor
public class BukkitPlatform implements Platform {

    private final boolean placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    private final JavaPlugin plugin;

    @Override
    public boolean isOnline(Object player) {
        return ((Player)player).isOnline();
    }

    @Override
    public UUID getUniqueId(Object player) {
        return ((Player)player).getUniqueId();
    }

    @Override
    public Channel getChannel(Object player) {
        if (NMSStorage.getInstance() == null) return null;
        return NMSStorage.getInstance().getChannelInjection().getChannel((Player) player);
    }

    @Override
    public void readUnlimitedNametagJoin(BridgePlayer player, ByteArrayDataInput input) {
        BukkitBridge.getInstance().nametagx.onJoin((BukkitBridgePlayer) player, input);
    }

    @Override
    public void readUnlimitedNametagMessage(BridgePlayer player, ByteArrayDataInput input) {
        BukkitBridge.getInstance().nametagx.readMessage((BukkitBridgePlayer) player, input);
    }

    @Override
    public void scheduleSyncRepeatingTask(Runnable task, int intervalTicks) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, intervalTicks, intervalTicks);
    }

    @Override
    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void cancelTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public void sendConsoleMessage(String message) {
        Bukkit.getConsoleSender().sendMessage("[TAB-Bridge] " + ChatColor.translateAlternateColorCodes('&', message));
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

    public void runEntityTask(Entity entity, Runnable task) {
        task.run();
    }
}