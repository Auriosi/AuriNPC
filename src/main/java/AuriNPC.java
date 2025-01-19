import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerEvent;

import java.util.ArrayList;

public class AuriNPC {
    private static final AuriNPC INSTANCE = null;

    private final EventNode<PlayerEvent> playerEventNode;
    private final ArrayList<NPC> npcs = new ArrayList<>();

    private AuriNPC() {
        playerEventNode = EventNode.type("aurinpc-player-events", EventFilter.PLAYER);
        playerEventNode.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            npcs.forEach(npc -> {
                npc.addViewer(player);

            });
        });
    }

    public void init() {
        AuriNPC lib = new AuriNPC();
    }

    public static AuriNPC getInstance() {
        return INSTANCE;
    }

    public void addNPC(NPC npc) {
        npcs.add(npc);
        npc.getInstance().getPlayers().forEach(npc::addViewer);
    }
}