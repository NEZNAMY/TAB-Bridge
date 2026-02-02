package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import lombok.Getter;
import lombok.NonNull;
import me.neznamy.tab.bridge.shared.placeholder.PlaceholderReplacementPattern;

import java.util.HashMap;
import java.util.Map;

/**
 * Incoming message for a player joining the server.
 */
@Getter
public class PlayerJoin {

    /** Whether group forwarding is enabled */
    private final boolean groupForwarding;

    /** Used placeholders along with their refresh intervals */
    private final Map<String, Integer> placeholders = new HashMap<>();

    /** Placeholder replacement patterns for tab expansion */
    private final Map<String, PlaceholderReplacementPattern> replacements = new HashMap<>();

    /**
     * Constructs a new instance and reads data from given input stream.
     *
     * @param   in
     *          Input stream to read data from
     */
    public PlayerJoin(@NonNull ByteArrayDataInput in) {
        in.readInt(); // Unused protocol version, forgot to remove it in v6
        groupForwarding = in.readBoolean();
        int placeholderCount = in.readInt();
        for (int i=0; i<placeholderCount; i++) {
            placeholders.put(in.readUTF(), in.readInt());
        }
        int replacementCount = in.readInt();
        for (int i=0; i<replacementCount; i++) {
            String placeholder = in.readUTF();
            Map<Object, Object> rules = new HashMap<>();
            int ruleCount = in.readInt();
            for (int j=0; j<ruleCount; j++) {
                rules.put(in.readUTF(), in.readUTF());
            }
            replacements.put(placeholder, new PlaceholderReplacementPattern(placeholder, rules));
        }
    }
}
