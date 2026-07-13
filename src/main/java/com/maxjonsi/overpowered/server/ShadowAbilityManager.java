package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.ShadowRemnantEntity;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ShadowAbilityManager {
    public static final int STEP_COST = 10;
    public static final int EXCHANGE_COST = 15;
    public static final int EXTRACTION_COST = 8;
    public static final int SUMMON_COST = 12;
    public static final int MONARCH_FORM_COST = 35;
    public static final int DOMAIN_COST = 90;
    public static final int MONARCH_FORM_DURATION = 20 * 30;
    public static final int DOMAIN_PREPARE_TICKS = 40;
    public static final int DOMAIN_DURATION = 20 * 25;
    public static final int DOMAIN_RADIUS = 36;
    public static final int MAX_SOULS = 50;
    public static final int MAX_SOLDIERS = 12;
    private static final String SOLDIER_TAG = "overpowered.shadow_soldier";

    private static final List<SoulEcho> ECHOES = new ArrayList<>();
    private static final Map<UUID, Integer> SOULS = new HashMap<>();
    private static final Map<UUID, List<UUID>> SOLDIERS = new HashMap<>();
    private static final Map<UUID, Long> MONARCH_FORMS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_DOMAINS = new HashMap<>();
    private static final Map<UUID, DomainState> DOMAINS = new HashMap<>();

    private ShadowAbilityManager() {
    }

    public static void shadowStep(ServerPlayer player) {
        Vec3 destination = targetedStepDestination(player);
        if (destination == null || !PlayerEnergyManager.tryConsumeOrNotify(player, STEP_COST)) return;

        Vec3 origin = player.position();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.serverLevel().playSound(null, destination.x, destination.y, destination.z,
                ModSounds.SHADOW_PORTAL, SoundSource.PLAYERS, 1.4f, 1f);
        player.serverLevel().sendParticles(
                new DustParticleOptions(new Vector3f(0.2f, 0.03f, 0.35f), 1.4f),
                origin.x, origin.y + 1, origin.z, 24, 0.45, 0.8, 0.45, 0.03);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 1,
                PowerEventPayload.PHASE_RELEASE, 8, 24);
    }

    public static void shadowExchange(ServerPlayer player) {
        Entity soldier = nearestSoldier(player);
        if (soldier == null) {
            player.displayClientMessage(Component.translatable("message.overpowered.shadow.no_soldier"), true);
            return;
        }
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, EXCHANGE_COST)) return;

        Vec3 playerPosition = player.position();
        Vec3 soldierPosition = soldier.position();
        player.teleportTo(soldierPosition.x, soldierPosition.y, soldierPosition.z);
        soldier.setPos(playerPosition.x, playerPosition.y, playerPosition.z);
        player.setDeltaMovement(Vec3.ZERO);
        soldier.setDeltaMovement(Vec3.ZERO);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SHADOW_PORTAL, SoundSource.PLAYERS, 1.4f, 0.9f);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 2,
                PowerEventPayload.PHASE_RELEASE, 10, 32);
    }

    public static void extract(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        SoulEcho echo = ECHOES.stream()
                .filter(candidate -> candidate.dimension.equals(player.serverLevel().dimension()))
                .filter(candidate -> candidate.expiresAt > now)
                .filter(candidate -> candidate.position.distanceToSqr(player.position()) <= 16 * 16)
                .min(Comparator.comparingDouble(candidate -> candidate.position.distanceToSqr(player.position())))
                .orElse(null);
        if (echo == null) {
            player.displayClientMessage(Component.translatable("message.overpowered.shadow.no_soul"), true);
            return;
        }
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, EXTRACTION_COST)) return;

        ECHOES.remove(echo);
        int souls = Math.min(MAX_SOULS, SOULS.getOrDefault(player.getUUID(), 0) + 1);
        SOULS.put(player.getUUID(), souls);
        spawnRemnant(player.serverLevel(), echo.position, echo.width);
        player.serverLevel().playSound(null, echo.position.x, echo.position.y, echo.position.z,
                ModSounds.SHADOW_ARISE, SoundSource.PLAYERS, 1.8f, 1f);
        player.displayClientMessage(Component.translatable("message.overpowered.shadow.souls", souls), true);
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_SHADOW, 3,
                PowerEventPayload.PHASE_RELEASE, 20, 16, souls);
    }

    public static void summon(ServerPlayer player) {
        int souls = SOULS.getOrDefault(player.getUUID(), 0);
        if (souls <= 0) {
            player.displayClientMessage(Component.translatable("message.overpowered.shadow.no_soul"), true);
            return;
        }
        if (activeSoldierCount(player.getUUID()) >= MAX_SOLDIERS) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, SUMMON_COST)) return;

        SOULS.put(player.getUUID(), souls - 1);
        spawnSoldier(player, player.position().add(player.getLookAngle().scale(2)), false);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SHADOW_ARISE, SoundSource.PLAYERS, 1.8f, 1f);
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_SHADOW, 4,
                PowerEventPayload.PHASE_RELEASE, 20, 12, souls - 1);
    }

    public static void toggleMonarchForm(ServerPlayer player) {
        if (MONARCH_FORMS.remove(player.getUUID()) != null) {
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 5,
                    PowerEventPayload.PHASE_STATE_END, 0, 12);
            return;
        }
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, MONARCH_FORM_COST)) return;

        MONARCH_FORMS.put(player.getUUID(), player.serverLevel().getGameTime() + MONARCH_FORM_DURATION);
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 5,
                PowerEventPayload.PHASE_STATE_START, MONARCH_FORM_DURATION, 12);
    }

    public static void shadowDomain(ServerPlayer player) {
        if (PENDING_DOMAINS.containsKey(player.getUUID()) || DOMAINS.containsKey(player.getUUID())) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, DOMAIN_COST)) return;

        PENDING_DOMAINS.put(player.getUUID(), player.serverLevel().getGameTime() + DOMAIN_PREPARE_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                DOMAIN_PREPARE_TICKS, 10, false, false));
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 6,
                PowerEventPayload.PHASE_PREPARE, DOMAIN_PREPARE_TICKS, DOMAIN_RADIUS);
    }

    public static void onDeath(LivingEntity dead, DamageSource source) {
        if (dead.getTags().contains(SOLDIER_TAG) || !(dead.level() instanceof ServerLevel level)) return;

        SoulEcho echo = new SoulEcho(level.dimension(), dead.position(), dead.getBbWidth(),
                level.getGameTime() + 20 * 30);
        ECHOES.add(echo);

        for (Map.Entry<UUID, DomainState> entry : DOMAINS.entrySet()) {
            DomainState domain = entry.getValue();
            if (!domain.dimension.equals(level.dimension())
                    || dead.position().distanceToSqr(domain.center) > DOMAIN_RADIUS * DOMAIN_RADIUS) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner == null || activeSoldierCount(owner.getUUID()) >= MAX_SOLDIERS) continue;
            ECHOES.remove(echo);
            spawnRemnant(level, dead.position(), dead.getBbWidth());
            spawnSoldier(owner, dead.position(), true);
            break;
        }
    }

    public static void tick(MinecraftServer server) {
        ECHOES.removeIf(echo -> {
            ServerLevel level = server.getLevel(echo.dimension);
            return level == null || level.getGameTime() >= echo.expiresAt;
        });
        tickSoldiers(server);
        tickMonarchForms(server);
        tickDomains(server);
    }

    private static void tickSoldiers(MinecraftServer server) {
        Iterator<Map.Entry<UUID, List<UUID>>> owners = SOLDIERS.entrySet().iterator();
        while (owners.hasNext()) {
            Map.Entry<UUID, List<UUID>> entry = owners.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) continue;

            entry.getValue().removeIf(soldierId -> {
                Entity entity = findEntity(server, soldierId);
                if (!(entity instanceof WitherSkeleton soldier) || !soldier.isAlive()) return true;
                if (!soldier.level().dimension().equals(owner.level().dimension())) return false;

                if (soldier.distanceToSqr(owner) > 40 * 40) {
                    Vec3 position = owner.position().add(owner.getLookAngle().scale(-2));
                    soldier.teleportTo(position.x, position.y, position.z);
                }
                LivingEntity preferred = owner.getLastHurtMob();
                if (preferred == null || !preferred.isAlive()) preferred = owner.getLastHurtByMob();
                if (preferred != null && preferred.isAlive() && preferred != owner) {
                    soldier.setTarget(preferred);
                } else if (soldier.getTarget() == null || !soldier.getTarget().isAlive()) {
                    Monster nearest = owner.serverLevel().getNearestEntity(
                            owner.serverLevel().getEntitiesOfClass(Monster.class,
                                    soldier.getBoundingBox().inflate(24),
                                    mob -> mob.isAlive() && !mob.getTags().contains(SOLDIER_TAG)),
                            net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat(),
                            soldier, soldier.getX(), soldier.getY(), soldier.getZ());
                    soldier.setTarget(nearest);
                }
                return false;
            });
            if (entry.getValue().isEmpty() && owner == null) owners.remove();
        }
    }

    private static void tickMonarchForms(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> iterator = MONARCH_FORMS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.serverLevel().getGameTime() >= entry.getValue()) {
                if (player != null) {
                    PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 5,
                            PowerEventPayload.PHASE_STATE_END, 0, 12);
                }
                iterator.remove();
                continue;
            }

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, 2, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 2, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
            if (player.tickCount % 4 == 0) {
                player.serverLevel().sendParticles(
                        new DustParticleOptions(new Vector3f(0.25f, 0.04f, 0.45f), 1.4f),
                        player.getX(), player.getY() + 1, player.getZ(), 5, 0.5, 0.9, 0.5, 0.025);
            }
        }
    }

    private static void tickDomains(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> pending = PENDING_DOMAINS.entrySet().iterator();
        while (pending.hasNext()) {
            Map.Entry<UUID, Long> entry = pending.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                pending.remove();
                continue;
            }
            long now = player.serverLevel().getGameTime();
            if (now < entry.getValue()) continue;

            DOMAINS.put(player.getUUID(), new DomainState(
                    player.serverLevel().dimension(), player.position(), now + DOMAIN_DURATION));
            for (int i = 0; i < 3 && activeSoldierCount(player.getUUID()) < MAX_SOLDIERS; i++) {
                double angle = i * Math.PI * 2 / 3;
                spawnSoldier(player, player.position().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4), true);
            }
            PowerEventDispatcher.broadcastAt(player, player.position(), PowerEventPayload.POWER_SHADOW, 6,
                    PowerEventPayload.PHASE_RELEASE, DOMAIN_DURATION, DOMAIN_RADIUS);
            pending.remove();
        }

        Iterator<Map.Entry<UUID, DomainState>> active = DOMAINS.entrySet().iterator();
        while (active.hasNext()) {
            Map.Entry<UUID, DomainState> entry = active.next();
            DomainState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (level == null || owner == null || level.getGameTime() >= state.endTick) {
                if (owner != null) {
                    PowerEventDispatcher.broadcastAt(owner, state.center, PowerEventPayload.POWER_SHADOW, 6,
                            PowerEventPayload.PHASE_AFTERMATH, 80, DOMAIN_RADIUS);
                }
                active.remove();
                continue;
            }

            if (level.getGameTime() % 20 == 0) {
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(state.center, state.center).inflate(DOMAIN_RADIUS),
                        entity -> entity != owner && entity.isAlive()
                                && !entity.getTags().contains(SOLDIER_TAG))) {
                    target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, true, false));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, false));
                }
            }
        }
    }

    private static void spawnSoldier(ServerPlayer owner, Vec3 position, boolean elite) {
        ServerLevel level = owner.serverLevel();
        WitherSkeleton soldier = new WitherSkeleton(EntityType.WITHER_SKELETON, level);
        soldier.setPos(position.x, position.y, position.z);
        soldier.addTag(SOLDIER_TAG);
        soldier.addTag("overpowered.shadow_owner." + owner.getUUID());
        soldier.setPersistenceRequired();
        soldier.setCanPickUpLoot(false);
        soldier.setCustomName(Component.translatable(elite
                ? "entity.overpowered.elite_shadow_soldier"
                : "entity.overpowered.shadow_soldier"));
        if (soldier.getAttribute(Attributes.MAX_HEALTH) != null) {
            soldier.getAttribute(Attributes.MAX_HEALTH).setBaseValue(elite ? 80 : 45);
            soldier.setHealth(elite ? 80 : 45);
        }
        if (soldier.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            soldier.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(elite ? 14 : 9);
        }
        soldier.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false));
        if (level.addFreshEntity(soldier)) {
            SOLDIERS.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>()).add(soldier.getUUID());
        }
    }

    private static void spawnRemnant(ServerLevel level, Vec3 position, float width) {
        ShadowRemnantEntity remnant = new ShadowRemnantEntity(ModEntities.SHADOW_REMNANT, level);
        remnant.setPos(position.x, position.y + 0.05, position.z);
        remnant.setSize(Mth.clamp(width * 2.4f, 1.2f, 5f));
        level.addFreshEntity(remnant);
    }

    private static Vec3 targetedStepDestination(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(24));
        HitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 clippedEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, clippedEnd,
                new AABB(eye, clippedEnd).inflate(1),
                entity -> entity instanceof LivingEntity && entity != player && entity.isAlive());
        if (entityHit != null) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            Vec3 behind = target.position().subtract(target.getLookAngle().scale(1.5));
            if (level.noCollision(player, player.getBoundingBox().move(behind.subtract(player.position())))) {
                return behind;
            }
        }

        for (int distance = 20; distance >= 2; distance--) {
            Vec3 candidate = player.position().add(player.getLookAngle().scale(distance));
            if (level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) {
                return candidate;
            }
        }
        return null;
    }

    private static Entity nearestSoldier(ServerPlayer owner) {
        return SOLDIERS.getOrDefault(owner.getUUID(), List.of()).stream()
                .map(id -> owner.serverLevel().getEntity(id))
                .filter(entity -> entity != null && entity.isAlive())
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
    }

    private static int activeSoldierCount(UUID ownerId) {
        return SOLDIERS.getOrDefault(ownerId, List.of()).size();
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    public static void clearPlayer(MinecraftServer server, UUID playerId) {
        MONARCH_FORMS.remove(playerId);
        PENDING_DOMAINS.remove(playerId);
        DOMAINS.remove(playerId);
        List<UUID> soldiers = SOLDIERS.remove(playerId);
        if (soldiers != null) {
            for (UUID soldierId : soldiers) {
                Entity soldier = findEntity(server, soldierId);
                if (soldier != null) soldier.discard();
            }
        }
    }

    public static void clear() {
        ECHOES.clear();
        SOULS.clear();
        SOLDIERS.clear();
        MONARCH_FORMS.clear();
        PENDING_DOMAINS.clear();
        DOMAINS.clear();
    }

    private record SoulEcho(ResourceKey<Level> dimension, Vec3 position, float width, long expiresAt) {
    }

    private record DomainState(ResourceKey<Level> dimension, Vec3 center, long endTick) {
    }
}
