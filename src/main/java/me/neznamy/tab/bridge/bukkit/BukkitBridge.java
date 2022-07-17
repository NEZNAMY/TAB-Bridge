package me.neznamy.tab.bridge.bukkit;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.neznamy.tab.bridge.bukkit.features.BridgeTabExpansion;
import me.neznamy.tab.bridge.bukkit.features.PetFix;
import me.neznamy.tab.bridge.bukkit.features.unlimitedtags.BridgeNameTagX;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.DataBridge;
import me.neznamy.tab.bridge.shared.TABBridge;
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

	private static BukkitBridge instance;
	private PetFix petFix;
	public BridgeNameTagX nametagx;
	
	public void onEnable() {
		instance = this;
		TABBridge.setInstance(new TABBridge(new BukkitPlatform(this), new DataBridge(), Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? new BridgeTabExpansion() : null));
		try {
			NMSStorage.setInstance(new NMSStorage());
			if (NMSStorage.getInstance().getMinorVersion() >= 9) petFix = new PetFix();
		} catch (ReflectiveOperationException e) {
			Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB-Bridge] Server version is not compatible, disabling advanced features");
		}
		nametagx = new BridgeNameTagX(this);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, TABBridge.CHANNEL_NAME, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, TABBridge.CHANNEL_NAME);
		Bukkit.getPluginManager().registerEvents(this, this);
		for (Player p : Bukkit.getOnlinePlayers()) {
			TABBridge.getInstance().getPlatform().inject(p, new CustomChannelDuplexHandler(p));
		}
		TABBridge.getInstance().getDataBridge().startTasks();
	}

	public static BukkitBridge getInstance() {
		return instance;
	}
	
	public void onDisable() {
		Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
		HandlerList.unregisterAll((Plugin)this);
		Bukkit.getScheduler().cancelTasks(this);
		nametagx.unload();
		TABBridge.getInstance().shutdownExecutor();
		for (Player p : Bukkit.getOnlinePlayers()) {
			TABBridge.getInstance().getPlatform().uninject(p);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		TABBridge.getInstance().getPlatform().inject(e.getPlayer(), new CustomChannelDuplexHandler(e.getPlayer()));
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
		p.sendMessage("World", e.getPlayer().getWorld().getName());
	}

	@Override
	public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes){
		if (!channel.equals(TABBridge.CHANNEL_NAME)) return;
		TABBridge.getInstance().submitTask(
				() -> TABBridge.getInstance().getDataBridge().processPluginMessage(player, bytes, false));
	}

	public class CustomChannelDuplexHandler extends ChannelDuplexHandler {

		private final Player player;

		public CustomChannelDuplexHandler(Player player) {
			this.player = player;
		}

		@Override
		public void channelRead(ChannelHandlerContext context, Object packet) {
			try {
				if (TABBridge.getInstance().getDataBridge().isPetFixEnabled() && petFix != null && petFix.onPacketReceive(player, packet)) return;
				BukkitBridgePlayer p = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(player.getUniqueId());
				if (p != null && nametagx.isEnabled()) {
					nametagx.getPacketListener().onPacketReceive(p, packet);
				}
				super.channelRead(context, packet);
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		@Override
		public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) {
			try {
				if (TABBridge.getInstance().getDataBridge().isPetFixEnabled() && petFix != null) petFix.onPacketSend(packet);
				BukkitBridgePlayer p = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(player.getUniqueId());
				if (p != null && nametagx.isEnabled()) {
					nametagx.getPacketListener().onPacketSend(p, packet);
				}
				super.write(context, packet, channelPromise);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}