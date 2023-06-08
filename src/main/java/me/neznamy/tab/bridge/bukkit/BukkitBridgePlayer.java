package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.Scoreboard;
import me.neznamy.tab.bridge.shared.TABBridge;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffectType;

public class BukkitBridgePlayer extends BridgePlayer {

    private static final boolean vault = Bukkit.getPluginManager().isPluginEnabled("Vault");

    @Getter private final Player player;
    @Getter private final Scoreboard scoreboard = new BukkitScoreboard(this);

    public BukkitBridgePlayer(Player player, int protocolVersion) {
        super(player.getName(), player.getUniqueId(), protocolVersion);
        this.player = player;
    }

    @Override
    public void sendPluginMessage(byte[] message) {
        player.sendPluginMessage(BukkitBridge.getInstance(), TABBridge.CHANNEL_NAME, message);
    }

    @Override
    public void sendPacket(Object packet) {
        if (NMSStorage.getInstance() == null) return;
        try {
            Object handle = NMSStorage.getInstance().getHandle.invoke(player);
            Object playerConnection = NMSStorage.getInstance().PLAYER_CONNECTION.get(handle);
            NMSStorage.getInstance().sendPacket.invoke(playerConnection, packet);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getWorld() {
        return player.getWorld().getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean checkInvisibility() {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public boolean checkVanish() {
        return player.getMetadata("vanished").stream().anyMatch(MetadataValue::asBoolean);
    }

    @Override
    public boolean checkDisguised() {
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
    public String checkGroup() {
        if (vault) {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
            if (rsp == null || rsp.getProvider().getName().equals("SuperPerms")) {
                return "No permission plugin found";
            } else {
                return rsp.getProvider().getPrimaryGroup(player);
            }
        } else {
            return "Vault not found";
        }
    }
}
