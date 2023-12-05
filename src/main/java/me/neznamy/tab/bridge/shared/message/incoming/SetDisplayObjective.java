package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import org.jetbrains.annotations.NotNull;

public class SetDisplayObjective implements IncomingMessage {

    private int slot;
    private String objective;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        slot = in.readInt();
        objective = in.readUTF();
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        player.getScoreboard().setDisplaySlot(slot, objective);
    }
}
