import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerEvent;

import java.util.ArrayList;

public class AuriNPC {
    private static AuriNPC INSTANCE = null;

    private final EventNode<PlayerEvent> playerEventNode;
    private final ArrayList<NPC> npcs = new ArrayList<>();

    private AuriNPC() {
        playerEventNode = EventNode.type("aurinpc-player-events", EventFilter.PLAYER);
        playerEventNode.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            npcs.forEach(npc -> {
                if (npc.getInstance() != player.getInstance()) {
                    return;
                }
                player.sendPacket(npc.getPlayerInfoUpdatePacket());
                npc.addViewer(player);
            });
        });
        MinecraftServer.getGlobalEventHandler().addChild(playerEventNode);
    }

    public static void init() {
        INSTANCE = new AuriNPC();
    }

    public static AuriNPC getInstance() {
        return INSTANCE;
    }

    public void addNPC(NPC npc) {
        npcs.add(npc);
        npc.getInstance().getPlayers().forEach(npc::addViewer);
    }
}