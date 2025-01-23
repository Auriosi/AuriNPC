import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A static NPC that has no health and cannot move.
 */
public class StaticNPC extends Entity implements NPC {
    private String skinSignature;
    private String skinValue;
    private boolean lookAtPlayers;
    private boolean listed;
    private long lookRangeSquared;

    private PlayerInfoUpdatePacket playerInfoUpdatePacket;

    protected StaticNPC(
        @NotNull UUID uuid,
        @NotNull Instance instance,
        @NotNull Pos position,
        @NotNull Component customName,
        @NotNull String skinSignature,
        @NotNull String skinValue,
        boolean lookAtPlayers,
        boolean listed,
        long lookRange,
        @NotNull Consumer<ClientInteractEntityPacket> interactListener
    ) {
        // Constructor for full initial customization
        super(EntityType.PLAYER, uuid);
        this.setInstance(instance, position);
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
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientInteractEntityPacket.class, (packet, player) -> {
            if (packet.targetId() == this.getEntityId()) {
                interactListener.accept(packet);
            }
        });
    }

    public static class Builder {
        // Required parameters
        private final UUID uuid;
        private final Instance instance;
        private final Pos position;

        // Optional parameters
        private Component customName = Component.text("NPC");
        private String skinSignature = "";
        private String skinValue = "";
        private boolean lookAtPlayers = true;
        private boolean listed = true;
        private long lookRange = 10;
        private Consumer<ClientInteractEntityPacket> interactListener = packet -> {};

        /**
         * Creates a new {@link StaticNPC} builder.
         * @param uuid The UUID of the NPC
         * @param instance The instance the NPC will be in
         * @param position The position of the NPC
         */
        public Builder(@NotNull UUID uuid, @NotNull Instance instance, @NotNull Pos position) {
            this.uuid = uuid;
            this.position = position;
            this.instance = instance;
        }

        /**
         * Builds the NPC with the chosen options.
         * @return The NPC
         */
        public StaticNPC build() {
            return new StaticNPC(uuid, instance, position, customName, skinSignature, skinValue, lookAtPlayers, listed, lookRange, interactListener);
        }

        /**
         * Sets the interact listener for the NPC.
         * @param interactListener The interact listener
         * @return The builder
         */
        public StaticNPC.Builder onInteract(Consumer<ClientInteractEntityPacket> interactListener) {
            this.interactListener = interactListener;
            return this;
        }

        /**
         * Sets the custom name of the NPC.
         * @param customName The custom name
         * @return The builder
         */
        public StaticNPC.Builder customName(Component customName) {
            this.customName = customName;
            return this;
        }

        /**
         * Sets the skin of the NPC.
         * @param skinValue The skin value
         * @param skinSignature The skin signature
         * @return The builder
         */
        public StaticNPC.Builder skin(String skinValue, String skinSignature) {
            this.skinValue = skinValue;
            this.skinSignature = skinSignature;
            return this;
        }

        /**
         * Sets the skin of the NPC.
         * @param skin The skin
         * @return The builder
         */
        public StaticNPC.Builder skin(PlayerSkin skin) {
            this.skinValue = skin.textures();
            this.skinSignature = skin.signature();
            return this;
        }

        /**
         * Sets whether the NPC should look at players.
         * @param lookAtPlayers Whether the NPC should look at players
         * @return The builder
         */
        public StaticNPC.Builder lookAtPlayers(boolean lookAtPlayers) {
            this.lookAtPlayers = lookAtPlayers;
            return this;
        }

        /**
         * Sets whether the NPC should be listed in the player list.
         * @param listed Whether the NPC should be listed
         * @return The builder
         */
        public StaticNPC.Builder listed(boolean listed) {
            this.listed = listed;
            return this;
        }

        /**
         * Sets the look range of the NPC.
         * @param lookRange The look range
         * @return The builder
         */
        public StaticNPC.Builder lookRange(long lookRange) {
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

    /**
     * Sets the instance of the NPC.
     * @param instance The instance to move the NPC to
     * @return A future that completes when the NPC has been moved
     */
    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance) {
        CompletableFuture<Void> future = super.setInstance(instance);
        future.thenRun(() -> AuriNPC.getInstance().addNPC(this));
        return future;
    }

    /**
     * Removes the NPC from the server
     */
    @Override
    public void remove() {
        super.remove();
        AuriNPC.getInstance().removeNPC(this);
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

    /**
     * Gets the skin signature of the NPC.
     * @return The skin signature
     */
    public String getSkinSignature() {
        return skinSignature;
    }

    /**
     * Gets the skin value of the NPC.
     * @return The skin value
     */
    public String getSkinValue() {
        return skinValue;
    }

    /**
     * Sets the skin of the NPC.
     * @param skinValue The skin value
     * @param skinSignature The skin signature
     */
    public void setSkin(@NotNull String skinValue, @NotNull String skinSignature) {
        this.skinValue = skinValue;
        this.skinSignature = skinSignature;
        remakeInfoUpdatePacket();
    }

    /**
     * Sets the skin of the NPC.
     * @param skin The skin
     */
    public void setSkin(PlayerSkin skin) {
        setSkin(skin.textures(), skin.signature());
    }

    /**
     * Gets whether the NPC looks at players.
     * @return Whether the NPC looks at players
     */
    public boolean looksAtPlayers() {
        return lookAtPlayers;
    }

    /**
     * Sets whether the NPC looks at players.
     * @param lookAtPlayers Whether the NPC looks at players
     */
    public void setLookAtPlayers(boolean lookAtPlayers) {
        this.lookAtPlayers = lookAtPlayers;
    }

    /**
     * Gets whether the NPC is listed in the player list.
     * @return Whether the NPC is listed
     */
    public boolean isListed() {
        return listed;
    }

    /**
     * Sets whether the NPC is listed in the player list.
     * @param listed Whether the NPC is listed
     */
    public void setListed(boolean listed) {
        this.listed = listed;
        remakeInfoUpdatePacket();
    }

    /**
     * Gets the squared look range of the NPC.
     * @return The squared look range
     */
    public long getLookRangeSquared() {
        return lookRangeSquared;
    }

    /**
     * Gets the look range of the NPC.
     * @return The look range
     */
    public long getLookRange() {
        return (long) Math.sqrt(lookRangeSquared);
    }

    /**
     * Sets the non-squared look range of the NPC.
     * @param lookRange The non-squared look range
     */
    public void setLookRange(long lookRange) {
        this.lookRangeSquared = lookRange * lookRange;
    }
}