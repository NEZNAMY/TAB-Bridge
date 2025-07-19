package me.neznamy.tab.bridge.fabric;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.neznamy.tab.bridge.fabric.hook.PermissionsAPIHook;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.hook.LuckPermsHook;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Fabric implementation of BridgePlayer.
 */
@Setter
@Getter
public class FabricBridgePlayer extends BridgePlayer {

    @NotNull
    private ServerPlayer player;

    /**
     * Constructs new instance for given player.
     *
     * @param   player
     *          Player to create this instance for
     */
    public FabricBridgePlayer(@NonNull ServerPlayer player) {
        super(player.getGameProfile().getName(), player.getUUID());
        this.player = player;
    }

    @Override
    public synchronized void sendPluginMessage(byte[] message) {
        FabricBridge.getInstance().getVersionLoader().sendCustomPayload(player, message);
    }

    @Override
    @NotNull
    public String getWorld() {
        return FabricBridge.getLevelName(FabricBridge.getInstance().getVersionLoader().getLevel(player));
    }

    @Override
    public boolean hasPermission(@NonNull String permission) {
        return PermissionsAPIHook.hasPermission(getPlayer().createCommandSourceStack(), permission);
    }

    @Override
    public boolean checkInvisibility() {
        return false;
    }

    @Override
    public boolean checkVanish() {
        return false;
    }

    @Override
    public boolean checkDisguised() {
        return false;
    }

    @Override
    @NotNull
    public String checkGroup() {
        if (LuckPermsHook.getInstance().isInstalled()) {
            return LuckPermsHook.getInstance().getGroupFunction().apply(this);
        }
        return "LuckPerms not found";
    }

    @Override
    public int checkGameMode() {
        return player.gameMode.getGameModeForPlayer().getId();
    }
}
