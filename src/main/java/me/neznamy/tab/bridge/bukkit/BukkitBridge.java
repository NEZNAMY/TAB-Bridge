package me.neznamy.tab.bridge.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import me.neznamy.tab.bridge.shared.DataBridge;
import org.jetbrains.annotations.NotNull;

public class BukkitBridge extends JavaPlugin implements PluginMessageListener {

	public static final String CHANNEL_NAME = "tab:bridge-1";

	private DataBridge data;
	
	public void onEnable() {
		Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL_NAME, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL_NAME);
		data = new BukkitDataBridge(this);
	}
	
	public void onDisable() {
		Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
		HandlerList.unregisterAll(this);
		data.exe.shutdownNow();
	}
	
	@Override
	public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes){
		if (!channel.equals(CHANNEL_NAME)) return;
		data.exe.submit(() -> {
			try {
				data.processPluginMessage(player, bytes, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}