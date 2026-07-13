package com.maxjonsi.overpowered.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Shadow Monarch's summoned soldier. Fights monsters, never targets players.
 * Dissolves back into shadow after ~60 seconds.
 */
public class ShadowSoldierEntity extends WitherSkeleton {
    private static final int LIFETIME_TICKS = 1200;

    public ShadowSoldierEntity(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        setPersistenceRequired();
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, LIFETIME_TICKS, 1, false, false));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, LIFETIME_TICKS, 0, false, false));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(goal -> true);
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ShadowSoldierEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                target -> !(target instanceof ShadowSoldierEntity)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (tickCount % 8 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 1, 0.2, 0.4, 0.2, 0.005);
            }
            if (tickCount >= LIFETIME_TICKS) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 15, 0.3, 0.6, 0.3, 0.02);
                discard();
            }
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
