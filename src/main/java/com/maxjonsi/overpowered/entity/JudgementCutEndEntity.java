package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class JudgementCutEndEntity extends EffectEntity {
    private static final double RADIUS = 24;

    public JudgementCutEndEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        if (tickCount == 1) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.YAMATO_JUDGEMENT_END, SoundSource.PLAYERS, 5f, 1f);
            for (LivingEntity target : victims(level)) {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 130, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 110, 3));
            }
        }

        if (tickCount >= 10 && tickCount <= 110) {
            Player owner = getOwnerPlayer();
            DamageSource source = owner != null
                    ? level.damageSources().indirectMagic(owner, owner)
                    : level.damageSources().magic();

            for (int i = 0; i < 14; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = random.nextDouble() * RADIUS;
                double x = getX() + Math.cos(angle) * dist;
                double z = getZ() + Math.sin(angle) * dist;
                double y = getY() + random.nextDouble() * 5;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, x, y, z, 4, 0.5, 0.5, 0.5, 0.2);
                }
            }

            if (tickCount % 5 == 0) {
                for (LivingEntity target : victims(level)) {
                    target.hurt(source, 10f);
                }
            }
            if (tickCount % 8 == 0) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.YAMATO_SLICE, SoundSource.PLAYERS,
                        2f, 0.8f + random.nextFloat() * 0.6f);
            }
        }

        if (tickCount == 120) {
            Player owner = getOwnerPlayer();
            DamageSource source = owner != null
                    ? level.damageSources().indirectMagic(owner, owner)
                    : level.damageSources().magic();
            for (LivingEntity target : victims(level)) {
                target.hurt(source, 25f);
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.8, 0));
                target.hurtMarked = true;
            }
            for (int ring = 4; ring <= 24; ring += 4) {
                for (int i = 0; i < ring * 3; i++) {
                    double angle = 2 * Math.PI * i / (ring * 3);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            getX() + ring * Math.cos(angle), getY() + 1.5, getZ() + ring * Math.sin(angle), 1, 0, 0, 0, 0);
                }
            }
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MAGIC_EXPLOSION, SoundSource.PLAYERS, 4f, 0.7f);
        }

        if (tickCount >= 130) discard();
    }

    private java.util.List<LivingEntity> victims(ServerLevel level) {
        Vec3 center = position();
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(RADIUS),
                e -> isVictim(e) && e.position().distanceTo(center) <= RADIUS);
    }
}
