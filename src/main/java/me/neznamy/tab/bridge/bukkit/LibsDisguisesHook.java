package me.neznamy.tab.bridge.bukkit;

import me.libraryaddict.disguise.DisguiseAPI;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bukkit.entity.Player;

/**
 * Class for hooking into LibsDisguises to get disguise status of players.
 */
public class LibsDisguisesHook {

    /** Flag tracking if LibsDisguises is installed or not */
    private static boolean installed = ReflectionUtils.classExists("me.libraryaddict.disguise.DisguiseAPI");

    /**
     * Returns {@code true} if LibsDisguises is installed and player is disguised,
     * {@code false} otherwise.
     *
     * @param   player
     *          Player to check
     * @return  {@code true} if disguised, {@code false} otherwise
     */
    public static boolean isDisguised(Player player) {
        try {
            return installed && DisguiseAPI.isDisguised(player);
        } catch (LinkageError e) {
            //java.lang.NoClassDefFoundError: Could not initialize class me.libraryaddict.disguise.DisguiseAPI
            installed = false;
            return false;
        }
    }
}