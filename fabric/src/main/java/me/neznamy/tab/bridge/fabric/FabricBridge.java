package me.neznamy.tab.bridge.fabric;

import lombok.Getter;
import lombok.SneakyThrows;
import me.neznamy.tab.bridge.fabric.hook.FabricTabExpansion;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.message.outgoing.WorldChange;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.NotNull;

/**
 * The entry point of the mod.
 */
public class FabricBridge implements DedicatedServerModInitializer {

    /**
     * Instance of this class.
     */
    @Getter
    private static FabricBridge instance;

    @Getter
    private final VersionLoader versionLoader;

    /**
     * Constructs a new instance of the FabricBridge class.
     */
    @SneakyThrows
    public FabricBridge() {
        versionLoader = (VersionLoader) Class.forName("me.neznamy.tab.bridge.fabric." + getImplPackage() + ".VersionLoaderImpl").getConstructor().newInstance();
    }

    @NotNull
    private String getImplPackage() {
        int serverVersion = SharedConstants.getProtocolVersion();
        if (serverVersion >= 766) {
            // 1.20.5+
            return "v1_21_8";
        } else if (serverVersion >= 763) {
            // 1.20 - 1.20.4
            return "v1_20_4";
        } else {
            // 1.19 - 1.19.4
            return "v1_19_4";
        }
    }

    @Override
    public void onInitializeServer() {
        instance = this;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            versionLoader.registerListeners();
            FabricTabExpansion expansion = FabricLoader.getInstance().isModLoaded("placeholder-api") ? new FabricTabExpansion() : null;
            TABBridge.setInstance(new TABBridge(new FabricPlatform(server), expansion));
            TABBridge.getInstance().getDataBridge().startTasks();
        });

        ServerPlayConnectionEvents.JOIN.register((connection, sender, server) ->
                TABBridge.getInstance().submitTask(() -> TABBridge.getInstance().getDataBridge().processQueue(connection.player, connection.player.getUUID())));

        ServerPlayConnectionEvents.DISCONNECT.register((connection, server) ->
                TABBridge.getInstance().submitTask(() -> {
                    FabricBridgePlayer p = (FabricBridgePlayer) TABBridge.getInstance().getPlayer(connection.player.getUUID());
                    if (p == null) return;
                    TABBridge.getInstance().removePlayer(p);
                }));

        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {
                    FabricBridgePlayer player = (FabricBridgePlayer) TABBridge.getInstance().getPlayer(oldPlayer.getUUID());
                    if (player != null) {
                        player.setPlayer(newPlayer);
                    }
                    // respawning from death & taking end portal in the end does not call world change event
                    worldChange(newPlayer, versionLoader.getLevel(newPlayer));
                });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                worldChange(player, destination));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> TABBridge.getInstance().unload());
    }

    private void worldChange(@NotNull ServerPlayer player, @NotNull Level destination) {
        BridgePlayer p = TABBridge.getInstance().getPlayer(player.getUUID());
        if (p == null) return;
        p.sendPluginMessage(new WorldChange(getLevelName(destination)));
    }

    /**
     * Returns the level name with a suffix based on the dimension.
     *
     * @param level
     *        The level to get the name of
     * @return
     *        The level name with a suffix based on the dimension
     */
    @NotNull
    public static String getLevelName(@NotNull Level level) {
        String path = level.dimension().location().getPath();
        return ((ServerLevelData)level.getLevelData()).getLevelName() + switch (path) {
            case "overworld" -> ""; // No suffix for overworld
            case "the_nether" -> "_nether";
            default -> "_" + path; // End + default behavior for other dimensions created by mods
        };
    }
}
