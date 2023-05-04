package me.neznamy.tab.bridge.shared;

import lombok.NonNull;

import java.util.Collection;

public interface Scoreboard {

    void setDisplaySlot(@NonNull DisplaySlot slot, @NonNull String objective);

    void setScore(@NonNull String objective, @NonNull String player, int score);

    void removeScore(@NonNull String objective, @NonNull String player);

    void registerObjective(@NonNull String objectiveName, @NonNull String title, @NonNull String titleComponent, boolean hearts);

    void unregisterObjective(@NonNull String objectiveName);

    void updateObjective(@NonNull String objectiveName, @NonNull String title, @NonNull String titleComponent, boolean hearts);

    void registerTeam(@NonNull String name, @NonNull String prefix, @NonNull String prefixComponent, @NonNull String suffix,
                      @NonNull String suffixComponent, @NonNull String visibility,
                      @NonNull String collision, @NonNull Collection<String> players, int options, int color);

    void unregisterTeam(@NonNull String name);

    void updateTeam(@NonNull String name, @NonNull String prefix, @NonNull String prefixComponent, @NonNull String suffix,
                    @NonNull String suffixComponent, @NonNull String visibility, @NonNull String collision, int options, int color);

    enum DisplaySlot {PLAYER_LIST, SIDEBAR, BELOW_NAME}
}