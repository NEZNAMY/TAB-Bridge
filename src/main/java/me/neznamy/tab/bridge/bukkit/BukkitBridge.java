package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import me.neznamy.tab.bridge.bukkit.features.BridgeTabExpansion;
import me.neznamy.tab.bridge.bukkit.features.unlimitedtags.BridgeNameTagX;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.bukkit.platform.BukkitPlatform;
import me.neznamy.tab.bridge.bukkit.platform.FoliaPlatform;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.Platform;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
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

    @Getter private static final String serverPackage = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    @Getter private static final int minorVersion = Integer.parseInt(serverPackage.split("_")[1]);
    @Getter private static final boolean is1_19_3Plus = ReflectionUtils.classExists("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
    @Getter private static final boolean is1_19_4Plus = is1_19_3Plus && !serverPackage.equals("v1_19_R2");
    @Getter private static final boolean is1_20_2Plus = minorVersion >= 20 && !serverPackage.equals("v1_20_R1");

    @Getter private static BukkitBridge instance;
    public BridgeNameTagX nametagx;
    
    public void onEnable() {
        instance = this;
        boolean folia = ReflectionUtils.classExists("io.papermc.paper.threadedregions.RegionizedServer");
        Platform platform = folia ? new FoliaPlatform(this) : new BukkitPlatform(this);
        BridgeTabExpansion expansion = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? new BridgeTabExpansion() : null;
        TABBridge.setInstance(new TABBridge(platform, expansion));
        if (expansion != null) expansion.register();
        try {
            NMSStorage.setInstance(new NMSStorage());
        } catch (ReflectiveOperationException e) {
            Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB-Bridge] Server version is not compatible, disabling advanced features");
        }
        nametagx = new BridgeNameTagX(this);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, TABBridge.CHANNEL_NAME, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, TABBridge.CHANNEL_NAME);
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Player p : Bukkit.getOnlinePlayers()) {
            TABBridge.getInstance().getPlatform().inject(p, new BridgeChannelDuplexHandler(p));
        }
        TABBridge.getInstance().getDataBridge().startTasks();
        new Metrics(this, 20810);
    }

    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        HandlerList.unregisterAll((Plugin)this);
        TABBridge.getInstance().getPlatform().cancelTasks();
        nametagx.unload();
        TABBridge.getInstance().shutdownExecutor();
        TabExpansion expansion = TABBridge.getInstance().getExpansion();
        if (expansion != null) expansion.unregister();
        for (Player p : Bukkit.getOnlinePlayers()) {
            TABBridge.getInstance().getPlatform().uninject(p);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        TABBridge.getInstance().getPlatform().inject(e.getPlayer(), new BridgeChannelDuplexHandler(e.getPlayer()));
        TABBridge.getInstance().submitTask(() -> TABBridge.getInstance().getDataBridge().processQueue(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        BukkitBridgePlayer p = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
        if (p == null) return;
        if (NMSStorage.getInstance() != null) nametagx.onQuit(p);
        TABBridge.getInstance().removePlayer(p);
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