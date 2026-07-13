package com.maxjonsi.overpowered.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class EffectEntity extends Entity {
    @Nullable
    protected UUID ownerId;

    protected EffectEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Nullable
    protected Player getOwnerPlayer() {
        if (ownerId != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(ownerId) instanceof Player player && player.isAlive()) {
            return player;
        }
        return null;
    }

    protected boolean isVictim(LivingEntity entity) {
        return entity.isAlive() && !entity.getUUID().equals(ownerId) && !entity.isSpectator();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }
}
