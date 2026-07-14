package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.LegendaryCombat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class JudgementCutEntity extends EffectEntity {
    private static final EntityDataAccessor<Boolean> PERFECT =
            SynchedEntityData.defineId(JudgementCutEntity.class, EntityDataSerializers.BOOLEAN);

    public JudgementCutEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setPerfect(boolean perfect) {
        entityData.set(PERFECT, perfect);
    }

    public boolean isPerfect() {
        return entityData.get(PERFECT);
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
                    new AABB(position(), position()).inflate(isPerfect() ? 5.0 : 3.5), this::isVictim)) {
                LegendaryCombat.damage(target, source,
                        isPerfect() ? 44f / 3f : 32f / 3f,
                        isPerfect() ? 0.26f / 3f : 0.18f / 3f,
                        LegendaryCombat.AttackKind.SPATIAL);
            }
            if (tickCount == 1) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.YAMATO_SLICE, SoundSource.PLAYERS, 1.8f, 1f);
            }
        }

        if (tickCount >= 24) discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PERFECT, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPerfect(tag.getBoolean("Perfect"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Perfect", isPerfect());
    }
}
