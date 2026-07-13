package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.ShadowRemnantEntity;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class VoidAbility {
    public static final double RANGE = 32;
    public static final int TRANSFORMATION_COST = 10;
    public static final int TOUCH_COST = 8;
    public static final int ERASE_COST = 25;
    public static final int WAVE_COST = 20;
    public static final int SILENCE_COST = 35;
    public static final int ABSOLUTE_VOID_COST = 90;
    public static final int SILENCE_DURATION = 100;
    public static final int ABSOLUTE_VOID_PREPARE = 40;
    public static final int ABSOLUTE_VOID_RELEASE = 120;
    public static final int ABSOLUTE_VOID_AFTERMATH = 80;
    public static final double ABSOLUTE_VOID_RADIUS = 36;

    private static final Map<UUID, Long> SILENCED = new HashMap<>();
    private static final Map<UUID, AbsoluteVoidState> FIELDS = new HashMap<>();

    private VoidAbility() {
    }

    public static void toggle(ServerPlayer player) {
        boolean active = !VoidServerState.isActive(player.getUUID());
        if (active && !PlayerEnergyManager.tryConsumeOrNotify(player, TRANSFORMATION_COST)) return;

        if (active) {
            VoidServerState.activate(player);
        } else {
            VoidServerState.deactivate(player);
        }
        broadcastState(player, active);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MAGIC_FORCEFIELD, SoundSource.PLAYERS, 1.5f, 0.55f);
        if (active) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.VOID_FORM_AMBIENCE, SoundSource.AMBIENT, 1.2f, 1f);
        }
        player.displayClientMessage(Component.translatable(active ? "message.overpowered.void.on" : "message.overpowered.void.off"), true);
    }

    public static void kill(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID()) || !player.isAlive() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        LivingEntity victim = findTarget(level, player);
        if (victim == null) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, ERASE_COST)) return;

        eraseVictim(level, victim);
    }

    public static void touch(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())) return;
        LivingEntity victim = findTarget(player.serverLevel(), player, 4.5);
        if (victim != null) touch(player, victim);
    }

    public static boolean touch(ServerPlayer player, LivingEntity victim) {
        if (!VoidServerState.isActive(player.getUUID()) || victim == player || !victim.isAlive()) return false;
        if (player.distanceToSqr(victim) > 5.5 * 5.5) return false;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, TOUCH_COST)) return false;

        eraseVictim(player.serverLevel(), victim);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 1,
                PowerEventPayload.PHASE_RELEASE, 8, 5);
        return true;
    }

    public static void wave(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())
                || !PlayerEnergyManager.tryConsumeOrNotify(player, WAVE_COST)) return;

        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0, 1, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(14), entity -> entity != player && entity.isAlive())) {
            Vec3 away = target.position().subtract(player.position());
            if (away.lengthSqr() < 0.001) away = player.getLookAngle();
            double strength = Math.max(0.5, 4.0 * (1.0 - Math.min(14, away.length()) / 16.0));
            target.setDeltaMovement(away.normalize().scale(strength).add(0, 0.7, 0));
            target.hurtMarked = true;
            target.hurt(level.damageSources().indirectMagic(player, player), 18f);
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z,
                80, 6, 2, 6, 0.12);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 3,
                PowerEventPayload.PHASE_RELEASE, 20, 14);
    }

    public static void silence(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())
                || !PlayerEnergyManager.tryConsumeOrNotify(player, SILENCE_COST)) return;

        ServerLevel level = player.serverLevel();
        long until = level.getGameTime() + SILENCE_DURATION;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(18), entity -> entity != player && entity.isAlive())) {
            SILENCED.put(target.getUUID(), until);
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, SILENCE_DURATION, 0, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SILENCE_DURATION, 3, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SILENCE_DURATION, 4, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, SILENCE_DURATION, 5, false, false));
        }
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 4,
                PowerEventPayload.PHASE_RELEASE, SILENCE_DURATION, 18);
    }

    public static void absoluteVoid(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID()) || FIELDS.containsKey(player.getUUID())) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, ABSOLUTE_VOID_COST)) return;

        long now = player.serverLevel().getGameTime();
        FIELDS.put(player.getUUID(), new AbsoluteVoidState(
                player.serverLevel().dimension(), player.position(),
                now + ABSOLUTE_VOID_PREPARE,
                now + ABSOLUTE_VOID_PREPARE + ABSOLUTE_VOID_RELEASE,
                now + ABSOLUTE_VOID_PREPARE + ABSOLUTE_VOID_RELEASE + ABSOLUTE_VOID_AFTERMATH));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                ABSOLUTE_VOID_PREPARE, 10, false, false));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.VOID_ABSOLUTE_RELEASE, SoundSource.MASTER, 4f, 1f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 6,
                PowerEventPayload.PHASE_PREPARE, ABSOLUTE_VOID_PREPARE, (int) ABSOLUTE_VOID_RADIUS);
    }

    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        return !(victim instanceof ServerPlayer player && VoidServerState.isActive(player.getUUID()));
    }

    public static boolean isSilenced(UUID entityId) {
        return SILENCED.containsKey(entityId);
    }

    public static void tick(MinecraftServer server) {
        SILENCED.entrySet().removeIf(entry -> {
            Entity entity = findEntity(server, entry.getKey());
            if (entity == null) return true;
            return entity.level().getGameTime() >= entry.getValue();
        });

        Iterator<Map.Entry<UUID, AbsoluteVoidState>> iterator = FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, AbsoluteVoidState> entry = iterator.next();
            AbsoluteVoidState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null) {
                iterator.remove();
                continue;
            }

            long now = level.getGameTime();
            if (now < state.prepareEnd) {
                if (now % 3 == 0) {
                    level.sendParticles(ParticleTypes.SCULK_SOUL,
                            state.center.x, state.center.y + 1, state.center.z,
                            12, 3, 2, 3, 0.02);
                }
                continue;
            }

            if (!state.releaseSignalled) {
                state.releaseSignalled = true;
                level.playSound(null, state.center.x, state.center.y, state.center.z,
                        ModSounds.VOID_ABSOLUTE_AMBIENCE, SoundSource.AMBIENT, 3f, 1f);
                PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_VOID, 6,
                        PowerEventPayload.PHASE_RELEASE, ABSOLUTE_VOID_RELEASE, (int) ABSOLUTE_VOID_RADIUS);
            }

            if (now < state.releaseEnd) {
                double progress = Math.min(1.0, (now - state.prepareEnd + 1) / 40.0);
                double radius = ABSOLUTE_VOID_RADIUS * progress;
                AABB bounds = new AABB(state.center, state.center).inflate(radius);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                        entity -> entity != owner && entity.isAlive()
                                && entity.position().distanceToSqr(state.center) <= radius * radius)) {
                    eraseVictim(level, target);
                }
                continue;
            }

            if (!state.aftermathSignalled) {
                state.aftermathSignalled = true;
                PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_VOID, 6,
                        PowerEventPayload.PHASE_AFTERMATH, ABSOLUTE_VOID_AFTERMATH,
                        (int) ABSOLUTE_VOID_RADIUS);
            }
            if (now >= state.aftermathEnd) iterator.remove();
        }
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    private static void eraseVictim(ServerLevel level, LivingEntity victim) {
        if (!victim.isAlive()) return;
        BlockPos ground = victim.blockPosition();
        for (int i = 0; i < 6 && level.getBlockState(ground.below()).isAir(); i++) {
            ground = ground.below();
        }

        ShadowRemnantEntity shadow = new ShadowRemnantEntity(ModEntities.SHADOW_REMNANT, level);
        shadow.setPos(victim.getX(), ground.getY() + 0.05, victim.getZ());
        shadow.setSize(Mth.clamp(victim.getBbWidth() * 2.4f, 1.2f, 5f));
        shadow.setYRot(victim.getYRot());
        level.addFreshEntity(shadow);

        level.sendParticles(ParticleTypes.SCULK_SOUL, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
                25, 0.3, victim.getBbHeight() / 3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
                40, 0.3, victim.getBbHeight() / 3, 0.3, 0.03);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.VOID_KILL, SoundSource.PLAYERS, 2f, 1f);

        victim.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
    }

    public static LivingEntity findTarget(Level level, Player player) {
        return findTarget(level, player, RANGE);
    }

    public static LivingEntity findTarget(Level level, Player player, double range) {
        if (!player.isAlive() || player.isSpectator()) return null;

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1), entity -> entity != player && isValidTarget(entity));
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public static void syncState(ServerPlayer player) {
        broadcastState(player, VoidServerState.isActive(player.getUUID()));
    }

    public static void sendState(ServerPlayer subject, ServerPlayer viewer, boolean active) {
        ServerPlayNetworking.send(viewer, new VoidStatePayload(subject.getId(), active));
    }

    private static boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity living
                && living.isAlive()
                && !living.isSpectator();
    }

    private static void broadcastState(ServerPlayer player, boolean active) {
        VoidStatePayload payload = new VoidStatePayload(player.getId(), active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    public static void clear() {
        SILENCED.clear();
        FIELDS.clear();
    }

    private static final class AbsoluteVoidState {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final long prepareEnd;
        private final long releaseEnd;
        private final long aftermathEnd;
        private boolean releaseSignalled;
        private boolean aftermathSignalled;

        private AbsoluteVoidState(
                ResourceKey<Level> dimension,
                Vec3 center,
                long prepareEnd,
                long releaseEnd,
                long aftermathEnd) {
            this.dimension = dimension;
            this.center = center;
            this.prepareEnd = prepareEnd;
            this.releaseEnd = releaseEnd;
            this.aftermathEnd = aftermathEnd;
        }
    }
}
