package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import me.neznamy.tab.bridge.bukkit.BukkitBridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * The event listener part for securing proper functionality of armor stands
 */
public class EventListener implements Listener {
	
	//the NameTag feature handler
	private final BridgeNameTagX feature;

	/**
	 * Constructs new instance with given parameters
	 * @param feature - NameTag feature handler
	 */
	public EventListener(BridgeNameTagX feature) {
		this.feature = feature;
	}
	
	/**
	 * Sneak event listener to de-spawn and spawn armor stands to skip animation
	 * @param e - sneak event
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSneak(PlayerToggleSneakEvent e) {
		ArmorStandManager asm = feature.getArmorStandManager(TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId()));
		if (asm != null) asm.sneak(e.isSneaking());
	}
	
	/**
	 * Respawning armor stands as respawn screen destroys all entities in client
	 * @param e - respawn event
	 */
	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		ArmorStandManager asm = feature.getArmorStandManager(TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId()));
		if (asm != null && !feature.isPlayerDisabled(TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId()))) {
			asm.teleport();
		}
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		BukkitBridgePlayer player = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
		if (player == null) return;
		if (feature.isUnlimitedDisabled(e.getPlayer().getWorld().getName())) {
			feature.getDisabledUnlimitedPlayers().add(player);
		} else {
			feature.getDisabledUnlimitedPlayers().remove(player);
		}
		if (feature.getPlayersPreviewingNameTag().contains(player)) {
			feature.getArmorStandManager(player).spawn(player);
		}
	}
}