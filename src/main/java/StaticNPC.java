import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StaticNPC extends Entity implements NPC {

    private String skinSignature;
    private String skinValue;
    private boolean lookAtPlayers;

    public StaticNPC(
            @NotNull UUID uuid,
            @NotNull Pos position,
            Component customName,
            String skinSignature,
            String skinValue,
            boolean lookAtPlayers
    ) {
        super(EntityType.PLAYER, uuid);
        this.position = position;
        this.skinSignature = skinSignature;
        this.skinValue = skinValue;
        this.lookAtPlayers = lookAtPlayers;

        if (customName != null) {
            this.setCustomName(customName);
            this.setCustomNameVisible(true);
        }
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance) {
        CompletableFuture<Void> future = super.setInstance(instance);
        future.thenRun(() -> {
            AuriNPC.getInstance().addNPC(this);
        });
        return future;
    }

    public PlayerInfoUpdatePacket getAddPlayerInfoPacket() {
        return new PlayerInfoUpdatePacket(
                PlayerInfoUpdatePacket.Action.ADD_PLAYER,
                List.of(

                )
        );
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    public String getSkinValue() {
        return skinValue;
    }

    public boolean looksAtPlayers() {
        return lookAtPlayers;
    }
}