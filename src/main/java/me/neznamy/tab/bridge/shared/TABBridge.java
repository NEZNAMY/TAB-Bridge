package me.neznamy.tab.bridge.shared;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@RequiredArgsConstructor
public class TABBridge {

    public static final String CHANNEL_NAME = "tab:bridge-5";
    public static final String PLUGIN_VERSION = "5.0.3";
    @Getter @Setter private static TABBridge instance;

    @Getter private final Platform platform;
    @Getter private final DataBridge dataBridge = new DataBridge();
    @Nullable @Getter private final TabExpansion expansion;
    private final Map<UUID, BridgePlayer> players = new ConcurrentHashMap<>();
    @Getter private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("TAB-Bridge Processing Thread").build());

    @Getter private final ScheduledExecutorService placeholderThread = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("TAB-Bridge Placeholder Refreshing Thread").build());

    public void addPlayer(BridgePlayer player) {
        players.put(player.getUniqueId(), player);
    }

    public void removePlayer(BridgePlayer player) {
        players.remove(player.getUniqueId());
    }

    public Collection<BridgePlayer> getOnlinePlayers() {
        return players.values();
    }

    @Nullable public BridgePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public void submitTask(Runnable task) {
        // Executor service swallows exceptions
        scheduler.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void shutdownExecutor() {
        scheduler.shutdownNow();
        placeholderThread.shutdownNow();
    }
}
