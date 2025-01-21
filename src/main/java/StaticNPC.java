import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StaticNPC extends Entity implements NPC {

    private String skinSignature;
    private String skinValue;
    private boolean lookAtPlayers;
    private boolean listed;
    private long lookRangeSquared;

    private PlayerInfoUpdatePacket playerInfoUpdatePacket;

    private StaticNPC(
        @NotNull UUID uuid,
        @NotNull Pos position,
        @NotNull Component customName,
        @NotNull String skinSignature,
        @NotNull String skinValue,
        boolean lookAtPlayers,
        boolean listed,
        long lookRange
    ) {
        // Constructor for full initial customization
        super(EntityType.PLAYER, uuid);
        this.position = position;
        this.skinSignature = skinSignature;
        this.skinValue = skinValue;
        this.lookAtPlayers = lookAtPlayers;
        this.listed = listed;
        this.lookRangeSquared = lookRange;
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
        private long lookRange = 10;

        public Builder(@NotNull UUID uuid, @NotNull Pos position) {
            this.uuid = uuid;
            this.position = position;
        }

        public StaticNPC build() {
            return new StaticNPC(uuid, position, customName, skinSignature, skinValue, lookAtPlayers, listed, lookRange);
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

        public Builder lookRange(long lookRange) {
            this.lookRange = lookRange;
            return this;
        }
    }

    @Override
    public void tick(long time) {
        if (lookAtPlayers) {
            Player closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (Entity nearbyEntity : getInstance().getNearbyEntities(this.position, this.lookRangeSquared)) {
                if (nearbyEntity instanceof Player) {
                    double distance = this.position.distance(nearbyEntity.getPosition());
                    if (distance < closestDistance) {
                        closest = (Player) nearbyEntity;
                        closestDistance = distance;
                    }
                }
            }
            if (closest != null) {
                this.lookAt(closest.getPosition());
            }
        }
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance) {
        CompletableFuture<Void> future = super.setInstance(instance);
        future.thenRun(() -> AuriNPC.getInstance().addNPC(this));
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
                Objects.requireNonNull(getCustomName()).toString(),
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
        getInstance().getPlayers().forEach(player -> player.sendPacket(playerInfoUpdatePacket));
    }

    public String getSkinSignature() {
        return skinSignature;
    }

    public String getSkinValue() {
        return skinValue;
    }

    public void setSkin(@NotNull String skinValue, @NotNull String skinSignature) {
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

    public long getLookRangeSquared() {
        return lookRangeSquared;
    }

    public long getLookRange() {
        return (long) Math.sqrt(lookRangeSquared);
    }

    public void setLookRange(long lookRange) {
        this.lookRangeSquared = lookRange * lookRange;
    }
}