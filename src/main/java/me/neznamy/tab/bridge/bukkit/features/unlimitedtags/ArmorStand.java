package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.bukkit.BridgePlayer;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.bukkit.BukkitPacketBuilder;
import me.neznamy.tab.bridge.bukkit.nms.DataWatcher;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pose;

import java.util.UUID;

/**
 * A class representing an armor stand attached to a player (if the feature is enabled)
 */
public class ArmorStand {

	//NameTag feature
	private final BridgeNameTagX manager = BukkitBridge.getInstance().nametagx;

	private final boolean alwaysVisible = manager.isAlwaysVisible();
	
	//entity id counter to pick unique entity IDs
	private static int idCounter = 2000000000;

	//armor stand owner
	private final BridgePlayer player;

	//offset in blocks, 0 for original height
	private double yOffset;

	//entity ID of this armor stand
	private final int entityId = idCounter++;

	//unique ID of this armor stand
	private final UUID uuid = UUID.randomUUID();

	//sneaking flag of armor stands
	private boolean sneaking;

	//armor stand visibility
	private boolean visible;

	private String text = "";
	private String componentText = "{\"text\":\"\"}";

	//if offset is static or dynamic based on other armor stands
	private final boolean staticOffset;

	public ArmorStand(BridgePlayer player, double yOffset, boolean staticOffset) {
		this.player = player;
		this.staticOffset = staticOffset;
		this.yOffset = yOffset;
		visible = getVisibility();
	}

	public void setText(String text, String componentText) {
		if (this.text.equals(text)) return;
		this.text = text;
		this.componentText = componentText;
		refresh();
		manager.getArmorStandManager(player).fixArmorStandHeights();
	}

	public String getText() {
		return text;
	}
	
	public void refresh() {
		visible = getVisibility();
		updateMetadata();
	}
	
	public boolean hasStaticOffset() {
		return staticOffset;
	}

	public void setOffset(double offset) {
		if (yOffset == offset) return;
		yOffset = offset;
		for (BridgePlayer all : manager.getArmorStandManager(player).getNearbyPlayers()) {
			all.sendPacket(getTeleportPacket(all));
		}
	}
	
	public void spawn(BridgePlayer viewer) {
		for (Object packet : getSpawnPackets(viewer)) {
			viewer.sendPacket(packet);
		}
	}

	public void destroy() {
		for (BridgePlayer all : manager.getArmorStandManager(player).getNearbyPlayers()) {
			all.sendPacket(BukkitPacketBuilder.getInstance().entityDestroy(entityId));
		}
	}
	
	public void destroy(BridgePlayer viewer) {
		viewer.sendPacket(BukkitPacketBuilder.getInstance().entityDestroy(entityId));
	}

	public void teleport() {
		for (BridgePlayer all : manager.getArmorStandManager(player).getNearbyPlayers()) {
			all.sendPacket(getTeleportPacket(all));
		}
	}

	public void teleport(BridgePlayer viewer) {
		if (!manager.getArmorStandManager(player).isNearby(viewer) && viewer != player) {
			manager.getArmorStandManager(player).spawn(viewer);
		} else {
			viewer.sendPacket(getTeleportPacket(viewer));
		}
	}

	public void sneak(boolean sneaking) {
		if (this.sneaking == sneaking) return; //idk
		this.sneaking = sneaking;
		for (BridgePlayer viewer : manager.getArmorStandManager(player).getNearbyPlayers()) {
			if (viewer.getProtocolVersion() >= 480 && viewer.getProtocolVersion() <= 498 && !alwaysVisible) {
				//1.14.x client sided bug, de-spawning completely
				if (sneaking) {
					viewer.sendPacket(BukkitPacketBuilder.getInstance().entityDestroy(entityId));
				} else {
					spawn(viewer);
				}
			} else {
				//respawning so there's no animation and it's instant
				respawn(viewer);
			}
		}
	}

	
	public void updateVisibility(boolean force) {
		boolean visibility = getVisibility();
		if (visible != visibility || force) {
			refresh();
		}
	}

	
	public int getEntityId() {
		return entityId;
	}

	/**
	 * Returns teleport packet for specified viewer
	 * @param viewer - player to get location for
	 * @return teleport packet
	 */
	public Object getTeleportPacket(BridgePlayer viewer) {
		return BukkitPacketBuilder.getInstance().entityTeleport(entityId, getArmorStandLocationFor(viewer));
	}

	/**
	 * Updates armor stand's metadata
	 */
	public void updateMetadata() {
		for (BridgePlayer viewer : manager.getArmorStandManager(player).getNearbyPlayers()) {
			viewer.sendPacket(BukkitPacketBuilder.getInstance().entityMetadata(entityId, createDataWatcher(viewer)));
		}
	}

	/**
	 * Returns general visibility rule for everyone with limited info
	 * @return true if armor stand should be visible, false if not
	 */
	public boolean getVisibility() {
		if (player.isDisguised() || manager.getVehicleManager().isOnBoat(player)) return false;
		if (alwaysVisible) return true;
		return !player.isInvisible() && player.getPlayer().getGameMode() != GameMode.SPECTATOR &&
				!manager.hasHiddenNametag(player) && text.length() > 0;
	}

	/**
	 * Returns general location where armor stand should be at time of calling
	 * @return Location where armor stand should be for everyone
	 */
	public Location getLocation() {
		double x = player.getPlayer().getLocation().getX();
		double y = getY() + yOffset + 2;
		double z = player.getPlayer().getLocation().getZ();
		if (player.getPlayer().isSleeping()) {
			y -= 1.76;
		} else {
			if (NMSStorage.getInstance().getMinorVersion() >= 9) {
				y -= (sneaking ? 0.45 : 0.18);
			} else {
				y -= (sneaking ? 0.30 : 0.18);
			}
		}
		return new Location(null,x,y,z);
	}

	/**
	 * Returns Y where player is based on player's vehicle due to bukkit API bug
	 * @return correct player's Y
	 */
	protected double getY() {
		//1.14+ server sided bug
		Entity vehicle = player.getPlayer().getVehicle();
		if (vehicle != null) {
			if (vehicle.getType().toString().contains("HORSE")) { //covering all 3 horse types
				return vehicle.getLocation().getY() + 0.85;
			}
			if (vehicle.getType().toString().equals("DONKEY")) { //1.11+
				return vehicle.getLocation().getY() + 0.525;
			}
			if (vehicle.getType() == EntityType.PIG) {
				return vehicle.getLocation().getY() + 0.325;
			}
			if (vehicle.getType().toString().equals("STRIDER")) { //1.16+
				return vehicle.getLocation().getY() + 1.15;
			}
		}
		//1.13+ swimming or 1.9+ flying with elytra
		if (isSwimming() || (NMSStorage.getInstance().getMinorVersion() >= 9 && player.getPlayer().isGliding())) {
			return player.getPlayer().getLocation().getY()-1.22;
		}
		return player.getPlayer().getLocation().getY();
	}

	private boolean isSwimming() {
		if (NMSStorage.getInstance().getMinorVersion() >= 14 && player.getPlayer().getPose() == Pose.SWIMMING) return true;
		return NMSStorage.getInstance().getMinorVersion() == 13 && player.getPlayer().isSwimming();
	}

	/**
	 * Creates data watcher with specified display name for viewer
	 * @param viewer - player to apply checks against
	 * @return DataWatcher for viewer
	 */
	public DataWatcher createDataWatcher(BridgePlayer viewer) {
		DataWatcher datawatcher = new DataWatcher();

		byte flag = 32; //invisible
		if (sneaking) flag += (byte)2;
		datawatcher.helper().setEntityFlags(flag);
		String text = this.text;
		String componentText = this.componentText;
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && this.text.contains("%rel_")) {
			text = PlaceholderAPI.setRelationalPlaceholders(viewer.getPlayer(), player.getPlayer(), this.text);
			componentText = PlaceholderAPI.setRelationalPlaceholders(viewer.getPlayer(), player.getPlayer(), this.componentText);
		}
		datawatcher.helper().setCustomName(text, componentText);

		boolean visibility;
		if (isNameVisiblyEmpty(text) || !viewer.getPlayer().canSee(player.getPlayer()) ||
				manager.hasHiddenNametag(player) || manager.hasHiddenNameTagVisibilityView(viewer)) {
			visibility = false;
		} else {
			visibility = visible;
		}
		datawatcher.helper().setCustomNameVisible(visibility);

		if (viewer.getProtocolVersion() > 47 || manager.isMarkerFor18x()) datawatcher.helper().setArmorStandFlags((byte)16);
		return datawatcher;
	}

	/**
	 * Returns true if display name is in fact empty, for example only containing color codes
	 * @param displayName - string to check
	 * @return true if it's empty, false if not
	 */
	private boolean isNameVisiblyEmpty(String displayName) {
		if (displayName.length() == 0) return true;
		if (!displayName.startsWith("\u00a7") && !displayName.startsWith("&") && !displayName.startsWith("#")) return false;
		String text = ChatColor.stripColor(displayName);
		if (text.contains(" ")) text = text.replace(" ", "");
		return text.length() == 0;
	}

	/**
	 * Returns list of packets to send to make armor stand spawn with metadata
	 * @param viewer - viewer to apply relational placeholders for
	 * @return List of packets that spawn the armor stand
	 */
	public Object[] getSpawnPackets(BridgePlayer viewer) {
		visible = getVisibility();
		DataWatcher dataWatcher = createDataWatcher(viewer);
		if (NMSStorage.getInstance().getMinorVersion() >= 15) {
			return new Object[] {
					BukkitPacketBuilder.getInstance().entitySpawn(entityId, uuid, EntityType.ARMOR_STAND, getArmorStandLocationFor(viewer), null),
					BukkitPacketBuilder.getInstance().entityMetadata(entityId, dataWatcher)
			};
		} else {
			return new Object[] {
					BukkitPacketBuilder.getInstance().entitySpawn(entityId, uuid, EntityType.ARMOR_STAND, getArmorStandLocationFor(viewer), dataWatcher),
			};
		}
	}

	/**
	 * Returns location where armor stand should be for specified viewer
	 * @param viewer - player to get location for
	 * @return location of armor stand
	 */
	public Location getArmorStandLocationFor(BridgePlayer viewer) {
		return viewer.getProtocolVersion() == 47 && !manager.isMarkerFor18x() ? getLocation().clone().add(0,-2,0) : getLocation();
	}

	public void respawn(BridgePlayer viewer) {
		viewer.sendPacket(BukkitPacketBuilder.getInstance().entityDestroy(entityId));
		spawn(viewer);
	}
}