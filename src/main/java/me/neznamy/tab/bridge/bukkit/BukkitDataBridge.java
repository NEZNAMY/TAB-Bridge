package me.neznamy.tab.bridge.bukkit;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.earth2me.essentials.Essentials;
import com.google.common.collect.Lists;
import me.neznamy.tab.bridge.shared.config.YamlConfigurationFile;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;
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

import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.shared.DataBridge;
import net.milkbowl.vault.permission.Permission;

@SuppressWarnings("unchecked")
public class BukkitDataBridge extends DataBridge implements Listener {

	private final Plugin plugin;
	private final Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	private final boolean placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
	private final Map<String, Placeholder> asyncPlaceholders = new ConcurrentHashMap<>();
	private final Map<String, Placeholder> syncPlaceholders = new ConcurrentHashMap<>();
	
	public BukkitDataBridge(Plugin plugin) {
		this.plugin = plugin;
		try {
			YamlConfigurationFile config = new YamlConfigurationFile(getClass().getClassLoader().getResourceAsStream("config.yml"), new File(getDataFolder(), "config.yml"));
			exceptionThrowing = config.getBoolean("throw-placeholderapi-exceptions", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Bukkit.getPluginManager().registerEvents(this, plugin);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> updatePlaceholders(syncPlaceholders.values(), false), 0, 1);
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> updatePlaceholders(asyncPlaceholders.values(), false), 0, 1);
		for (Player p : Bukkit.getOnlinePlayers()) {
			updatePlaceholders(p, syncPlaceholders.values());
			exe.submit(() -> {
				updatePlaceholders(p, asyncPlaceholders.values());
				loadedPlayers.add(p);
			});
		}
	}

	private void updatePlaceholders(Collection<Placeholder> placeholders, boolean force) {
		for (Placeholder placeholder : placeholders) {
			if (!placeholder.isInPeriod() && !force) continue;
			if (placeholder instanceof ServerPlaceholder) {
				ServerPlaceholder pl = (ServerPlaceholder) placeholder;
				if (pl.update()) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						sendMessage(p, "Placeholder", pl.getIdentifier(), pl.getLastValue());
					}
				}
			}
			if (placeholder instanceof PlayerPlaceholder) {
				PlayerPlaceholder pl = (PlayerPlaceholder) placeholder;
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (pl.update(p)) {
						sendMessage(p, "Placeholder", pl.getIdentifier(), pl.getLastValue(p));
					}
				}
			}
			if (placeholder instanceof RelationalPlaceholder) {
				RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
				for (Player viewer : Bukkit.getOnlinePlayers()) {
					for (Player target : Bukkit.getOnlinePlayers()) {
						if (pl.update(viewer, target)) {
							sendMessage(viewer, "Placeholder", pl.getIdentifier(), target.getName(), pl.getLastValue(viewer, target));
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		updatePlaceholders(e.getPlayer(), syncPlaceholders.values());
		exe.submit(() -> {
			updatePlaceholders(e.getPlayer(), asyncPlaceholders.values());
			loadedPlayers.add(e.getPlayer());
		});
	}

	private void updatePlaceholders(Player player, Collection<Placeholder> placeholders) {
		for (Placeholder placeholder : placeholders) {
			if (placeholder instanceof PlayerPlaceholder) {
				((PlayerPlaceholder)placeholder).update(player);
			}
			if (placeholder instanceof RelationalPlaceholder) {
				for (Player target : Bukkit.getOnlinePlayers()) {
					((RelationalPlaceholder)placeholder).update(player, target);
					((RelationalPlaceholder)placeholder).update(target, player);
					sendMessage(target, "Placeholder", placeholder.getIdentifier(), player.getName(), ((RelationalPlaceholder)placeholder).getLastValue(target, player));
				}
			}
		}
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		sendMessage(e.getPlayer(), "World", e.getPlayer().getWorld().getName());
	}
	
	@Override
	public boolean isDisguised(Object player) {
		Entity entity = (Entity) player;
		if (Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
			try {
				return (boolean) Class.forName("me.libraryaddict.disguise.DisguiseAPI").getMethod("isDisguised", Entity.class).invoke(null, entity);
			} catch (Throwable e) {
				//java.lang.NoClassDefFoundError: Could not initialize class me.libraryaddict.disguise.DisguiseAPI
			}
		}
		return false;
	}

	@Override
	public boolean isVanished(Object player) {
		Player p = (Player) player;
		if (essentials != null && essentials.getUser(p).isVanished()) return true;
		return !p.getMetadata("vanished").isEmpty() && p.getMetadata("vanished").get(0).asBoolean();
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
				Bukkit.getConsoleSender().sendMessage("[TAB-Bridge] Placeholder " + placeholder + " threw an exception when parsing");
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

	@Override
	public boolean isOnline(Object player) {
		return ((Player)player).isOnline();
	}

	@Override
	public Collection getOnlinePlayers() {
		return Bukkit.getOnlinePlayers();
	}

	@Override
	public void registerPlaceholder(String identifier, int refresh) {
		if (syncPlaceholders.containsKey(identifier)) {
			syncPlaceholders.get(identifier).setRefresh(refresh);
		} else if (asyncPlaceholders.containsKey(identifier)) {
			asyncPlaceholders.get(identifier).setRefresh(refresh);
		} else {
			boolean sync = false;
			String finalIdentifier; //forwarded identifier without sync: prefix
			if (identifier.startsWith("%sync:")) {
				finalIdentifier = "%" + identifier.substring(6);
				sync = true;
			} else {
				finalIdentifier = identifier;
			}
			Placeholder placeholder;
			if (identifier.startsWith("%server_")) {
				placeholder = new ServerPlaceholder(identifier, refresh, () -> parsePlaceholder(null, finalIdentifier));
			} else if (identifier.startsWith("%rel_")) {
				placeholder = new RelationalPlaceholder(identifier, refresh, (viewer, target) ->
						placeholderAPI ? PlaceholderAPI.setRelationalPlaceholders((Player) viewer, (Player) target, finalIdentifier) : finalIdentifier);
			} else {
				placeholder = new PlayerPlaceholder(identifier, refresh, p -> parsePlaceholder(p, finalIdentifier));
			}
			if (sync) {
				syncPlaceholders.put(identifier, placeholder);
			} else {
				asyncPlaceholders.put(identifier, placeholder);
			}
			updatePlaceholders(Collections.singletonList(placeholder), true);
		}
	}

	@Override
	public Map<String, Object> parsePlaceholders(Object player) {
		Map<String, Object> outputs = new LinkedHashMap<>();
		List<Placeholder> allPlaceholders = Lists.newArrayList(asyncPlaceholders.values());
		allPlaceholders.addAll(syncPlaceholders.values());
		for (Placeholder placeholder : allPlaceholders) {
			if (placeholder instanceof ServerPlaceholder) {
				outputs.put(placeholder.getIdentifier(), ((ServerPlaceholder) placeholder).getLastValue());
			}
			if (placeholder instanceof PlayerPlaceholder) {
				outputs.put(placeholder.getIdentifier(), ((PlayerPlaceholder) placeholder).getLastValue(player));
			}
			if (placeholder instanceof RelationalPlaceholder) {
				Map<String, String> relMap = (Map<String, String>) outputs.computeIfAbsent(placeholder.getIdentifier(), p -> new HashMap<>());
				for (Player target : Bukkit.getOnlinePlayers()) {
					relMap.put(target.getName(), ((RelationalPlaceholder)placeholder).getLastValue(player, target));
				}
			}
		}
		return outputs;
	}
}