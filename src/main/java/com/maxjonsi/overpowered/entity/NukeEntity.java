package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NukeEntity extends Entity implements GeoEntity {
    private static final int RADIUS = 50;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private boolean detonated;
    private BlockPos center = BlockPos.ZERO;
    private int layerY;
    private int lingerTicks;
    private int cloudTicks;

    public NukeEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        if (!detonated) {
            if (tickCount == 1) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.LAUNCHER_SIREN, SoundSource.MASTER, 12f, 1f);
            }
            Vec3 next = position().add(0, -0.9, 0);
            BlockPos below = BlockPos.containing(next);
            if (!level.getBlockState(below).isAir() || next.y <= level.getMinBuildHeight() + 1) {
                detonate(level);
            } else {
                setPos(next);
                if (tickCount % 4 == 0) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 1, getZ(), 2, 0.1, 0.3, 0.1, 0.01);
                }
            }
            return;
        }

        cloudTicks++;

        destroyLayers(level, 2);
        spawnMushroomCloud(level);
        damageWave(level);

        if (layerY < center.getY() - RADIUS) {
            lingerTicks++;
            if (lingerTicks > 60) discard();
        }
    }

    private void detonate(ServerLevel level) {
        detonated = true;
        center = blockPosition();
        layerY = center.getY() + RADIUS;
        setInvisible(true);
        setDeltaMovement(Vec3.ZERO);

        level.playSound(null, getX(), getY(), getZ(), ModSounds.LAUNCHER_NUKE, SoundSource.MASTER, 16f, 0.8f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 20, 8, 4, 8, 0);
        level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 3, 0, 0, 0, 0);
    }

    private void destroyLayers(ServerLevel level, int layersPerTick) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int pass = 0; pass < layersPerTick && layerY >= center.getY() - RADIUS; pass++, layerY--) {
            if (layerY <= level.getMinBuildHeight() || layerY >= level.getMaxBuildHeight()) continue;
            int dy = layerY - center.getY();
            int layerRadius = (int) Math.floor(Math.sqrt((double) RADIUS * RADIUS - (double) dy * dy));
            int radiusSq = layerRadius * layerRadius;
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    if (dx * dx + dz * dz > radiusSq) continue;
                    cursor.set(center.getX() + dx, layerY, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() && state.getDestroySpeed(level, cursor) >= 0) {
                        level.setBlock(cursor, air, 2 | 16);
                    }
                }
            }
        }
    }

    private void spawnMushroomCloud(ServerLevel level) {
        if (cloudTicks % 2 != 0) return;
        double stemHeight = Math.min(46, cloudTicks * 0.9);

        for (int i = 0; i < 8; i++) {
            double h = random.nextDouble() * stemHeight;
            double r = (2.5 + h * 0.04) * random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2;
            level.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.42f + random.nextFloat() * 0.22f, 0.06f), 3.2f),
                    center.getX() + Math.cos(angle) * r, center.getY() + h, center.getZ() + Math.sin(angle) * r,
                    2, 0.6, 0.9, 0.6, 0.01);
        }
        level.sendParticles(ParticleTypes.FLAME, center.getX(), center.getY() + 1.5, center.getZ(), 5, 1.6, 2.5, 1.6, 0.04);

        double capRadius = Math.min(17, stemHeight * 0.42);
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = capRadius * Math.sqrt(random.nextDouble());
            level.sendParticles(new DustParticleOptions(new Vector3f(0.36f, 0.37f, 0.4f), 4.5f),
                    center.getX() + Math.cos(angle) * r, center.getY() + stemHeight + random.nextDouble() * 5,
                    center.getZ() + Math.sin(angle) * r, 2, 1.2, 0.8, 1.2, 0.01);
        }

        if (cloudTicks % 8 == 0) {
            double ringRadius = capRadius * 1.7;
            double ringHeight = stemHeight * (cloudTicks % 16 == 0 ? 0.45 : 0.85);
            for (int i = 0; i < 24; i++) {
                double angle = 2 * Math.PI * i / 24;
                level.sendParticles(ParticleTypes.CLOUD,
                        center.getX() + Math.cos(angle) * ringRadius, center.getY() + ringHeight,
                        center.getZ() + Math.sin(angle) * ringRadius, 1, 0.3, 0.1, 0.3, 0);
            }
        }
        if (cloudTicks % 20 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.getX(), center.getY() + 2, center.getZ(), 2, 6, 2, 6, 0);
        }
    }

    private void damageWave(ServerLevel level) {
        if (tickCount % 10 != 0) return;
        Vec3 c = Vec3.atCenterOf(center);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(70),
                e -> e.isAlive() && !e.isSpectator())) {
            double dist = target.position().distanceTo(c);
            if (dist > 70) continue;
            float damage = (float) (120 * (1 - dist / 70));
            target.hurt(level.damageSources().explosion(this, null), damage);
            target.igniteForSeconds(5);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        detonated = tag.getBoolean("Detonated");
        layerY = tag.getInt("LayerY");
        if (tag.contains("CenterX")) {
            center = new BlockPos(tag.getInt("CenterX"), tag.getInt("CenterY"), tag.getInt("CenterZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Detonated", detonated);
        tag.putInt("LayerY", layerY);
        tag.putInt("CenterX", center.getX());
        tag.putInt("CenterY", center.getY());
        tag.putInt("CenterZ", center.getZ());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
