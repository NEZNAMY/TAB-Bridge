package me.neznamy.tab.bridge.shared;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface Scoreboard {

    void setDisplaySlot(@NotNull DisplaySlot slot, @NotNull String objective);

    void setScore(@NotNull String objective, @NotNull String player, int score,
                  @Nullable String displayName, @Nullable String numberFormat);

    void removeScore(@NotNull String objective, @NotNull String player);

    void registerObjective(@NotNull String objectiveName, @NotNull String title,
                           boolean hearts, @Nullable String numberFormat);

    void unregisterObjective(@NotNull String objectiveName);

    void updateObjective(@NotNull String objectiveName, @NotNull String title,
                         boolean hearts, @Nullable String numberFormat);

    void registerTeam(@NotNull String name, @NotNull String prefix, @NotNull String suffix, @NotNull String visibility,
                      @NotNull String collision, @NotNull Collection<String> players, int options, int color);

    void unregisterTeam(@NotNull String name);

    void updateTeam(@NotNull String name, @NotNull String prefix, @NotNull String suffix,
                    @NotNull String visibility, @NotNull String collision, int options, int color);

    enum DisplaySlot {PLAYER_LIST, SIDEBAR, BELOW_NAME}
}