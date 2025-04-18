package me.neznamy.tab.bridge.shared;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface Platform {

    boolean isOnline(@NonNull Object player);

    @NotNull
    UUID getUniqueId(@NonNull Object player);

    void scheduleSyncRepeatingTask(@NonNull Runnable task, int intervalTicks);

    void runTask(@NonNull Runnable task);

    @NotNull
    BridgePlayer newPlayer(@NonNull Object player);

    @NotNull
    Placeholder createPlaceholder(@NonNull String publicIdentifier, @NonNull String privateIdentifier, int refresh);

    void cancelTasks();
}
