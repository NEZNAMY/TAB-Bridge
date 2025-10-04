package me.neznamy.tab.bridge.fabric;

import com.mojang.authlib.GameProfile;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.TABBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Interface for sending custom payloads to players for different MC versions.
 */
public interface VersionLoader {

    /** Resource location for the custom payload channel */
    @NotNull
    ResourceLocation ID = Objects.requireNonNull(ResourceLocation.tryParse(TABBridge.CHANNEL_NAME));

    /**
     * Registers the custom payload manager with the server.
     */
    void registerListeners();

    /**
     * Sends a plugin message to the specified player.
     *
     * @param player
     *        The player to send the message to
     * @param message
     *        The message to send
     */
    void sendCustomPayload(@NonNull ServerPlayer player, byte @NonNull [] message);

    /**
     * Gets the current level of the specified player.
     *
     * @param player
     *        The player to get the level for
     * @return
     *        The current level of the player
     */
    @NotNull
    Level getLevel(@NonNull ServerPlayer player);

    /**
     * Creates a new text component with the specified text.
     *
     * @param text
     *        The text to create the component with
     * @return
     *        A new text component containing the specified text
     */
    @NotNull
    Component newTextComponent(@NonNull String text);

    /**
     * Gets the name of the specified game profile.
     *
     * @param profile
     *        The game profile to get the name for
     * @return
     *        The name of the game profile
     */
    @NotNull
    String getName(@NonNull GameProfile profile);
}
