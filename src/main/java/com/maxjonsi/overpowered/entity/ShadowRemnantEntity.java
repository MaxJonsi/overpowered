package com.maxjonsi.overpowered.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import java.util.UUID;

public class ShadowRemnantEntity extends Entity {
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(ShadowRemnantEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> VOID_SHADOW =
            SynchedEntityData.defineId(ShadowRemnantEntity.class, EntityDataSerializers.BOOLEAN);
    private UUID ownerId;

    public ShadowRemnantEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public void setSize(float size) {
        entityData.set(SIZE, size);
    }

    public float getSize() {
        return entityData.get(SIZE);
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setVoidShadow(boolean voidShadow) {
        entityData.set(VOID_SHADOW, voidShadow);
    }

    public boolean isVoidShadow() {
        return entityData.get(VOID_SHADOW);
    }

    @Override
    public void tick() {
        baseTick();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1.8f);
        builder.define(VOID_SHADOW, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSize(tag.getFloat("Size"));
        setVoidShadow(tag.getBoolean("VoidShadow"));
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Size", getSize());
        tag.putBoolean("VoidShadow", isVoidShadow());
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }
}
