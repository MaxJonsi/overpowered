package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.JudgementCutEndEntity;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.network.YamatoAnimationPayload;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class YamatoAbilityManager {
    public static final int AIR_TRICK_COST = 7;
    public static final int DIMENSION_RIFT_COST = 25;
    public static final int COUNTER_COST = 15;
    public static final int DEVIL_TRIGGER_COST = 45;
    public static final int WORLD_SPLIT_COST = 60;
    public static final int FINAL_COST = 100;
    public static final int RIFT_RANGE = 32;
    public static final int AIR_TRICK_RANGE = 60;
    public static final int FINAL_PREPARE_TICKS = 100;
    public static final int FINAL_RELEASE_TICKS = 60;
    public static final int FINAL_RADIUS = 75;

    private static final Map<UUID, Long> DEVIL_TRIGGER = new HashMap<>();
    private static final Map<UUID, Long> PRECISION_WINDOWS = new HashMap<>();
    private static final Map<UUID, Boolean> EMPOWERED_STRIKES = new HashMap<>();
    private static final Map<UUID, Long> COUNTERS = new HashMap<>();
    private static final Map<UUID, RiftState> RIFTS = new HashMap<>();
    private static final Map<UUID, WorldSplitState> WORLD_SPLITS = new HashMap<>();
    private static final Map<UUID, FinalState> FINALS = new HashMap<>();

    private YamatoAbilityManager() {
    }

    public static boolean airTrick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(AIR_TRICK_RANGE));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1.75),
                entity -> entity instanceof LivingEntity && entity != player && entity.isAlive());
        LivingEntity target = hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;

        Vec3 destination;
        if (target != null) {
            Vec3 targetFacing = target.getLookAngle().multiply(1, 0, 1);
            if (targetFacing.lengthSqr() < 0.001) {
                targetFacing = target.position().subtract(player.position()).multiply(1, 0, 1);
            }
            if (targetFacing.lengthSqr() < 0.001) targetFacing = new Vec3(0, 0, 1);
            destination = findSafeDestination(player, target.position(), targetFacing.normalize().scale(-2.1));
        } else {
            destination = findDirectionalDestination(player, 12);
        }
        if (destination == null) return false;

        int cost = isDevilTrigger(player.getUUID()) ? 4 : AIR_TRICK_COST;
        if (!LegendaryCombat.begin(player, cost, 4)) return false;
        Vec3 origin = player.position();
        level.sendParticles(new DustParticleOptions(new Vector3f(0.08f, 0.36f, 1f), 1.35f),
                origin.x, origin.y + 1, origin.z, 22, 0.18, 0.75, 0.18, 0.01);
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        level.sendParticles(new DustParticleOptions(new Vector3f(0.6f, 0.9f, 1f), 1.5f),
                destination.x, destination.y + 1, destination.z, 28, 0.2, 0.8, 0.2, 0.015);
        level.playSound(null, destination.x, destination.y, destination.z,
                ModSounds.YAMATO_DASH, SoundSource.PLAYERS, 1.3f, 1.25f);
        if (target != null) EMPOWERED_STRIKES.put(player.getUUID(), true);
        markPrecisionWindow(player);
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_YAMATO, 2,
                PowerEventPayload.PHASE_RELEASE, 10, AIR_TRICK_RANGE, target == null ? 0 : 1);
        return true;
    }

    private static Vec3 findSafeDestination(ServerPlayer player, Vec3 base, Vec3 behind) {
        Vec3 lateral = new Vec3(-behind.z, 0, behind.x).normalize();
        Vec3[] offsets = {behind, behind.add(0, 1, 0), behind.add(lateral.scale(1.25)),
                behind.add(lateral.scale(-1.25))};
        for (Vec3 offset : offsets) {
            Vec3 candidate = base.add(offset);
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(player.position())))) return candidate;
        }
        return null;
    }

    private static Vec3 findDirectionalDestination(ServerPlayer player, int maxRange) {
        Vec3 origin = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        for (int distance = maxRange; distance >= 2; distance--) {
            Vec3 candidate = origin.add(direction.scale(distance));
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(origin)))) return candidate;
        }
        return null;
    }

    public static void dimensionRift(ServerPlayer player) {
        RiftState existing = RIFTS.remove(player.getUUID());
        if (existing != null) {
            PowerEventDispatcher.broadcastAt(player, existing.entry, PowerEventPayload.POWER_YAMATO, 3,
                    PowerEventPayload.PHASE_STATE_END, 0, 3);
            return;
        }
        Vec3 destination = findDirectionalDestination(player, RIFT_RANGE);
        if (destination == null || !LegendaryCombat.begin(player, DIMENSION_RIFT_COST, 12)) return;

        Vec3 entry = player.position().add(player.getLookAngle().normalize().scale(2));
        long endTick = player.serverLevel().getGameTime() + 20 * 15;
        RIFTS.put(player.getUUID(), new RiftState(
                player.serverLevel().dimension(), entry, destination, endTick));
        player.serverLevel().playSound(null, entry.x, entry.y, entry.z,
                ModSounds.YAMATO_RIFT, SoundSource.PLAYERS, 1.8f, 1f);
        PowerEventDispatcher.broadcastAt(player, entry, PowerEventPayload.POWER_YAMATO, 3,
                PowerEventPayload.PHASE_STATE_START, 20 * 15, 3);
        PowerEventDispatcher.broadcastAt(player, destination, PowerEventPayload.POWER_YAMATO, 3,
                PowerEventPayload.PHASE_STATE_START, 20 * 15, 3);
    }

    public static void counter(ServerPlayer player) {
        if (!LegendaryCombat.begin(player, COUNTER_COST, 6)) return;
        COUNTERS.put(player.getUUID(), player.serverLevel().getGameTime() + 10);
        YamatoItem.broadcastPlayerAnimation(player, YamatoAnimationPayload.COUNTER);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 5,
                PowerEventPayload.PHASE_PREPARE, 10, 5);
    }

    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer player)) return true;
        Long endTick = COUNTERS.get(player.getUUID());
        if (endTick == null || player.serverLevel().getGameTime() > endTick) return true;
        Entity attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (!(attacker instanceof LivingEntity living) || attacker == player) return true;

        COUNTERS.remove(player.getUUID());
        Vec3 behind = living.position().subtract(living.getLookAngle().multiply(1.7, 0, 1.7));
        if (player.serverLevel().noCollision(player,
                player.getBoundingBox().move(behind.subtract(player.position())))) {
            player.teleportTo(behind.x, behind.y, behind.z);
        }
        LegendaryCombat.damage(player, living, 36f, 0.22f, LegendaryCombat.AttackKind.SPATIAL);
        LegendaryCombat.stagger(living, 25, 2);
        player.serverLevel().playSound(null, living.getX(), living.getY(), living.getZ(),
                ModSounds.YAMATO_SLICE, SoundSource.PLAYERS, 2f, 1.35f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 5,
                PowerEventPayload.PHASE_RELEASE, 12, 6);
        return amount >= 50f || LegendaryCombat.bypassesInfinity(player);
    }

    public static void toggleDevilTrigger(ServerPlayer player) {
        if (DEVIL_TRIGGER.remove(player.getUUID()) != null) {
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 4,
                    PowerEventPayload.PHASE_STATE_END, 0, 8);
            return;
        }
        if (!LegendaryCombat.begin(player, DEVIL_TRIGGER_COST, 20)) return;
        DEVIL_TRIGGER.put(player.getUUID(), Long.MAX_VALUE);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 4,
                PowerEventPayload.PHASE_STATE_START, -1, 8);
    }

    public static boolean isDevilTrigger(UUID playerId) {
        return DEVIL_TRIGGER.containsKey(playerId);
    }

    public static void worldSplit(ServerPlayer player) {
        if (WORLD_SPLITS.containsKey(player.getUUID())
                || !LegendaryCombat.begin(player, WORLD_SPLIT_COST, 30)) return;
        long releaseTick = player.serverLevel().getGameTime() + 20;
        WORLD_SPLITS.put(player.getUUID(), new WorldSplitState(
                player.serverLevel().dimension(), player.getEyePosition(),
                player.getLookAngle().normalize(), releaseTick));
        YamatoItem.broadcastPlayerAnimation(player, YamatoAnimationPayload.WORLD_SPLIT);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.YAMATO_SHEATH, SoundSource.PLAYERS, 1.6f, 0.8f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 7,
                PowerEventPayload.PHASE_PREPARE, 20, 200);
    }

    public static boolean startFinal(ServerPlayer player) {
        if (FINALS.containsKey(player.getUUID())
                || !LegendaryCombat.begin(player, FINAL_COST, FINAL_PREPARE_TICKS)) return false;
        long releaseTick = player.serverLevel().getGameTime() + FINAL_PREPARE_TICKS;
        FINALS.put(player.getUUID(), new FinalState(releaseTick, 0, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                FINAL_PREPARE_TICKS, 10, false, false));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.YAMATO_SKY_BREAK, SoundSource.MASTER, 3f, 1f);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.YAMATO_FINAL_MUSIC, SoundSource.RECORDS, 4f, 1f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 6,
                PowerEventPayload.PHASE_PREPARE, FINAL_PREPARE_TICKS, FINAL_RADIUS);
        return true;
    }

    public static void markPrecisionWindow(ServerPlayer player) {
        PRECISION_WINDOWS.put(player.getUUID(), player.serverLevel().getGameTime() + 10);
    }

    public static boolean consumePrecisionWindow(ServerPlayer player) {
        Long end = PRECISION_WINDOWS.remove(player.getUUID());
        return end != null && player.serverLevel().getGameTime() <= end;
    }

    public static boolean consumeEmpoweredStrike(ServerPlayer player) {
        return EMPOWERED_STRIKES.remove(player.getUUID()) != null;
    }

    public static void tick(MinecraftServer server) {
        tickPassive(server);
        tickRifts(server);
        tickDevilTrigger(server);
        tickWorldSplits(server);
        tickFinals(server);
        COUNTERS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            return player == null || player.serverLevel().getGameTime() > entry.getValue();
        });
    }

    private static void tickPassive(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.getMainHandItem().getItem() instanceof YamatoItem)
                    && !(player.getOffhandItem().getItem() instanceof YamatoItem)) continue;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 25, 0, true, false));
        }
    }

    private static void tickRifts(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RiftState>> iterator = RIFTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RiftState> entry = iterator.next();
            RiftState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null || level.getGameTime() >= state.endTick) {
                if (owner != null) {
                    PowerEventDispatcher.broadcastAt(owner, state.entry, PowerEventPayload.POWER_YAMATO, 3,
                            PowerEventPayload.PHASE_STATE_END, 0, 3);
                }
                iterator.remove();
                continue;
            }
            long now = level.getGameTime();
            state.cooldowns.entrySet().removeIf(cooldown -> cooldown.getValue() <= now);
            transportAt(level, state, state.entry, state.exit, now);
            transportAt(level, state, state.exit, state.entry, now);
            if (now % 3 == 0) {
                level.sendParticles(new DustParticleOptions(new Vector3f(0.08f, 0.35f, 1f), 1.3f),
                        state.entry.x, state.entry.y + 1.5, state.entry.z, 4, 0.15, 1.2, 0.15, 0.01);
                level.sendParticles(new DustParticleOptions(new Vector3f(0.08f, 0.35f, 1f), 1.3f),
                        state.exit.x, state.exit.y + 1.5, state.exit.z, 4, 0.15, 1.2, 0.15, 0.01);
            }
        }
    }

    private static void transportAt(ServerLevel level, RiftState state, Vec3 from, Vec3 to, long now) {
        AABB bounds = new AABB(from, from).inflate(1.1, 1.8, 1.1);
        for (Entity entity : level.getEntities((Entity) null, bounds,
                candidate -> candidate.isAlive() && !state.cooldowns.containsKey(candidate.getUUID()))) {
            Vec3 destination = to.add(0, 0.1, 0);
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(destination.x, destination.y, destination.z);
            } else {
                entity.setPos(destination.x, destination.y, destination.z);
            }
            entity.hurtMarked = true;
            state.cooldowns.put(entity.getUUID(), now + 10);
        }
    }

    private static void tickDevilTrigger(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> iterator = DEVIL_TRIGGER.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            boolean hasYamato = player != null && (player.getMainHandItem().getItem() instanceof YamatoItem
                    || player.getOffhandItem().getItem() instanceof YamatoItem);
            if (!hasYamato || (player.tickCount % 20 == 0 && !PlayerEnergyManager.tryConsume(player, 3))) {
                if (player != null) {
                    PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 4,
                            PowerEventPayload.PHASE_STATE_END, 0, 8);
                }
                iterator.remove();
                continue;
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 2, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 25, 1, true, false));
            if (player.tickCount % 4 == 0) {
                player.serverLevel().sendParticles(
                        new DustParticleOptions(new Vector3f(0.08f, 0.35f, 1f), 1.1f),
                        player.getX(), player.getY() + 1, player.getZ(), 4, 0.45, 0.8, 0.45, 0.02);
            }
        }
    }

    private static void tickWorldSplits(MinecraftServer server) {
        Iterator<Map.Entry<UUID, WorldSplitState>> iterator = WORLD_SPLITS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WorldSplitState> entry = iterator.next();
            WorldSplitState state = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = server.getLevel(state.dimension);
            if (player == null || level == null) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() < state.releaseTick) continue;
            Vec3 end = state.origin.add(state.direction.scale(200));
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(state.origin, end).inflate(2),
                    entity -> entity != player && entity.isAlive()
                            && distanceToSegment(entity.getEyePosition(), state.origin, end) <= 1.8)) {
                LegendaryCombat.damage(player, target, 80f, 0.45f, LegendaryCombat.AttackKind.SPATIAL);
                LegendaryCombat.stagger(target, 60, 3);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.YAMATO_SKY_BREAK, SoundSource.MASTER, 3f, 0.72f);
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 7,
                    PowerEventPayload.PHASE_RELEASE, 30, 200);
            iterator.remove();
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
            if (!state.released()) {
                for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(FINAL_RADIUS),
                        entity -> entity != player && entity.isAlive()
                                && entity.distanceToSqr(player) <= FINAL_RADIUS * FINAL_RADIUS)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 9, true, false));
                }
            }
            if (!state.released() && now >= state.releaseTick()) {
                JudgementCutEndEntity effect = new JudgementCutEndEntity(
                        ModEntities.JUDGEMENT_CUT_END, player.serverLevel());
                effect.setOwnerId(player.getUUID());
                effect.setPos(player.position());
                player.serverLevel().addFreshEntity(effect);
                entry.setValue(new FinalState(state.releaseTick(), now + FINAL_RELEASE_TICKS, true));
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 6,
                        PowerEventPayload.PHASE_RELEASE, FINAL_RELEASE_TICKS, FINAL_RADIUS);
            } else if (state.released() && now >= state.aftermathTick()) {
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_YAMATO, 6,
                        PowerEventPayload.PHASE_AFTERMATH, 20 * 10, FINAL_RADIUS);
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
        PRECISION_WINDOWS.remove(playerId);
        EMPOWERED_STRIKES.remove(playerId);
        COUNTERS.remove(playerId);
        RIFTS.remove(playerId);
        WORLD_SPLITS.remove(playerId);
        FINALS.remove(playerId);
    }

    public static void clear() {
        DEVIL_TRIGGER.clear();
        PRECISION_WINDOWS.clear();
        EMPOWERED_STRIKES.clear();
        COUNTERS.clear();
        RIFTS.clear();
        WORLD_SPLITS.clear();
        FINALS.clear();
    }

    private static final class RiftState {
        private final ResourceKey<Level> dimension;
        private final Vec3 entry;
        private final Vec3 exit;
        private final long endTick;
        private final Map<UUID, Long> cooldowns = new HashMap<>();

        private RiftState(ResourceKey<Level> dimension, Vec3 entry, Vec3 exit, long endTick) {
            this.dimension = dimension;
            this.entry = entry;
            this.exit = exit;
            this.endTick = endTick;
        }
    }

    private record WorldSplitState(
            ResourceKey<Level> dimension, Vec3 origin, Vec3 direction, long releaseTick) {
    }

    private record FinalState(long releaseTick, long aftermathTick, boolean released) {
    }
}
