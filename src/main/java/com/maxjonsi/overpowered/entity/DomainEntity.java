package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.LegendaryCombat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DomainEntity extends EffectEntity {
    public static final double RADIUS = 25;
    private static final int LIFETIME = 240;
    private static final Map<UUID, DomainEntity> ACTIVE = new ConcurrentHashMap<>();

    private final Map<UUID, Vec3> frozenPositions = new HashMap<>();

    public DomainEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Nullable
    public static DomainEntity getActive(UUID ownerId) {
        DomainEntity domain = ACTIVE.get(ownerId);
        return domain != null && domain.isAlive() ? domain : null;
    }

    public boolean isInside(Entity entity) {
        return entity.position().distanceToSqr(position()) <= RADIUS * RADIUS;
    }

    public static boolean isTrapped(Entity entity) {
        return ACTIVE.values().stream().anyMatch(domain -> domain.isAlive()
                && domain.ownerId != null && !domain.ownerId.equals(entity.getUUID())
                && domain.level() == entity.level() && domain.isInside(entity));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        if (ownerId != null && tickCount == 1) {
            ACTIVE.put(ownerId, this);
        }

        Vec3 center = position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(RADIUS), e -> isVictim(e) && isInside(e))) {
            Vec3 lockPos = frozenPositions.computeIfAbsent(target.getUUID(), k -> target.position());
            if (target.position().distanceToSqr(lockPos) > 0.02) {
                if (target instanceof ServerPlayer player) {
                    player.teleportTo(lockPos.x, lockPos.y, lockPos.z);
                } else {
                    target.setPos(lockPos.x, lockPos.y, lockPos.z);
                }
            }
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 10, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, true, false));
            target.fallDistance = 0;
            if (tickCount % 20 == 0) {
                Player owner = getOwnerPlayer();
                LegendaryCombat.damage(target,
                        owner != null ? level.damageSources().indirectMagic(owner, owner)
                                : level.damageSources().magic(),
                        5f, 0.025f, LegendaryCombat.AttackKind.CONCEPTUAL);
            }
        }

        if (tickCount >= LIFETIME || (tickCount % 20 == 0 && getOwnerPlayer() == null)) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MAGIC_FORCEFIELD, SoundSource.MASTER, 4f, 0.6f);
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (ownerId != null) ACTIVE.remove(ownerId, this);
        super.remove(reason);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }
}
