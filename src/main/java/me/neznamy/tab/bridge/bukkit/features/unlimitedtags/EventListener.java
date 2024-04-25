package me.neznamy.tab.bridge.bukkit.features.unlimitedtags;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class EventListener implements Listener {
    
    //the NameTag feature handler
    private final BridgeNameTagX feature;

    /**
     * Sneak event listener to de-spawn and spawn armor stands to skip animation
     * @param e - sneak event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent e) {
        BukkitBridgePlayer player = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
        if (player == null) return;
        ArmorStandManager asm = player.unlimitedNametagData.armorStandManager;
        TABBridge.getInstance().submitTask(() -> {
            if (asm != null && !feature.isPlayerDisabled(player))
                asm.sneak(e.isSneaking());
        });
    }
    
    /**
     * Respawning armor stands as respawn screen destroys all entities in client
     * @param e - respawn event
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        BukkitBridgePlayer player = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
        if (player == null) return;
        ArmorStandManager asm = player.unlimitedNametagData.armorStandManager;
        if (asm != null && !feature.isPlayerDisabled(player)) {
            asm.teleport();
        }
    }
    
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        BukkitBridgePlayer player = (BukkitBridgePlayer) TABBridge.getInstance().getPlayer(e.getPlayer().getUniqueId());
        if (player == null) return;
        if (!player.unlimitedNametagData.disabled && player.unlimitedNametagData.previewing) {
            player.unlimitedNametagData.armorStandManager.spawn(player);
        }
    }
}