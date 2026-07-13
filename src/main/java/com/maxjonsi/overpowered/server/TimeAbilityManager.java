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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class TimeAbilityManager {
    public static final int KNIFE_COST = 12;
    public static final int DASH_COST = 10;
    public static final int STOP_COST = 45;
    public static final int ACCELERATION_COST = 25;
    public static final int REWIND_COST = 90;
    public static final int STOP_DURATION = 100;
    public static final int STOP_RADIUS = 32;
    public static final int REWIND_SECONDS = 6;
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

    private TimeAbilityManager() {
    }

    public static void throwKnives(ServerPlayer player) {
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, KNIFE_COST)) return;

        ServerLevel level = player.serverLevel();
        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.8));
        for (int index = -2; index <= 2; index++) {
            Vec3 direction = player.getLookAngle().yRot(index * 0.07f).normalize();
            ItemEntity knife = new ItemEntity(level, origin.x, origin.y, origin.z,
                    new ItemStack(ModItems.DIO_KNIFE));
            knife.setNoGravity(true);
            knife.setInvulnerable(true);
            knife.setPickUpDelay(200);
            knife.setDeltaMovement(direction.scale(1.9));
            if (level.addFreshEntity(knife)) {
                KNIVES.put(knife.getUUID(), new KnifeState(
                        player.getUUID(), level.dimension(), origin, level.getGameTime() + 60));
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.1f, 0.72f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 1,
                PowerEventPayload.PHASE_RELEASE, 12, 24);
    }

    public static boolean combo(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive()) return false;
        long now = player.serverLevel().getGameTime();
        long[] combo = COMBOS.computeIfAbsent(player.getUUID(), ignored -> new long[]{-1, Long.MIN_VALUE / 2});
        if (now - combo[1] < 3) return false;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, 2)) return false;

        int stage = now - combo[1] <= 12 ? (int) ((combo[0] + 1) % 4) : 0;
        combo[0] = stage;
        combo[1] = now;
        target.hurt(player.damageSources().playerAttack(player), 7f + stage * 1.5f);
        if (stage == 3) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.65, 0));
            target.hurtMarked = true;
        }
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_DIO, 0,
                PowerEventPayload.PHASE_RELEASE, 5, 4, stage);
        return true;
    }

    public static void timeDash(ServerPlayer player) {
        Vec3 origin = player.position();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 destination = null;
        for (int distance = 14; distance >= 2; distance--) {
            Vec3 candidate = origin.add(direction.scale(distance));
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(origin)))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null || !PlayerEnergyManager.tryConsumeOrNotify(player, DASH_COST)) return;

        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(direction.scale(0.8));
        player.fallDistance = 0;
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 2,
                PowerEventPayload.PHASE_RELEASE, 6, 14);
    }

    public static void timeStop(ServerPlayer player) {
        if (STOPS.containsKey(player.getUUID())) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, STOP_COST)) return;

        ServerLevel level = player.serverLevel();
        STOPS.put(player.getUUID(), new TimeStopState(
                level.dimension(), player.position(), level.getGameTime() + STOP_DURATION));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.DIO_TIME_STOP, SoundSource.MASTER, 3f, 1f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 3,
                PowerEventPayload.PHASE_STATE_START, STOP_DURATION, STOP_RADIUS);
    }

    public static void accelerate(ServerPlayer player) {
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, ACCELERATION_COST)) return;

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 30, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 30, 1, false, false));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 4,
                PowerEventPayload.PHASE_STATE_START, 20 * 30, 8);
    }

    public static void rewind(ServerPlayer player) {
        if (PENDING_REWINDS.containsKey(player.getUUID()) || REWIND_JOBS.containsKey(player.getUUID())) return;
        Deque<TimeSnapshot> history = HISTORIES.get(player.getUUID());
        TimeSnapshot snapshot = history == null ? null : history.peekFirst();
        if (snapshot == null || !snapshot.dimension.equals(player.serverLevel().dimension())) {
            player.displayClientMessage(Component.translatable("message.overpowered.dio.no_history"), true);
            return;
        }
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, REWIND_COST)) return;

        long releaseTick = player.serverLevel().getGameTime() + REWIND_PREPARE_TICKS;
        PENDING_REWINDS.put(player.getUUID(), new PendingRewind(snapshot, releaseTick));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_DIO, 6,
                PowerEventPayload.PHASE_PREPARE, REWIND_PREPARE_TICKS, HISTORY_BLOCK_RADIUS);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) captureHistories(server);
        tickKnives(server);
        tickStops(server);
        tickPendingRewinds(server);
        tickRewindJobs(server);
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

            Vec3 current = knife.position();
            HitResult blockHit = level.clip(new ClipContext(
                    state.lastPosition, current, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, knife));
            if (blockHit.getType() != HitResult.Type.MISS) {
                knife.discard();
                iterator.remove();
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
                        : level.damageSources().magic(), 11f);
                knife.discard();
                iterator.remove();
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
                if (level != null) restoreFrozen(level, state);
                if (owner != null) {
                    PowerEventDispatcher.broadcast(owner, PowerEventPayload.POWER_DIO, 3,
                            PowerEventPayload.PHASE_STATE_END, 0, STOP_RADIUS);
                }
                iterator.remove();
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
    }

    private static final class KnifeState {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private Vec3 lastPosition;
        private final long expireTick;

        private KnifeState(UUID ownerId, ResourceKey<Level> dimension, Vec3 lastPosition, long expireTick) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.lastPosition = lastPosition;
            this.expireTick = expireTick;
        }
    }

    private static final class TimeStopState {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final long endTick;
        private final Map<UUID, FrozenEntity> frozen = new HashMap<>();

        private TimeStopState(ResourceKey<Level> dimension, Vec3 center, long endTick) {
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

    private static final class RewindJob {
        private final TimeSnapshot snapshot;
        private int cursor;

        private RewindJob(TimeSnapshot snapshot) {
            this.snapshot = snapshot;
        }
    }
}
