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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class VoidAbility {
    public static final double RANGE = 80;
    public static final int TRANSFORMATION_COST = 0;
    public static final int STEP_COST = 8;
    public static final int TOUCH_COST = 20;
    public static final int ERASE_COST = 30;
    public static final int WAVE_COST = 25;
    public static final int SILENCE_COST = 55;
    public static final int ABSOLUTE_VOID_COST = 100;
    public static final int SILENCE_DURATION = 20 * 8;
    public static final int ABSOLUTE_VOID_PREPARE = 60;
    public static final int ABSOLUTE_VOID_RELEASE = 20 * 10;
    public static final int ABSOLUTE_VOID_AFTERMATH = 80;
    public static final double ABSOLUTE_VOID_RADIUS = 50;

    private static final Map<UUID, long[]> COMBOS = new HashMap<>();
    private static final Map<UUID, VoidMark> MARKS = new HashMap<>();
    private static final Map<UUID, Long> SILENCED = new HashMap<>();
    private static final Map<UUID, SilenceField> SILENCE_FIELDS = new HashMap<>();
    private static final Map<UUID, PendingGaze> GAZES = new HashMap<>();
    private static final Map<UUID, AbsoluteVoidState> FIELDS = new HashMap<>();

    private VoidAbility() {
    }

    public static void toggle(ServerPlayer player) {
        boolean active = !VoidServerState.isActive(player.getUUID());
        if (active && !LegendaryCombat.begin(player, TRANSFORMATION_COST, 12)) return;
        if (active) VoidServerState.activate(player);
        else VoidServerState.deactivate(player);
        broadcastState(player, active);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1,
                player.getZ(), 28, 0.35, 0.8, 0.35, 0.01);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MAGIC_FORCEFIELD, SoundSource.PLAYERS, 1.2f, 0.45f);
        if (active) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.VOID_FORM_AMBIENCE, SoundSource.AMBIENT, 1.2f, 1f);
        }
        player.displayClientMessage(Component.translatable(
                active ? "message.overpowered.void.on" : "message.overpowered.void.off"), true);
    }

    public static boolean basicStrike(ServerPlayer player, LivingEntity victim) {
        if (!VoidServerState.isActive(player.getUUID()) || victim == player || !victim.isAlive()) return false;
        long now = player.serverLevel().getGameTime();
        long[] combo = COMBOS.computeIfAbsent(player.getUUID(), ignored ->
                new long[]{-1, Long.MIN_VALUE / 2});
        if (now - combo[1] < 7 || !LegendaryCombat.beginFree(player, 6)) return false;
        int stage = now - combo[1] <= 22 ? (int) ((combo[0] + 1) % 3) : 0;
        combo[0] = stage;
        combo[1] = now;
        LegendaryCombat.damage(player, victim, stage == 2 ? 24f : 14f,
                stage == 2 ? 0.16f : 0.09f, LegendaryCombat.AttackKind.PHYSICAL);
        Vec3 away = victim.position().subtract(player.position());
        if (away.lengthSqr() > 0.001) {
            victim.setDeltaMovement(away.normalize().scale(stage == 2 ? 1.8 : 0.8).add(0, 0.25, 0));
            victim.hurtMarked = true;
        }
        if (stage == 2) LegendaryCombat.stagger(victim, 30, 2);
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_VOID, 0,
                PowerEventPayload.PHASE_RELEASE, 8, 5, stage);
        return true;
    }

    public static void step(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())) return;
        Vec3 origin = player.position();
        Vec3 direction = player.getLookAngle().multiply(1, 0.25, 1).normalize();
        Vec3 destination = null;
        for (int distance = 25; distance >= 2; distance--) {
            Vec3 candidate = origin.add(direction.scale(distance));
            if (player.serverLevel().noCollision(player,
                    player.getBoundingBox().move(candidate.subtract(origin)))) {
                destination = candidate;
                break;
            }
        }
        if (destination == null || !LegendaryCombat.begin(player, STEP_COST, 5)) return;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 1,
                PowerEventPayload.PHASE_RELEASE, 10, 25);
    }

    public static void touch(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())) return;
        LivingEntity victim = findTarget(player.serverLevel(), player, 5.5);
        if (victim != null) touch(player, victim);
    }

    public static boolean touch(ServerPlayer player, LivingEntity victim) {
        if (!VoidServerState.isActive(player.getUUID()) || victim == player || !victim.isAlive()
                || player.distanceToSqr(victim) > 6 * 6
                || !LegendaryCombat.begin(player, TOUCH_COST, 12)) return false;
        long now = player.serverLevel().getGameTime();
        if (LegendaryCombat.isLegendary(victim)) {
            VoidMark mark = MARKS.get(victim.getUUID());
            if (mark != null && mark.ownerId.equals(player.getUUID()) && now < mark.endTick) {
                LegendaryCombat.damage(player, victim, 10000f, 1.0f,
                        LegendaryCombat.AttackKind.CONCEPTUAL);
                MARKS.remove(victim.getUUID());
            } else {
                LegendaryCombat.damage(player, victim, 10000f, 0.40f,
                        LegendaryCombat.AttackKind.CONCEPTUAL);
                MARKS.put(victim.getUUID(), new VoidMark(player.getUUID(), now + 20 * 20));
                victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
            }
        } else {
            eraseVictim(player.serverLevel(), player, victim);
        }
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 2,
                PowerEventPayload.PHASE_RELEASE, 12, 6);
        return true;
    }

    /** Compatibility entry point: the old erase action is now the specified Void Gaze. */
    public static void kill(ServerPlayer player) {
        gaze(player);
    }

    public static void gaze(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID()) || GAZES.containsKey(player.getUUID())
                || !LegendaryCombat.begin(player, ERASE_COST, 20)) return;
        GAZES.put(player.getUUID(), new PendingGaze(
                player.serverLevel().dimension(), player.getEyePosition(),
                player.getLookAngle().normalize(), player.serverLevel().getGameTime() + 20));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 3,
                PowerEventPayload.PHASE_PREPARE, 20, 80);
    }

    /** Compatibility entry point: the old wave slot now performs Void Grasp. */
    public static void wave(ServerPlayer player) {
        grasp(player);
    }

    public static void grasp(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())) return;
        LivingEntity target = findTarget(player.serverLevel(), player, 50);
        if (target == null || !LegendaryCombat.begin(player, WAVE_COST, 14)) return;
        target.setDeltaMovement(player.getLookAngle().normalize().scale(3.2).add(0, 0.7, 0));
        target.hurtMarked = true;
        LegendaryCombat.damage(player, target, 35f, 0.20f, LegendaryCombat.AttackKind.CONCEPTUAL);
        LegendaryCombat.stagger(target, 20, 1);
        PowerEventDispatcher.broadcastAt(player, target.position(), PowerEventPayload.POWER_VOID, 4,
                PowerEventPayload.PHASE_RELEASE, 16, 50);
    }

    public static void silence(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())
                || SILENCE_FIELDS.containsKey(player.getUUID())
                || !LegendaryCombat.begin(player, SILENCE_COST, 24)) return;
        long endTick = player.serverLevel().getGameTime() + SILENCE_DURATION;
        SILENCE_FIELDS.put(player.getUUID(), new SilenceField(
                player.serverLevel().dimension(), player.position(), endTick));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_VOID, 5,
                PowerEventPayload.PHASE_STATE_START, SILENCE_DURATION, 25);
    }

    public static void absoluteVoid(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID()) || FIELDS.containsKey(player.getUUID())
                || !LegendaryCombat.begin(player, ABSOLUTE_VOID_COST, ABSOLUTE_VOID_PREPARE)) return;
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
        if (!(victim instanceof ServerPlayer player) || !VoidServerState.isActive(player.getUUID())
                || LegendaryCombat.bypassesVoid(player)) return true;
        if (source.getDirectEntity() instanceof Projectile) return false;
        return !(source.getDirectEntity() instanceof LivingEntity && amount <= 6f);
    }

    public static boolean isSilenced(UUID entityId) {
        return SILENCED.containsKey(entityId);
    }

    public static void tick(MinecraftServer server) {
        MARKS.entrySet().removeIf(entry -> {
            Entity entity = findEntity(server, entry.getKey());
            return entity == null || entity.level().getGameTime() >= entry.getValue().endTick;
        });
        SILENCED.entrySet().removeIf(entry -> {
            Entity entity = findEntity(server, entry.getKey());
            return entity == null || entity.level().getGameTime() >= entry.getValue();
        });
        tickGazes(server);
        tickSilenceFields(server);
        tickAbsoluteVoid(server);
    }

    private static void tickGazes(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PendingGaze>> iterator = GAZES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingGaze> entry = iterator.next();
            PendingGaze gaze = entry.getValue();
            ServerLevel level = server.getLevel(gaze.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() < gaze.releaseTick) continue;
            Vec3 end = gaze.origin.add(gaze.direction.scale(80));
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(gaze.origin, end).inflate(1.5),
                    entity -> entity != owner && entity.isAlive()
                            && distanceToSegment(entity.getEyePosition(), gaze.origin, end) <= 1.5)) {
                if (!LegendaryCombat.isLegendary(target) && target.getMaxHealth() <= 20f) {
                    eraseVictim(level, owner, target);
                } else {
                    LegendaryCombat.damage(owner, target, 60f, 0.28f,
                            LegendaryCombat.AttackKind.CONCEPTUAL);
                    target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
                }
            }
            PowerEventDispatcher.broadcast(owner, PowerEventPayload.POWER_VOID, 3,
                    PowerEventPayload.PHASE_RELEASE, 16, 80);
            iterator.remove();
        }
    }

    private static void tickSilenceFields(MinecraftServer server) {
        Iterator<Map.Entry<UUID, SilenceField>> iterator = SILENCE_FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SilenceField> entry = iterator.next();
            SilenceField state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null) {
                iterator.remove();
                continue;
            }
            long now = level.getGameTime();
            AABB bounds = new AABB(state.center, state.center).inflate(25);
            if (now >= state.endTick) {
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                        entity -> entity != owner && entity.isAlive()
                                && entity.position().distanceToSqr(state.center) <= 25 * 25)) {
                    if (LegendaryCombat.isLegendary(target)) {
                        LegendaryCombat.damage(owner, target, 60f, 0.25f,
                                LegendaryCombat.AttackKind.CONCEPTUAL);
                    } else if (target.getMaxHealth() <= 24f) {
                        eraseVictim(level, owner, target);
                    }
                }
                PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_VOID, 5,
                        PowerEventPayload.PHASE_AFTERMATH, 40, 25);
                iterator.remove();
                continue;
            }
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                    entity -> entity != owner && entity.isAlive()
                            && entity.position().distanceToSqr(state.center) <= 25 * 25)) {
                SILENCED.put(target.getUUID(), state.endTick);
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, true, false));
                if (now % 20 == 0) {
                    LegendaryCombat.damage(owner, target, 6f, 0.035f,
                            LegendaryCombat.AttackKind.CONCEPTUAL);
                }
            }
        }
    }

    private static void tickAbsoluteVoid(MinecraftServer server) {
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
            if (now < state.prepareEnd) continue;
            if (!state.releaseSignalled) {
                state.releaseSignalled = true;
                level.playSound(null, state.center.x, state.center.y, state.center.z,
                        ModSounds.VOID_ABSOLUTE_AMBIENCE, SoundSource.AMBIENT, 3f, 1f);
                PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_VOID, 6,
                        PowerEventPayload.PHASE_RELEASE, ABSOLUTE_VOID_RELEASE,
                        (int) ABSOLUTE_VOID_RADIUS);
            }
            if (now < state.releaseEnd) {
                double progress = Math.min(1.0, (now - state.prepareEnd + 1) / 50.0);
                double radius = ABSOLUTE_VOID_RADIUS * progress;
                AABB bounds = new AABB(state.center, state.center).inflate(radius);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                        entity -> entity != owner && entity.isAlive()
                                && entity.position().distanceToSqr(state.center) <= radius * radius)) {
                    SILENCED.put(target.getUUID(), state.releaseEnd);
                    if (LegendaryCombat.isLegendary(target)) {
                        if (now % 20 == 0) {
                            LegendaryCombat.damage(owner, target, 60f, 0.065f,
                                    LegendaryCombat.AttackKind.CONCEPTUAL);
                            if (target instanceof ServerPlayer targetPlayer) {
                                PlayerEnergyManager.setEnergy(targetPlayer,
                                        Math.max(0, PlayerEnergyManager.getEnergy(targetPlayer) - 5));
                            }
                        }
                    } else if (now % 10 == 0) {
                        eraseVictim(level, owner, target);
                        break;
                    }
                }
                continue;
            }
            if (!state.aftermathSignalled) {
                state.aftermathSignalled = true;
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(state.center, state.center).inflate(ABSOLUTE_VOID_RADIUS),
                        entity -> entity != owner && LegendaryCombat.isLegendary(entity))) {
                    MARKS.put(target.getUUID(), new VoidMark(owner.getUUID(), now + 20 * 30));
                }
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

    private static void eraseVictim(ServerLevel level, ServerPlayer owner, LivingEntity victim) {
        if (!victim.isAlive()) return;
        BlockPos ground = victim.blockPosition();
        for (int i = 0; i < 6 && level.getBlockState(ground.below()).isAir(); i++) ground = ground.below();
        ShadowRemnantEntity shadow = new ShadowRemnantEntity(ModEntities.SHADOW_REMNANT, level);
        shadow.setPos(victim.getX(), ground.getY() + 0.05, victim.getZ());
        shadow.setSize(Mth.clamp(victim.getBbWidth() * 2.4f, 1.2f, 5f));
        shadow.setOwnerId(owner.getUUID());
        shadow.setVoidShadow(true);
        shadow.setYRot(victim.getYRot());
        level.addFreshEntity(shadow);
        level.sendParticles(ParticleTypes.SMOKE, victim.getX(),
                victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
                30, 0.25, victim.getBbHeight() / 3, 0.25, 0.02);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                ModSounds.VOID_KILL, SoundSource.PLAYERS, 2f, 1f);
        LegendaryCombat.damage(owner, victim, Float.MAX_VALUE, 1.0f,
                LegendaryCombat.AttackKind.CONCEPTUAL);
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
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1), entity -> entity != player && isValidTarget(entity));
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 0.0001) return point.distanceTo(start);
        double t = Math.max(0, Math.min(1, point.subtract(start).dot(segment) / lengthSquared));
        return point.distanceTo(start.add(segment.scale(t)));
    }

    public static void syncState(ServerPlayer player) {
        broadcastState(player, VoidServerState.isActive(player.getUUID()));
    }

    public static void sendState(ServerPlayer subject, ServerPlayer viewer, boolean active) {
        ServerPlayNetworking.send(viewer, new VoidStatePayload(subject.getId(), active));
    }

    private static boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator();
    }

    private static void broadcastState(ServerPlayer player, boolean active) {
        VoidStatePayload payload = new VoidStatePayload(player.getId(), active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) ServerPlayNetworking.send(viewer, payload);
    }

    public static int clearVoidShadows(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ShadowRemnantEntity shadow && shadow.isVoidShadow()) {
                shadow.discard();
                count++;
            }
        }
        return count;
    }

    public static void clear() {
        COMBOS.clear();
        MARKS.clear();
        SILENCED.clear();
        SILENCE_FIELDS.clear();
        GAZES.clear();
        FIELDS.clear();
    }

    private record VoidMark(UUID ownerId, long endTick) {
    }

    private record SilenceField(ResourceKey<Level> dimension, Vec3 center, long endTick) {
    }

    private record PendingGaze(
            ResourceKey<Level> dimension, Vec3 origin, Vec3 direction, long releaseTick) {
    }

    private static final class AbsoluteVoidState {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final long prepareEnd;
        private final long releaseEnd;
        private final long aftermathEnd;
        private boolean releaseSignalled;
        private boolean aftermathSignalled;

        private AbsoluteVoidState(ResourceKey<Level> dimension, Vec3 center,
                long prepareEnd, long releaseEnd, long aftermathEnd) {
            this.dimension = dimension;
            this.center = center;
            this.prepareEnd = prepareEnd;
            this.releaseEnd = releaseEnd;
            this.aftermathEnd = aftermathEnd;
        }
    }
}
