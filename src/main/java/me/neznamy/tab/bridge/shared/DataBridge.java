package me.neznamy.tab.bridge.shared;

import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unchecked")
public abstract class DataBridge {

	public final ExecutorService exe = Executors.newCachedThreadPool();
	protected boolean exceptionThrowing;
	private boolean groupForwarding;
	private boolean petFix;
	protected final Set<Object> loadedPlayers = Collections.newSetFromMap(new WeakHashMap<>());

	private final Map<Object, Boolean> vanishMap = new WeakHashMap<>();
	private final Map<Object, Boolean> disguiseMap = new WeakHashMap<>();
	private final Map<Object, Boolean> invisibleMap = new WeakHashMap<>();
	private final Map<Object, String> groupMap = new WeakHashMap<>();
	
	protected DataBridge() {
		exe.submit(() -> {
			while (true) {
				try {
					Thread.sleep(1000);
					for (Object player : getOnlinePlayers()) {
						boolean vanished = isVanished(player);
						if (!vanishMap.getOrDefault(player, false).equals(vanished)) {
							vanishMap.put(player, vanished);
							sendMessage(player, "Vanished", vanished);
						}

						boolean disguised = isDisguised(player);
						if (!disguiseMap.getOrDefault(player, false).equals(disguised)) {
							disguiseMap.put(player, disguised);
							sendMessage(player, "Disguised", disguised);
						}

						boolean invisible = hasInvisibilityPotion(player);
						if (!invisibleMap.getOrDefault(player, false).equals(invisible)) {
							invisibleMap.put(player, invisible);
							sendMessage(player, "Invisible", invisible);
						}

						if (groupForwarding) {
							String group = getGroup(player);
							if (!groupMap.getOrDefault(player, "NONE").equals(group)) {
								groupMap.put(player, group);
								sendMessage(player, "Group", group);
							}
						}
					}
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		});
	}
	
	public abstract File getDataFolder();
	
	public abstract boolean isDisguised(Object player);

	public abstract boolean isVanished(Object player);
	
	public abstract void sendPluginMessage(Object player, ByteArrayDataOutput message);
	
	public abstract String parsePlaceholder(Object player, String placeholder);
	
	public abstract boolean hasPermission(Object player, String permission);
	
	public abstract String getWorld(Object player);
	
	public abstract String getGroup(Object player);
	
	public abstract boolean hasInvisibilityPotion(Object player);

	public abstract boolean isOnline(Object player);

	public abstract Collection getOnlinePlayers();

	public abstract void registerPlaceholder(String identifier, int refresh);

	public abstract Map<String, Object> parsePlaceholders(Object player);
	
	public void processPluginMessage(Object player, byte[] bytes, int retryLevel) {
		if (retryLevel == 4) return;
		if (!isOnline(player) || !loadedPlayers.contains(player)) {
			try {
				Thread.sleep(50);
				processPluginMessage(player, bytes, retryLevel+1);
			} catch (InterruptedException ignored) {
			}
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
		String subChannel = in.readUTF();
		if (subChannel.equals("PlayerJoin")) {
			groupForwarding = in.readBoolean();
			petFix = in.readBoolean();
			int placeholderCount = in.readInt();
			for (int i=0; i<placeholderCount; i++) {
				registerPlaceholder(in.readUTF(), in.readInt());
			}
			List<Object> args = Lists.newArrayList("PlayerJoinResponse", vanishMap.getOrDefault(player, false),
					disguiseMap.getOrDefault(player, false),
					invisibleMap.getOrDefault(player, false), getWorld(player));
			if (groupForwarding) args.add(getGroup(player));
			Map<String, Object> placeholders = parsePlaceholders(player);
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
			sendMessage(player, args.toArray());
		}
		if (subChannel.equals("Placeholder")){
			registerPlaceholder(in.readUTF(), in.readInt());
		}
		if (subChannel.equals("Permission")){
			String permission = in.readUTF();
			sendMessage(player, "Permission", permission, hasPermission(player, permission));
		}
	}

	public void sendMessage(Object player, Object... args) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		for (Object arg : args) {
			writeObject(out, arg);
		}
		sendPluginMessage(player, out);
	}

	private void writeObject(ByteArrayDataOutput out, Object value) {
		if (value == null) return;
		if (value instanceof String) {
			out.writeUTF((String) value);
		} else if (value instanceof Boolean) {
			out.writeBoolean((boolean) value);
		} else if (value instanceof Integer) {
			out.writeInt((int) value);
		} else throw new IllegalArgumentException("Unhandled message data type " + value.getClass().getName());
	}

	public boolean isPetFixEnabled() {
		return petFix;
	}
}
