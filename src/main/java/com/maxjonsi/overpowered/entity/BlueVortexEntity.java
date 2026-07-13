package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BlueVortexEntity extends EffectEntity {
    private static final double PULL_RADIUS = 9;
    private static final int WARMUP_TICKS = 10;
    private static final int ACTIVE_TICKS = 50;
    private static final DustParticleOptions BLUE_DUST =
            new DustParticleOptions(new Vector3f(0.15f, 0.55f, 1f), 1.8f);

    public BlueVortexEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        if (tickCount <= WARMUP_TICKS) {
            chargeAtOwner(level);
            if (tickCount == WARMUP_TICKS) {
                releaseCharge(level);
            }
            return;
        }

        Vec3 center = position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(PULL_RADIUS), this::isVictim)) {
            Vec3 pull = center.subtract(target.position().add(0, target.getBbHeight() / 2, 0));
            double dist = pull.length();
            if (dist < 0.1) continue;
            target.setDeltaMovement(target.getDeltaMovement().scale(0.85).add(pull.normalize().scale(0.45)));
            target.hurtMarked = true;
            target.fallDistance = 0;
        }

        int activeTick = tickCount - WARMUP_TICKS;
        if (activeTick % 10 == 0) {
            Player owner = getOwnerPlayer();
            DamageSource source = owner != null
                    ? level.damageSources().indirectMagic(owner, owner)
                    : level.damageSources().magic();
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(center, center).inflate(1.8), this::isVictim)) {
                target.hurt(source, 7f);
            }
        }

        double swirl = activeTick * 0.35;
        for (int i = 0; i < 6; i++) {
            double angle = swirl + i * Math.PI / 3;
            double r = 1.4 + 0.5 * Math.sin(activeTick * 0.2 + i);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(angle) * r, getY() + Math.sin(activeTick * 0.15 + i) * 0.8, getZ() + Math.sin(angle) * r,
                    1, 0, 0, 0, 0);
        }
        level.sendParticles(BLUE_DUST, getX(), getY(), getZ(), 4, 0.4, 0.4, 0.4, 0);

        if (activeTick >= ACTIVE_TICKS) discard();
    }

    private void chargeAtOwner(ServerLevel level) {
        Player owner = getOwnerPlayer();
        if (owner == null) return;

        Vec3 look = owner.getLookAngle().normalize();
        Vec3 center = owner.getEyePosition().add(look.scale(0.8)).add(0, -0.45, 0);
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() > 0.001) side = side.normalize().scale(0.32);

        float scale = 0.7f + tickCount / (float) WARMUP_TICKS;
        DustParticleOptions growingBlue = new DustParticleOptions(new Vector3f(0.1f, 0.6f, 1f), scale);
        level.sendParticles(growingBlue, center.x, center.y, center.z, 7,
                0.08 + tickCount * 0.006, 0.08 + tickCount * 0.006, 0.08 + tickCount * 0.006, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                center.x + side.x, center.y + side.y, center.z + side.z, 1, 0.02, 0.02, 0.02, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                center.x - side.x, center.y - side.y, center.z - side.z, 1, 0.02, 0.02, 0.02, 0);
    }

    private void releaseCharge(ServerLevel level) {
        Player owner = getOwnerPlayer();
        if (owner == null) return;

        Vec3 start = owner.getEyePosition().add(owner.getLookAngle().scale(0.8)).add(0, -0.45, 0);
        Vec3 travel = position().subtract(start);
        for (int i = 0; i <= 16; i++) {
            Vec3 point = start.add(travel.scale(i / 16.0));
            level.sendParticles(BLUE_DUST, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0);
        }
        level.playSound(null, start.x, start.y, start.z, ModSounds.GOJO_BLUE, SoundSource.PLAYERS, 2f, 1f);
    }
}
