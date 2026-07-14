package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.registry.ModItems;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class TimeAbilityManager {
    public static final int VAMPIRE_STRIKE_COST = 10;
    public static final int KNIFE_COST = 8;
    public static final int DASH_COST = 10;
    public static final int TEMPORAL_LOCK_COST = 20;
    public static final int STOP_COST = 60;
    public static final int ACCELERATION_COST = 35;
    public static final int REWIND_COST = 100;
    public static final int STOP_DURATION = 120;
    public static final int STOP_RADIUS = 32;
    public static final int REWIND_SECONDS = 5;
    public static final int HISTORY_BLOCK_RADIUS = 16;
    public static final int HISTORY_VERTICAL_RADIUS = 8;
    public static final int REWIND_PREPARE_TICKS = 40;

    private static final int MAX_HISTORY_FRAMES = REWIND_SECONDS + 1;
    private static final int BLOCKS_PER_REWIND_TICK = 2_500;
    private static final Map<UUID, KnifeState> KNIVES = new HashMap<>();
    private static final Map<UUID, TimeStopState> STOPS = new HashMap<>();
    private static final Map<UUID, Deque<TimeSnapshot>> HISTORIES = new HashMap<>();
    private static final Map<UUID, PendingRewind> PENDING_REWINDS = new HashMap<>();
    private static final Map<UUID, RewindJob> REWIND_JOBS = new HashMap<>();
    private static final Map<UUID, long[]> COMBOS = new HashMap<>();
    private static final Map<UUID, BarrageState> BARRAGES = new HashMap<>();
    private static final Map<UUID, TemporalLockState> TEMPORAL_LOCKS = new HashMap<>();

    private TimeAbilityManager() {
    }

    public static void throwKnives(ServerPlayer player) {
        if (!LegendaryCombat.begin(player, KNIFE_COST, 8)) return;

        ServerLevel level = player.serverLevel();
        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.8));
        for (int index = 0; index < 6; index++) {
            Vec3 direction = player.getLookAngle().yRot((float) ((index - 2.5) * 0.055)).normalize();
            ItemEntity knife = new ItemEntity(level, origin.x, origin.y, origin.z,
                    new ItemStack(ModItems.DIO_KNIFE));
            knife.setNoGravity(true);
            knife.setInvulnerable(true);
            knife.setPickUpDelay(200);
            knife.setDeltaMovement(direction.scale(1.9));
            if (level.addFreshEntity(knife)) {
                KNIVES.put(knife.getUUID(), new KnifeState(
                        player.getUUID(), level.dimension(), origin, level.getGameTime() + 400));
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.1f, 0.72f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 1,
                PowerEventPayload.PHASE_RELEASE, 12, 24);
    }

    public static boolean combo(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || BARRAGES.containsKey(player.getUUID())) return false;
        long now = player.serverLevel().getGameTime();
        if (!LegendaryCombat.beginFree(player, 40)) return false;
        BARRAGES.put(player.getUUID(), new BarrageState(target.getUUID(), now, 0));
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_DIO, 0,
                PowerEventPayload.PHASE_STATE_START, 40, 5, 0);
        return true;
    }

    public static void vampireStrike(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = findTarget(player, 7);
        if (target == null || !LegendaryCombat.begin(player, VAMPIRE_STRIKE_COST, 10)) return;
        Vec3 direction = target.position().subtract(player.position()).normalize();
        player.setDeltaMovement(direction.scale(1.4).add(0, 0.15, 0));
        LegendaryCombat.damage(player, target, 26f, 0.17f, LegendaryCombat.AttackKind.PHYSICAL);
        Vec3 impactMotion = direction.scale(2.2).add(0, 0.35, 0);
        if (!level.noCollision(target, target.getBoundingBox().move(impactMotion))) {
            LegendaryCombat.damage(player, target, 10f, 0.06f, LegendaryCombat.AttackKind.PHYSICAL);
        }
        target.setDeltaMovement(impactMotion);
        target.hurtMarked = true;
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 1,
                PowerEventPayload.PHASE_RELEASE, 12, 7);
    }

    public static void temporalLock(ServerPlayer player) {
        LivingEntity target = findTarget(player, 32);
        if (target == null || TEMPORAL_LOCKS.containsKey(target.getUUID())
                || !LegendaryCombat.begin(player, TEMPORAL_LOCK_COST, 12)) return;
        TEMPORAL_LOCKS.put(target.getUUID(), new TemporalLockState(
                player.getUUID(), player.serverLevel().dimension(), target.position(),
                target.getDeltaMovement(), player.serverLevel().getGameTime() + 60));
        PowerEventDispatcher.broadcastAt(player, target.position(), PowerEventPayload.POWER_DIO, 3,
                PowerEventPayload.PHASE_STATE_START, 60, 5);
    }

    public static void timeDash(ServerPlayer player) {
        Vec3 origin = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 destination = null;
        for (int distance = 20; distance >= 2; distance--) {
            Vec3 candidate = origin.add(direction.scale(distance));
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(origin)))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null || !LegendaryCombat.begin(player, DASH_COST, 4)) return;

        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(direction.scale(0.8));
        player.fallDistance = 0;
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 2,
                PowerEventPayload.PHASE_RELEASE, 6, 20);
    }

    public static void timeStop(ServerPlayer player) {
        if (STOPS.containsKey(player.getUUID())) return;
        if (!LegendaryCombat.begin(player, STOP_COST, 30)) return;

        ServerLevel level = player.serverLevel();
        STOPS.put(player.getUUID(), new TimeStopState(
                player.getUUID(), level.dimension(), player.position(), level.getGameTime() + STOP_DURATION));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.DIO_TIME_STOP, SoundSource.MASTER, 3f, 1f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 3,
                PowerEventPayload.PHASE_STATE_START, STOP_DURATION, STOP_RADIUS);
    }

    public static void accelerate(ServerPlayer player) {
        if (!LegendaryCombat.begin(player, ACCELERATION_COST, 20)) return;

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 20, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 20, 1, false, false));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 4,
                PowerEventPayload.PHASE_STATE_START, 20 * 20, 8);
    }

    public static void rewind(ServerPlayer player) {
        if (PENDING_REWINDS.containsKey(player.getUUID()) || REWIND_JOBS.containsKey(player.getUUID())) return;
        Deque<TimeSnapshot> history = HISTORIES.get(player.getUUID());
        TimeSnapshot snapshot = history == null ? null : history.peekFirst();
        if (snapshot == null || !snapshot.dimension.equals(player.serverLevel().dimension())) {
            player.displayClientMessage(Component.translatable("message.overpowered.dio.no_history"), true);
            return;
        }
        if (!LegendaryCombat.begin(player, REWIND_COST, REWIND_PREPARE_TICKS)) return;

        long releaseTick = player.serverLevel().getGameTime() + REWIND_PREPARE_TICKS;
        PENDING_REWINDS.put(player.getUUID(), new PendingRewind(snapshot, releaseTick));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 6,
                PowerEventPayload.PHASE_PREPARE, REWIND_PREPARE_TICKS, HISTORY_BLOCK_RADIUS);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) captureHistories(server);
        tickPassive(server);
        tickBarrages(server);
        tickTemporalLocks(server);
        tickKnives(server);
        tickStops(server);
        tickPendingRewinds(server);
        tickRewindJobs(server);
    }

    public static boolean allowDamage(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source,
            float amount) {
        TemporalLockState lock = TEMPORAL_LOCKS.get(victim.getUUID());
        if (lock != null) {
            lock.accumulatedDamage += amount;
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker == null) return true;
        for (TimeStopState stop : STOPS.values()) {
            if (!stop.ownerId.equals(attacker.getUUID())
                    || !stop.dimension.equals(victim.level().dimension())
                    || victim.position().distanceToSqr(stop.center) > STOP_RADIUS * STOP_RADIUS) continue;
            stop.storedDamage.merge(victim.getUUID(), amount, Float::sum);
            return false;
        }
        return true;
    }

    private static void tickPassive(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.getMainHandItem().getItem() instanceof com.maxjonsi.overpowered.item.StoneMaskItem)
                    && !(player.getOffhandItem().getItem() instanceof com.maxjonsi.overpowered.item.StoneMaskItem)) {
                continue;
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, 0, true, false));
            if (!player.serverLevel().isDay()) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 25, 0, true, false));
            }
        }
    }

    private static void tickBarrages(MinecraftServer server) {
        Iterator<Map.Entry<UUID, BarrageState>> iterator = BARRAGES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BarrageState> entry = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            BarrageState state = entry.getValue();
            Entity entity = owner == null ? null : owner.serverLevel().getEntity(state.targetId);
            if (owner == null || !(entity instanceof LivingEntity target) || !target.isAlive()
                    || owner.distanceToSqr(target) > 7 * 7) {
                iterator.remove();
                continue;
            }
            long now = owner.serverLevel().getGameTime();
            if (now < state.nextHitTick) continue;
            boolean finisher = state.hits >= 9;
            LegendaryCombat.damage(owner, target, finisher ? 12f : 3f,
                    finisher ? 0.09f : 0.022f, LegendaryCombat.AttackKind.PHYSICAL);
            if (finisher) {
                Vec3 away = target.position().subtract(owner.position());
                if (away.lengthSqr() > 0.001) {
                    target.setDeltaMovement(away.normalize().scale(1.4).add(0, 0.45, 0));
                    target.hurtMarked = true;
                }
                PowerEventDispatcher.broadcastDetailed(owner, PowerEventPayload.POWER_DIO, 0,
                        PowerEventPayload.PHASE_STATE_END, 8, 5, 10);
                iterator.remove();
            } else {
                state.hits++;
                state.nextHitTick = now + 4;
                PowerEventDispatcher.broadcastDetailed(owner, PowerEventPayload.POWER_DIO, 0,
                        PowerEventPayload.PHASE_RELEASE, 4, 5, state.hits);
            }
        }
    }

    private static void tickTemporalLocks(MinecraftServer server) {
        Iterator<Map.Entry<UUID, TemporalLockState>> iterator = TEMPORAL_LOCKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TemporalLockState> entry = iterator.next();
            TemporalLockState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
            if (!(entity instanceof LivingEntity target) || owner == null) {
                iterator.remove();
                continue;
            }
            long now = level.getGameTime();
            if (now >= state.endTick) {
                iterator.remove();
                target.setDeltaMovement(state.motion);
                target.hurtMarked = true;
                for (Map.Entry<UUID, FrozenEntity> frozenEntry : state.projectiles.entrySet()) {
                    Entity projectile = level.getEntity(frozenEntry.getKey());
                    if (projectile != null) {
                        projectile.setDeltaMovement(frozenEntry.getValue().motion);
                        projectile.hurtMarked = true;
                    }
                }
                float ordinary = Math.min(80f, state.accumulatedDamage);
                float fraction = Math.min(0.35f,
                        state.accumulatedDamage / Math.max(1f, target.getMaxHealth()));
                if (ordinary > 0) {
                    LegendaryCombat.damage(owner, target, ordinary, fraction,
                            LegendaryCombat.AttackKind.TEMPORAL);
                }
                PowerEventDispatcher.broadcastAt(owner, target.position(), PowerEventPayload.POWER_DIO, 3,
                        PowerEventPayload.PHASE_STATE_END, 0, 5);
                continue;
            }
            if (target instanceof ServerPlayer targetPlayer) {
                targetPlayer.teleportTo(state.position.x, state.position.y, state.position.z);
            } else target.setPos(state.position.x, state.position.y, state.position.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
                    target.getBoundingBox().inflate(3), Projectile::isAlive)) {
                state.projectiles.computeIfAbsent(projectile.getUUID(), ignored ->
                        new FrozenEntity(projectile.position(), projectile.getDeltaMovement(),
                                projectile.getYRot(), projectile.getXRot()));
                projectile.setDeltaMovement(Vec3.ZERO);
                projectile.hurtMarked = true;
            }
        }
    }

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult blockHit = player.serverLevel().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.serverLevel(), player, eye, end,
                new AABB(eye, end).inflate(1),
                candidate -> candidate instanceof LivingEntity && candidate != player && candidate.isAlive());
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void captureHistories(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!hasStoneMask(player)) {
                HISTORIES.remove(player.getUUID());
                continue;
            }
            Deque<TimeSnapshot> history = HISTORIES.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
            history.addLast(captureSnapshot(player));
            while (history.size() > MAX_HISTORY_FRAMES) history.removeFirst();
        }
    }

    private static TimeSnapshot captureSnapshot(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - HISTORY_VERTICAL_RADIUS);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + HISTORY_VERTICAL_RADIUS);
        int side = HISTORY_BLOCK_RADIUS * 2 + 1;
        int count = side * side * (maxY - minY + 1);
        long[] positions = new long[count];
        int[] stateIds = new int[count];
        boolean[] protectedBlockEntities = new boolean[count];
        int cursor = 0;
        for (int x = center.getX() - HISTORY_BLOCK_RADIUS; x <= center.getX() + HISTORY_BLOCK_RADIUS; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = center.getZ() - HISTORY_BLOCK_RADIUS; z <= center.getZ() + HISTORY_BLOCK_RADIUS; z++) {
                    BlockPos position = new BlockPos(x, y, z);
                    positions[cursor] = position.asLong();
                    stateIds[cursor] = Block.getId(level.getBlockState(position));
                    protectedBlockEntities[cursor] = level.getBlockEntity(position) != null;
                    cursor++;
                }
            }
        }

        List<EntitySnapshot> entities = new ArrayList<>();
        AABB bounds = new AABB(center).inflate(
                HISTORY_BLOCK_RADIUS, HISTORY_VERTICAL_RADIUS, HISTORY_BLOCK_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds,
                candidate -> candidate != player && candidate.isAlive())) {
            entities.add(new EntitySnapshot(
                    entity.getUUID(), entity.position(), entity.getDeltaMovement(),
                    entity.getYRot(), entity.getXRot(), entity.getHealth()));
        }
        return new TimeSnapshot(level.dimension(), positions, stateIds, protectedBlockEntities, entities);
    }

    private static void tickKnives(MinecraftServer server) {
        Iterator<Map.Entry<UUID, KnifeState>> iterator = KNIVES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, KnifeState> entry = iterator.next();
            KnifeState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (!(entity instanceof ItemEntity knife) || level.getGameTime() >= state.expireTick) {
                if (entity != null) entity.discard();
                iterator.remove();
                continue;
            }

            if (state.embedded) {
                knife.setDeltaMovement(Vec3.ZERO);
                continue;
            }

            Vec3 current = knife.position();
            HitResult blockHit = level.clip(new ClipContext(
                    state.lastPosition, current, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, knife));
            if (blockHit.getType() != HitResult.Type.MISS) {
                state.embedded = true;
                knife.setPos(blockHit.getLocation().x, blockHit.getLocation().y,
                        blockHit.getLocation().z);
                knife.setDeltaMovement(Vec3.ZERO);
                continue;
            }

            ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(state.lastPosition, current).inflate(0.6),
                    target -> target.isAlive() && (owner == null || target != owner));
            if (!hits.isEmpty()) {
                LivingEntity target = hits.getFirst();
                target.hurt(owner != null
                        ? owner.damageSources().indirectMagic(knife, owner)
                        : level.damageSources().magic(), 7f);
                state.embedded = true;
                knife.setDeltaMovement(Vec3.ZERO);
                continue;
            }
            state.lastPosition = current;
        }
    }

    private static void tickStops(MinecraftServer server) {
        Iterator<Map.Entry<UUID, TimeStopState>> iterator = STOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TimeStopState> entry = iterator.next();
            TimeStopState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null || level.getGameTime() >= state.endTick) {
                iterator.remove();
                if (level != null) restoreFrozen(level, state);
                if (owner != null) {
                    PowerEventDispatcher.broadcast(owner, PowerEventPayload.POWER_DIO, 3,
                            PowerEventPayload.PHASE_STATE_END, 0, STOP_RADIUS);
                }
                continue;
            }

            AABB bounds = new AABB(state.center, state.center).inflate(STOP_RADIUS);
            for (Entity entity : level.getEntities(owner, bounds,
                    candidate -> candidate != owner && candidate.isAlive())) {
                FrozenEntity frozen = state.frozen.computeIfAbsent(entity.getUUID(), ignored ->
                        new FrozenEntity(entity.position(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot()));
                freeze(entity, frozen);
            }
        }
    }

    private static void freeze(Entity entity, FrozenEntity frozen) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(frozen.position.x, frozen.position.y, frozen.position.z);
        } else {
            entity.setPos(frozen.position.x, frozen.position.y, frozen.position.z);
        }
        entity.setYRot(frozen.yRot);
        entity.setXRot(frozen.xRot);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
        entity.hurtMarked = true;
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 10, true, false));
        }
    }

    private static void restoreFrozen(ServerLevel level, TimeStopState state) {
        state.frozen.forEach((entityId, frozen) -> {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                entity.setDeltaMovement(frozen.motion);
                entity.hurtMarked = true;
            }
        });
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(state.ownerId);
        if (owner != null) {
            state.storedDamage.forEach((targetId, stored) -> {
                Entity entity = level.getEntity(targetId);
                if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
                float ordinary = Math.min(80f, stored);
                float fraction = Math.min(0.40f, stored / Math.max(1f, target.getMaxHealth()));
                LegendaryCombat.damage(owner, target, ordinary, fraction,
                        LegendaryCombat.AttackKind.TEMPORAL);
            });
        }
    }

    private static void tickPendingRewinds(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PendingRewind>> iterator = PENDING_REWINDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRewind> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            PendingRewind pending = entry.getValue();
            if (player.serverLevel().getGameTime() < pending.releaseTick) continue;

            REWIND_JOBS.put(entry.getKey(), new RewindJob(pending.snapshot));
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 6,
                    PowerEventPayload.PHASE_RELEASE, 20, HISTORY_BLOCK_RADIUS);
            iterator.remove();
        }
    }

    private static void tickRewindJobs(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RewindJob>> iterator = REWIND_JOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RewindJob> entry = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            RewindJob job = entry.getValue();
            ServerLevel level = server.getLevel(job.snapshot.dimension);
            if (owner == null || level == null) {
                iterator.remove();
                continue;
            }

            int limit = Math.min(job.snapshot.positions.length, job.cursor + BLOCKS_PER_REWIND_TICK);
            for (; job.cursor < limit; job.cursor++) {
                BlockPos position = BlockPos.of(job.snapshot.positions[job.cursor]);
                if (job.snapshot.protectedBlockEntities[job.cursor]
                        || level.getBlockEntity(position) != null) continue;
                BlockState target = Block.stateById(job.snapshot.stateIds[job.cursor]);
                if (target != null && !level.getBlockState(position).equals(target)) {
                    level.setBlock(position, target, 2 | 16);
                }
            }
            if (job.cursor < job.snapshot.positions.length) continue;

            restoreEntities(level, owner, job.snapshot.entities);
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.MASTER, 2f, 0.55f);
            PowerEventDispatcher.broadcast(owner, PowerEventPayload.POWER_DIO, 6,
                    PowerEventPayload.PHASE_AFTERMATH, 40, HISTORY_BLOCK_RADIUS);
            iterator.remove();
        }
    }

    private static void restoreEntities(ServerLevel level, ServerPlayer owner, List<EntitySnapshot> snapshots) {
        for (EntitySnapshot snapshot : snapshots) {
            Entity entity = level.getEntity(snapshot.entityId);
            if (!(entity instanceof LivingEntity living) || entity == owner) continue;
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(snapshot.position.x, snapshot.position.y, snapshot.position.z);
            } else {
                entity.setPos(snapshot.position.x, snapshot.position.y, snapshot.position.z);
            }
            entity.setDeltaMovement(snapshot.motion);
            entity.setYRot(snapshot.yRot);
            entity.setXRot(snapshot.xRot);
            living.setHealth(Math.min(living.getMaxHealth(), Math.max(1, snapshot.health)));
            entity.hurtMarked = true;
        }
    }

    private static boolean hasStoneMask(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.STONE_MASK)) return true;
        }
        return false;
    }

    public static void clearPlayer(MinecraftServer server, UUID playerId) {
        HISTORIES.remove(playerId);
        PENDING_REWINDS.remove(playerId);
        REWIND_JOBS.remove(playerId);
        COMBOS.remove(playerId);
        BARRAGES.remove(playerId);
        TEMPORAL_LOCKS.entrySet().removeIf(entry -> entry.getKey().equals(playerId)
                || entry.getValue().ownerId.equals(playerId));
        TimeStopState stop = STOPS.remove(playerId);
        if (stop != null) {
            ServerLevel level = server.getLevel(stop.dimension);
            if (level != null) restoreFrozen(level, stop);
        }
    }

    public static void clear() {
        KNIVES.clear();
        STOPS.clear();
        HISTORIES.clear();
        PENDING_REWINDS.clear();
        REWIND_JOBS.clear();
        COMBOS.clear();
        BARRAGES.clear();
        TEMPORAL_LOCKS.clear();
    }

    private static final class KnifeState {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private Vec3 lastPosition;
        private final long expireTick;
        private boolean embedded;

        private KnifeState(UUID ownerId, ResourceKey<Level> dimension, Vec3 lastPosition, long expireTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.lastPosition = lastPosition;
            this.expireTick = expireTick;
        }
    }

    private static final class TimeStopState {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final long endTick;
        private final Map<UUID, FrozenEntity> frozen = new HashMap<>();
        private final Map<UUID, Float> storedDamage = new HashMap<>();

        private TimeStopState(UUID ownerId, ResourceKey<Level> dimension, Vec3 center, long endTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.center = center;
            this.endTick = endTick;
        }
    }

    private record FrozenEntity(Vec3 position, Vec3 motion, float yRot, float xRot) {
    }

    private record EntitySnapshot(UUID entityId, Vec3 position, Vec3 motion, float yRot, float xRot, float health) {
    }

    private record TimeSnapshot(
            ResourceKey<Level> dimension,
            long[] positions,
            int[] stateIds,
            boolean[] protectedBlockEntities,
            List<EntitySnapshot> entities) {
    }

    private record PendingRewind(TimeSnapshot snapshot, long releaseTick) {
    }

    private static final class BarrageState {
        private final UUID targetId;
        private long nextHitTick;
        private int hits;

        private BarrageState(UUID targetId, long nextHitTick, int hits) {
            this.targetId = targetId;
            this.nextHitTick = nextHitTick;
            this.hits = hits;
        }
    }

    private static final class TemporalLockState {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final Vec3 position;
        private final Vec3 motion;
        private final long endTick;
        private final Map<UUID, FrozenEntity> projectiles = new HashMap<>();
        private float accumulatedDamage;

        private TemporalLockState(UUID ownerId, ResourceKey<Level> dimension,
                Vec3 position, Vec3 motion, long endTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.position = position;
            this.motion = motion;
            this.endTick = endTick;
        }
    }

    private static final class RewindJob {
        private final TimeSnapshot snapshot;
        private int cursor;

        private RewindJob(TimeSnapshot snapshot) {
            this.snapshot = snapshot;
        }
    }
}
