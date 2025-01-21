import net.minestom.server.entity.EntityCreature;

public class NavigationalNPC extends EntityCreature implements NPC {
    private String skinSignature;
    private String skinValue;
    private boolean lookAtPlayers;
    private boolean listed;
    private long lookRangeSquared;
    private double maxHealth;
    private boolean respawns;
    private long respawnDelay;
}
