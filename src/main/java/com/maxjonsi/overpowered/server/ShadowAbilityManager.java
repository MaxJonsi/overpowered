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
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ShadowAbilityManager {
    public static final int STEP_COST = 8;
    public static final int AUTHORITY_COST = 20;
    public static final int EXCHANGE_COST = 12;
    public static final int EXTRACTION_COST = 18;
    public static final int SUMMON_COST = 12;
    public static final int KNIGHT_COST = 25;
    public static final int MAGE_COST = 30;
    public static final int MONARCH_FORM_COST = 45;
    public static final int DOMAIN_COST = 100;
    public static final int MONARCH_FORM_DURATION = -1;
    public static final int DOMAIN_PREPARE_TICKS = 40;
    public static final int DOMAIN_DURATION = 20 * 20;
    public static final int DOMAIN_RADIUS = 60;
    public static final int MAX_SOULS = 50;
    public static final int MAX_SOLDIERS = 8;
    private static final String SOLDIER_TAG = "overpowered.shadow_soldier";

    private static final List<SoulEcho> ECHOES = new ArrayList<>();
    private static final Map<UUID, Integer> SOULS = new HashMap<>();
    private static final Map<UUID, List<UUID>> SOLDIERS = new HashMap<>();
    private static final Map<UUID, Long> MONARCH_FORMS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_DOMAINS = new HashMap<>();
    private static final Map<UUID, DomainState> DOMAINS = new HashMap<>();
    private static final Map<UUID, long[]> COMBOS = new HashMap<>();

    private ShadowAbilityManager() {
    }

    public static boolean combo(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive()) return false;
        long now = player.serverLevel().getGameTime();
        long[] combo = COMBOS.computeIfAbsent(player.getUUID(), ignored ->
                new long[]{-1, Long.MIN_VALUE / 2});
        if (now - combo[1] < 4 || !LegendaryCombat.beginFree(player, 3)) return false;
        int stage = now - combo[1] <= 18 ? (int) ((combo[0] + 1) % 6) : 0;
        combo[0] = stage;
        combo[1] = now;
        Vec3 targetForward = target.getLookAngle().multiply(1, 0, 1);
        Vec3 fromTarget = player.position().subtract(target.position()).multiply(1, 0, 1);
        boolean behind = targetForward.lengthSqr() > 0.001 && fromTarget.lengthSqr() > 0.001
                && targetForward.normalize().dot(fromTarget.normalize()) < -0.45;
        float multiplier = behind ? 1.3f : 1f;
        LegendaryCombat.damage(player, target, (stage == 5 ? 16f : 6f) * multiplier,
                (stage == 5 ? 0.11f : 0.04f) * multiplier,
                LegendaryCombat.AttackKind.PHYSICAL);
        if (stage == 5) LegendaryCombat.stagger(target, 24, 1);
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_SHADOW, 0,
                PowerEventPayload.PHASE_RELEASE, 5, 5, stage);
        return true;
    }

    public static void shadowStep(ServerPlayer player) {
        Vec3 destination = targetedStepDestination(player);
        if (destination == null || !LegendaryCombat.begin(player, STEP_COST, 5)) return;

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
                PowerEventPayload.PHASE_RELEASE, 10, 30);
    }

    public static void rulersAuthority(ServerPlayer player) {
        LivingEntity target = findTarget(player, 50);
        if (target == null || !LegendaryCombat.begin(player, AUTHORITY_COST, 14)) return;
        Vec3 motion = player.getLookAngle().normalize().scale(3.0).add(0, 0.75, 0);
        target.setDeltaMovement(motion);
        target.hurtMarked = true;
        LegendaryCombat.damage(player, target, 35f, 0.20f, LegendaryCombat.AttackKind.ENERGY);
        PowerEventDispatcher.broadcastAt(player, target.position(), PowerEventPayload.POWER_SHADOW, 2,
                PowerEventPayload.PHASE_RELEASE, 16, 50);
    }

    public static void shadowExchange(ServerPlayer player) {
        Entity soldier = nearestSoldier(player);
        if (soldier == null) {
            player.displayClientMessage(Component.translatable("message.overpowered.shadow.no_soldier"), true);
            return;
        }
        if (!LegendaryCombat.begin(player, EXCHANGE_COST, 6)) return;

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
        if (!LegendaryCombat.begin(player, EXTRACTION_COST, 16)) return;

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
        SummonType type = player.isSprinting() && player.isShiftKeyDown()
                ? SummonType.MAGE : player.isShiftKeyDown() ? SummonType.KNIGHT : SummonType.SOLDIER;
        int typeLimit = type == SummonType.SOLDIER ? 5 : type == SummonType.KNIGHT ? 2 : 1;
        if (activeSoldierCount(player, type) >= typeLimit) return;
        int cost = switch (type) {
            case SOLDIER -> SUMMON_COST;
            case KNIGHT -> KNIGHT_COST;
            case MAGE -> MAGE_COST;
        };
        if (!LegendaryCombat.begin(player, cost, 18)) return;

        SOULS.put(player.getUUID(), souls - 1);
        spawnSoldier(player, player.position().add(player.getLookAngle().scale(2)), type, false);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SHADOW_ARISE, SoundSource.PLAYERS, 1.8f, 1f);
        PowerEventDispatcher.broadcastDetailed(player, PowerEventPayload.POWER_SHADOW, 4,
                PowerEventPayload.PHASE_RELEASE, 20, 12, souls - 1);
    }

    public static void toggleMonarchForm(ServerPlayer player) {
        if (MONARCH_FORMS.remove(player.getUUID()) != null) {
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 5,
                    PowerEventPayload.PHASE_STATE_END, 0, 12);
            return;
        }
        if (!LegendaryCombat.begin(player, MONARCH_FORM_COST, 30)) return;

        MONARCH_FORMS.put(player.getUUID(), Long.MAX_VALUE);
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        PowerEventDispatcher.broadcast(player, PowerEventPayload.POWER_SHADOW, 5,
                PowerEventPayload.PHASE_STATE_START, MONARCH_FORM_DURATION, 12);
    }

    public static void shadowDomain(ServerPlayer player) {
        if (PENDING_DOMAINS.containsKey(player.getUUID()) || DOMAINS.containsKey(player.getUUID())) return;
        if (!LegendaryCombat.begin(player, DOMAIN_COST, DOMAIN_PREPARE_TICKS)) return;

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
            spawnSoldier(owner, dead.position(), SummonType.KNIGHT, true);
            break;
        }
    }

    public static void tick(MinecraftServer server) {
        ECHOES.removeIf(echo -> {
            ServerLevel level = server.getLevel(echo.dimension);
            return level == null || level.getGameTime() >= echo.expiresAt;
        });
        tickPassive(server);
        tickSoldiers(server);
        tickMonarchForms(server);
        tickDomains(server);
    }

    private static void tickPassive(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.getMainHandItem().getItem() instanceof com.maxjonsi.overpowered.item.ShadowDaggerItem)
                    && !(player.getOffhandItem().getItem() instanceof com.maxjonsi.overpowered.item.ShadowDaggerItem)) {
                continue;
            }
            if (!player.serverLevel().isDay()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, 0, true, false));
                if (player.tickCount % 20 == 0) {
                    PlayerEnergyManager.setEnergy(player,
                            Math.min(PlayerEnergyManager.MAX_ENERGY,
                                    PlayerEnergyManager.getEnergy(player) + 1));
                }
            }
            if (player.tickCount % 100 == 0) recoverPersistentSoldiers(player);
        }
    }

    private static void tickSoldiers(MinecraftServer server) {
        Iterator<Map.Entry<UUID, List<UUID>>> owners = SOLDIERS.entrySet().iterator();
        while (owners.hasNext()) {
            Map.Entry<UUID, List<UUID>> entry = owners.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) continue;

            entry.getValue().removeIf(soldierId -> {
                Entity entity = findEntity(server, soldierId);
                if (!(entity instanceof Mob soldier) || !soldier.isAlive()) return true;
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
            boolean exhausted = player != null && player.tickCount % 10 == 0
                    && !PlayerEnergyManager.tryConsume(player, 1);
            if (player == null || exhausted) {
                if (player != null) {
                    if (!player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
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
            player.getAbilities().mayfly = true;
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
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SHADOW_PORTAL, SoundSource.MASTER, 4f, 0.7f);
            for (int i = 0; i < 3 && activeSoldierCount(player.getUUID()) < MAX_SOLDIERS; i++) {
                double angle = i * Math.PI * 2 / 3;
                spawnSoldier(player, player.position().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4),
                        SummonType.KNIGHT, true);
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
                    removeTemporarySoldiers(server, owner.getUUID());
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

    private static void spawnSoldier(ServerPlayer owner, Vec3 position, SummonType type, boolean temporary) {
        ServerLevel level = owner.serverLevel();
        Mob soldier = type == SummonType.MAGE
                ? new Skeleton(EntityType.SKELETON, level)
                : new WitherSkeleton(EntityType.WITHER_SKELETON, level);
        soldier.setPos(position.x, position.y, position.z);
        soldier.addTag(SOLDIER_TAG);
        soldier.addTag("overpowered.shadow_owner." + owner.getUUID());
        soldier.addTag("overpowered.shadow_type." + type.name().toLowerCase(java.util.Locale.ROOT));
        if (temporary) soldier.addTag("overpowered.shadow_temporary");
        soldier.setPersistenceRequired();
        soldier.setCanPickUpLoot(false);
        soldier.setCustomName(Component.literal("Shadow " + switch (type) {
            case SOLDIER -> "Soldier";
            case KNIGHT -> "Knight";
            case MAGE -> "Mage";
        }));
        if (type == SummonType.MAGE) soldier.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
        if (soldier.getAttribute(Attributes.MAX_HEALTH) != null) {
            double health = type == SummonType.KNIGHT ? 90 : type == SummonType.MAGE ? 50 : 45;
            soldier.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            soldier.setHealth((float) health);
        }
        if (soldier.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            soldier.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                    type == SummonType.KNIGHT ? 14 : type == SummonType.MAGE ? 7 : 9);
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
        Vec3 end = eye.add(player.getLookAngle().scale(30));
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

        for (int distance = 30; distance >= 2; distance--) {
            Vec3 candidate = player.position().add(player.getLookAngle().scale(distance));
            if (level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) {
                return candidate;
            }
        }
        return null;
    }

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult blockHit = player.serverLevel().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.serverLevel(), player, eye, end,
                new AABB(eye, end).inflate(1),
                entity -> entity instanceof LivingEntity && entity != player && entity.isAlive());
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void recoverPersistentSoldiers(ServerPlayer owner) {
        String ownerTag = "overpowered.shadow_owner." + owner.getUUID();
        List<UUID> tracked = SOLDIERS.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>());
        for (Entity entity : owner.serverLevel().getAllEntities()) {
            if (entity.getTags().contains(SOLDIER_TAG) && entity.getTags().contains(ownerTag)
                    && entity.isAlive() && !tracked.contains(entity.getUUID())) {
                tracked.add(entity.getUUID());
            }
        }
    }

    private static void removeTemporarySoldiers(MinecraftServer server, UUID ownerId) {
        List<UUID> tracked = SOLDIERS.get(ownerId);
        if (tracked == null) return;
        tracked.removeIf(id -> {
            Entity entity = findEntity(server, id);
            if (entity != null && entity.getTags().contains("overpowered.shadow_temporary")) {
                entity.discard();
                return true;
            }
            return false;
        });
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

    private static int activeSoldierCount(ServerPlayer owner, SummonType type) {
        String typeTag = "overpowered.shadow_type." + type.name().toLowerCase(java.util.Locale.ROOT);
        int count = 0;
        for (UUID id : SOLDIERS.getOrDefault(owner.getUUID(), List.of())) {
            Entity entity = owner.serverLevel().getEntity(id);
            if (entity != null && entity.isAlive() && entity.getTags().contains(typeTag)) count++;
        }
        return count;
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
        SOLDIERS.remove(playerId);
        COMBOS.remove(playerId);
    }

    public static void clear() {
        ECHOES.clear();
        SOULS.clear();
        SOLDIERS.clear();
        MONARCH_FORMS.clear();
        PENDING_DOMAINS.clear();
        DOMAINS.clear();
        COMBOS.clear();
    }

    private record SoulEcho(ResourceKey<Level> dimension, Vec3 position, float width, long expiresAt) {
    }

    private record DomainState(ResourceKey<Level> dimension, Vec3 center, long endTick) {
    }

    private enum SummonType {
        SOLDIER,
        KNIGHT,
        MAGE
    }
}
