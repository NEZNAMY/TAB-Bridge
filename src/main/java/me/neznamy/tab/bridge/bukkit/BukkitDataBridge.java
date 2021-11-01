package me.neznamy.tab.bridge.bukkit;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.shared.DataBridge;
import net.milkbowl.vault.permission.Permission;

public class BukkitDataBridge extends DataBridge implements Listener {

	private Plugin plugin;
	
	public BukkitDataBridge(Plugin plugin) {
		this.plugin = plugin;
		loadConfig();
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Attribute");
			out.writeUTF("world");
			out.writeUTF(e.getPlayer().getWorld().getName());
			sendPluginMessage(e.getPlayer(), out);
		}, 2);
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Attribute");
		out.writeUTF("world");
		out.writeUTF(e.getPlayer().getWorld().getName());
		sendPluginMessage(e.getPlayer(), out);
	}
	
	@Override
	public boolean isDisguised(Object player) {
		if (Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
			try {
				return (boolean) Class.forName("me.libraryaddict.disguise.DisguiseAPI").getMethod("isDisguised", Entity.class).invoke(null, player);
			} catch (Throwable e) {
				//java.lang.NoClassDefFoundError: Could not initialize class me.libraryaddict.disguise.DisguiseAPI
			}
		}
		return false;
	}

	@Override
	public boolean isVanished(Object player) {
		Player p = (Player) player;
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
			Bukkit.getConsoleSender().sendMessage("[TAB-Bridge] Failed to get vanish status of " + p.getName());
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void sendPluginMessage(Object player, ByteArrayDataOutput message) {
		((Player)player).sendPluginMessage(plugin, BukkitBridge.CHANNEL_NAME, message.toByteArray());
	}

	@Override
	public String parsePlaceholder(Object player, String placeholder) {
		try {
			if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
				return PlaceholderAPI.setPlaceholders((Player) player, placeholder);
			} else {
				return "<PlaceholderAPI is not installed>";
			}
		} catch (Throwable e) {
			if (exceptionThrowing) {
				System.out.println("[TAB-Bridge] Placeholder " + placeholder + " threw an exception when parsing for player " + ((Player) player).getName());
				e.printStackTrace();
			}
			return "<PlaceholderAPI ERROR>";
		}
	}

	@Override
	public boolean hasPermission(Object player, String permission) {
		return ((Player) player).hasPermission(permission);
	}

	@Override
	public String getWorld(Object player) {
		return ((Player) player).getWorld().getName();
	}

	@Override
	public File getDataFolder() {
		return plugin.getDataFolder();
	}

	@Override
	public String getGroup(Object player) {
		if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
			Permission perm = Bukkit.getServicesManager().getRegistration(Permission.class).getProvider();
			if (perm.getName().equals("SuperPerms")) {
				return "No permission plugin found";
			} else {
				return perm.getPrimaryGroup((Player) player);
			}
		} else {
			return "Vault not found";
		}
	}

	@Override
	public boolean hasInvisibilityPotion(Object player) {
		return ((Player)player).hasPotionEffect(PotionEffectType.INVISIBILITY);
	}
}