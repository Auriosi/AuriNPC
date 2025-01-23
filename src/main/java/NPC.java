import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;

import java.util.concurrent.CompletableFuture;

public interface NPC {
    String getSkinSignature();
    String getSkinValue();
    void setSkin(String skinSignature, String skinValue);
    void setSkin(PlayerSkin skin);
    boolean isListed();
    void setListed(boolean listed);
    CompletableFuture<Void> setInstance(Instance instance);
    boolean addViewer(Player player);
    boolean removeViewer(Player player);
    Instance getInstance();
    PlayerInfoUpdatePacket getPlayerInfoUpdatePacket();
    void remakeInfoUpdatePacket();
}