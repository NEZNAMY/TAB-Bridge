package me.neznamy.tab.bridge.bukkit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.neznamy.tab.bridge.bukkit.features.PetFix;
import me.neznamy.tab.bridge.bukkit.features.unlimitedtags.BridgeNameTagX;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitBridge extends JavaPlugin implements PluginMessageListener, Listener {

	public static final String CHANNEL_NAME = "tab:bridge-2";
	public static BukkitBridge instance;

	private DataBridge data;
	private PetFix petFix;
	public BridgeNameTagX nametagx;

	private final Map<Player, BridgePlayer> players = new ConcurrentHashMap<>();
	
	public void onEnable() {
		instance = this;
		try {
			NMSStorage.setInstance(new NMSStorage());
			if (NMSStorage.getInstance().getMinorVersion() >= 9) petFix = new PetFix();
		} catch (ReflectiveOperationException e) {
			Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB-Bridge] Server version is not compatible, disabling advanced features");
		}
		nametagx = new BridgeNameTagX();
		Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL_NAME, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL_NAME);
		Bukkit.getPluginManager().registerEvents(this, this);
		data = new DataBridge(this);
		for (Player p : Bukkit.getOnlinePlayers()) {
			inject(p);
		}
	}
	
	public void onDisable() {
		Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
		HandlerList.unregisterAll((Plugin)this);
		Bukkit.getScheduler().cancelTasks(this);
		data.exe.shutdownNow();
		for (Player p : Bukkit.getOnlinePlayers()) {
			uninject(p);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		inject(e.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		if (!players.containsKey(e.getPlayer())) return;
		if (NMSStorage.getInstance() != null) nametagx.onQuit(players.get(e.getPlayer()));
		players.remove(e.getPlayer());
	}
	
	@Override
	public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes){
		if (!channel.equals(CHANNEL_NAME)) return;
		data.submitTask(() -> {
			try {
				data.processPluginMessage(player, bytes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public Collection<BridgePlayer> getOnlinePlayers() {
		return players.values();
	}

	public void addPlayer(BridgePlayer player) {
		players.put(player.getPlayer(), player);
	}

	public void removePlayer(Player player) {
		players.remove(player);
	}

	public BridgePlayer getPlayer(Player player) {
		return players.get(player);
	}

	public static BukkitBridge getInstance() {
		return instance;
	}

	public DataBridge getDataBridge() {
		return data;
	}

	public void inject(Player player) {
		Channel channel = getChannel(player);
		if (channel == null) return;
		if (!channel.pipeline().names().contains("packet_handler")) {
			//fake player or waterfall bug
			return;
		}
		uninject(player);
		try {
			channel.pipeline().addBefore("packet_handler", "TAB-Bridge", new CustomChannelDuplexHandler(player));
		} catch (NoSuchElementException | IllegalArgumentException ignored) {
		}
	}

	public void uninject(Player player) {
		Channel channel = getChannel(player);
		if (channel == null) return;
		try {
			if (channel.pipeline().names().contains("TAB-Bridge")) channel.pipeline().remove("TAB-Bridge");
		} catch (NoSuchElementException ignored) {
		}
	}

	private Channel getChannel(Player player) {
		if (NMSStorage.getInstance() == null) return null;
		try {
			Object handle = NMSStorage.getInstance().getHandle.invoke(player);
			Object playerConnection = NMSStorage.getInstance().PLAYER_CONNECTION.get(handle);
			return (Channel) NMSStorage.getInstance().CHANNEL.get(NMSStorage.getInstance().NETWORK_MANAGER.get(playerConnection));
		} catch (ReflectiveOperationException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public class CustomChannelDuplexHandler extends ChannelDuplexHandler {

		private final Player player;

		public CustomChannelDuplexHandler(Player player) {
			this.player = player;
		}

		@Override
		public void channelRead(ChannelHandlerContext context, Object packet) {
			try {
				if (data.isPetFixEnabled() && petFix != null && petFix.onPacketReceive(player, packet)) return;
				if (players.containsKey(player) && nametagx.isEnabled()) {
					nametagx.getPacketListener().onPacketReceive(players.get(player), packet);
				}
				super.channelRead(context, packet);
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		@Override
		public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) {
			try {
				if (data.isPetFixEnabled() && petFix != null) petFix.onPacketSend(player, packet);
				if (players.containsKey(player) && nametagx.isEnabled()) {
					nametagx.getPacketListener().onPacketSend(players.get(player), packet);
				}
				super.write(context, packet, channelPromise);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}