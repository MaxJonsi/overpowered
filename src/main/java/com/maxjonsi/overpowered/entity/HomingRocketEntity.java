package com.maxjonsi.overpowered.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HomingRocketEntity extends Projectile implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final double SPEED = 1.4;
    private int targetId = -1;

    public HomingRocketEntity(EntityType<? extends HomingRocketEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 delta = getDeltaMovement();

        if (level() instanceof ServerLevel level) {
            if (tickCount > 300) {
                explode(level);
                return;
            }

            LivingEntity target = resolveTarget(level);
            if (target != null) {
                Vec3 toTarget = target.getEyePosition().subtract(position()).normalize();
                delta = delta.normalize().scale(0.8).add(toTarget.scale(0.25)).normalize().scale(SPEED);
                setDeltaMovement(delta);
            }

            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                explode(level);
                return;
            }

            level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.005);
        }

        setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
        setYRot((float) (Mth.atan2(delta.x, delta.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(delta.y, delta.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    private LivingEntity resolveTarget(ServerLevel level) {
        if (targetId >= 0 && level.getEntity(targetId) instanceof LivingEntity marked && marked.isAlive()) {
            return marked;
        }
        if (tickCount % 5 != 0 && targetId >= 0) return null;

        Vec3 dir = getDeltaMovement().normalize();
        LivingEntity best = null;
        double bestScore = 0.3;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(40), e -> e.isAlive() && e != getOwner())) {
            Vec3 toCandidate = candidate.position().subtract(position());
            double alignment = toCandidate.normalize().dot(dir);
            if (alignment > bestScore) {
                bestScore = alignment;
                best = candidate;
            }
        }
        if (best != null) targetId = best.getId();
        return best;
    }

    private void explode(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), 3.5f, Level.ExplosionInteraction.TNT);
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof HomingRocketEntity);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
