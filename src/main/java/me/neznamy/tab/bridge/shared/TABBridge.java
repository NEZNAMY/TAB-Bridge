package me.neznamy.tab.bridge.shared;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class TABBridge {

    public static final String CHANNEL_NAME = "tab:bridge-3";
    public static final String PLUGIN_VERSION = "3.0.0";
    @Getter @Setter private static TABBridge instance;

    @Getter private final Platform platform;
    @Getter private final DataBridge dataBridge;
    @Nullable @Getter private final TabExpansion expansion;
    private final Map<UUID, BridgePlayer> players = new ConcurrentHashMap<>();
    private final ExecutorService executorThread = Executors.newSingleThreadExecutor();

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
        executorThread.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void shutdownExecutor() {
        executorThread.shutdownNow();
    }
}
