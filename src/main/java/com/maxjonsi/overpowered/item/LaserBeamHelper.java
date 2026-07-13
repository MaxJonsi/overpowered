package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.registry.ModDataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LaserBeamHelper {
    public static final double MAX_RANGE = 512.0;
    private static final double CHUNK_EDGE_EPSILON = 1.0E-4;
    private static final double DIRECTION_EPSILON = 1.0E-8;

    private LaserBeamHelper() {
    }

    public static boolean isFiring(Player player) {
        if (!player.isUsingItem()) return false;
        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof RocketLauncherItem
                && stack.getOrDefault(ModDataComponents.MODE, RocketLauncherItem.MODE_HOMING)
                == RocketLauncherItem.MODE_LASER;
    }

    public static Trace trace(Level level, Entity source, Vec3 start, Vec3 direction) {
        if (direction.lengthSqr() < DIRECTION_EPSILON) {
            return new Trace(start, start, null, null);
        }

        Vec3 normalizedDirection = direction.normalize();
        double loadedRange = findLoadedRange(level, start, normalizedDirection);
        Vec3 loadedEnd = start.add(normalizedDirection.scale(loadedRange));
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, loadedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        Vec3 blockEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : loadedEnd;

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level,
                source,
                start,
                blockEnd,
                new AABB(start, blockEnd).inflate(0.6),
                candidate -> candidate instanceof LivingEntity
                        && candidate != source
                        && candidate.isAlive()
                        && !candidate.isSpectator());
        Vec3 end = entityHit == null ? blockEnd : entityHit.getLocation();
        return new Trace(start, end, blockHit, entityHit);
    }

    private static double findLoadedRange(Level level, Vec3 start, Vec3 direction) {
        int chunkX = Mth.floor(start.x) >> 4;
        int chunkZ = Mth.floor(start.z) >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) return 0;

        int stepX = direction.x > 0 ? 1 : direction.x < 0 ? -1 : 0;
        int stepZ = direction.z > 0 ? 1 : direction.z < 0 ? -1 : 0;
        double nextBoundaryX = stepX > 0 ? (chunkX + 1) * 16.0 : chunkX * 16.0;
        double nextBoundaryZ = stepZ > 0 ? (chunkZ + 1) * 16.0 : chunkZ * 16.0;
        double distanceToBoundaryX = stepX == 0
                ? Double.POSITIVE_INFINITY
                : Math.max(0, (nextBoundaryX - start.x) / direction.x);
        double distanceToBoundaryZ = stepZ == 0
                ? Double.POSITIVE_INFINITY
                : Math.max(0, (nextBoundaryZ - start.z) / direction.z);
        double chunkStepX = stepX == 0 ? Double.POSITIVE_INFINITY : 16.0 / Math.abs(direction.x);
        double chunkStepZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 16.0 / Math.abs(direction.z);

        while (true) {
            double nextDistance = Math.min(distanceToBoundaryX, distanceToBoundaryZ);
            if (nextDistance >= MAX_RANGE || !Double.isFinite(nextDistance)) return MAX_RANGE;

            boolean crossX = distanceToBoundaryX <= distanceToBoundaryZ;
            boolean crossZ = distanceToBoundaryZ <= distanceToBoundaryX;
            if (crossX) {
                chunkX += stepX;
                distanceToBoundaryX += chunkStepX;
            }
            if (crossZ) {
                chunkZ += stepZ;
                distanceToBoundaryZ += chunkStepZ;
            }

            if (!level.hasChunk(chunkX, chunkZ)) {
                return Math.max(0, nextDistance - CHUNK_EDGE_EPSILON);
            }
        }
    }

    public record Trace(Vec3 start, Vec3 end, BlockHitResult blockHit, EntityHitResult entityHit) {
    }
}
