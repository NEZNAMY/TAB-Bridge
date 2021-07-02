package me.neznamy.tab.bridge;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffectType;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.config.YamlConfigurationFile;
import net.milkbowl.vault.permission.Permission;

public class Main extends JavaPlugin implements PluginMessageListener {

	private final String CHANNEL_NAME = "tab:placeholders";
	
	private YamlConfigurationFile config;
	private boolean exceptionThrowing;
	
	public void onEnable() {
		long time = System.currentTimeMillis();
		Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL_NAME, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL_NAME);
		
		try {
			config = new YamlConfigurationFile(getClass().getClassLoader().getResourceAsStream("config.yml"), new File(getDataFolder(), "config.yml"));
			exceptionThrowing = config.getBoolean("throw-placeholderapi-exceptions", false);
			Bukkit.getConsoleSender().sendMessage("\u00a7a[TAB-BukkitBridge] Enabled in " + (System.currentTimeMillis()-time) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onDisable() {
		Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] bytes){
		if (!channel.equalsIgnoreCase(CHANNEL_NAME)) return;
		ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
		String subChannel = in.readUTF();
		if (subChannel.equalsIgnoreCase("Placeholder")){
			String identifier = in.readUTF();
			long start = System.nanoTime();
			String output;
			try {
				if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
					output = PlaceholderAPI.setPlaceholders(player, identifier);
				} else {
					output = "<PlaceholderAPI is not installed>";
				}
			} catch (Throwable e) {
				output = "<PlaceholderAPI ERROR>";
				if (exceptionThrowing) {
					System.out.println("[TAB-BukkitBridge] Placeholder " + identifier + " threw an exception when parsing for player " + player.getName());
					e.printStackTrace();
				}
			}
			long time = System.nanoTime() - start;
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Placeholder");
			out.writeUTF(identifier);
			out.writeUTF(output);
			out.writeLong(time);
			player.sendPluginMessage(this, CHANNEL_NAME, out.toByteArray());
		}
		if (subChannel.equalsIgnoreCase("Attribute")){
			String attribute = in.readUTF();
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Attribute");
			out.writeUTF(attribute);
			if (attribute.startsWith("hasPermission:")) {
				String permission = attribute.substring(attribute.indexOf(":") + 1);
				out.writeUTF(player.hasPermission(permission)+"");
				player.sendPluginMessage(this, CHANNEL_NAME, out.toByteArray());
				return;
			}
			if (attribute.equals("invisible")) {
				out.writeUTF(player.hasPotionEffect(PotionEffectType.INVISIBILITY)+"");
				player.sendPluginMessage(this, CHANNEL_NAME, out.toByteArray());
				return;
			}
			if (attribute.equals("disguised")) {
				out.writeUTF(isDisguised(player)+"");
				player.sendPluginMessage(this, CHANNEL_NAME, out.toByteArray());
				return;
			}
			if (attribute.equals("vanished")) {
				out.writeUTF(isVanished(player)+"");
				player.sendPluginMessage(this, CHANNEL_NAME, out.toByteArray());
				return;
			}
		}
		if (subChannel.equalsIgnoreCase("Group")) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Group");
			if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
				Permission perm = Bukkit.getServicesManager().getRegistration(Permission.class).getProvider();
				if (perm.getName().equals("SuperPerms")) {
					out.writeUTF("No permission plugin found");
				} else {
					out.writeUTF(perm.getPrimaryGroup(player));
				}
			} else {
				out.writeUTF("Vault not found");
			}
			player.sendPluginMessage(this, CHANNEL_NAME, out.toByteArray());
		}
	}
	
	private boolean isDisguised(Player p) {
		if (Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
			try {
				return (boolean) Class.forName("me.libraryaddict.disguise.DisguiseAPI").getMethod("isDisguised", Entity.class).invoke(null, p);
			} catch (Throwable e) {
				//java.lang.NoClassDefFoundError: Could not initialize class me.libraryaddict.disguise.DisguiseAPI
			}
		}
		return false;
	}
	public boolean isVanished(Player p) {
		try {
			if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
				Object essentials = Bukkit.getPluginManager().getPlugin("Essentials");
				Object user = essentials.getClass().getMethod("getUser", Player.class).invoke(essentials, p);
				boolean vanished = (boolean) user.getClass().getMethod("isVanished").invoke(user);
				if (vanished) return true;
			}
			if (p.hasMetadata("vanished") && !p.getMetadata("vanished").isEmpty()) {
				return p.getMetadata("vanished").get(0).asBoolean();
			}
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage("[TAB-BukkitBridge] Failed to get vanish status of " + p.getName());
			e.printStackTrace();
		}
		return false;
	}
}