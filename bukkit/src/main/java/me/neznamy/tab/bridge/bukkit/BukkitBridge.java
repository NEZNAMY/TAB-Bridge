package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.neznamy.tab.bridge.bukkit.hook.BridgeTabExpansion;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.message.outgoing.WorldChange;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * The entry point of the plugin.
 */
@Getter
public class BukkitBridge extends JavaPlugin implements Listener {

    /**
     * Instance of this class.
     */
    @Getter
    private static BukkitBridge instance;

    @SneakyThrows
    public void onEnable() {
        instance = this;
        boolean folia = ReflectionUtils.classExists("io.papermc.paper.threadedregions.RegionizedServer");
        BridgeTabExpansion expansion = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? new BridgeTabExpansion() : null;
        TABBridge.setInstance(new TABBridge(new BukkitPlatform(this, folia), expansion));
        if (expansion != null) expansion.register();

        PluginMessageListener pluginMessageListener;
        if (PluginMessageListener.class.getMethods().length > 1) {
            // Paper 1.21.7+
            pluginMessageListener = (PluginMessageListener) Class.forName("me.neznamy.tab.bridge.bukkit.paper.PaperPluginMessageListener").getConstructor().newInstance();
        } else {
            pluginMessageListener = new BukkitPluginMessageListener();
        }
        Bukkit.getMessenger().registerIncomingPluginChannel(this, TABBridge.CHANNEL_NAME, pluginMessageListener);
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

    /**
     * Processes player join event by processing all queued plugin messages for that player.
     *
     * @param   e
     *          The event
     */
    @EventHandler
    public void onJoin(@NonNull PlayerJoinEvent e) {
        TABBridge.getInstance().submitTask(() -> TABBridge.getInstance().getDataBridge().processQueue(e.getPlayer(), e.getPlayer().getUniqueId()));
    }

    /**
     * Processes player quit event by removing the player from list of players.
     *
     * @param   e
     *          The event
     */
    @EventHandler
    public void onQuit(@NonNull PlayerQuitEvent e) {
        TABBridge.getInstance().submitTask(() -> {
            BukkitBridgePlayer p = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
            if (p == null) return;
            TABBridge.getInstance().removePlayer(p);
        });
    }

    /**
     * Processes world change event by sending a world change plugin message to the proxy.
     *
     * @param   e
     *          The event
     */
    @EventHandler
    public void onWorldChange(@NonNull PlayerChangedWorldEvent e) {
        BridgePlayer p = TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
        if (p == null) return;
        p.sendPluginMessage(new WorldChange(e.getPlayer().getWorld().getName()));
    }

    /**
     * Processes queued plugin messages for given player. This is used to ensure that plugin messages are not swallowed
     * if sent too early.
     *
     * @param   e
     *          The event
     */
    @EventHandler
    public void onChannelRegister(@NonNull PlayerRegisterChannelEvent e) {
        if (!e.getChannel().equals(TABBridge.CHANNEL_NAME)) return;
        BukkitBridgePlayer player = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
        if (player != null) {
            player.sendQueuedMessages();
        }
    }
}