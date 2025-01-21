import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;

import java.util.concurrent.CompletableFuture;

public interface NPC {
    String getSkinSignature();
    String getSkinValue();
    CompletableFuture<Void> setInstance(Instance instance);
    boolean addViewer(Player player);
    boolean removeViewer(Player player);
    Instance getInstance();
    PlayerInfoUpdatePacket getPlayerInfoUpdatePacket();
    public void remakeInfoUpdatePacket();
}