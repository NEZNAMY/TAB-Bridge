package me.neznamy.tab.bridge.shared;

import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class DataBridge {

	private final Map<Object, List<byte[]>> messageQueue = new WeakHashMap<>();
	private final Map<String, Placeholder> asyncPlaceholders = new ConcurrentHashMap<>();
	private final Map<String, Placeholder> syncPlaceholders = new ConcurrentHashMap<>();
	private boolean groupForwarding;

	public void startTasks() {
		TABBridge.getInstance().getPlatform().scheduleSyncRepeatingTask(() -> updatePlaceholders(syncPlaceholders.values(), false), 1);
		TABBridge.getInstance().getPlatform().runTaskTimerAsynchronously(() -> updatePlaceholders(asyncPlaceholders.values(), false), 1);
		TABBridge.getInstance().getPlatform().runTaskTimerAsynchronously(() -> {
			for (BridgePlayer player : TABBridge.getInstance().getOnlinePlayers()) {
				player.setVanished(TABBridge.getInstance().getPlatform().isVanished(player));
				player.setDisguised(TABBridge.getInstance().getPlatform().isDisguised(player));
				player.setInvisible(TABBridge.getInstance().getPlatform().isInvisible(player));
				if (groupForwarding) {
					player.setGroup(TABBridge.getInstance().getPlatform().getGroup(player));
				}
			}
		}, 20);
	}
	@SuppressWarnings("UnstableApiUsage")
	public void processPluginMessage(Object player, byte[] bytes, boolean retry) {
		if (!TABBridge.getInstance().getPlatform().isOnline(player)) {
			messageQueue.computeIfAbsent(player, p -> new ArrayList<>()).add(bytes);
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
		String subChannel = in.readUTF();
		if (subChannel.equals("PlayerJoin")) {
			// Read join input
			int protocolVersion = in.readInt();
			groupForwarding = in.readBoolean();
			if (in.readBoolean() && TABBridge.getInstance().getExpansion() != null && !TABBridge.getInstance().getExpansion().isRegistered()) {
				TABBridge.getInstance().getPlatform().registerExpansion();
			}
			int placeholderCount = in.readInt();
			for (int i=0; i<placeholderCount; i++) {
				registerPlaceholder(in.readUTF(), in.readInt());
			}
			BridgePlayer bp = TABBridge.getInstance().getPlatform().newPlayer(player, protocolVersion);
			TABBridge.getInstance().getPlatform().readUnlimitedNametagJoin(bp, in);
			TABBridge.getInstance().addPlayer(bp);

			// Send response
			List<Object> args = Lists.newArrayList("PlayerJoinResponse", bp.getWorld());
			if (groupForwarding) args.add(TABBridge.getInstance().getPlatform().getGroup(bp));
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
			bp.sendMessage(args.toArray());
			for (Placeholder placeholder : asyncPlaceholders.values()) {
				if (placeholder instanceof RelationalPlaceholder) {
					RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
					for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
						if (pl.update(viewer, bp)) {
							viewer.sendMessage("Placeholder", pl.getIdentifier(), bp.getName(), pl.getLastValue(viewer, bp));
						}
					}
				}
			}
			processQueue(player);
			return;
		}
		BridgePlayer pl = TABBridge.getInstance().getPlayer(TABBridge.getInstance().getPlatform().getUniqueId(player));
		if (pl == null) {
			messageQueue.computeIfAbsent(player, p -> new ArrayList<>()).add(bytes);
			return;
		}
		if (subChannel.equals("Placeholder")){
			registerPlaceholder(in.readUTF(), in.readInt());
		}
		if (subChannel.equals("Permission")){
			String permission = in.readUTF();
			pl.sendMessage("Permission", permission, pl.hasPermission(permission));
		}
		if (subChannel.equals("NameTagX")) {
			TABBridge.getInstance().getPlatform().readUnlimitedNametagMessage(pl, in);
		}
		if (subChannel.equals("Unload") && !retry) {
			TABBridge.getInstance().removePlayer(pl);
		}
		if (subChannel.equals("Expansion")) {
			TABBridge.getInstance().getPlatform().setExpansionValue(player, in.readUTF(), in.readUTF());
		}
		if (subChannel.equals("PacketPlayOutScoreboardDisplayObjective")) {
			pl.getScoreboard().setDisplaySlot(Scoreboard.DisplaySlot.values()[in.readInt()], in.readUTF());
		}
		if (subChannel.equals("PacketPlayOutScoreboardObjective")) {
			String objective = in.readUTF();
			int action = in.readInt();
			String display = null;
			String displayComponent = null;
			int renderType = 0;
			if (action == 0 || action == 2) {
				display = in.readUTF();
				displayComponent = in.readUTF();
				renderType = in.readInt();
			}
			if (action == 0) {
				pl.getScoreboard().registerObjective(objective, display, displayComponent, renderType == 1);
			} else if (action == 1) {
				pl.getScoreboard().unregisterObjective(objective);
			} else if (action == 2) {
				pl.getScoreboard().updateObjective(objective, display, displayComponent, renderType == 1);
			}
		}
		if (subChannel.equals("PacketPlayOutScoreboardScore")) {
			String objective = in.readUTF();
			int action = in.readInt();
			String playerName = in.readUTF();
			int score = in.readInt();
			if (action == 0) {
				pl.getScoreboard().setScore(objective, playerName, score);
			} else {
				pl.getScoreboard().removeScore(objective, playerName);
			}
		}
		if (subChannel.equals("PacketPlayOutScoreboardTeam")) {
			String name = in.readUTF();
			int action = in.readInt();
			int playerCount = in.readInt();
			List<String> players = new ArrayList<>();
			for (int i=0; i<playerCount; i++) {
				players.add(in.readUTF());
			}
			String prefix = null;
			String prefixComponent = null;
			String suffix = null;
			String suffixComponent = null;
			int options = 0;
			String visibility = null;
			String collision = null;
			int color = 0;
			if (action == 0 || action == 2) {
				prefix = in.readUTF();
				prefixComponent = in.readUTF();
				suffix = in.readUTF();
				suffixComponent = in.readUTF();
				options = in.readInt();
				visibility = in.readUTF();
				collision = in.readUTF();
				color = in.readInt();
			}
			if (action == 0) {
				pl.getScoreboard().registerTeam(name, prefix, prefixComponent, suffix, suffixComponent, visibility, collision, players, options, color);
			} else if (action == 1) {
				pl.getScoreboard().unregisterTeam(name);
			} else if (action == 2) {
				pl.getScoreboard().updateTeam(name, prefix, prefixComponent, suffix, suffixComponent, visibility, collision, options, color);
			}
		}
	}

	public void processQueue(Object player) {
		List<byte[]> list = new ArrayList<>(messageQueue.computeIfAbsent(player, p -> new ArrayList<>()));
		messageQueue.remove(player);
		list.forEach(msg -> processPluginMessage(player, msg, true));
	}

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
			Placeholder placeholder = TABBridge.getInstance().getPlatform().createPlaceholder(identifier, finalIdentifier, refresh);
			if (sync) {
				syncPlaceholders.put(identifier, placeholder);
			} else {
				asyncPlaceholders.put(identifier, placeholder);
			}
		}
		if (identifier.startsWith("%sync:")) {
			TABBridge.getInstance().getPlatform().runTask(() -> updatePlaceholders(Collections.singletonList(syncPlaceholders.get(identifier)), true));
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
				for (BridgePlayer target : TABBridge.getInstance().getOnlinePlayers()) {
					relMap.put(target.getName(), ((RelationalPlaceholder)placeholder).getLastValue(player, target));
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
					for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
						p.sendMessage("Placeholder", pl.getIdentifier(), pl.getLastValue());
					}
				}
			}
			if (placeholder instanceof PlayerPlaceholder) {
				PlayerPlaceholder pl = (PlayerPlaceholder) placeholder;
				for (BridgePlayer p : TABBridge.getInstance().getOnlinePlayers()) {
					if (pl.update(p)) {
						p.sendMessage("Placeholder", pl.getIdentifier(), pl.getLastValue(p));
					}
				}
			}
			if (placeholder instanceof RelationalPlaceholder) {
				RelationalPlaceholder pl = (RelationalPlaceholder) placeholder;
				for (BridgePlayer viewer : TABBridge.getInstance().getOnlinePlayers()) {
					for (BridgePlayer target : TABBridge.getInstance().getOnlinePlayers()) {
						if (pl.update(viewer, target)) {
							viewer.sendMessage("Placeholder", pl.getIdentifier(), target.getName(), pl.getLastValue(viewer, target));
						}
					}
				}
			}
		}
	}
}
