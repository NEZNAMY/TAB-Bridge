package me.neznamy.tab.bridge.bukkit;

import com.google.common.collect.Iterables;
import me.neznamy.tab.bridge.bukkit.nms.DataWatcher;
import me.neznamy.tab.bridge.bukkit.nms.NMSStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.UUID;

public class BukkitPacketBuilder {

	private static final BukkitPacketBuilder instance = new BukkitPacketBuilder();

	//nms storage
	private final NMSStorage nms = NMSStorage.getInstance();

	//entity type ids
	private final EnumMap<EntityType, Integer> entityIds = new EnumMap<>(EntityType.class);

    private Object emptyScoreboard;
	private Object dummyEntity;

	/**
	 * Constructs new instance
	 */
	public BukkitPacketBuilder() {
		if (nms.getMinorVersion() >= 13) {
			entityIds.put(EntityType.ARMOR_STAND, 1);
			entityIds.put(EntityType.WITHER, 83);
		} else {
			entityIds.put(EntityType.WITHER, 64);
			if (nms.getMinorVersion() >= 8){
				entityIds.put(EntityType.ARMOR_STAND, 30);
			}
		}
        try {
            emptyScoreboard = nms.newScoreboard.newInstance();
        } catch (ReflectiveOperationException e) {
            Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB] Failed to create instance of \"Scoreboard\"");
        }
		if (nms.getMinorVersion() >= 8) {
			try {
				dummyEntity = nms.newEntityArmorStand.newInstance(nms.World_getHandle.invoke(Bukkit.getWorlds().get(0)), 0, 0, 0);
			} catch (ReflectiveOperationException e) {
				Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB] Failed to create instance of \"EntityArmorStand\"");
			}
		}
	}

	public static BukkitPacketBuilder getInstance() {
		return instance;
	}

	public Object entityDestroy(int... entities) {
        try {
            try {
                return nms.newPacketPlayOutEntityDestroy.newInstance(new Object[]{entities});
            } catch (IllegalArgumentException e) {
                //1.17.0
                return nms.newPacketPlayOutEntityDestroy.newInstance(entities[0]);
            }
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

	public Object entityMetadata(int entityId, DataWatcher dataWatcher) {
        try {
            return nms.newPacketPlayOutEntityMetadata.newInstance(entityId, dataWatcher.toNMS(), true);
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

	public Object entitySpawn(int entityId, UUID uniqueId, EntityType entityType, Location location, DataWatcher dataWatcher) {
        try {
            Object nmsPacket;
            if (nms.getMinorVersion() >= 17) {
                nmsPacket = nms.newPacketPlayOutSpawnEntityLiving.newInstance(dummyEntity);
            } else {
                nmsPacket = nms.newPacketPlayOutSpawnEntityLiving.newInstance();
            }
            nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_ENTITYID, entityId);
            nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_ENTITYTYPE, entityIds.get(entityType));
            nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_YAW, (byte)(location.getYaw() * 256.0f / 360.0f));
            nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_PITCH, (byte)(location.getPitch() * 256.0f / 360.0f));
            if (nms.getMinorVersion() <= 14) {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_DATAWATCHER, dataWatcher.toNMS());
            }
            if (nms.getMinorVersion() >= 9) {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_UUID, uniqueId);
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_X, location.getX());
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Y, location.getY());
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Z, location.getZ());
            } else {
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_X, floor(location.getX()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Y, floor(location.getY()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutSpawnEntityLiving_Z, floor(location.getZ()*32));
            }
            return nmsPacket;
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

	public Object entityTeleport(int entityId, Location location) {
        try {
            Object nmsPacket;
            if (nms.getMinorVersion() >= 17) {
                nmsPacket = nms.newPacketPlayOutEntityTeleport.newInstance(dummyEntity);
            } else {
                nmsPacket = nms.newPacketPlayOutEntityTeleport.newInstance();
            }
            nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_ENTITYID, entityId);
            if (nms.getMinorVersion() >= 9) {
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_X, location.getX());
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Y, location.getY());
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Z, location.getZ());
            } else {
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_X, floor(location.getX()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Y, floor(location.getY()*32));
                nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_Z, floor(location.getZ()*32));
            }
            nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_YAW, (byte) (location.getYaw()/360*256));
            nms.setField(nmsPacket, nms.PacketPlayOutEntityTeleport_PITCH, (byte) (location.getPitch()/360*256));
            return nmsPacket;
        } catch (ReflectiveOperationException e) {
            return null;
        }
	}

    public Object scoreboardDisplayObjective(int slot, String objective) throws ReflectiveOperationException {
        return nms.newPacketPlayOutScoreboardDisplayObjective.newInstance(slot, newScoreboardObjective(objective));
    }

    public Object scoreboardObjective(String objective, int action, String displayName, String displayComponent,
                                      int renderType) throws ReflectiveOperationException {
        if (nms.getMinorVersion() >= 13) {
            return nms.newPacketPlayOutScoreboardObjective.newInstance(nms.newScoreboardObjective.newInstance(null, objective, null,
                    nms.DESERIALIZE.invoke(null, displayComponent), nms.EnumScoreboardHealthDisplay_values[renderType]), action);
        }

        Object nmsPacket = nms.newPacketPlayOutScoreboardObjective.newInstance();
        nms.setField(nmsPacket, nms.PacketPlayOutScoreboardObjective_OBJECTIVENAME, objective);
        nms.setField(nmsPacket, nms.PacketPlayOutScoreboardObjective_DISPLAYNAME, displayName);
        if (nms.getMinorVersion() >= 8) {
            nms.setField(nmsPacket, nms.PacketPlayOutScoreboardObjective_RENDERTYPE, nms.EnumScoreboardHealthDisplay_values[renderType]);
        }
        nms.setField(nmsPacket, nms.PacketPlayOutScoreboardObjective_METHOD, action);
        return nmsPacket;
    }

    public Object scoreboardScore(String objective, int action, String player, int score) throws ReflectiveOperationException {
        if (nms.getMinorVersion() >= 13) {
            return nms.newPacketPlayOutScoreboardScore_1_13.newInstance(nms.EnumScoreboardAction_values[action], objective, player, score);
        }
        if (action == 1) {
            return nms.newPacketPlayOutScoreboardScore_String.newInstance(player);
        }
        Object scoreboardScore = nms.newScoreboardScore.newInstance(emptyScoreboard, newScoreboardObjective(objective), player);
        nms.ScoreboardScore_setScore.invoke(scoreboardScore, score);
        if (nms.getMinorVersion() >= 8) {
            return nms.newPacketPlayOutScoreboardScore.newInstance(scoreboardScore);
        }
        return nms.newPacketPlayOutScoreboardScore.newInstance(scoreboardScore, 0);
    }

    public Object scoreboardTeam(String name, int action, Collection<String> players, String prefix, String prefixComponent,
                                 String suffix, String suffixComponent, int options, String visibility,
                                 String collision, int color) throws ReflectiveOperationException {
        if (nms.PacketPlayOutScoreboardTeam == null) return null; //fabric
        Object team = nms.newScoreboardTeam.newInstance(emptyScoreboard, name);
        ((Collection<String>)nms.ScoreboardTeam_getPlayerNameSet.invoke(team)).addAll(players);
        nms.ScoreboardTeam_setAllowFriendlyFire.invoke(team, (options & 0x1) > 0);
        nms.ScoreboardTeam_setCanSeeFriendlyInvisibles.invoke(team, (options & 0x2) > 0);
        if (nms.getMinorVersion() >= 13) {
            createTeamModern(team, prefixComponent, suffixComponent, color, visibility, collision);
        } else {
            createTeamLegacy(team, prefix, suffix, visibility, collision);
        }
        if (nms.getMinorVersion() >= 17) {
            switch (action) {
                case 0:
                    return nms.PacketPlayOutScoreboardTeam_ofBoolean.invoke(null, team, true);
                case 1:
                    return nms.PacketPlayOutScoreboardTeam_of.invoke(null, team);
                case 2:
                    return nms.PacketPlayOutScoreboardTeam_ofBoolean.invoke(null, team, false);
                case 3:
                    return nms.PacketPlayOutScoreboardTeam_ofString.invoke(null, team, Iterables.getFirst(players, ""), nms.PacketPlayOutScoreboardTeam_PlayerAction_values[0]);
                case 4:
                    return nms.PacketPlayOutScoreboardTeam_ofString.invoke(null, team, Iterables.getFirst(players, ""), nms.PacketPlayOutScoreboardTeam_PlayerAction_values[1]);
                default:
                    throw new IllegalArgumentException("Invalid action: " + action);
            }
        }
        return nms.newPacketPlayOutScoreboardTeam.newInstance(team, action);
    }

    private void createTeamModern(Object team, String prefixComponent, String suffixComponent, int color, String visibility, String collision) throws ReflectiveOperationException {
        nms.ScoreboardTeam_setPrefix.invoke(team, nms.DESERIALIZE.invoke(null, prefixComponent));
        nms.ScoreboardTeam_setSuffix.invoke(team, nms.DESERIALIZE.invoke(null, suffixComponent));
        nms.ScoreboardTeam_setColor.invoke(team, nms.EnumChatFormat_values[color]);
        nms.ScoreboardTeam_setNameTagVisibility.invoke(team, String.valueOf(visibility).equals("always") ? nms.EnumNameTagVisibility_values[0] : nms.EnumNameTagVisibility_values[1]);
        nms.ScoreboardTeam_setCollisionRule.invoke(team, String.valueOf(collision).equals("always") ? nms.EnumTeamPush_values[0] : nms.EnumTeamPush_values[1]);
    }

    private void createTeamLegacy(Object team, String prefix, String suffix, String visibility, String collision) throws ReflectiveOperationException {
        if (prefix != null) nms.ScoreboardTeam_setPrefix.invoke(team, prefix);
        if (suffix != null) nms.ScoreboardTeam_setSuffix.invoke(team, suffix);
        if (nms.getMinorVersion() >= 8) nms.ScoreboardTeam_setNameTagVisibility.invoke(team, String.valueOf(visibility).equals("always") ? nms.EnumNameTagVisibility_values[0] : nms.EnumNameTagVisibility_values[1]);
        if (nms.getMinorVersion() >= 9) nms.ScoreboardTeam_setCollisionRule.invoke(team, String.valueOf(collision).equals("always") ? nms.EnumTeamPush_values[0] : nms.EnumTeamPush_values[1]);
    }

	private int floor(double paramDouble){
		int i = (int)paramDouble;
		return paramDouble < i ? i - 1 : i;
	}

    private Object newScoreboardObjective(String objectiveName) throws ReflectiveOperationException {
        if (nms.getMinorVersion() >= 13) {
            return nms.newScoreboardObjective.newInstance(null, objectiveName, null, nms.newChatComponentText.newInstance(""), null);
        }
        return nms.newScoreboardObjective.newInstance(null, objectiveName, nms.IScoreboardCriteria_self.get(null));
    }
}