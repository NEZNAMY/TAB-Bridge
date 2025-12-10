package me.neznamy.tab.bridge.fabric;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.bridge.fabric.hook.PlaceholderAPIHook;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.Platform;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import me.neznamy.tab.bridge.shared.placeholder.PlayerPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.RelationalPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.ServerPlaceholder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fabric implementation of the Platform interface.
 */
@RequiredArgsConstructor
public class FabricPlatform implements Platform {

    private final boolean placeholderAPI = FabricLoader.getInstance().isModLoaded("placeholder-api");
    private final MinecraftServer server;

    @Override
    public void scheduleSyncRepeatingTask(@NonNull Runnable task, int intervalTicks) {
        // Sync placeholders are not supported
    }

    @Override
    public void runTask(@NonNull Runnable task) {
        // Sync placeholders are not supported
    }

    @Override
    public void cancelTasks() {
        // Sync placeholders are not supported
    }

    @Override
    @Nullable
    public Object getPlayer(@NonNull UUID uniqueId) {
        return server.getPlayerList().getPlayer(uniqueId);
    }

    @Override
    @NotNull
    public BridgePlayer newPlayer(@NonNull Object player) {
        return new FabricBridgePlayer((ServerPlayer) player);
    }

    @Override
    @NotNull
    public Placeholder createPlaceholder(@NonNull String publicIdentifier, @NonNull String privateIdentifier, int refresh) {
        if (!placeholderAPI) {
            if (privateIdentifier.startsWith("%rel_")) {
                return new RelationalPlaceholder(publicIdentifier, -1, (viewer, target) -> "<PlaceholderAPI is not installed>");
            } else {
                return new ServerPlaceholder(publicIdentifier, -1, () -> "<PlaceholderAPI is not installed>");
            }
        }
        if (privateIdentifier.startsWith("%rel_")) {
            return new RelationalPlaceholder(publicIdentifier, refresh, (viewer, target) -> "<Relational placeholders are not supported>");
        } else {
            return new PlayerPlaceholder(publicIdentifier, refresh, p ->
                    parseWithNestedPlaceholders(((FabricBridgePlayer)p).getPlayer(), privateIdentifier));
        }
    }

    @NotNull
    private String parseWithNestedPlaceholders(@NotNull ServerPlayer player, @NonNull String identifier) {
        String text = identifier;
        String textBefore;
        do {
            textBefore = text;
            text = PlaceholderAPIHook.parsePlaceholders(text, player);
        } while (!textBefore.equals(text));
        return text;
    }
}
