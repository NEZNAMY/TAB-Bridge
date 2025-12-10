package me.neznamy.tab.bridge.fabric.hook;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import lombok.Getter;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import me.neznamy.tab.bridge.shared.features.TabExpansion;
import me.neznamy.tab.bridge.shared.message.outgoing.RegisterPlaceholder;
import me.neznamy.tab.bridge.shared.placeholder.PlaceholderReplacementPattern;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * TAB's expansion for Text PlaceholderAPI
 */
@Getter
public class FabricTabExpansion implements TabExpansion {

    /** Map holding all placeholder values for all players */
    private final Map<BridgePlayer, Map<String, String>> values = new WeakHashMap<>();

    private final Map<BridgePlayer, Set<String>> sentRequests = new WeakHashMap<>();

    /**
     * Constructs a new instance of this class and registers all placeholders.
     */
    public FabricTabExpansion() {
        List<String> placeholders = Arrays.asList(
                "tabprefix",
                "tabsuffix",
                "tagprefix",
                "tagsuffix",
                "customtabname",
                "tabprefix_raw",
                "tabsuffix_raw",
                "tagprefix_raw",
                "tagsuffix_raw",
                "customtabname_raw",
                "scoreboard_name",
                "scoreboard_visible",
                "bossbar_visible",
                "nametag_visibility"
        );
        for (String placeholder : placeholders) {
            registerPlaceholder(placeholder, (ctx, arg) -> {
                if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
                BridgePlayer player = TABBridge.getInstance().getPlayer(ctx.player().getUUID());
                return PlaceholderResult.value(values.get(player).get(placeholder));
            });
        }

        registerPlaceholder("replace", (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            if (arg == null) return PlaceholderResult.invalid("No placeholder!");

            String text = "%" + arg.substring(8) + "%";
            String textBefore;
            do {
                textBefore = text;
                for (String placeholder : detectPlaceholders(text)) {
                    PlaceholderReplacementPattern pattern = TABBridge.getInstance().getDataBridge().getReplacements().get(placeholder);
                    if (pattern != null) text = text.replace(placeholder, pattern.findReplacement(Placeholders.parseText(
                            Component.literal(placeholder),
                            PlaceholderContext.of(ctx.player())
                    ).getString()));
                }
            } while (!textBefore.equals(text));

            return PlaceholderResult.value(text);
        });

        registerPlaceholder("placeholder", (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            if (arg == null) return PlaceholderResult.invalid("No placeholder!");

            BridgePlayer player = TABBridge.getInstance().getPlayer(ctx.player().getUUID());

            String placeholder = "%" + arg + "%";
            if (!sentRequests.computeIfAbsent(player, pl -> new HashSet<>()).contains(placeholder)){
                BridgePlayer pl = TABBridge.getInstance().getPlayer(player.getUniqueId());
                if (pl != null) {
                    sentRequests.get(player).add(placeholder);
                    pl.sendPluginMessage(new RegisterPlaceholder(placeholder));
                }
            }
            return PlaceholderResult.value(values.get(player).get(arg));
        });
    }

    private void registerPlaceholder(String identifier, PlaceholderHandler handler) {
        Placeholders.register(Identifier.tryParse("tab:" + identifier), handler);
    }

    @Override
    public void setValue(@NotNull BridgePlayer player, @NotNull String identifier, @NotNull String value) {
        values.computeIfAbsent(player, p -> new HashMap<>()).put(identifier, value);
    }

    @Override
    public boolean unregister() {
        return false;
    }
}
