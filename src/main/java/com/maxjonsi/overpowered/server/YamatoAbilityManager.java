package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.JudgementCutEndEntity;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class YamatoAbilityManager {
    public static final int AIR_TRICK_COST = 12;
    public static final int DIMENSION_RIFT_COST = 24;
    public static final int DEVIL_TRIGGER_COST = 35;
    public static final int FINAL_COST = 45;
    public static final int RIFT_RANGE = 32;
    public static final int AIR_TRICK_RANGE = 36;
    public static final int DEVIL_TRIGGER_DURATION = 20 * 30;
    public static final int FINAL_PREPARE_TICKS = 40;
    public static final int FINAL_RELEASE_TICKS = 130;

    private static final Map<UUID, Long> DEVIL_TRIGGER = new HashMap<>();
    private static final Map<UUID, FinalState> FINALS = new HashMap<>();

    private YamatoAbilityManager() {
    }

    /**
     * Locks onto the living target nearest the crosshair and folds space to place the
     * player safely behind it. Unlike the normal dash this deliberately ignores
     * intervening blocks; collision is only tested at the arrival point.
     */
    public static boolean airTrick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(AIR_TRICK_RANGE));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1.75),
                entity -> entity instanceof LivingEntity && entity != player && entity.isAlive());
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.overpowered.yamato.no_target"), true);
            return false;
        }

        Vec3 targetFacing = target.getLookAngle();
        Vec3 horizontalFacing = new Vec3(targetFacing.x, 0, targetFacing.z);
        if (horizontalFacing.lengthSqr() < 0.001) {
            horizontalFacing = target.position().subtract(player.position()).multiply(1, 0, 1);
        }
        if (horizontalFacing.lengthSqr() < 0.001) horizontalFacing = new Vec3(0, 0, 1);
        horizontalFacing = horizontalFacing.normalize();

        Vec3 destination = findSafeAirTrickDestination(player, target, horizontalFacing.scale(-2.1));
        if (destination == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.overpowered.yamato.no_space"), true);
            return false;
        }
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, AIR_TRICK_COST)) return false;

        Vec3 origin = player.position();
        level.sendParticles(new DustParticleOptions(new Vector3f(0.08f, 0.36f, 1f), 1.35f),
                origin.x, origin.y + 1, origin.z, 22, 0.3, 0.75, 0.3, 0.02);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setYRot(target.getYRot());
        level.sendParticles(new DustParticleOptions(new Vector3f(0.6f, 0.9f, 1f), 1.5f),
                destination.x, destination.y + 1, destination.z, 28, 0.35, 0.8, 0.35, 0.025);
        level.playSound(null, destination.x, destination.y, destination.z,
                ModSounds.YAMATO_DASH, SoundSource.PLAYERS, 1.3f, 1.25f);
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_YAMATO, 2,
                PowerEventPayload.PHASE_RELEASE, 10, AIR_TRICK_RANGE, 1);
        return true;
    }

    private static Vec3 findSafeAirTrickDestination(ServerPlayer player, LivingEntity target, Vec3 behind) {
        Vec3 base = target.position().add(behind);
        Vec3 lateral = new Vec3(-behind.z, 0, behind.x).normalize();
        Vec3[] offsets = {
                Vec3.ZERO,
                lateral.scale(1.25),
                lateral.scale(-1.25),
                behind.normalize().scale(-1.25),
                new Vec3(0, 1, 0)
        };
        for (Vec3 offset : offsets) {
            Vec3 candidate = base.add(offset);
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(player.position())))) {
                return candidate;
            }
        }
        return null;
    }

    public static void dimensionRift(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 destination = null;
        for (int distance = RIFT_RANGE; distance >= 4; distance--) {
            Vec3 candidate = start.add(direction.scale(distance));
            if (level.noCollision(player, player.getBoundingBox().move(candidate.subtract(start)))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null || !PlayerEnergyManager.tryConsumeOrNotify(player, DIMENSION_RIFT_COST)) return;

        Vec3 segmentEnd = destination.add(0, 1, 0);
        Vec3 segmentStart = start.add(0, 1, 0);
        AABB corridor = new AABB(segmentStart, segmentEnd).inflate(2.25);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, corridor,
                entity -> entity != player && entity.isAlive())) {
            if (distanceToSegment(target.getEyePosition(), segmentStart, segmentEnd) <= 2.25) {
                target.hurt(player.damageSources().indirectMagic(player, player), 24f);
            }
        }

        for (int i = 0; i <= 32; i++) {
            Vec3 point = segmentStart.lerp(segmentEnd, i / 32.0);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.12f, 0.55f, 1f), 1.25f),
                    point.x, point.y, point.z, 1, 0.05, 0.12, 0.05, 0);
        }
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        level.playSound(null, destination.x, destination.y, destination.z,
                ModSounds.YAMATO_RIFT, SoundSource.PLAYERS, 1.8f, 1f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 3,
                PowerEventPayload.PHASE_RELEASE, 12, RIFT_RANGE);
    }

    public static void toggleDevilTrigger(ServerPlayer player) {
        if (DEVIL_TRIGGER.remove(player.getUUID()) != null) {
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 4,
                    PowerEventPayload.PHASE_STATE_END, 0, 8);
            return;
        }
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, DEVIL_TRIGGER_COST)) return;

        DEVIL_TRIGGER.put(player.getUUID(), player.serverLevel().getGameTime() + DEVIL_TRIGGER_DURATION);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 4,
                PowerEventPayload.PHASE_STATE_START, DEVIL_TRIGGER_DURATION, 8);
    }

    public static boolean startFinal(ServerPlayer player) {
        if (FINALS.containsKey(player.getUUID())) return false;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, FINAL_COST)) return false;

        long releaseTick = player.serverLevel().getGameTime() + FINAL_PREPARE_TICKS;
        FINALS.put(player.getUUID(), new FinalState(releaseTick, 0, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FINAL_PREPARE_TICKS, 10, false, false));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.YAMATO_SKY_BREAK, SoundSource.MASTER, 3f, 1f);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.YAMATO_FINAL_MUSIC, SoundSource.RECORDS, 4f, 1f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 6,
                PowerEventPayload.PHASE_PREPARE, FINAL_PREPARE_TICKS, 24);
        return true;
    }

    public static void tick(MinecraftServer server) {
        tickDevilTrigger(server);
        tickFinals(server);
    }

    private static void tickDevilTrigger(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> iterator = DEVIL_TRIGGER.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.serverLevel().getGameTime() >= entry.getValue()) {
                if (player != null) {
                    PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 4,
                            PowerEventPayload.PHASE_STATE_END, 0, 8);
                }
                iterator.remove();
                continue;
            }

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, true, false));
            if (player.tickCount % 4 == 0) {
                player.serverLevel().sendParticles(
                        new DustParticleOptions(new Vector3f(0.08f, 0.35f, 1f), 1.1f),
                        player.getX(), player.getY() + 1, player.getZ(), 4, 0.45, 0.8, 0.45, 0.02);
            }
        }
    }

    private static void tickFinals(MinecraftServer server) {
        Iterator<Map.Entry<UUID, FinalState>> iterator = FINALS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FinalState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            long now = player.serverLevel().getGameTime();
            FinalState state = entry.getValue();
            if (!state.released() && now >= state.releaseTick()) {
                JudgementCutEndEntity effect = new JudgementCutEndEntity(ModEntities.JUDGEMENT_CUT_END, player.serverLevel());
                effect.setOwnerId(player.getUUID());
                effect.setPos(player.position());
                player.serverLevel().addFreshEntity(effect);
                entry.setValue(new FinalState(state.releaseTick(), now + FINAL_RELEASE_TICKS, true));
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 6,
                        PowerEventPayload.PHASE_RELEASE, FINAL_RELEASE_TICKS, 24);
            } else if (state.released() && now >= state.aftermathTick()) {
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 6,
                        PowerEventPayload.PHASE_AFTERMATH, 60, 24);
                iterator.remove();
            }
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 0.0001) return point.distanceTo(start);
        double t = Math.max(0, Math.min(1, point.subtract(start).dot(segment) / lengthSquared));
        return point.distanceTo(start.add(segment.scale(t)));
    }

    public static void clearPlayer(UUID playerId) {
        DEVIL_TRIGGER.remove(playerId);
        FINALS.remove(playerId);
    }

    public static void clear() {
        DEVIL_TRIGGER.clear();
        FINALS.clear();
    }

    private record FinalState(long releaseTick, long aftermathTick, boolean released) {
    }
}
