package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.neznamy.tab.bridge.bukkit.hook.LibsDisguisesHook;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.hook.LuckPermsHook;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Bukkit implementation of BridgePlayer.
 */
@Getter
public class BukkitBridgePlayer extends BridgePlayer {

    private static final boolean vault = Bukkit.getPluginManager().isPluginEnabled("Vault");

    @NotNull
    private final Player player;

    @NotNull
    private final Set<String> channels;

    @Nullable
    private Permission permission;

    /**
     * Constructs new instance for given player.
     *
     * @param   player
     *          Player to create this instance for
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public BukkitBridgePlayer(@NonNull Player player) {
        super(player.getName(), player.getUniqueId());
        this.player = player;
        Field channelsField = player.getClass().getDeclaredField("channels");
        channelsField.setAccessible(true);
        channels = ((Set<String>) channelsField.get(player));
        if (vault) {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
            if (rsp != null) permission = rsp.getProvider();
        }
    }

    @Override
    @SneakyThrows
    public void sendPluginMessage(byte[] message) {
        // 1.20.2 bug adding it with a significant delay, add ourselves to make it work
        // apparently it affects those players on older server version as well, so do this always
        channels.add(TABBridge.CHANNEL_NAME);
        player.sendPluginMessage(BukkitBridge.getInstance(), TABBridge.CHANNEL_NAME, message);
    }

    @Override
    @NotNull
    public String getWorld() {
        return player.getWorld().getName();
    }

    @Override
    public boolean hasPermission(@NonNull String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean checkInvisibility() {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public boolean checkVanish() {
        for (MetadataValue v : player.getMetadata("vanished")) {
            if (v.asBoolean()) return true;
        }
        return false;
    }

    @Override
    public boolean checkDisguised() {
        return LibsDisguisesHook.isDisguised(player);
    }

    @Override
    @NotNull
    public String checkGroup() {
        if (LuckPermsHook.getInstance().isInstalled()) {
            return LuckPermsHook.getInstance().getGroupFunction().apply(this);
        }
        if (vault) {
            if (permission == null || permission.getName().equals("SuperPerms")) {
                return "No permission plugin found";
            } else {
                return permission.getPrimaryGroup(player);
            }
        }
        return "Vault not found";
    }

    @Override
    @SuppressWarnings("deprecation")
    public int checkGameMode() {
        return player.getGameMode().getValue();
    }
}
