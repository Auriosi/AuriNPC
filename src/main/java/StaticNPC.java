import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.PlayerSkin;
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
    private boolean listed;

    private PlayerInfoUpdatePacket playerInfoUpdatePacket;

    private StaticNPC(
        @NotNull UUID uuid,
        @NotNull Pos position,
        Component customName,
        String skinSignature,
        String skinValue,
        boolean lookAtPlayers,
        boolean listed
    ) {
        // Constructor for full initial customization
        super(EntityType.PLAYER, uuid);
        this.position = position;
        this.skinSignature = skinSignature;
        this.skinValue = skinValue;
        this.lookAtPlayers = lookAtPlayers;
        this.listed = listed;
        this.setCustomName(customName);
        this.setCustomNameVisible(true);
        this.playerInfoUpdatePacket = new PlayerInfoUpdatePacket(
            PlayerInfoUpdatePacket.Action.ADD_PLAYER,
            new PlayerInfoUpdatePacket.Entry(
                uuid,
                customName.examinableName(),
                List.of(
                    new PlayerInfoUpdatePacket.Property("textures", skinValue, skinSignature) // The client should be able to handle a blank skin property
                ),
                this.listed,
                0,
                GameMode.CREATIVE,
                customName,
                null,
                0
            )
        );
    }

    public static class Builder {
        // Required parameters
        private final UUID uuid;
        private final Pos position;

        // Optional parameters
        private Component customName = Component.text("NPC");
        private String skinSignature = "";
        private String skinValue = "";
        private boolean lookAtPlayers = true;
        private boolean listed = true;

        public Builder(UUID uuid, Pos position) {
            this.uuid = uuid;
            this.position = position;
        }

        public StaticNPC build() {
            return new StaticNPC(uuid, position, customName, skinSignature, skinValue, lookAtPlayers, listed);
        }

        public Builder customName(Component customName) {
            this.customName = customName;
            return this;
        }

        public Builder skin(String skinValue, String skinSignature) {
            this.skinValue = skinValue;
            this.skinSignature = skinSignature;
            return this;
        }

        public Builder skin(PlayerSkin skin) {
            this.skinValue = skin.textures();
            this.skinSignature = skin.signature();
            return this;
        }

        public Builder lookAtPlayers(boolean lookAtPlayers) {
            this.lookAtPlayers = lookAtPlayers;
            return this;
        }

        public Builder listed(boolean listed) {
            this.listed = listed;
            return this;
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

    public PlayerInfoUpdatePacket getPlayerInfoUpdatePacket() {
        return playerInfoUpdatePacket;
    }

    public void remakeInfoUpdatePacket() {
        this.playerInfoUpdatePacket = new PlayerInfoUpdatePacket(
            PlayerInfoUpdatePacket.Action.ADD_PLAYER,
            new PlayerInfoUpdatePacket.Entry(
                getUuid(),
                getCustomName().examinableName(),
                List.of(
                        new PlayerInfoUpdatePacket.Property("textures", skinValue, skinSignature) // The client should be able to handle a blank skin property
                ),
                this.listed,
                0,
                GameMode.CREATIVE,
                getCustomName(),
                null,
                0
            )
        );
        getInstance().getPlayers().forEach(player -> {
            player.sendPacket(playerInfoUpdatePacket);
        });
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    public String getSkinValue() {
        return skinValue;
    }

    public void setSkin(String skinValue, String skinSignature) {
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
        remakeInfoUpdatePacket();
    }

    public void setSkin(PlayerSkin skin) {
        setSkin(skin.textures(), skin.signature());
    }

    public boolean looksAtPlayers() {
        return lookAtPlayers;
    }

    public void setLookAtPlayers(boolean lookAtPlayers) {
        this.lookAtPlayers = lookAtPlayers;
    }

    public boolean isListed() {
        return listed;
    }

    public void setListed(boolean listed) {
        this.listed = listed;
        remakeInfoUpdatePacket();
    }
}