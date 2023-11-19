package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.Scoreboard;
import me.neznamy.tab.bridge.shared.chat.IChatBaseComponent;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class BukkitScoreboard implements Scoreboard {

    @Getter
    private static boolean available;
    private static final int minorVersion = BukkitBridge.getMinorVersion();
    private static final boolean is1_20_2Plus = BukkitBridge.is1_20_2Plus();
    private static Method DESERIALIZE;

    private static Object emptyScoreboard;
    private static Field IScoreboardCriteria_self;

    // PacketPlayOutScoreboardDisplayObjective
    public static Class<?> DisplayObjectiveClass;
    private static Constructor<?> newDisplayObjective;
    public static Field DisplayObjective_POSITION;
    public static Field DisplayObjective_OBJECTIVE_NAME;
    public static Enum[] DisplaySlot_values;

    // PacketPlayOutScoreboardScore
    private static Class<Enum> EnumScoreboardAction;
    private static Constructor<?> newScorePacket_1_13;
    private static Constructor<?> newScorePacket_String;
    private static Constructor<?> newScorePacket;
    private static Constructor<?> newScoreboardScore;
    private static Method ScoreboardScore_setScore;

    // PacketPlayOutScoreboardObjective
    public static Class<?> ObjectivePacketClass;
    private static Constructor<?> newObjectivePacket;
    public static Field Objective_OBJECTIVE_NAME;
    public static Field Objective_METHOD;
    private static Field Objective_RENDER_TYPE;
    private static Field Objective_DISPLAY_NAME;
    private static Class<Enum> EnumScoreboardHealthDisplay;
    private static Constructor<?> newScoreboardObjective;

    // PacketPlayOutScoreboardTeam
    public static Class<?> TeamPacketClass;
    private static Constructor<?> newTeamPacket;
    private static Method TeamPacketConstructor_of;
    private static Method TeamPacketConstructor_ofBoolean;
    public static Field TeamPacket_NAME;
    public static Field TeamPacket_ACTION;
    public static Field TeamPacket_PLAYERS;
    private static Class<Enum> EnumNameTagVisibility;
    private static Class<Enum> EnumTeamPush;
    private static Constructor<?> newScoreboardTeam;
    private static Method ScoreboardTeam_getPlayerNameSet;
    private static Method ScoreboardTeam_setNameTagVisibility;
    private static Method ScoreboardTeam_setCollisionRule;
    private static Method ScoreboardTeam_setPrefix;
    private static Method ScoreboardTeam_setSuffix;
    private static Method ScoreboardTeam_setColor;
    private static Method ScoreboardTeam_setAllowFriendlyFire;
    private static Method ScoreboardTeam_setCanSeeFriendlyInvisibles;
    private static Enum[] EnumChatFormat_values;

    @NotNull private final BukkitBridgePlayer player;
    @Getter private final Map<String, String> expectedTeams = new ConcurrentHashMap<>();

    public static void load(NMSStorage nms) throws ReflectiveOperationException {

        Class<?> scoreboardTeam;
        Class<?> scoreboardObjective;
        Class<?> scoreboardScoreClass;
        Class<?> IScoreboardCriteria;
        Class<?> scoreboard;
        Class<?> scorePacketClass;
        Class<?> IChatBaseComponent;
        Class<Enum> enumChatFormatClass;
        Class<?> ChatSerializer;
        if (minorVersion >= 17) {
            IChatBaseComponent = Class.forName("net.minecraft.network.chat.IChatBaseComponent");
            ChatSerializer = Class.forName("net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");
            enumChatFormatClass = (Class<Enum>) Class.forName("net.minecraft.EnumChatFormat");
            DisplayObjectiveClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective");
            ObjectivePacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective");
            scoreboard = Class.forName("net.minecraft.world.scores.Scoreboard");
            scorePacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore");
            scoreboardObjective = Class.forName("net.minecraft.world.scores.ScoreboardObjective");
            scoreboardScoreClass = Class.forName("net.minecraft.world.scores.ScoreboardScore");
            IScoreboardCriteria = Class.forName("net.minecraft.world.scores.criteria.IScoreboardCriteria");
            EnumScoreboardHealthDisplay = (Class<Enum>) Class.forName("net.minecraft.world.scores.criteria.IScoreboardCriteria$EnumScoreboardHealthDisplay");
            EnumScoreboardAction = (Class<Enum>) Class.forName("net.minecraft.server.ScoreboardServer$Action");
            TeamPacketClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam");
            scoreboardTeam = Class.forName("net.minecraft.world.scores.ScoreboardTeam");
            EnumNameTagVisibility = (Class<Enum>) Class.forName("net.minecraft.world.scores.ScoreboardTeamBase$EnumNameTagVisibility");
            EnumTeamPush = (Class<Enum>) Class.forName("net.minecraft.world.scores.ScoreboardTeamBase$EnumTeamPush");
        } else {
            enumChatFormatClass = (Class<Enum>) nms.getLegacyClass("EnumChatFormat");
            DisplayObjectiveClass = nms.getLegacyClass("PacketPlayOutScoreboardDisplayObjective", "Packet208SetScoreboardDisplayObjective");
            ObjectivePacketClass = nms.getLegacyClass("PacketPlayOutScoreboardObjective", "Packet206SetScoreboardObjective");
            TeamPacketClass = nms.getLegacyClass("PacketPlayOutScoreboardTeam", "Packet209SetScoreboardTeam");
            scorePacketClass = nms.getLegacyClass("PacketPlayOutScoreboardScore", "Packet207SetScoreboardScore");
            scoreboard = nms.getLegacyClass("Scoreboard");
            scoreboardObjective = nms.getLegacyClass("ScoreboardObjective");
            scoreboardScoreClass = nms.getLegacyClass("ScoreboardScore");
            IScoreboardCriteria = nms.getLegacyClass("IScoreboardCriteria", "IObjective"); // 1.5.1+, 1.5
            scoreboardTeam = nms.getLegacyClass("ScoreboardTeam");
            IChatBaseComponent = nms.getLegacyClass("IChatBaseComponent");
            ChatSerializer = nms.getLegacyClass("IChatBaseComponent$ChatSerializer", "ChatSerializer");
            EnumScoreboardHealthDisplay = (Class<Enum>) nms.getLegacyClass("IScoreboardCriteria$EnumScoreboardHealthDisplay", "EnumScoreboardHealthDisplay");
            EnumScoreboardAction = (Class<Enum>) nms.getLegacyClass("ScoreboardServer$Action", "PacketPlayOutScoreboardScore$EnumScoreboardAction", "EnumScoreboardAction");
            EnumNameTagVisibility = (Class<Enum>) nms.getLegacyClass("ScoreboardTeamBase$EnumNameTagVisibility", "EnumNameTagVisibility");
            if (minorVersion >= 9) {
                EnumTeamPush = (Class<Enum>) nms.getLegacyClass("ScoreboardTeamBase$EnumTeamPush");
            }
        }
        DESERIALIZE = ReflectionUtils.getMethods(ChatSerializer, Object.class, String.class).get(0);
        emptyScoreboard = scoreboard.getConstructor().newInstance();
        IScoreboardCriteria_self = ReflectionUtils.getFields(IScoreboardCriteria, IScoreboardCriteria).get(0);
        if (is1_20_2Plus) {
            Class<?> DisplaySlot = Class.forName("net.minecraft.world.scores.DisplaySlot");
            DisplayObjective_POSITION = ReflectionUtils.getOnlyField(DisplayObjectiveClass, DisplaySlot);
            newDisplayObjective = DisplayObjectiveClass.getConstructor(DisplaySlot, scoreboardObjective);
            DisplaySlot_values = (Enum[]) DisplaySlot.getDeclaredMethod("values").invoke(null);
        } else {
            DisplayObjective_POSITION = ReflectionUtils.getOnlyField(DisplayObjectiveClass, int.class);
            newDisplayObjective = DisplayObjectiveClass.getConstructor(int.class, scoreboardObjective);
        }
        DisplayObjective_OBJECTIVE_NAME = ReflectionUtils.getOnlyField(DisplayObjectiveClass, String.class);
        newScoreboardObjective = ReflectionUtils.getOnlyConstructor(scoreboardObjective);
        Objective_OBJECTIVE_NAME = ReflectionUtils.getFields(ObjectivePacketClass, String.class).get(0);
        List<Field> list = ReflectionUtils.getFields(ObjectivePacketClass, int.class);
        Objective_METHOD = list.get(list.size()-1);
        newScoreboardScore = scoreboardScoreClass.getConstructor(scoreboard, scoreboardObjective, String.class);
        newScoreboardTeam = scoreboardTeam.getConstructor(scoreboard, String.class);
        TeamPacket_NAME = ReflectionUtils.getFields(TeamPacketClass, String.class).get(0);
        TeamPacket_ACTION = ReflectionUtils.getInstanceFields(TeamPacketClass, int.class).get(0);
        TeamPacket_PLAYERS = ReflectionUtils.getOnlyField(TeamPacketClass, Collection.class);
        ScoreboardTeam_getPlayerNameSet = ReflectionUtils.getOnlyMethod(scoreboardTeam, Collection.class);
        if (minorVersion >= 13) {
            newScorePacket_1_13 = scorePacketClass.getConstructor(EnumScoreboardAction, String.class, String.class, int.class);
            newObjectivePacket = ObjectivePacketClass.getConstructor(scoreboardObjective, int.class);
            Objective_DISPLAY_NAME = ReflectionUtils.getOnlyField(ObjectivePacketClass, IChatBaseComponent);
            ScoreboardTeam_setColor = ReflectionUtils.getOnlyMethod(scoreboardTeam, void.class, enumChatFormatClass);
            EnumChatFormat_values = nms.getEnumValues(enumChatFormatClass);
        } else {
            newScorePacket_String = scorePacketClass.getConstructor(String.class);
            newObjectivePacket = ObjectivePacketClass.getConstructor();
            Objective_DISPLAY_NAME = ReflectionUtils.getFields(ObjectivePacketClass, String.class).get(1);
            if (minorVersion >= 8) {
                newScorePacket = scorePacketClass.getConstructor(scoreboardScoreClass);
                Objective_RENDER_TYPE = ReflectionUtils.getOnlyField(ObjectivePacketClass, EnumScoreboardHealthDisplay);
            } else {
                newScorePacket = scorePacketClass.getConstructor(scoreboardScoreClass, int.class);
            }
        }
        if (minorVersion >= 9) {
            ScoreboardTeam_setCollisionRule = ReflectionUtils.getOnlyMethod(scoreboardTeam, void.class, EnumTeamPush);
        }
        if (minorVersion >= 17) {
            TeamPacketConstructor_of = ReflectionUtils.getOnlyMethod(TeamPacketClass, TeamPacketClass, scoreboardTeam);
            TeamPacketConstructor_ofBoolean = ReflectionUtils.getOnlyMethod(TeamPacketClass, TeamPacketClass, scoreboardTeam, boolean.class);
        } else {
            newTeamPacket = TeamPacketClass.getConstructor(scoreboardTeam, int.class);
        }
        ScoreboardScore_setScore = ReflectionUtils.getMethod(
                scoreboardScoreClass,
                new String[] {"func_96647_c", "setScore", "b", "c", "m_83402_"}, // {Thermos, 1.5.1 - 1.17.1 & 1.20.2+, 1.18 - 1.20.1, 1.5, Mohist 1.18.2}
                int.class
        );
        ScoreboardTeam_setAllowFriendlyFire = ReflectionUtils.getMethod(
                scoreboardTeam,
                new String[] {"func_96660_a", "setAllowFriendlyFire", "a", "m_83355_"}, // {Thermos, 1.5.1+, 1.5 & 1.18+, Mohist 1.18.2}
                boolean.class
        );
        ScoreboardTeam_setCanSeeFriendlyInvisibles = ReflectionUtils.getMethod(
                scoreboardTeam,
                new String[] {"func_98300_b", "setCanSeeFriendlyInvisibles", "b", "m_83362_", "setSeeFriendlyInvisibles"}, // {Thermos, 1.5.1+, 1.5 & 1.18+, Mohist 1.18.2, 1.20.2+}
                boolean.class
        );
        if (minorVersion >= 13) {
            ScoreboardTeam_setPrefix = ReflectionUtils.getMethod(
                    scoreboardTeam,
                    new String[]{"setPrefix", "b", "m_83360_", "setPlayerPrefix"}, // {1.17.1-, 1.18 - 1.20.1, Mohist 1.18.2, 1.20.2+}
                    IChatBaseComponent
            );
            ScoreboardTeam_setSuffix = ReflectionUtils.getMethod(
                    scoreboardTeam,
                    new String[]{"setSuffix", "c", "m_83365_", "setPlayerSuffix"}, // {1.17.1-, 1.18 - 1.20.1, Mohist 1.18.2, 1.20.2+}
                    IChatBaseComponent
            );
        } else {
            ScoreboardTeam_setPrefix = ReflectionUtils.getMethod(
                    scoreboardTeam,
                    new String[] {"func_96666_b", "setPrefix", "b"}, // {Thermos, 1.5.1+, 1.5}
                    String.class
            );
            ScoreboardTeam_setSuffix = ReflectionUtils.getMethod(
                    scoreboardTeam,
                    new String[] {"func_96662_c", "setSuffix", "c"}, // {Thermos, 1.5.1+, 1.5}
                    String.class
            );
        }
        if (minorVersion >= 8) {
            ScoreboardTeam_setNameTagVisibility = ReflectionUtils.getMethod(
                    scoreboardTeam,
                    new String[] {"setNameTagVisibility", "a", "m_83346_"}, // {1.8.1+, 1.8 & 1.18+, Mohist 1.18.2}
                    EnumNameTagVisibility
            );
        }
        available = true;
    }

    @Override
    @SneakyThrows
    public void setDisplaySlot(@NonNull DisplaySlot slot, @NonNull String objective) {
        if (!available) return;
        Object displaySlot;
        if (is1_20_2Plus) {
            displaySlot = DisplaySlot_values[slot.ordinal()];
        } else {
            displaySlot = slot.ordinal();
        }
        player.sendPacket(newDisplayObjective.newInstance(displaySlot, newScoreboardObjective(objective)));
    }

    @Override
    @SneakyThrows
    public void setScore(@NonNull String objective, @NonNull String playerName, int score,
                         @Nullable String displayName, @Nullable String numberFormat) {
        if (!available) return;
        if (minorVersion >= 13) {
            player.sendPacket(newScorePacket_1_13.newInstance(
                    Enum.valueOf(EnumScoreboardAction, "CHANGE"), objective, playerName, score));
        } else {
            Object scoreboardScore = newScoreboardScore.newInstance(emptyScoreboard, newScoreboardObjective(objective), playerName);
            ScoreboardScore_setScore.invoke(scoreboardScore, score);
            player.sendPacket(newScorePacket.newInstance(scoreboardScore));
        }
    }

    @Override
    @SneakyThrows
    public void removeScore(@NonNull String objective, @NonNull String playerName) {
        if (!available) return;
        if (minorVersion >= 13) {
            player.sendPacket(newScorePacket_1_13.newInstance(
                    Enum.valueOf(EnumScoreboardAction, "REMOVE"), objective, playerName, 0));
        } else {
            player.sendPacket(newScorePacket_String.newInstance(playerName));
        }
    }

    @Override
    public void registerObjective(@NonNull String objectiveName, @NonNull String title,
                                  boolean hearts, @Nullable String numberFormat) {
        sendObjectivePacket(0, objectiveName, title, hearts);
    }

    @Override
    public void unregisterObjective(@NonNull String objectiveName) {
        sendObjectivePacket(1, objectiveName, "", false);
    }

    @Override
    public void updateObjective(@NonNull String objectiveName, @NonNull String title,
                                boolean hearts, @Nullable String numberFormat) {
        sendObjectivePacket(2, objectiveName, title, hearts);
    }

    @SneakyThrows
    private void sendObjectivePacket(int action, String objectiveName, String title, boolean hearts) {
        if (!available) return;
        Object display = Enum.valueOf(EnumScoreboardHealthDisplay, hearts ? "HEARTS" : "INTEGER");
        if (minorVersion >= 13) {
            player.sendPacket(newObjectivePacket.newInstance(
                    newScoreboardObjective.newInstance(
                            null,
                            objectiveName,
                            null,
                            toComponent(title),
                            display
                    ), action
            ));
        } else {
            Object nmsPacket = newObjectivePacket.newInstance();
            Objective_OBJECTIVE_NAME.set(nmsPacket, objectiveName);
            Objective_DISPLAY_NAME.set(nmsPacket, title);
            Objective_RENDER_TYPE.set(nmsPacket, display);
            Objective_METHOD.set(nmsPacket, action);
            player.sendPacket(nmsPacket);
        }
    }

    @Override
    @SneakyThrows
    public void registerTeam(@NonNull String name, @NonNull String prefix, @NonNull String suffix, @NonNull String visibility,
                             @NonNull String collision, @NonNull Collection<String> players, int options, int color) {
        if (!available) return;
        Object team = newScoreboardTeam.newInstance(emptyScoreboard, name);
        ((Collection<String>)ScoreboardTeam_getPlayerNameSet.invoke(team)).addAll(players);
        ScoreboardTeam_setAllowFriendlyFire.invoke(team, (options & 0x1) > 0);
        ScoreboardTeam_setCanSeeFriendlyInvisibles.invoke(team, (options & 0x2) > 0);
        ScoreboardTeam_setNameTagVisibility.invoke(team, Enum.valueOf(EnumNameTagVisibility, visibility.equals("always") ? "ALWAYS" : "NEVER"));
        if (minorVersion >= 9) ScoreboardTeam_setCollisionRule.invoke(team, Enum.valueOf(EnumTeamPush, collision.equals("always") ? "ALWAYS" : "NEVER"));
        if (minorVersion >= 13) {
            ScoreboardTeam_setPrefix.invoke(team, toComponent(prefix));
            ScoreboardTeam_setSuffix.invoke(team, toComponent(suffix));
            ScoreboardTeam_setColor.invoke(team, EnumChatFormat_values[color]);
        } else {
            ScoreboardTeam_setPrefix.invoke(team, prefix);
            ScoreboardTeam_setSuffix.invoke(team, suffix);
        }
        if (minorVersion >= 17) {
            player.sendPacket(TeamPacketConstructor_ofBoolean.invoke(null, team, true));
        } else {
            player.sendPacket(newTeamPacket.newInstance(team, 0));
        }
        players.forEach(player -> expectedTeams.put(player, name));
    }

    @Override
    @SneakyThrows
    public void unregisterTeam(@NonNull String name) {
        if (!available) return;
        Object team = newScoreboardTeam.newInstance(emptyScoreboard, name);
        if (minorVersion >= 17) {
            player.sendPacket(TeamPacketConstructor_of.invoke(null, team));
        } else {
            player.sendPacket(newTeamPacket.newInstance(team, 1));
        }
        expectedTeams.keySet().forEach(p -> expectedTeams.remove(p, name));
    }

    @Override
    @SneakyThrows
    public void updateTeam(@NonNull String name, @NonNull String prefix, @NonNull String suffix, @NonNull String visibility,
                           @NonNull String collision, int options, int color) {
        if (!available) return;
        Object team = newScoreboardTeam.newInstance(emptyScoreboard, name);
        ScoreboardTeam_setAllowFriendlyFire.invoke(team, (options & 0x1) > 0);
        ScoreboardTeam_setCanSeeFriendlyInvisibles.invoke(team, (options & 0x2) > 0);
        ScoreboardTeam_setNameTagVisibility.invoke(team, Enum.valueOf(EnumNameTagVisibility, visibility.equals("always") ? "ALWAYS" : "NEVER"));
        if (minorVersion >= 9) ScoreboardTeam_setCollisionRule.invoke(team, Enum.valueOf(EnumTeamPush, collision.equals("always") ? "ALWAYS" : "NEVER"));
        if (minorVersion >= 13) {
            ScoreboardTeam_setPrefix.invoke(team, toComponent(prefix));
            ScoreboardTeam_setSuffix.invoke(team, toComponent(suffix));
            ScoreboardTeam_setColor.invoke(team, EnumChatFormat_values[color]);
        } else {
            ScoreboardTeam_setPrefix.invoke(team, prefix);
            ScoreboardTeam_setSuffix.invoke(team, suffix);
        }
        if (minorVersion >= 17) {
            player.sendPacket(TeamPacketConstructor_ofBoolean.invoke(null, team, false));
        } else {
            player.sendPacket(newTeamPacket.newInstance(team, 2));
        }
    }

    @SneakyThrows
    private Object newScoreboardObjective(String objectiveName) {
        if (!available) throw new IllegalStateException();
        if (minorVersion >= 13) {
            return newScoreboardObjective.newInstance(null, objectiveName, null, deserialize("{\"text\":\"\"}"), null);
        }
        return newScoreboardObjective.newInstance(null, objectiveName, IScoreboardCriteria_self.get(null));
    }

    @SneakyThrows
    private Object deserialize(String json) {
        return DESERIALIZE.invoke(null, json);
    }

    private Object toComponent(String string) {
        return deserialize(IChatBaseComponent.optimizedComponent(string).toString(player.getProtocolVersion()));
    }
}
