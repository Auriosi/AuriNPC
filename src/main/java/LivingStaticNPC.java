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

public class LivingStaticNPC extends LivingEntity implements NPC {
    private String skinSignature;
    private String skinValue;
    private boolean lookAtPlayers;
    private boolean listed;
    private long lookRangeSquared;
    private double maxHealth;
    private boolean respawns;
    private long respawnDelay;

    private PlayerInfoUpdatePacket playerInfoUpdatePacket;

    protected LivingStaticNPC(
            @NotNull UUID uuid,
            @NotNull Instance instance,
            @NotNull Pos position,
            @NotNull Component customName,
            @NotNull String skinSignature,
            @NotNull String skinValue,
            boolean lookAtPlayers,
            boolean listed,
            long lookRange,
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
        this.lookAtPlayers = lookAtPlayers;
        this.listed = listed;
        this.lookRangeSquared = lookRange;
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
        private boolean lookAtPlayers = true;
        private boolean listed = true;
        private long lookRange = 10;
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

        public LivingStaticNPC build() {
            return new LivingStaticNPC(uuid, instance, position, customName, skinSignature, skinValue, lookAtPlayers, listed, lookRange, maxHealth, health, invulnerable, respawns, respawnDelay);
        }

        public LivingStaticNPC.Builder customName(Component customName) {
            this.customName = customName;
            return this;
        }

        public LivingStaticNPC.Builder skin(String skinValue, String skinSignature) {
            this.skinValue = skinValue;
            this.skinSignature = skinSignature;
            return this;
        }

        public LivingStaticNPC.Builder skin(PlayerSkin skin) {
            this.skinValue = skin.textures();
            this.skinSignature = skin.signature();
            return this;
        }

        public LivingStaticNPC.Builder lookAtPlayers(boolean lookAtPlayers) {
            this.lookAtPlayers = lookAtPlayers;
            return this;
        }

        public LivingStaticNPC.Builder listed(boolean listed) {
            this.listed = listed;
            return this;
        }

        public LivingStaticNPC.Builder lookRange(long lookRange) {
            this.lookRange = lookRange;
            return this;
        }

        public LivingStaticNPC.Builder health(float health) {
            this.health = health;
            return this;
        }

        public LivingStaticNPC.Builder invulnerable(boolean invulnerable) {
            this.invulnerable = invulnerable;
            return this;
        }

        public LivingStaticNPC.Builder respawns(boolean respawns) {
            this.respawns = respawns;
            return this;
        }

        public LivingStaticNPC.Builder respawnDelay(long respawnDelay) {
            this.respawnDelay = respawnDelay;
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

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }
}