package me.neznamy.tab.bridge.bukkit;

import com.google.common.io.ByteArrayDataInput;
import io.netty.channel.Channel;
import me.clip.placeholderapi.PlaceholderAPI;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.Platform;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.UUID;

public class BukkitPlatform implements Platform {

    private final boolean vault = Bukkit.getPluginManager().isPluginEnabled("Vault");
    private final boolean placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    private final JavaPlugin plugin;

    public BukkitPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isInvisible(BridgePlayer player) {
        return ((BukkitBridgePlayer)player).getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public boolean isVanished(BridgePlayer player) {
        return ((BukkitBridgePlayer)player).getPlayer().getMetadata("vanished").stream().anyMatch(MetadataValue::asBoolean);
    }

    @Override
    public boolean isDisguised(BridgePlayer player) {
        Entity entity = ((BukkitBridgePlayer)player).getPlayer();
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
    public String getGroup(BridgePlayer player) {
        if (vault) {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
            if (rsp == null || rsp.getProvider().getName().equals("SuperPerms")) {
                return "No permission plugin found";
            } else {
                return rsp.getProvider().getPrimaryGroup(((BukkitBridgePlayer)player).getPlayer());
            }
        } else {
            return "Vault not found";
        }
    }

    @Override
    public boolean isOnline(Object player) {
        return ((Player)player).isOnline();
    }

    @Override
    public UUID getUniqueId(Object player) {
        return ((Player)player).getUniqueId();
    }

    @Override
    public Channel getChannel(Object player) {
        if (NMSStorage.getInstance() == null) return null;
        try {
            Object handle = NMSStorage.getInstance().getHandle.invoke(player);
            Object playerConnection = NMSStorage.getInstance().PLAYER_CONNECTION.get(handle);
            return (Channel) NMSStorage.getInstance().CHANNEL.get(NMSStorage.getInstance().NETWORK_MANAGER.get(playerConnection));
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void runTaskTimerAsynchronously(Runnable task, int intervalTicks) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, intervalTicks, intervalTicks);
    }

    @Override
    public void scheduleSyncRepeatingTask(Runnable task, int intervalTicks) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, intervalTicks, intervalTicks);
    }

    @Override
    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void readUnlimitedNametagJoin(BridgePlayer player, ByteArrayDataInput input) {
        BukkitBridge.getInstance().nametagx.onJoin((BukkitBridgePlayer) player, input);
    }

    @Override
    public void readUnlimitedNametagMessage(BridgePlayer player, ByteArrayDataInput input) {
        BukkitBridge.getInstance().nametagx.readMessage((BukkitBridgePlayer) player, input);
    }

    @Override
    public void sendScoreboardDisplayObjective(BridgePlayer player, int slot, String objective) {
        try {
            player.sendPacket(BukkitPacketBuilder.getInstance().scoreboardDisplayObjective(slot, objective));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendScoreboardObjective(BridgePlayer player, String objective, int action, String displayName, String displayComponent, int renderType) {
        try {
            player.sendPacket(BukkitPacketBuilder.getInstance().scoreboardObjective(objective, action, displayName, displayComponent, renderType));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendScoreboardScore(BridgePlayer player, String objective, int action, String playerName, int score) {
        try {
            player.sendPacket(BukkitPacketBuilder.getInstance().scoreboardScore(objective, action, playerName, score));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendScoreboardTeam(BridgePlayer player, String name, int action, Collection<String> players, String prefix, String prefixComponent,
                                   String suffix, String suffixComponent, int options, String visibility, String collision, int color) {
        try {
            player.sendPacket(BukkitPacketBuilder.getInstance().scoreboardTeam(name, action, players, prefix, prefixComponent,
                    suffix, suffixComponent, options, visibility, collision, color));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setExpansionValue(Object player, String identifier, String value) {
        TABBridge.getInstance().getExpansion().setValue(player, identifier, value);
    }

    @Override
    public boolean hasPermission(Object player, String permission) {
        return ((Player)player).hasPermission(permission);
    }

    @Override
    public void registerExpansion() {
        Bukkit.getScheduler().runTask(BukkitBridge.getInstance(), TABBridge.getInstance().getExpansion()::register);
    }

    @Override
    public String getWorld(Object player) {
        return ((Player)player).getWorld().getName();
    }

    @Override
    public BridgePlayer newPlayer(Object player, int protocolVersion) {
        return new BukkitBridgePlayer((Player) player, protocolVersion);
    }

    @Override
    public Placeholder createPlaceholder(String publicIdentifier, String privateIdentifier, int refresh) {
        Placeholder placeholder;
        if (privateIdentifier.startsWith("%server_")) {
            placeholder = new ServerPlaceholder(publicIdentifier, refresh, () ->
                    placeholderAPI ? PlaceholderAPI.setPlaceholders(null, privateIdentifier) : "<PlaceholderAPI is not installed>");
        } else if (privateIdentifier.startsWith("%rel_")) {
            placeholder = new RelationalPlaceholder(publicIdentifier, refresh, (viewer, target) ->
                    placeholderAPI ? PlaceholderAPI.setRelationalPlaceholders(((BukkitBridgePlayer)viewer).getPlayer(),
                            ((BukkitBridgePlayer)target).getPlayer(), privateIdentifier) : "<PlaceholderAPI is not installed>");
        } else {
            placeholder = new PlayerPlaceholder(publicIdentifier, refresh, p ->
                    placeholderAPI ? PlaceholderAPI.setPlaceholders(((BukkitBridgePlayer)p).getPlayer(), privateIdentifier) : "<PlaceholderAPI is not installed>");
        }
        return placeholder;
    }
}