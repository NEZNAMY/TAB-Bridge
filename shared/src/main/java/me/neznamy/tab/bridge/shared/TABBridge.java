package me.neznamy.tab.bridge.shared;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@RequiredArgsConstructor
public class TABBridge {

    public static final String CHANNEL_NAME = "tab:bridge-6";
    public static final String PLUGIN_VERSION = "6.1.1";
    @Getter @Setter private static TABBridge instance;

    @Getter private final Platform platform;
    @Getter private final DataBridge dataBridge = new DataBridge();
    @Nullable @Getter private final TabExpansion expansion;
    private final Map<UUID, BridgePlayer> players = new ConcurrentHashMap<>();
    private BridgePlayer[] playerArray = new BridgePlayer[0];

    @Getter private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("TAB-Bridge Processing Thread").build());

    @Getter private final ScheduledExecutorService placeholderThread = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("TAB-Bridge Placeholder Refreshing Thread").build());

    public void addPlayer(@NonNull BridgePlayer player) {
        players.put(player.getUniqueId(), player);
        playerArray = players.values().toArray(new BridgePlayer[0]);
    }

    public void removePlayer(@NonNull BridgePlayer player) {
        players.remove(player.getUniqueId());
        playerArray = players.values().toArray(new BridgePlayer[0]);
    }

    @NotNull
    public BridgePlayer[] getOnlinePlayers() {
        return playerArray;
    }

    @Nullable
    public BridgePlayer getPlayer(@NonNull UUID uuid) {
        return players.get(uuid);
    }

    public void submitTask(@NonNull Runnable task) {
        if (scheduler.isShutdown()) return;

        // Executor service swallows exceptions
        scheduler.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void unload() {
        platform.cancelTasks();
        shutdownExecutor();
        if (expansion != null) expansion.unregister();
    }

    public void shutdownExecutor() {
        scheduler.shutdownNow();
        placeholderThread.shutdownNow();
    }
}
