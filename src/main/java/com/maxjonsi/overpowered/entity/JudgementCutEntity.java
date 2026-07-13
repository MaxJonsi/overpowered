package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
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

        if (tickCount == 1 || tickCount == 8 || tickCount == 16) {
            Player owner = getOwnerPlayer();
            DamageSource source = owner != null
                    ? level.damageSources().indirectMagic(owner, owner)
                    : level.damageSources().magic();
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(position(), position()).inflate(3.5), this::isVictim)) {
                target.hurt(source, 12f);
            }
            if (tickCount == 1) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.YAMATO_SLICE, SoundSource.PLAYERS, 1.8f, 1f);
            }
        }

        if (tickCount >= 24) discard();
    }
}
