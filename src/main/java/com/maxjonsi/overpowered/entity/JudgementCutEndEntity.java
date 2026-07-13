package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
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
