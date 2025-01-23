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

public class NavigationalNPC extends EntityCreature implements NPC {
    private String skinSignature;
    private String skinValue;
    private boolean listed;
    private double maxHealth;
    private boolean respawns;
    private long respawnDelay;

    private PlayerInfoUpdatePacket playerInfoUpdatePacket;

    protected NavigationalNPC(
            @NotNull UUID uuid,
            @NotNull Instance instance,
            @NotNull Pos position,
            @NotNull Component customName,
            @NotNull String skinSignature,
            @NotNull String skinValue,
            boolean listed,
            double maxHealth,
            float health,
            boolean invulnerable,
            boolean respawns,
            long respawnDelay
    ) {
        // Constructor for full initial customization
        super(EntityType.PLAYER, uuid);
        this.setInstance(instance, position);
        this.skinSignature = skinSignature;
        this.skinValue = skinValue;
        this.listed = listed;
        this.setMaxHealth(maxHealth);
        this.setHealth(health);
        this.setInvulnerable(invulnerable);
        this.respawns = respawns;
        this.respawnDelay = respawnDelay;
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
        private final Instance instance;
        private final Pos position;
        private final double maxHealth;

        // Optional parameters
        private Component customName = Component.text("NPC");
        private String skinSignature = "";
        private String skinValue = "";
        private boolean listed = true;
        private float health = 1;
        private boolean invulnerable = false;
        private boolean respawns = false;
        private long respawnDelay = 0;

        public Builder(@NotNull UUID uuid, @NotNull Instance instance, @NotNull Pos position, double maxHealth) {
            this.uuid = uuid;
            this.instance = instance;
            this.position = position;
            this.maxHealth = maxHealth;
        }

        public NavigationalNPC build() {
            return new NavigationalNPC(uuid, instance, position, customName, skinSignature, skinValue, listed, maxHealth, health, invulnerable, respawns, respawnDelay);
        }

        public NavigationalNPC.Builder customName(Component customName) {
            this.customName = customName;
            return this;
        }

        public NavigationalNPC.Builder skin(String skinValue, String skinSignature) {
            this.skinValue = skinValue;
            this.skinSignature = skinSignature;
            return this;
        }

        public NavigationalNPC.Builder skin(PlayerSkin skin) {
            this.skinValue = skin.textures();
            this.skinSignature = skin.signature();
            return this;
        }

        public NavigationalNPC.Builder listed(boolean listed) {
            this.listed = listed;
            return this;
        }

        public NavigationalNPC.Builder health(float health) {
            this.health = health;
            return this;
        }

        public NavigationalNPC.Builder invulnerable(boolean invulnerable) {
            this.invulnerable = invulnerable;
            return this;
        }

        public NavigationalNPC.Builder respawns(boolean respawns) {
            this.respawns = respawns;
            return this;
        }

        public NavigationalNPC.Builder respawnDelay(long respawnDelay) {
            this.respawnDelay = respawnDelay;
            return this;
        }
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance) {
        AuriNPC.getInstance().removeNPC(this);
        CompletableFuture<Void> future = super.setInstance(instance);
        future.thenRun(() -> AuriNPC.getInstance().addNPC(this));
        return future;
    }

    @Override
    public void remove() {
        super.remove();
        AuriNPC.getInstance().removeNPC(this);
    }

    @Override
    public void kill() {
        super.kill();
        if (respawns) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(respawnDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            });
            future.thenRun(() -> {
                refreshIsDead(false);
                setPose(EntityPose.STANDING);
                setHealth((float) maxHealth);
            });
        } else {
            AuriNPC.getInstance().removeNPC(this);
        }
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

    public boolean isListed() {
        return listed;
    }

    public void setListed(boolean listed) {
        this.listed = listed;
        remakeInfoUpdatePacket();
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }
}