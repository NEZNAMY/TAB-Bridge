package me.neznamy.tab.bridge.shared;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public interface Scoreboard {

    void setDisplaySlot(int slot, @NotNull String objective);

    void setScore(@NotNull String objective, @NotNull String player, int score,
                  @Nullable String displayName, @Nullable String numberFormat);

    void removeScore(@NotNull String objective, @NotNull String player);

    void registerObjective(@NotNull String objectiveName, @NotNull String title,
                           int renderType, @Nullable String numberFormat);

    void unregisterObjective(@NotNull String objectiveName);

    void updateObjective(@NotNull String objectiveName, @NotNull String title,
                         int renderType, @Nullable String numberFormat);

    void registerTeam(@NotNull String name, @NotNull String prefix, @NotNull String suffix, @NotNull String visibility,
                      @NotNull String collision, @NotNull Collection<String> players, int options, int color);

    void unregisterTeam(@NotNull String name);

    void updateTeam(@NotNull String name, @NotNull String prefix, @NotNull String suffix,
                    @NotNull String visibility, @NotNull String collision, int options, int color);

    @AllArgsConstructor
    enum CollisionRule {

        ALWAYS("always"),
        NEVER("never"),
        PUSH_OTHER_TEAMS("pushOtherTeams"),
        PUSH_OWN_TEAM("pushOwnTeam");

        private static final Map<String, CollisionRule> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(collisionRule -> collisionRule.string, collisionRule -> collisionRule));
        private final String string;

        @Override
        public String toString() {
            return string;
        }

        public static CollisionRule getByName(String name) {
            return BY_NAME.getOrDefault(name, ALWAYS);
        }
    }

    @AllArgsConstructor
    enum NameVisibility {

        ALWAYS("always"),
        NEVER("never"),
        HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
        HIDE_FOR_OWN_TEAM("hideForOwnTeam");

        private static final Map<String, NameVisibility> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(visibility -> visibility.string, visibility -> visibility));
        private final String string;

        @Override
        public String toString() {
            return string;
        }

        public static NameVisibility getByName(String name) {
            return BY_NAME.getOrDefault(name, ALWAYS);
        }
    }

    class ObjectiveAction {

        public static final int REGISTER = 0;
        public static final int UNREGISTER = 1;
        public static final int UPDATE = 2;
    }

    class HealthDisplay {

        public static final int INTEGER = 0;
        public static final int HEARTS = 1;
    }

    class DisplaySlot {

        public static final int PLAYER_LIST = 0;
        public static final int SIDEBAR = 1;
        public static final int BELOW_NAME = 2;
    }

    class ScoreAction {

        public static final int CHANGE = 0;
        public static final int REMOVE = 1;
    }

    class TeamAction {

        public static final int CREATE = 0;
        public static final int REMOVE = 1;
        public static final int UPDATE = 2;
        public static final int ADD_PLAYER = 3;
        public static final int REMOVE_PLAYER = 4;
    }
}