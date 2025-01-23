import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
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

    /**
     * Initializes the AuriNPC library.
     */
    public static void init() {
        INSTANCE = new AuriNPC();
    }

    /**
     * Gets the instance of the AuriNPC library.
     *
     * @return the instance of the AuriNPC library
     */
    public static AuriNPC getInstance() {
        return INSTANCE;
    }

    /**
     * Adds an NPC to the tracker, making it visible to players in the same instance.
     *
     * @param npc the NPC to add
     */
    public void addNPC(NPC npc) {
        npcs.add(npc);
        npc.getInstance().getPlayers().forEach(npc::addViewer);
    }

    /**
     * Removes an NPC from the tracker, making it invisible to players.
     *
     * @param npc the NPC to remove
     */
    public void removeNPC(NPC npc) {
        npcs.remove(npc);
        npc.getInstance().getPlayers().forEach(npc::removeViewer);
    }

    /**
     * Gets all NPCs currently being tracked.
     *
     * @return all NPCs currently being tracked
     */
    public ArrayList<NPC> getNPCs() {
        return npcs;
    }

    public EventNode<PlayerEvent> getPlayerEventNode() {
        return playerEventNode;
    }
}