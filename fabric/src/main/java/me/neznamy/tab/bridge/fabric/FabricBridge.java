package me.neznamy.tab.bridge.fabric;

import lombok.Getter;
import me.neznamy.tab.bridge.fabric.hook.FabricTabExpansion;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.message.outgoing.WorldChange;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
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

    @Override
    public void onInitializeServer() {
        instance = this;

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            // Client to server, makes perfect sense to me registering this on server side
            PayloadTypeRegistry.playC2S().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));
            PayloadTypeRegistry.configurationC2S().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));

            // This is needed, otherwise sending packet will cause ClassCastException
            PayloadTypeRegistry.playS2C().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));
            PayloadTypeRegistry.configurationS2C().register(TabCustomPacketPayload.TYPE, TabCustomPacketPayload.codec(Integer.MAX_VALUE));

            ServerPlayNetworking.registerGlobalReceiver(TabCustomPacketPayload.TYPE, TabCustomPacketPayload::handle);
            ServerConfigurationNetworking.registerGlobalReceiver(TabCustomPacketPayload.TYPE, TabCustomPacketPayload::handle);
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

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            BridgePlayer p = TABBridge.getInstance().getPlayer(player.getUUID());
            if (p == null) return;
            p.sendPluginMessage(new WorldChange(getLevelName(destination)));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> TABBridge.getInstance().unload());
    }

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
