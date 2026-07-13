package com.maxjonsi.overpowered.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

    public BlueVortexEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

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

        if (tickCount % 10 == 0) {
            Player owner = getOwnerPlayer();
            DamageSource source = owner != null
                    ? level.damageSources().indirectMagic(owner, owner)
                    : level.damageSources().magic();
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(center, center).inflate(1.8), this::isVictim)) {
                target.hurt(source, 7f);
            }
        }

        double swirl = tickCount * 0.35;
        for (int i = 0; i < 6; i++) {
            double angle = swirl + i * Math.PI / 3;
            double r = 1.4 + 0.5 * Math.sin(tickCount * 0.2 + i);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(angle) * r, getY() + Math.sin(tickCount * 0.15 + i) * 0.8, getZ() + Math.sin(angle) * r,
                    1, 0, 0, 0, 0);
        }
        level.sendParticles(new DustParticleOptions(new Vector3f(0.2f, 0.5f, 1f), 1.8f),
                getX(), getY(), getZ(), 4, 0.4, 0.4, 0.4, 0);

        if (tickCount >= 50) discard();
    }
}
