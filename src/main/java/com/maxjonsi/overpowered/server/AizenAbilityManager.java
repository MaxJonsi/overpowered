package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.network.PowerEventPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class AizenAbilityManager {
    public static final int FLASH_STEP_COST = 10;
    public static final int HYPNOSIS_COST = 20;
    public static final int PRESSURE_COST = 28;
    public static final int EVOLUTION_BASE_COST = 35;
    public static final int PERFECT_HYPNOSIS_COST = 85;
    public static final int HYPNOSIS_DURATION = 20 * 30;
    public static final int EVOLUTION_DURATION = 20 * 45;
    public static final int PERFECT_PREPARE_TICKS = 40;
    public static final int PERFECT_DURATION = 20 * 30;

    private static final Map<UUID, HypnosisState> HYPNOSIS = new HashMap<>();
    private static final Map<UUID, EvolutionState> EVOLUTIONS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_ULTIMATES = new HashMap<>();
    private static final Map<UUID, Long> PERFECT_ENDS = new HashMap<>();

    private AizenAbilityManager() {
    }

    public static void flashStep(ServerPlayer player) {
        Vec3 origin = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 destination = null;
        for (int distance = 18; distance >= 2; distance--) {
            Vec3 candidate = origin.add(direction.scale(distance));
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(origin)))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null || !PlayerEnergyManager.tryConsumeOrNotify(player, FLASH_STEP_COST)) return;

        ServerLevel level = player.serverLevel();
        level.sendParticles(new DustParticleOptions(new Vector3f(0.48f, 0.1f, 0.72f), 1.25f),
                origin.x, origin.y + 1, origin.z, 20, 0.4, 0.8, 0.4, 0.02);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.75f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_AIZEN, 1,
                PowerEventPayload.PHASE_RELEASE, 8, 18);
    }

    public static void hypnotize(ServerPlayer player) {
        LivingEntity target = findTarget(player, 32);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.overpowered.aizen.no_target"), true);
            return;
        }
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, HYPNOSIS_COST)) return;

        long endTick = player.serverLevel().getGameTime() + HYPNOSIS_DURATION;
        HYPNOSIS.put(target.getUUID(), new HypnosisState(player.getUUID(), endTick, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        if (target instanceof ServerPlayer targetPlayer) {
            sendHypnosisState(player, targetPlayer, HYPNOSIS_DURATION, false,
                    PowerEventPayload.PHASE_STATE_START);
        }
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_AIZEN, 2,
                PowerEventPayload.PHASE_STATE_START, HYPNOSIS_DURATION, 32);
    }

    public static void spiritualPressure(ServerPlayer player) {
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, PRESSURE_COST)) return;

        ServerLevel level = player.serverLevel();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(18), entity -> entity != player && entity.isAlive())) {
            target.hurt(player.damageSources().indirectMagic(player, player), 12f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 3, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 3, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
            target.setDeltaMovement(target.getDeltaMovement().scale(0.1));
            target.hurtMarked = true;
        }
        level.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.08f, 0.75f), 2.2f),
                player.getX(), player.getY() + 1, player.getZ(), 100, 8, 2, 8, 0.06);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_AIZEN, 3,
                PowerEventPayload.PHASE_RELEASE, 80, 18);
    }

    public static void evolve(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        EvolutionState existing = EVOLUTIONS.get(player.getUUID());
        int nextStage = existing == null ? 1 : Math.min(3, existing.stage + 1);
        int cost = EVOLUTION_BASE_COST + (nextStage - 1) * 10;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, cost)) return;

        EVOLUTIONS.put(player.getUUID(), new EvolutionState(nextStage, now + EVOLUTION_DURATION));
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_AIZEN, 4,
                PowerEventPayload.PHASE_STATE_START, EVOLUTION_DURATION, 12, nextStage);
    }

    public static void perfectHypnosis(ServerPlayer player) {
        if (PENDING_ULTIMATES.containsKey(player.getUUID()) || PERFECT_ENDS.containsKey(player.getUUID())) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, PERFECT_HYPNOSIS_COST)) return;

        long releaseTick = player.serverLevel().getGameTime() + PERFECT_PREPARE_TICKS;
        PENDING_ULTIMATES.put(player.getUUID(), releaseTick);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                PERFECT_PREPARE_TICKS, 10, false, false));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_AIZEN, 6,
                PowerEventPayload.PHASE_PREPARE, PERFECT_PREPARE_TICKS, 40);
    }

    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer aizen)) return true;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return true;

        HypnosisState state = HYPNOSIS.get(livingAttacker.getUUID());
        if (state == null || !state.casterId.equals(aizen.getUUID())
                || aizen.serverLevel().getGameTime() >= state.endTick) return true;

        float evadeChance = state.perfect ? 0.85f : 0.55f;
        if (aizen.getRandom().nextFloat() >= evadeChance) return true;

        livingAttacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
        livingAttacker.hurt(aizen.damageSources().indirectMagic(aizen, aizen), Math.min(8f, amount));
        PowerEventDispatcher.broadcast(aizen, PowerEventPayload.POWER_AIZEN, 2,
                PowerEventPayload.PHASE_RELEASE, 8, 6);
        return false;
    }

    public static void tick(MinecraftServer server) {
        tickHypnosis(server);
        tickEvolutions(server);
        tickUltimates(server);
    }

    private static void tickHypnosis(MinecraftServer server) {
        Iterator<Map.Entry<UUID, HypnosisState>> iterator = HYPNOSIS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HypnosisState> entry = iterator.next();
            Entity target = findEntity(server, entry.getKey());
            HypnosisState state = entry.getValue();
            ServerPlayer caster = server.getPlayerList().getPlayer(state.casterId);
            boolean expired = target != null && target.level().getGameTime() >= state.endTick;
            if (target == null || caster == null || expired) {
                if (target instanceof ServerPlayer targetPlayer && caster != null) {
                    sendHypnosisState(caster, targetPlayer, 0, state.perfect,
                            PowerEventPayload.PHASE_STATE_END);
                }
                iterator.remove();
                continue;
            }
            if (target instanceof LivingEntity living && target.tickCount % 60 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false));
                if (target instanceof ServerPlayer targetPlayer) {
                    sendHypnosisState(caster, targetPlayer,
                            (int) Math.max(0, state.endTick - target.level().getGameTime()),
                            state.perfect, PowerEventPayload.PHASE_STATE_START);
                }
            }
        }
    }

    private static void tickEvolutions(MinecraftServer server) {
        Iterator<Map.Entry<UUID, EvolutionState>> iterator = EVOLUTIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EvolutionState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            EvolutionState state = entry.getValue();
            if (player == null || player.serverLevel().getGameTime() >= state.endTick) {
                if (player != null) {
                    PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_AIZEN, 4,
                            PowerEventPayload.PHASE_STATE_END, 0, state.stage);
                }
                iterator.remove();
                continue;
            }

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, state.stage - 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, state.stage - 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 25,
                    Math.max(0, state.stage - 2), true, false));
            if (player.tickCount % 5 == 0) {
                player.serverLevel().sendParticles(
                        new DustParticleOptions(new Vector3f(0.62f, 0.18f, 0.9f), 1.4f + state.stage * 0.2f),
                        player.getX(), player.getY() + 1, player.getZ(),
                        3 + state.stage, 0.5, 0.9, 0.5, 0.02);
            }
        }
    }

    private static void tickUltimates(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> pending = PENDING_ULTIMATES.entrySet().iterator();
        while (pending.hasNext()) {
            Map.Entry<UUID, Long> entry = pending.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            if (caster == null) {
                pending.remove();
                continue;
            }
            long now = caster.serverLevel().getGameTime();
            if (now < entry.getValue()) continue;

            long endTick = now + PERFECT_DURATION;
            for (LivingEntity target : caster.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    caster.getBoundingBox().inflate(40), entity -> entity != caster && entity.isAlive())) {
                HYPNOSIS.put(target.getUUID(), new HypnosisState(caster.getUUID(), endTick, true));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
                if (target instanceof ServerPlayer targetPlayer) {
                    sendHypnosisState(caster, targetPlayer, PERFECT_DURATION, true,
                            PowerEventPayload.PHASE_STATE_START);
                }
            }
            PERFECT_ENDS.put(caster.getUUID(), endTick);
            PowerEventDispatcher.broadcast(caster, PowerEventPayload.POWER_AIZEN, 6,
                    PowerEventPayload.PHASE_RELEASE, PERFECT_DURATION, 40);
            pending.remove();
        }

        Iterator<Map.Entry<UUID, Long>> active = PERFECT_ENDS.entrySet().iterator();
        while (active.hasNext()) {
            Map.Entry<UUID, Long> entry = active.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            if (caster == null || caster.serverLevel().getGameTime() >= entry.getValue()) {
                if (caster != null) {
                    PowerEventDispatcher.broadcast(caster, PowerEventPayload.POWER_AIZEN, 6,
                            PowerEventPayload.PHASE_AFTERMATH, 60, 40);
                }
                active.remove();
            }
        }
    }

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1),
                entity -> entity instanceof LivingEntity && entity != player && entity.isAlive());
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    private static void sendHypnosisState(ServerPlayer caster, ServerPlayer target,
            int durationTicks, boolean perfect, int phase) {
        PowerEventDispatcher.sendIfSupported(target, new PowerEventPayload(
                caster.getId(), PowerEventPayload.POWER_AIZEN, 2, phase,
                durationTicks, 40, perfect ? 2 : 1, caster.blockPosition().asLong()));
    }

    public static void clearPlayer(UUID playerId) {
        EVOLUTIONS.remove(playerId);
        PENDING_ULTIMATES.remove(playerId);
        PERFECT_ENDS.remove(playerId);
        HYPNOSIS.entrySet().removeIf(entry ->
                entry.getKey().equals(playerId) || entry.getValue().casterId.equals(playerId));
    }

    public static void clear() {
        HYPNOSIS.clear();
        EVOLUTIONS.clear();
        PENDING_ULTIMATES.clear();
        PERFECT_ENDS.clear();
    }

    private record HypnosisState(UUID casterId, long endTick, boolean perfect) {
    }

    private record EvolutionState(int stage, long endTick) {
    }
}
