package me.neznamy.tab.bridge.bukkit;

import com.earth2me.essentials.Essentials;
import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.bukkit.features.BridgeTabExpansion;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unchecked")
public class DataBridge implements Listener {

	private final boolean placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
	private final Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
	private final Map<String, Placeholder> asyncPlaceholders = new ConcurrentHashMap<>();
	private final Map<String, Placeholder> syncPlaceholders = new ConcurrentHashMap<>();
	public final ExecutorService exe = Executors.newSingleThreadExecutor();
	private boolean groupForwarding;
	private boolean petFix;
	private final BridgeTabExpansion expansion = placeholderAPI ? new BridgeTabExpansion() : null;

	public DataBridge(Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> updatePlaceholders(syncPlaceholders.values(), false), 0, 1);
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> exe.submit(() -> updatePlaceholders(asyncPlaceholders.values(), false)), 0, 1);
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			for (BridgePlayer player : BukkitBridge.getInstance().getOnlinePlayers()) {
				player.setVanished(isVanished(player));
				player.setDisguised(isDisguised(player));
				player.setInvisible(player.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY));
				if (groupForwarding) {
					player.setGroup(getGroup(player));
				}
			}
		}, 20, 20);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		BridgePlayer p = BukkitBridge.getInstance().getPlayer(e.getPlayer());
		if (p == null) return;
		p.sendMessage("World", e.getPlayer().getWorld().getName());
	}
	
	public void processPluginMessage(Player player, byte[] bytes) throws ReflectiveOperationException {
		if (!player.isOnline()) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(BukkitBridge.getInstance(), () -> {
				try {
					processPluginMessage(player, bytes);
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}, 1);
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
		String subChannel = in.readUTF();
		if (subChannel.equals("PlayerJoin")) {
			int protocolVersion = in.readInt();
			groupForwarding = in.readBoolean();
			petFix = in.readBoolean();
			if (in.readBoolean() && expansion != null && !expansion.isRegistered()) {
				Bukkit.getScheduler().runTask(BukkitBridge.getInstance(), expansion::register);
			}
			int placeholderCount = in.readInt();
			for (int i=0; i<placeholderCount; i++) {
				registerPlaceholder(in.readUTF(), in.readInt());
			}
			BukkitBridge.getInstance().nametagx.readJoinInput(in);
			BridgePlayer bp = new BridgePlayer(player, protocolVersion);
			BukkitBridge.getInstance().nametagx.onJoin(bp);
			BukkitBridge.getInstance().addPlayer(bp);
			List<Object> args = Lists.newArrayList("PlayerJoinResponse", player.getWorld().getName());
			if (groupForwarding) args.add(getGroup(bp));
			Map<String, Object> placeholders = parsePlaceholders(bp);
			args.add(placeholders.size());
			for (Map.Entry<String, Object> placeholder : placeholders.entrySet()) {
				args.add(placeholder.getKey());
				if (placeholder.getKey().startsWith("%rel_")) {
					Map<String, String> perPlayer = (Map<String, String>) placeholder.getValue();
					args.add(perPlayer.size());
					for (Map.Entry<String, String> entry : perPlayer.entrySet()) {
						args.add(entry.getKey());
						args.add(entry.getValue());
					}
				} else {
					args.add(placeholder.getValue());
				}
			}
//			System.out.println(args);
			bp.sendMessage(args.toArray());
			for (Placeholder placeholder : asyncPlaceholders.values()) {
				if (placeholder instanceof RelationalPlaceholder) {
					RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
					for (BridgePlayer viewer : BukkitBridge.getInstance().getOnlinePlayers()) {
						if (pl.update(viewer, bp)) {
							viewer.sendMessage("Placeholder", pl.getIdentifier(), bp.getPlayer().getName(), pl.getLastValue(viewer, bp));
						}
					}
				}
			}
		}
		if (subChannel.equals("Placeholder")){
			registerPlaceholder(in.readUTF(), in.readInt());
		}
		if (subChannel.equals("Permission")){
			String permission = in.readUTF();
			BridgePlayer bp = BukkitBridge.getInstance().getPlayer(player);
			if (bp == null) {
				bp = new BridgePlayer(player, 0); //dummy instance to access sendMessage method
			}
			bp.sendMessage("Permission", permission, player.hasPermission(permission));
		}
		if (subChannel.equals("NameTagX")) {
			BridgePlayer pl = BukkitBridge.getInstance().getPlayer(player);
			if (pl == null) {
				Bukkit.getScheduler().runTaskLaterAsynchronously(BukkitBridge.getInstance(), () -> {
					try {
						processPluginMessage(player, bytes);
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}, 1);
			} else {
				BukkitBridge.getInstance().nametagx.readMessage(pl, in);
			}
		}
		if (subChannel.equals("Unload")) {
			BukkitBridge.getInstance().removePlayer(player);
		}
		if (subChannel.equals("Expansion")) {
			expansion.setValue(BukkitBridge.getInstance().getPlayer(player), in.readUTF(), in.readUTF());
		}
	}

	public boolean isPetFixEnabled() {
		return petFix;
	}

	public void submitTask(Runnable task) {
		exe.submit(() -> {
			try {
				task.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private String getGroup(BridgePlayer player) {
		if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
			RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
			if (rsp == null || rsp.getProvider().getName().equals("SuperPerms")) {
				return "No permission plugin found";
			} else {
				return rsp.getProvider().getPrimaryGroup(player.getPlayer());
			}
		} else {
			return "Vault not found";
		}
	}

	private boolean isVanished(BridgePlayer player) {
		if (essentials != null && ((Essentials)essentials).getUser(player.getPlayer()).isVanished()) return true;
		return !player.getPlayer().getMetadata("vanished").isEmpty() && player.getPlayer().getMetadata("vanished").get(0).asBoolean();
	}

	private boolean isDisguised(BridgePlayer player) {
		Entity entity = player.getPlayer();
		if (Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
			try {
				return (boolean) Class.forName("me.libraryaddict.disguise.DisguiseAPI").getMethod("isDisguised", Entity.class).invoke(null, entity);
			} catch (Throwable e) {
				//java.lang.NoClassDefFoundError: Could not initialize class me.libraryaddict.disguise.DisguiseAPI
			}
		}
		return false;
	}

	public void registerPlaceholder(String identifier, int refresh) {
//		System.out.println("register placeholder " + identifier);
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
				placeholder = new ServerPlaceholder(identifier, refresh, () ->
						placeholderAPI ? PlaceholderAPI.setPlaceholders(null, finalIdentifier) : "<PlaceholderAPI is not installed>");
			} else if (identifier.startsWith("%rel_")) {
				placeholder = new RelationalPlaceholder(identifier, refresh, (viewer, target) ->
						placeholderAPI ? PlaceholderAPI.setRelationalPlaceholders(viewer.getPlayer(), target.getPlayer(), finalIdentifier) : "<PlaceholderAPI is not installed>");
			} else {
				placeholder = new PlayerPlaceholder(identifier, refresh, p ->
						placeholderAPI ? PlaceholderAPI.setPlaceholders(p.getPlayer(), finalIdentifier) : "<PlaceholderAPI is not installed>");
			}
			if (sync) {
				syncPlaceholders.put(identifier, placeholder);
			} else {
				asyncPlaceholders.put(identifier, placeholder);
			}
		}
		if (identifier.startsWith("%sync:")) {
			Bukkit.getScheduler().runTask(BukkitBridge.getInstance(), () -> updatePlaceholders(Collections.singletonList(syncPlaceholders.get(identifier)), true));
		} else {
			updatePlaceholders(Collections.singletonList(asyncPlaceholders.get(identifier)), true);
		}
	}

	public Map<String, Object> parsePlaceholders(BridgePlayer player) {
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
				for (BridgePlayer target : BukkitBridge.getInstance().getOnlinePlayers()) {
					relMap.put(target.getPlayer().getName(), ((RelationalPlaceholder)placeholder).getLastValue(player, target));
				}
			}
		}
		return outputs;
	}

	private void updatePlaceholders(Collection<Placeholder> placeholders, boolean force) {
		for (Placeholder placeholder : placeholders) {
			if (!placeholder.isInPeriod() && !force) continue;
			if (placeholder instanceof ServerPlaceholder) {
				ServerPlaceholder pl = (ServerPlaceholder) placeholder;
				if (pl.update()) {
					for (BridgePlayer p : BukkitBridge.getInstance().getOnlinePlayers()) {
						p.sendMessage("Placeholder", pl.getIdentifier(), pl.getLastValue());
					}
				}
			}
			if (placeholder instanceof PlayerPlaceholder) {
				PlayerPlaceholder pl = (PlayerPlaceholder) placeholder;
				for (BridgePlayer p : BukkitBridge.getInstance().getOnlinePlayers()) {
					if (pl.update(p)) {
						p.sendMessage("Placeholder", pl.getIdentifier(), pl.getLastValue(p));
					}
				}
			}
			if (placeholder instanceof RelationalPlaceholder) {
				RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
				for (BridgePlayer viewer : BukkitBridge.getInstance().getOnlinePlayers()) {
					for (BridgePlayer target : BukkitBridge.getInstance().getOnlinePlayers()) {
						if (pl.update(viewer, target)) {
							viewer.sendMessage("Placeholder", pl.getIdentifier(), target.getPlayer().getName(), pl.getLastValue(viewer, target));
						}
					}
				}
			}
		}
	}
}
