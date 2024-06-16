package me.neznamy.tab.bridge.shared;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.chat.IChatBaseComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public abstract class Scoreboard {

    /** Player-to-Team map of expected teams of players */
    private final Map<String, String> expectedTeams = new HashMap<>();

    public void registerTeam(@NotNull String name, @NotNull IChatBaseComponent prefix, @NotNull IChatBaseComponent suffix, @NotNull String visibility,
                                      @NotNull String collision, @NotNull Collection<String> players, int options, int color) {
        for (String player : players) {
            expectedTeams.put(player, name);
        }
        registerTeam0(name, prefix, suffix, visibility, collision, players, options, color);
    }

    public void unregisterTeam(@NotNull String name) {
        for (Map.Entry<String, String> entry : expectedTeams.entrySet()) {
            if (entry.getValue().equals(name)) {
                expectedTeams.remove(entry.getKey());
                break;
            }
        }
        unregisterTeam0(name);
    }

    public abstract void setDisplaySlot(int slot, @NotNull String objective);

    public abstract void setScore(@NotNull String objective, @NotNull String player, int score,
                  @Nullable String displayName, @Nullable String numberFormat);

    public abstract void removeScore(@NotNull String objective, @NotNull String player);

    public abstract void registerObjective(@NotNull String objectiveName, @NotNull IChatBaseComponent title,
                           int renderType, @Nullable String numberFormat);

    public abstract void unregisterObjective(@NotNull String objectiveName);

    public abstract void updateObjective(@NotNull String objectiveName, @NotNull IChatBaseComponent title,
                         int renderType, @Nullable String numberFormat);

    public abstract void registerTeam0(@NotNull String name, @NotNull IChatBaseComponent prefix, @NotNull IChatBaseComponent suffix, @NotNull String visibility,
                                       @NotNull String collision, @NotNull Collection<String> players, int options, int color);

    public abstract void unregisterTeam0(@NotNull String name);

    public abstract void updateTeam(@NotNull String name, @NotNull IChatBaseComponent prefix, @NotNull IChatBaseComponent suffix,
                    @NotNull String visibility, @NotNull String collision, int options, int color);

    /**
     * Checks if team contains a player who should belong to a different team and if override attempt was detected,
     * sends a warning and removes player from the collection.
     *
     * @param   action
     *          Team packet action
     * @param   teamName
     *          Team name in the packet
     * @param   players
     *          Players in the packet
     * @return  Modified collection of players
     */
    @NotNull
    public Collection<String> onTeamPacket(int action, @NonNull String teamName, @NonNull Collection<String> players) {
        Collection<String> newList = new ArrayList<>();
        for (String entry : players) {
            String expectedTeam = expectedTeams.get(entry);
            if (expectedTeam == null) {
                newList.add(entry);
                continue;
            }
            if (!teamName.equals(expectedTeam)) {
                if (action == TeamAction.CREATE || action == TeamAction.ADD_PLAYER) {
                    String msg = "[TAB-Bridge] Blocked attempt to add player " + entry + " into team " + teamName +
                            " (expected team: " + expectedTeam + ")";
                    //Bukkit.getConsoleSender().sendMessage(msg);
                }
            } else {
                newList.add(entry);
            }
        }
        return newList;
    }

    @AllArgsConstructor
    public enum CollisionRule {

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
    public enum NameVisibility {

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

    public static class ObjectiveAction {

        public static final int REGISTER = 0;
        public static final int UNREGISTER = 1;
        public static final int UPDATE = 2;
    }

    public static class HealthDisplay {

        public static final int INTEGER = 0;
        public static final int HEARTS = 1;
    }

    public static class DisplaySlot {

        public static final int PLAYER_LIST = 0;
        public static final int SIDEBAR = 1;
        public static final int BELOW_NAME = 2;
    }

    public static class ScoreAction {

        public static final int CHANGE = 0;
        public static final int REMOVE = 1;
    }

    public static class TeamAction {

        public static final int CREATE = 0;
        public static final int REMOVE = 1;
        public static final int UPDATE = 2;
        public static final int ADD_PLAYER = 3;
        public static final int REMOVE_PLAYER = 4;
    }
}