package me.neznamy.tab.bridge.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import me.neznamy.tab.bridge.shared.DataBridge;

public class BukkitBridge extends JavaPlugin implements PluginMessageListener {

	public static final String CHANNEL_NAME = "tab:placeholders";

	private DataBridge data;
	
	public void onEnable() {
		Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL_NAME, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL_NAME);
		data = new BukkitDataBridge(this);
	}
	
	public void onDisable() {
		Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] bytes){
		if (!channel.equalsIgnoreCase(CHANNEL_NAME)) return;
		data.processPluginMessage(player, bytes);
	}
}