package me.neznamy.tab.bridge.shared;

import lombok.NonNull;
import me.neznamy.tab.bridge.shared.placeholder.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Interface representing a platform that the bridge is running on.
 * It provides methods to interact with the platform and manage players.
 */
public interface Platform {

    /**
     * Starts a new repeating task that runs on the main thread.
     *
     * @param   task
     *          Task to run
     * @param   intervalTicks
     *          Interval in ticks between task executions
     */
    void scheduleSyncRepeatingTask(@NonNull Runnable task, int intervalTicks);

    /**
     * Runs the given task on the main thread.
     *
     * @param   task
     *          Task to run
     */
    void runTask(@NonNull Runnable task);

    /**
     * Creates a new bridge player from given platform player object.
     *
     * @param   player
     *          Platform-specific player object
     * @return  New player instance from given platform player object
     */
    @NotNull
    BridgePlayer newPlayer(@NonNull Object player);

    /**
     * Creates a new placeholder with given parameters.
     *
     * @param   publicIdentifier
     *          Identifier this placeholder will be registered with
     * @param   privateIdentifier
     *          The actual identifier that will be passed into PlaceholderAPI
     * @param   refresh
     *          Refresh interval in milliseconds
     * @return  Newly created placeholder
     */
    @NotNull
    Placeholder createPlaceholder(@NonNull String publicIdentifier, @NonNull String privateIdentifier, int refresh);

    /**
     * Cancels all sync tasks that were created.
     */
    void cancelTasks();

    /**
     * Returns the player object for given UUID.
     *
     * @param   uniqueId
     *          UUID of the player to get
     * @return  Player object for given UUID, or {@code null} if not found
     */
    @Nullable
    Object getPlayer(@NonNull UUID uniqueId);
}
