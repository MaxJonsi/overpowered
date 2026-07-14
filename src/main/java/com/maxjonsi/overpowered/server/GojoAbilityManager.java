package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.item.GojoMaskItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class GojoAbilityManager {
    public static final int INFINITY_TOGGLE_COST = 0;
    public static final int INFINITY_FOCUS_COST = 25;
    public static final int MAXIMUM_BLUE_COST = 45;
    public static final int TELEPORT_COST = 8;
    public static final int COMBO_COST = 0;
    public static final int TELEPORT_RANGE = 40;
    public static final int FOCUS_DURATION = 20 * 10;

    private static final Set<UUID> INFINITY_USERS = new HashSet<>();
    private static final Set<UUID> INFINITY_DISABLED = new HashSet<>();
    private static final Map<UUID, Long> FOCUS_ENDS = new HashMap<>();
    private static final Map<UUID, long[]> COMBOS = new HashMap<>();
    private static final Map<UUID, SuspendedProjectile> SUSPENDED = new HashMap<>();
    private static final Map<UUID, MaximumBlueState> MAXIMUM_BLUES = new HashMap<>();

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
            resumeFor(player.serverLevel().getServer(), player.getUUID());
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                    PowerEventPayload.PHASE_STATE_END, 0, 5);
            return;
        }
        INFINITY_DISABLED.remove(player.getUUID());
        INFINITY_USERS.add(player.getUUID());
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                PowerEventPayload.PHASE_STATE_START, -1, 5);
    }

    public static void infinityFocus(ServerPlayer player) {
        if (!hasGojoLoadout(player) || !LegendaryCombat.begin(player, INFINITY_FOCUS_COST, 12)) return;
        INFINITY_DISABLED.remove(player.getUUID());
        INFINITY_USERS.add(player.getUUID());
        FOCUS_ENDS.put(player.getUUID(), player.serverLevel().getGameTime() + FOCUS_DURATION);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 6,
                PowerEventPayload.PHASE_STATE_START, FOCUS_DURATION, 8);
    }

    public static void teleport(ServerPlayer player) {
        if (!hasGojoLoadout(player)) return;
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
        if (destination == null || !LegendaryCombat.begin(player, TELEPORT_COST, 4)) return;
        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        level.sendParticles(new DustParticleOptions(new Vector3f(0.72f, 0.9f, 1f), 1.5f),
                start.x, start.y + 1, start.z, 18, 0.2, 0.7, 0.2, 0.02);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.72f, 0.9f, 1f), 1.5f),
                destination.x, destination.y + 1, destination.z, 18, 0.2, 0.7, 0.2, 0.02);
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 4,
                PowerEventPayload.PHASE_RELEASE, 8, TELEPORT_RANGE);
    }

    public static boolean combo(ServerPlayer player, LivingEntity target) {
        if (!hasGojoLoadout(player) || target == player || !target.isAlive()) return false;
        long now = player.serverLevel().getGameTime();
        long[] combo = COMBOS.computeIfAbsent(player.getUUID(), ignored ->
                new long[]{-1, Long.MIN_VALUE / 2});
        if (now - combo[1] < 5 || !LegendaryCombat.beginFree(player, 4)) return false;
        int stage = now - combo[1] <= 18 ? (int) ((combo[0] + 1) % 4) : 0;
        combo[0] = stage;
        combo[1] = now;
        float[] damage = {7f, 8f, 9f, 16f};
        float[] percent = {0.05f, 0.06f, 0.07f, 0.12f};
        LegendaryCombat.damage(player, target, damage[stage], percent[stage],
                LegendaryCombat.AttackKind.PHYSICAL);
        if (stage == 2) {
            Vec3 pull = player.position().subtract(target.position());
            if (pull.lengthSqr() > 0.001) target.setDeltaMovement(pull.normalize().scale(0.55));
        } else if (stage == 3) {
            Vec3 push = target.position().subtract(player.position());
            if (push.lengthSqr() > 0.001) {
                target.setDeltaMovement(push.normalize().scale(1.25).add(0, 0.35, 0));
                target.hurtMarked = true;
            }
            LegendaryCombat.stagger(target, 24, 1);
        }
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_GOJO, 0,
                PowerEventPayload.PHASE_RELEASE, 6, 5, stage);
        return true;
    }

    public static void maximumBlue(ServerPlayer player) {
        if (!hasGojoLoadout(player) || MAXIMUM_BLUES.containsKey(player.getUUID())
                || !LegendaryCombat.begin(player, MAXIMUM_BLUE_COST, 20)) return;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(60));
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 center = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        MAXIMUM_BLUES.put(player.getUUID(), new MaximumBlueState(
                player.serverLevel().dimension(), center, player.serverLevel().getGameTime() + 100));
        player.serverLevel().playSound(null, center.x, center.y, center.z,
                ModSounds.GOJO_BLUE, SoundSource.PLAYERS, 3f, 0.65f);
        PowerEventDispatcher.broadcastAt(player, center, PowerEventPayload.POWER_GOJO, 7,
                PowerEventPayload.PHASE_STATE_START, 100, 40);
    }

    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer player)
                || !INFINITY_USERS.contains(player.getUUID())
                || !hasGojoLoadout(player)
                || LegendaryCombat.bypassesInfinity(player)) return true;
        Entity threat = source.getDirectEntity();
        if (threat == null) return true;
        if (threat instanceof Projectile projectile) suspendProjectile(player, projectile);
        else {
            threat.setDeltaMovement(Vec3.ZERO);
            threat.hurtMarked = true;
        }
        player.serverLevel().sendParticles(
                new DustParticleOptions(new Vector3f(0.6f, 0.86f, 1f), 1.25f),
                player.getX(), player.getY() + 1, player.getZ(), 12, 0.7, 1.0, 0.7, 0.02);
        return false;
    }

    private static void suspendProjectile(ServerPlayer defender, Projectile projectile) {
        if (projectile.getOwner() == defender || SUSPENDED.containsKey(projectile.getUUID())) return;
        SUSPENDED.put(projectile.getUUID(), new SuspendedProjectile(
                defender.getUUID(), defender.serverLevel().dimension(), projectile.position(),
                projectile.getDeltaMovement(), projectile.isNoGravity()));
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.setNoGravity(true);
        projectile.hurtMarked = true;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            if (!hasGojoLoadout(player)) {
                INFINITY_DISABLED.remove(playerId);
                FOCUS_ENDS.remove(playerId);
                if (INFINITY_USERS.remove(playerId)) {
                    resumeFor(server, playerId);
                    PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                            PowerEventPayload.PHASE_STATE_END, 0, 5);
                }
                continue;
            }
            if (!INFINITY_DISABLED.contains(playerId) && INFINITY_USERS.add(playerId)) {
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                        PowerEventPayload.PHASE_STATE_START, -1, 5);
            }
            if (INFINITY_USERS.contains(playerId) && player.tickCount % 40 == 0
                    && !PlayerEnergyManager.tryConsume(player, 1)) {
                INFINITY_USERS.remove(playerId);
                INFINITY_DISABLED.add(playerId);
                resumeFor(server, playerId);
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 3,
                        PowerEventPayload.PHASE_STATE_END, 0, 5);
                continue;
            }
            Long focusEnd = FOCUS_ENDS.get(playerId);
            if (focusEnd != null && player.serverLevel().getGameTime() >= focusEnd) {
                FOCUS_ENDS.remove(playerId);
                PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_GOJO, 6,
                        PowerEventPayload.PHASE_STATE_END, 0, 8);
            }
            boolean focused = FOCUS_ENDS.containsKey(playerId);
            if (focused) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 3, true, false));
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 1, 1));
            }
            if (INFINITY_USERS.contains(playerId)) {
                double radius = focused ? 7.0 : 4.0;
                for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class,
                        player.getBoundingBox().inflate(radius),
                        candidate -> candidate.isAlive() && candidate.getOwner() != player)) {
                    Vec3 toward = player.position().subtract(projectile.position());
                    if (toward.lengthSqr() > 0.001
                            && projectile.getDeltaMovement().dot(toward.normalize()) > 0.05) {
                        suspendProjectile(player, projectile);
                    }
                }
            }
        }
        tickSuspended(server);
        tickMaximumBlues(server);
        INFINITY_USERS.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        INFINITY_DISABLED.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static void tickSuspended(MinecraftServer server) {
        Iterator<Map.Entry<UUID, SuspendedProjectile>> iterator = SUSPENDED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SuspendedProjectile> entry = iterator.next();
            SuspendedProjectile state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            ServerPlayer defender = server.getPlayerList().getPlayer(state.defenderId);
            if (!(entity instanceof Projectile projectile) || !projectile.isAlive()) {
                iterator.remove();
                continue;
            }
            boolean hold = defender != null && INFINITY_USERS.contains(defender.getUUID())
                    && defender.level().dimension().equals(state.dimension)
                    && defender.position().distanceToSqr(projectile.position()) <= 9 * 9;
            if (!hold) {
                projectile.setDeltaMovement(state.motion);
                projectile.setNoGravity(state.noGravity);
                projectile.hurtMarked = true;
                iterator.remove();
                continue;
            }
            projectile.setPos(state.position.x, state.position.y, state.position.z);
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.setNoGravity(true);
            projectile.hurtMarked = true;
        }
    }

    private static void tickMaximumBlues(MinecraftServer server) {
        Iterator<Map.Entry<UUID, MaximumBlueState>> iterator = MAXIMUM_BLUES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MaximumBlueState> entry = iterator.next();
            MaximumBlueState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null || level.getGameTime() >= state.endTick) {
                if (owner != null) {
                    PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_GOJO, 7,
                            PowerEventPayload.PHASE_AFTERMATH, 40, 40);
                }
                iterator.remove();
                continue;
            }
            AABB bounds = new AABB(state.center, state.center).inflate(40);
            for (Entity entity : level.getEntities(owner, bounds, Entity::isAlive)) {
                Vec3 pull = state.center.subtract(entity.position().add(0, entity.getBbHeight() / 2, 0));
                if (pull.lengthSqr() < 0.01) continue;
                entity.setDeltaMovement(entity.getDeltaMovement().scale(0.65)
                        .add(pull.normalize().scale(0.65)));
                entity.hurtMarked = true;
            }
            if (level.getGameTime() % 20 == 0) {
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                        candidate -> candidate != owner && candidate.isAlive())) {
                    double distance = target.position().distanceTo(state.center);
                    if (distance <= 40) {
                        float scale = (float) Math.max(0.25, 1.0 - distance / 48.0);
                        LegendaryCombat.damage(owner, target, 14f * scale, 0.076f * scale,
                                LegendaryCombat.AttackKind.ENERGY);
                    }
                }
            }
            level.sendParticles(new DustParticleOptions(new Vector3f(0.08f, 0.42f, 1f), 2.5f),
                    state.center.x, state.center.y, state.center.z, 20, 5, 5, 5, 0.05);
        }
    }

    private static void resumeFor(MinecraftServer server, UUID defenderId) {
        for (Map.Entry<UUID, SuspendedProjectile> entry : SUSPENDED.entrySet()) {
            SuspendedProjectile state = entry.getValue();
            if (!state.defenderId.equals(defenderId)) continue;
            ServerLevel level = server.getLevel(state.dimension);
            if (level != null && level.getEntity(entry.getKey()) instanceof Projectile projectile) {
                projectile.setDeltaMovement(state.motion);
                projectile.setNoGravity(state.noGravity);
                projectile.hurtMarked = true;
            }
        }
        SUSPENDED.entrySet().removeIf(entry -> entry.getValue().defenderId.equals(defenderId));
    }

    public static void clearPlayer(UUID playerId) {
        INFINITY_USERS.remove(playerId);
        INFINITY_DISABLED.remove(playerId);
        FOCUS_ENDS.remove(playerId);
        COMBOS.remove(playerId);
        MAXIMUM_BLUES.remove(playerId);
        SUSPENDED.entrySet().removeIf(entry -> entry.getValue().defenderId.equals(playerId));
    }

    public static void clear() {
        INFINITY_USERS.clear();
        INFINITY_DISABLED.clear();
        FOCUS_ENDS.clear();
        COMBOS.clear();
        SUSPENDED.clear();
        MAXIMUM_BLUES.clear();
    }

    private record SuspendedProjectile(
            UUID defenderId, ResourceKey<Level> dimension, Vec3 position, Vec3 motion, boolean noGravity) {
    }

    private record MaximumBlueState(ResourceKey<Level> dimension, Vec3 center, long endTick) {
    }
}
