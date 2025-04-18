package me.neznamy.tab.bridge.shared.message.outgoing;

import com.google.common.io.ByteArrayDataOutput;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public interface OutgoingMessage {

    /** Map of packet IDs of custom plugin messages */
    Map<Class<? extends OutgoingMessage>, Byte> PACKET_IDS = new HashMap<Class<? extends OutgoingMessage>, Byte>() {{
        byte i = 0;
        put(PlaceholderError.class, i++);
        put(UpdateGameMode.class, i++);
        put(HasPermission.class, i++);
        put(SetInvisible.class, i++);
        put(SetDisguised.class, i++);
        put(WorldChange.class, i++);
        put(GroupChange.class, i++);
        put(SetVanished.class, i++);
        put(UpdatePlaceholder.class, i); // Same ID as relational
        put(UpdateRelationalPlaceholder.class, i++);
        put(PlayerJoinResponse.class, i++);
        put(RegisterPlaceholder.class, i);
    }};

    /**
     * Writes the content into provided byte output.
     *
     * @param   out
     *          Output to write into
     */
    void write(@NonNull ByteArrayDataOutput out);
}
