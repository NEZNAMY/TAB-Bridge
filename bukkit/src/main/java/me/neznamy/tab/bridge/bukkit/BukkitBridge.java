package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import me.neznamy.tab.bridge.bukkit.hook.BridgeTabExpansion;
import me.neznamy.tab.bridge.bukkit.platform.BukkitPlatform;
import me.neznamy.tab.bridge.bukkit.platform.FoliaPlatform;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.Platform;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.message.outgoing.WorldChange;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class BukkitBridge extends JavaPlugin implements PluginMessageListener, Listener {

    @Getter private static BukkitBridge instance;
    
    public void onEnable() {
        instance = this;
        boolean folia = ReflectionUtils.classExists("io.papermc.paper.threadedregions.RegionizedServer");
        Platform platform = folia ? new FoliaPlatform(this) : new BukkitPlatform(this);
        BridgeTabExpansion expansion = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? new BridgeTabExpansion() : null;
        TABBridge.setInstance(new TABBridge(platform, expansion));
        if (expansion != null) expansion.register();
        Bukkit.getMessenger().registerIncomingPluginChannel(this, TABBridge.CHANNEL_NAME, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, TABBridge.CHANNEL_NAME);
        Bukkit.getPluginManager().registerEvents(this, this);
        TABBridge.getInstance().getDataBridge().startTasks();
        new Metrics(this, 20810);
    }

    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        HandlerList.unregisterAll((Plugin)this);
        TABBridge.getInstance().unload();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        TABBridge.getInstance().submitTask(() -> TABBridge.getInstance().getDataBridge().processQueue(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        TABBridge.getInstance().submitTask(() -> {
            BukkitBridgePlayer p = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
            if (p == null) return;
            TABBridge.getInstance().removePlayer(p);
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        BridgePlayer p = TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
        if (p == null) return;
        p.sendPluginMessage(new WorldChange(e.getPlayer().getWorld().getName()));
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes){
        if (!channel.equals(TABBridge.CHANNEL_NAME)) return;
        TABBridge.getInstance().submitTask(
                () -> TABBridge.getInstance().getDataBridge().processPluginMessage(player, bytes, false));
    }
}