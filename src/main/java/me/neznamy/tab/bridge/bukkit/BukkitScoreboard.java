package me.neznamy.tab.bridge.bukkit;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import me.neznamy.tab.bridge.shared.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class BukkitScoreboard implements Scoreboard {

    @NotNull private final BukkitBridgePlayer player;
    @Nullable private final NMSStorage nms = NMSStorage.getInstance();
    @Getter private final Map<String, String> expectedTeams = new HashMap<>();

    @Override
    public void setDisplaySlot(@NonNull DisplaySlot slot, @NonNull String objective) {
        if (nms == null) return;
        try {
            player.sendPacket(nms.newPacketPlayOutScoreboardDisplayObjective.newInstance(
                    slot.ordinal(),
                    newScoreboardObjective(objective)
            ));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setScore(@NonNull String objective, @NonNull String playerName, int score) {
        if (nms == null) return;
        try {
            if (nms.getMinorVersion() >= 13) {
                player.sendPacket(nms.newPacketPlayOutScoreboardScore_1_13.newInstance(
                        Enum.valueOf(nms.EnumScoreboardAction, "CHANGE"), objective, playerName, score));
            }
            Object scoreboardScore = nms.newScoreboardScore.newInstance(nms.emptyScoreboard, newScoreboardObjective(objective), playerName);
            nms.ScoreboardScore_setScore.invoke(scoreboardScore, score);
            player.sendPacket(nms.newPacketPlayOutScoreboardScore.newInstance(scoreboardScore));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void removeScore(@NonNull String objective, @NonNull String playerName) {
        if (nms == null) return;
        try {
            if (nms.getMinorVersion() >= 13) {
                player.sendPacket(nms.newPacketPlayOutScoreboardScore_1_13.newInstance(
                        Enum.valueOf(nms.EnumScoreboardAction, "REMOVE"), objective, playerName, 0));
            }
            player.sendPacket(nms.newPacketPlayOutScoreboardScore_String.newInstance(playerName));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void registerObjective(@NonNull String objectiveName, @NonNull String title, @NonNull String titleComponent, boolean hearts) {
        sendObjectivePacket(0, objectiveName, title, titleComponent, hearts);
    }

    @Override
    public void unregisterObjective(@NonNull String objectiveName) {
        sendObjectivePacket(1, objectiveName, "", "{\"text\":\"\"}", false);
    }

    @Override
    public void updateObjective(@NonNull String objectiveName, @NonNull String title, @NonNull String titleComponent, boolean hearts) {
        sendObjectivePacket(2, objectiveName, title, titleComponent, hearts);
    }

    private void sendObjectivePacket(int action, String objectiveName, String title, String titleComponent, boolean hearts) {
        if (nms == null) return;
        try {
            Object display = Enum.valueOf(nms.EnumScoreboardHealthDisplay, hearts ? "HEARTS" : "INTEGER");
            if (nms.getMinorVersion() >= 13) {
                player.sendPacket(nms.newPacketPlayOutScoreboardObjective.newInstance(
                        nms.newScoreboardObjective.newInstance(
                                null,
                                objectiveName,
                                null,
                                nms.DESERIALIZE.invoke(null, titleComponent),
                                display
                        ), action
                ));
            }
            Object nmsPacket = nms.newPacketPlayOutScoreboardObjective.newInstance();
            nms.PacketPlayOutScoreboardObjective_OBJECTIVENAME.set(nmsPacket, objectiveName);
            nms.PacketPlayOutScoreboardObjective_DISPLAYNAME.set(nmsPacket, title);
            nms.PacketPlayOutScoreboardObjective_RENDERTYPE.set(nmsPacket, display);
            nms.PacketPlayOutScoreboardObjective_METHOD.set(nmsPacket, action);
            player.sendPacket(nmsPacket);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void registerTeam(@NonNull String name, @NonNull String prefix, @NonNull String prefixComponent,
                             @NonNull String suffix, @NonNull String suffixComponent, @NonNull String visibility,
                             @NonNull String collision, @NonNull Collection<String> players, int options, int color) {
        if (nms == null) return;
        try {
            Object team = nms.newScoreboardTeam.newInstance(nms.emptyScoreboard, name);
            ((Collection<String>)nms.ScoreboardTeam_getPlayerNameSet.invoke(team)).addAll(players);
            nms.ScoreboardTeam_setAllowFriendlyFire.invoke(team, (options & 0x1) > 0);
            nms.ScoreboardTeam_setCanSeeFriendlyInvisibles.invoke(team, (options & 0x2) > 0);
            nms.ScoreboardTeam_setNameTagVisibility.invoke(team, Enum.valueOf(nms.EnumNameTagVisibility, visibility.equals("always") ? "ALWAYS" : "NEVER"));
            if (nms.getMinorVersion() >= 9) nms.ScoreboardTeam_setCollisionRule.invoke(team, Enum.valueOf(nms.EnumTeamPush, collision.equals("always") ? "ALWAYS" : "NEVER"));
            if (nms.getMinorVersion() >= 13) {
                nms.ScoreboardTeam_setPrefix.invoke(team, nms.DESERIALIZE.invoke(null, prefixComponent));
                nms.ScoreboardTeam_setSuffix.invoke(team, nms.DESERIALIZE.invoke(null, suffixComponent));
                nms.ScoreboardTeam_setColor.invoke(team, nms.EnumChatFormat_values[color]);
            } else {
                nms.ScoreboardTeam_setPrefix.invoke(team, prefix);
                nms.ScoreboardTeam_setSuffix.invoke(team, suffix);
            }
            if (nms.getMinorVersion() >= 17) {
                player.sendPacket(nms.PacketPlayOutScoreboardTeam_ofBoolean.invoke(null, team, true));
            } else {
                player.sendPacket(nms.newPacketPlayOutScoreboardTeam.newInstance(team, 0));
            }
            players.forEach(player -> expectedTeams.put(player, name));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void unregisterTeam(@NonNull String name) {
        if (nms == null) return;
        try {
            Object team = nms.newScoreboardTeam.newInstance(nms.emptyScoreboard, name);
            if (nms.getMinorVersion() >= 17) {
                player.sendPacket(nms.PacketPlayOutScoreboardTeam_of.invoke(null, team));
            } else {
                player.sendPacket(nms.newPacketPlayOutScoreboardTeam.newInstance(team, 1));
            }
            expectedTeams.keySet().forEach(p -> expectedTeams.remove(p, name));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void updateTeam(@NonNull String name, @NonNull String prefix, @NonNull String prefixComponent,
                           @NonNull String suffix, @NonNull String suffixComponent, @NonNull String visibility,
                           @NonNull String collision, int options, int color) {
        if (nms == null) return;
        try {
            Object team = nms.newScoreboardTeam.newInstance(nms.emptyScoreboard, name);
            nms.ScoreboardTeam_setAllowFriendlyFire.invoke(team, (options & 0x1) > 0);
            nms.ScoreboardTeam_setCanSeeFriendlyInvisibles.invoke(team, (options & 0x2) > 0);
            nms.ScoreboardTeam_setNameTagVisibility.invoke(team, Enum.valueOf(nms.EnumNameTagVisibility, visibility.equals("always") ? "ALWAYS" : "NEVER"));
            if (nms.getMinorVersion() >= 9) nms.ScoreboardTeam_setCollisionRule.invoke(team, Enum.valueOf(nms.EnumTeamPush, collision.equals("always") ? "ALWAYS" : "NEVER"));
            if (nms.getMinorVersion() >= 13) {
                nms.ScoreboardTeam_setPrefix.invoke(team, nms.DESERIALIZE.invoke(null, prefixComponent));
                nms.ScoreboardTeam_setSuffix.invoke(team, nms.DESERIALIZE.invoke(null, suffixComponent));
                nms.ScoreboardTeam_setColor.invoke(team, nms.EnumChatFormat_values[color]);
            } else {
                nms.ScoreboardTeam_setPrefix.invoke(team, prefix);
                nms.ScoreboardTeam_setSuffix.invoke(team, suffix);
            }
            if (nms.getMinorVersion() >= 17) {
                player.sendPacket(nms.PacketPlayOutScoreboardTeam_ofBoolean.invoke(null, team, false));
            } else {
                player.sendPacket(nms.newPacketPlayOutScoreboardTeam.newInstance(team, 2));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object newScoreboardObjective(String objectiveName) throws ReflectiveOperationException {
        if (nms == null) throw new IllegalStateException();
        if (nms.getMinorVersion() >= 13) {
            return nms.newScoreboardObjective.newInstance(null, objectiveName, null, nms.DESERIALIZE.invoke(null, "{\"text\":\"\"}"), null);
        }
        return nms.newScoreboardObjective.newInstance(null, objectiveName, nms.IScoreboardCriteria_self.get(null));
    }
}
