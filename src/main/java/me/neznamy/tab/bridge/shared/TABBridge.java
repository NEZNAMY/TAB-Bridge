package me.neznamy.tab.bridge.shared;

import me.neznamy.tab.bridge.shared.features.TabExpansion;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TABBridge {

    public static final String CHANNEL_NAME = "tab:bridge-2";
    public static final String PLUGIN_VERSION = "2.0.3";
    private static TABBridge instance;

    private final Platform platform;
    private final DataBridge dataBridge;
    private final TabExpansion expansion;
    private final Map<UUID, BridgePlayer> players = new ConcurrentHashMap<>();
    private final ExecutorService executorThread = Executors.newSingleThreadExecutor();

    public TABBridge(Platform platform, DataBridge dataBridge, TabExpansion expansion) {
        this.platform = platform;
        this.dataBridge = dataBridge;
        this.expansion = expansion;
    }

    public static void setInstance(TABBridge instance) {
        TABBridge.instance = instance;
    }

    public static TABBridge getInstance() {
        return instance;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void addPlayer(BridgePlayer player) {
        players.put(player.getUniqueId(), player);
    }

    public void removePlayer(BridgePlayer player) {
        players.remove(player.getUniqueId());
    }

    public Collection<BridgePlayer> getOnlinePlayers() {
        return players.values();
    }

    public BridgePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public DataBridge getDataBridge() {
        return dataBridge;
    }

    public void submitTask(Runnable task) {
        executorThread.submit(task);
    }

    public void shutdownExecutor() {
        executorThread.shutdownNow();
    }

    public TabExpansion getExpansion() {
        return expansion;
    }
}
