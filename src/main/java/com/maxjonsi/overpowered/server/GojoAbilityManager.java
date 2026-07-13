package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.item.GojoMaskItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class GojoAbilityManager {
    public static final int INFINITY_TOGGLE_COST = 0;
    public static final int INFINITY_BLOCK_COST = 2;
    public static final int TELEPORT_COST = 12;
    public static final int COMBO_COST = 2;
    public static final int TELEPORT_RANGE = 36;

    private static final Set<UUID> INFINITY_USERS = new HashSet<>();
    private static final Set<UUID> INFINITY_DISABLED = new HashSet<>();
    private static final java.util.Map<UUID, Long> LAST_COMBO = new java.util.HashMap<>();

    private GojoAbilityManager() {
    }

    public static boolean hasMask(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GojoMaskItem;
    }

    public static boolean hasGojoLoadout(LivingEntity entity) {
        return hasMask(entity)
                && (entity.getMainHandItem().getItem() instanceof SixEyesItem
                || entity.getOffhandItem().getItem() instanceof SixEyesItem);
    }

    public static boolean isInfinityActive(UUID playerId) {
        return INFINITY_USERS.contains(playerId);
    }

    public static void toggleInfinity(ServerPlayer player) {
        if (!hasGojoLoadout(player)) return;

        if (INFINITY_USERS.remove(player.getUUID())) {
            INFINITY_DISABLED.add(player.getUUID());
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                    PowerEventPayload.PHASE_STATE_END, 0, 5);
            return;
        }

        INFINITY_DISABLED.remove(player.getUUID());
        INFINITY_USERS.add(player.getUUID());
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                PowerEventPayload.PHASE_STATE_START, 0, 5);
    }

    public static void teleport(ServerPlayer player) {
        if (!hasMask(player)) return;

        Vec3 origin = player.position();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 destination = null;
        for (int distance = TELEPORT_RANGE; distance >= 2; distance--) {
            Vec3 candidate = origin.add(look.scale(distance));
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(origin)))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null || !PlayerEnergyManager.tryConsumeOrNotify(player, TELEPORT_COST)) return;

        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        level.sendParticles(new DustParticleOptions(new Vector3f(0.72f, 0.9f, 1f), 1.5f),
                start.x, start.y + 1, start.z, 18, 0.35, 0.7, 0.35, 0.03);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.72f, 0.9f, 1f), 1.5f),
                destination.x, destination.y + 1, destination.z, 18, 0.35, 0.7, 0.35, 0.03);
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 4,
                PowerEventPayload.PHASE_RELEASE, 8, TELEPORT_RANGE);
    }

    public static boolean combo(ServerPlayer player, LivingEntity target) {
        if (!hasMask(player) || target == player || !target.isAlive()) return false;
        long now = player.serverLevel().getGameTime();
        if (now - LAST_COMBO.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < 4) return false;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, COMBO_COST)) return false;

        LAST_COMBO.put(player.getUUID(), now);
        target.hurt(player.damageSources().indirectMagic(player, player), 9f);
        Vec3 push = target.position().subtract(player.position());
        if (push.lengthSqr() > 0.001) {
            target.setDeltaMovement(target.getDeltaMovement().add(push.normalize().scale(0.55)).add(0, 0.15, 0));
            target.hurtMarked = true;
        }
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 0,
                PowerEventPayload.PHASE_RELEASE, 6, 5);
        return true;
    }

    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer player)
                || !INFINITY_USERS.contains(player.getUUID())
                || !hasGojoLoadout(player)) {
            return true;
        }

        Entity threat = source.getDirectEntity();
        if (threat == null) return true;
        if (!PlayerEnergyManager.tryConsume(player, INFINITY_BLOCK_COST)) {
            INFINITY_USERS.remove(player.getUUID());
            INFINITY_DISABLED.add(player.getUUID());
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                    PowerEventPayload.PHASE_STATE_END, 0, 5);
            return true;
        }

        Vec3 away = threat.position().subtract(player.position());
        if (away.lengthSqr() < 0.001) away = player.getLookAngle().scale(-1);
        Vec3 rejection = away.normalize().scale(threat instanceof Projectile ? 1.8 : 1.1).add(0, 0.25, 0);
        threat.setDeltaMovement(rejection);
        threat.hurtMarked = true;
        player.serverLevel().sendParticles(
                new DustParticleOptions(new Vector3f(0.6f, 0.86f, 1f), 1.25f),
                player.getX(), player.getY() + 1, player.getZ(), 12, 0.7, 1.0, 0.7, 0.02);
        return false;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            if (!hasGojoLoadout(player)) {
                INFINITY_DISABLED.remove(playerId);
                if (INFINITY_USERS.remove(playerId)) {
                    PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                            PowerEventPayload.PHASE_STATE_END, 0, 5);
                }
                continue;
            }

            if (!INFINITY_DISABLED.contains(playerId) && INFINITY_USERS.add(playerId)) {
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                        PowerEventPayload.PHASE_STATE_START, 0, 5);
            }
            if (INFINITY_USERS.contains(playerId) && player.tickCount % 10 == 0) {
                player.serverLevel().sendParticles(
                        new DustParticleOptions(new Vector3f(0.5f, 0.82f, 1f), 0.85f),
                        player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.8, 0.5, 0.005);
            }
        }
        INFINITY_USERS.removeIf(playerId -> server.getPlayerList().getPlayer(playerId) == null);
        INFINITY_DISABLED.removeIf(playerId -> server.getPlayerList().getPlayer(playerId) == null);
    }

    public static void clearPlayer(UUID playerId) {
        INFINITY_USERS.remove(playerId);
        INFINITY_DISABLED.remove(playerId);
        LAST_COMBO.remove(playerId);
    }

    public static void clear() {
        INFINITY_USERS.clear();
        INFINITY_DISABLED.clear();
        LAST_COMBO.clear();
    }
}
