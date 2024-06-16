package me.neznamy.tab.bridge.shared.message.incoming;

import com.google.common.io.ByteArrayDataInput;
import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.chat.IChatBaseComponent;
import org.jetbrains.annotations.NotNull;

public class ScoreboardObjective implements IncomingMessage {

    private String objective;
    private int action;
    private String title;
    private int renderType;
    private String numberFormat;

    @Override
    public void read(@NotNull ByteArrayDataInput in) {
        objective = in.readUTF();
        action = in.readInt();
        if (action == 0 || action == 2) {
            title = in.readUTF();
            renderType = in.readInt();
            if (in.readBoolean()) {
                numberFormat = in.readUTF();
            }
        }
    }

    @Override
    public void process(@NotNull BridgePlayer player) {
        if (action == 0) {
            player.getScoreboard().registerObjective(objective, IChatBaseComponent.optimizedComponent(title), renderType, numberFormat);
        } else if (action == 1) {
            player.getScoreboard().unregisterObjective(objective);
        } else if (action == 2) {
            player.getScoreboard().updateObjective(objective, IChatBaseComponent.optimizedComponent(title), renderType, numberFormat);
        }
    }
}
