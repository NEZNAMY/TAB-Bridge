package me.neznamy.tab.bridge.shared.hook;

import lombok.Getter;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.function.Function;

/**
 * Class that hooks into LuckPerms if installed.
 */
@Getter
public class LuckPermsHook {

    /** Instance of the class */
    @Getter private static final LuckPermsHook instance = new LuckPermsHook();

    /** Flag tracking if LuckPerms is installed or not */
    private final boolean installed = ReflectionUtils.classExists("net.luckperms.api.LuckPerms");

    /** Function retrieving group of player from LuckPerms */
    private final Function<BridgePlayer, String> groupFunction = p -> {
        User user = LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId());
        if (user == null) return "NONE";
        return user.getPrimaryGroup();
    };
}