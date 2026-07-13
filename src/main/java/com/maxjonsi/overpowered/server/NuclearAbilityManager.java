package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.HomingRocketEntity;
import com.maxjonsi.overpowered.entity.NukeEntity;
import com.maxjonsi.overpowered.item.LaserBeamHelper;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class NuclearAbilityManager {
    public static final int MINI_NUKE_COST = 18;
    public static final int MIRV_COST = 35;
    public static final int ORBITAL_COST = 60;
    public static final int LASER_BURST_COST = 5;
    public static final int APOCALYPSE_COST = 95;
    public static final int APOCALYPSE_PREPARE_TICKS = 60;
    public static final int APOCALYPSE_STRIKES = 5;
    public static final int RADIATION_DURATION = 20 * 30;

    private static final Map<UUID, LaserBurstState> LASER_BURSTS = new HashMap<>();
    private static final Map<UUID, ApocalypseState> APOCALYPSES = new HashMap<>();
    private static final List<RadiationZone> RADIATION = new ArrayList<>();

    private NuclearAbilityManager() {
    }

    public static void miniNuke(ServerPlayer player, ItemStack launcher) {
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, MINI_NUKE_COST)) return;
        spawnRocket(player, launcher, player.getLookAngle(), 7.5f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_NUCLEAR, 1,
                PowerEventPayload.PHASE_RELEASE, 20, 12);
    }

    public static void mirv(ServerPlayer player, ItemStack launcher) {
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, MIRV_COST)) return;

        for (int index = 0; index < 8; index++) {
            double angle = (index - 3.5) * 0.105;
            Vec3 direction = player.getLookAngle().yRot((float) angle)
                    .add(0, 0.08 + Math.abs(index - 3.5) * 0.015, 0)
                    .normalize();
            spawnRocket(player, launcher, direction, 4.5f);
        }
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_NUCLEAR, 2,
                PowerEventPayload.PHASE_RELEASE, 30, 28);
    }

    public static void orbitalStrike(ServerPlayer player) {
        Vec3 target = aimedBlock(player, 300);
        if (target == null || !PlayerEnergyManager.tryConsumeOrNotify(player, ORBITAL_COST)) return;

        spawnNuke(player.serverLevel(), target, 48);
        PowerEventDispatcher.broadcastAt(player, target, PowerEventPayload.POWER_NUCLEAR, 3,
                PowerEventPayload.PHASE_RELEASE, 120, 48);
    }

    public static void laserBurst(ServerPlayer player) {
        if (LASER_BURSTS.containsKey(player.getUUID())) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, LASER_BURST_COST)) return;

        LASER_BURSTS.put(player.getUUID(), new LaserBurstState(
                player.serverLevel().dimension(), player.serverLevel().getGameTime() + 80));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LAUNCHER_LASER, SoundSource.PLAYERS, 1.8f, 1f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_NUCLEAR, 4,
                PowerEventPayload.PHASE_STATE_START, 80, 64);
    }

    public static void apocalypse(ServerPlayer player) {
        if (APOCALYPSES.containsKey(player.getUUID())) return;
        Vec3 target = aimedBlock(player, 300);
        if (target == null || !PlayerEnergyManager.tryConsumeOrNotify(player, APOCALYPSE_COST)) return;

        long releaseTick = player.serverLevel().getGameTime() + APOCALYPSE_PREPARE_TICKS;
        APOCALYPSES.put(player.getUUID(), new ApocalypseState(
                player.serverLevel().dimension(), target, releaseTick, releaseTick,
                APOCALYPSE_STRIKES, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                APOCALYPSE_PREPARE_TICKS, 10, false, false));
        PowerEventDispatcher.broadcastAt(player, target, PowerEventPayload.POWER_NUCLEAR, 6,
                PowerEventPayload.PHASE_PREPARE, APOCALYPSE_PREPARE_TICKS, 90);
    }

    private static void spawnRocket(ServerPlayer player, ItemStack launcher, Vec3 direction, float explosionPower) {
        ServerLevel level = player.serverLevel();
        Vec3 spawn = player.getEyePosition().add(direction.scale(1.2)).subtract(0, 0.2, 0);
        HomingRocketEntity rocket = new HomingRocketEntity(ModEntities.HOMING_ROCKET, level);
        rocket.setOwner(player);
        rocket.setPos(spawn.x, spawn.y, spawn.z);
        rocket.setDeltaMovement(direction.scale(1.4));
        rocket.setExplosionPower(explosionPower);
        UUID targetId = launcher.get(ModDataComponents.TARGET);
        if (targetId != null && level.getEntity(targetId) instanceof LivingEntity target && target.isAlive()) {
            rocket.setTargetId(target.getId());
        }
        level.addFreshEntity(rocket);
    }

    private static void spawnNuke(ServerLevel level, Vec3 target, int radius) {
        double spawnY = Math.min(level.getMaxBuildHeight() - 12, target.y + 95);
        NukeEntity nuke = new NukeEntity(ModEntities.NUKE, level);
        nuke.setBlastRadius(radius);
        nuke.setPos(target.x, spawnY, target.z);
        nuke.prepareForLaunch(level);
        if (!level.addFreshEntity(nuke)) nuke.discard();
    }

    private static Vec3 aimedBlock(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(
                eye, eye.add(look.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    public static void addRadiationZone(ServerLevel level, Vec3 center, int radius, int durationTicks) {
        RADIATION.add(new RadiationZone(
                level.dimension(), center, radius, level.getGameTime() + durationTicks));
    }

    public static void tick(MinecraftServer server) {
        tickLaserBursts(server);
        tickApocalypses(server);
        tickRadiation(server);
    }

    private static void tickLaserBursts(MinecraftServer server) {
        Iterator<Map.Entry<UUID, LaserBurstState>> iterator = LASER_BURSTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, LaserBurstState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            LaserBurstState state = entry.getValue();
            if (player == null || !player.serverLevel().dimension().equals(state.dimension)
                    || player.serverLevel().getGameTime() >= state.endTick) {
                if (player != null) {
                    PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_NUCLEAR, 4,
                            PowerEventPayload.PHASE_STATE_END, 0, 64);
                }
                iterator.remove();
                continue;
            }

            if (player.tickCount % RocketLauncherItem.LASER_ENERGY_INTERVAL_TICKS == 0
                    && !PlayerEnergyManager.tryConsumeOrNotify(player, RocketLauncherItem.LASER_TICK_COST)) {
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_NUCLEAR, 4,
                        PowerEventPayload.PHASE_STATE_END, 0, 64);
                iterator.remove();
                continue;
            }

            ServerLevel level = player.serverLevel();
            LaserBeamHelper.Trace trace = LaserBeamHelper.trace(
                    level, player, player.getEyePosition(), player.getLookAngle());
            EntityHitResult entityHit = trace.entityHit();
            if (entityHit != null) {
                entityHit.getEntity().hurt(player.damageSources().indirectMagic(player, player), 5f);
                entityHit.getEntity().igniteForSeconds(2);
            } else if (trace.blockHit() != null && trace.blockHit().getType() == HitResult.Type.BLOCK) {
                BlockPos position = trace.blockHit().getBlockPos();
                if (level.getBlockState(position).getDestroySpeed(level, position) >= 0) {
                    level.destroyBlock(position, false, player);
                }
            }
            Vec3 end = trace.end();
            level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z,
                    5, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private static void tickApocalypses(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ApocalypseState>> iterator = APOCALYPSES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ApocalypseState> entry = iterator.next();
            ApocalypseState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null) {
                iterator.remove();
                continue;
            }
            long now = level.getGameTime();
            if (now < state.releaseTick) continue;

            if (!state.releaseSignalled) {
                state.releaseSignalled = true;
                PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_NUCLEAR, 6,
                        PowerEventPayload.PHASE_RELEASE, 20 * APOCALYPSE_STRIKES, 90);
            }
            if (state.remainingStrikes > 0 && now >= state.nextStrikeTick) {
                int index = APOCALYPSE_STRIKES - state.remainingStrikes;
                double angle = index * Math.PI * 2 / APOCALYPSE_STRIKES;
                double distance = index == 0 ? 0 : 32;
                Vec3 target = state.center.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
                spawnNuke(level, target, 65);
                state.remainingStrikes--;
                state.nextStrikeTick = now + 20;
            }
            if (state.remainingStrikes == 0 && now >= state.nextStrikeTick + 20) {
                PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_NUCLEAR, 6,
                        PowerEventPayload.PHASE_AFTERMATH, RADIATION_DURATION, 90);
                iterator.remove();
            }
        }
    }

    private static void tickRadiation(MinecraftServer server) {
        Iterator<RadiationZone> iterator = RADIATION.iterator();
        while (iterator.hasNext()) {
            RadiationZone zone = iterator.next();
            ServerLevel level = server.getLevel(zone.dimension);
            if (level == null || level.getGameTime() >= zone.endTick) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() % 20 != 0) continue;

            AABB bounds = new AABB(zone.center, zone.center).inflate(zone.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                    entity -> entity.isAlive()
                            && entity.position().distanceToSqr(zone.center) <= zone.radius * zone.radius)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 2, true, false));
            }
            level.sendParticles(new DustParticleOptions(new Vector3f(0.45f, 0.65f, 0.05f), 1.2f),
                    zone.center.x, zone.center.y + 12, zone.center.z,
                    40, zone.radius * 0.65, 10, zone.radius * 0.65, 0.02);
        }
    }

    public static void clearPlayer(UUID playerId) {
        LASER_BURSTS.remove(playerId);
        APOCALYPSES.remove(playerId);
    }

    public static void clear() {
        LASER_BURSTS.clear();
        APOCALYPSES.clear();
        RADIATION.clear();
    }

    private record LaserBurstState(ResourceKey<Level> dimension, long endTick) {
    }

    private static final class ApocalypseState {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final long releaseTick;
        private long nextStrikeTick;
        private int remainingStrikes;
        private boolean releaseSignalled;

        private ApocalypseState(
                ResourceKey<Level> dimension,
                Vec3 center,
                long releaseTick,
                long nextStrikeTick,
                int remainingStrikes,
                boolean releaseSignalled) {
            this.dimension = dimension;
            this.center = center;
            this.releaseTick = releaseTick;
            this.nextStrikeTick = nextStrikeTick;
            this.remainingStrikes = remainingStrikes;
            this.releaseSignalled = releaseSignalled;
        }
    }

    private record RadiationZone(ResourceKey<Level> dimension, Vec3 center, int radius, long endTick) {
    }
}
