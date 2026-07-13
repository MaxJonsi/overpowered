package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class JudgementCutEntity extends EffectEntity {
    public JudgementCutEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        if (tickCount % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                double theta = random.nextDouble() * Math.PI * 2;
                double phi = random.nextDouble() * Math.PI;
                double r = 2.5;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        getX() + r * Math.sin(phi) * Math.cos(theta),
                        getY() + r * Math.cos(phi),
                        getZ() + r * Math.sin(phi) * Math.sin(theta),
                        1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 8, 1.5, 1.5, 1.5, 0.1);
        }

        if (tickCount == 1 || tickCount == 8 || tickCount == 16) {
            Player owner = getOwnerPlayer();
            DamageSource source = owner != null
                    ? level.damageSources().indirectMagic(owner, owner)
                    : level.damageSources().magic();
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(position(), position()).inflate(3.5), this::isVictim)) {
                target.hurt(source, 12f);
            }
            level.playSound(null, getX(), getY(), getZ(), ModSounds.YAMATO_SLICE.get(), SoundSource.PLAYERS,
                    1.5f, 1.3f - tickCount * 0.03f);
        }

        if (tickCount >= 24) discard();
    }
}
