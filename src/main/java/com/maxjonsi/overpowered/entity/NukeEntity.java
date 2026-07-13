package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.NuclearAbilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NukeEntity extends Entity implements GeoEntity {
    public static final int DEFAULT_RADIUS = 100;
    private static final int MAX_VISITED_POSITIONS_PER_TICK = 20_000;
    private static final int CENTER_TICKET_RADIUS = 2;
    private static final int CHUNK_TICKET_RADIUS = 0;
    private static final int TICKET_TIMEOUT_TICKS = 100;
    private static final int TICKET_REFRESH_INTERVAL = 20;
    private static final TicketType<UUID> NUKE_TICKET = TicketType.create(
            "overpowered_nuke", Comparator.<UUID>naturalOrder(), TICKET_TIMEOUT_TICKS);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<Long> destructionChunkTickets = new HashSet<>();

    private boolean detonated;
    private BlockPos center = BlockPos.ZERO;
    private int cursorY;
    private int cursorX;
    private int cursorZ;
    private boolean destructionComplete;
    private int lingerTicks;
    private int cloudTicks;
    private ChunkPos anchorTicketChunk;
    private int blastRadius = DEFAULT_RADIUS;

    public NukeEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        maintainChunkTickets(level);
        if (!detonated) {
            if (tickCount == 1) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.LAUNCHER_SIREN, SoundSource.MASTER, 12f, 1f);
                level.playSound(null, getX(), getY(), getZ(), ModSounds.LAUNCHER_NUKE_FALL, SoundSource.MASTER, 8f, 1f);
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

        destroyBlocks(level);
        spawnMushroomCloud(level);
        damageWave(level);

        if (destructionComplete) {
            lingerTicks++;
            if (lingerTicks > 60) discard();
        }
    }

    private void detonate(ServerLevel level) {
        detonated = true;
        center = blockPosition();
        cursorY = center.getY() + blastRadius;
        resetCursorForLayer();
        destructionComplete = false;
        setInvisible(true);
        setDeltaMovement(Vec3.ZERO);
        maintainChunkTickets(level);

        level.playSound(null, getX(), getY(), getZ(), ModSounds.LAUNCHER_NUKE, SoundSource.MASTER, 16f, 0.8f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 20, 8, 4, 8, 0);
        level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 3, 0, 0, 0, 0);
        NuclearAbilityManager.addRadiationZone(level, Vec3.atCenterOf(center),
                Math.max(18, (int) (blastRadius * 0.75)), 20 * 30);
    }

    public void prepareForLaunch(ServerLevel level) {
        maintainChunkTickets(level);
    }

    public void setBlastRadius(int blastRadius) {
        if (detonated) return;
        this.blastRadius = Mth.clamp(blastRadius, 8, 120);
    }

    public int getBlastRadius() {
        return blastRadius;
    }

    private void destroyBlocks(ServerLevel level) {
        BlockPos.MutableBlockPos blockCursor = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int visited = 0;

        while (!destructionComplete && visited < MAX_VISITED_POSITIONS_PER_TICK) {
            int layerRadius = radiusAt(cursorY);
            int y = cursorY;
            int dx = cursorX;
            int dz = cursorZ;

            int dy = y - center.getY();
            if (dx * dx + dy * dy + dz * dz > blastRadius * blastRadius
                    || y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                advanceCursor(layerRadius);
                visited++;
                continue;
            }

            blockCursor.set(center.getX() + dx, y, center.getZ() + dz);
            LevelChunk chunk = getOrRequestChunk(level, blockCursor);
            if (chunk == null) return;

            advanceCursor(layerRadius);
            visited++;
            BlockState state = chunk.getBlockState(blockCursor);
            if (!state.isAir() && state.getDestroySpeed(level, blockCursor) >= 0) {
                level.setBlock(blockCursor, air, 2 | 16);
            }
        }

        if (destructionComplete) {
            releaseDestructionChunkTickets(level);
        }
    }

    private LevelChunk getOrRequestChunk(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        if (destructionChunkTickets.add(chunkKey)) {
            level.getChunkSource().addRegionTicket(
                    NUKE_TICKET, new ChunkPos(chunkKey), CHUNK_TICKET_RADIUS, getUUID());
        }
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    private void maintainChunkTickets(ServerLevel level) {
        ChunkPos desiredAnchor = new ChunkPos(detonated ? center : blockPosition());
        if (anchorTicketChunk != null && !anchorTicketChunk.equals(desiredAnchor)) {
            level.getChunkSource().removeRegionTicket(
                    NUKE_TICKET, anchorTicketChunk, CENTER_TICKET_RADIUS, getUUID());
            anchorTicketChunk = null;
        }
        if (anchorTicketChunk == null || tickCount % TICKET_REFRESH_INTERVAL == 0) {
            level.getChunkSource().addRegionTicket(
                    NUKE_TICKET, desiredAnchor, CENTER_TICKET_RADIUS, getUUID());
            anchorTicketChunk = desiredAnchor;
        }
        if (tickCount % TICKET_REFRESH_INTERVAL != 0) return;

        for (long chunkKey : destructionChunkTickets) {
            level.getChunkSource().addRegionTicket(
                    NUKE_TICKET, new ChunkPos(chunkKey), CHUNK_TICKET_RADIUS, getUUID());
        }
    }

    private void releaseDestructionChunkTickets(ServerLevel level) {
        for (long chunkKey : destructionChunkTickets) {
            level.getChunkSource().removeRegionTicket(
                    NUKE_TICKET, new ChunkPos(chunkKey), CHUNK_TICKET_RADIUS, getUUID());
        }
        destructionChunkTickets.clear();
    }

    private void releaseChunkTickets(ServerLevel level) {
        releaseDestructionChunkTickets(level);
        if (anchorTicketChunk == null) return;

        level.getChunkSource().removeRegionTicket(
                NUKE_TICKET, anchorTicketChunk, CENTER_TICKET_RADIUS, getUUID());
        anchorTicketChunk = null;
    }

    private void advanceCursor(int layerRadius) {
        cursorZ++;
        if (cursorZ <= layerRadius) return;

        cursorZ = -layerRadius;
        cursorX++;
        if (cursorX <= layerRadius) return;

        cursorY--;
        if (cursorY < center.getY() - blastRadius) {
            destructionComplete = true;
            return;
        }
        resetCursorForLayer();
    }

    private void resetCursorForLayer() {
        int layerRadius = radiusAt(cursorY);
        cursorX = -layerRadius;
        cursorZ = -layerRadius;
    }

    private int radiusAt(int y) {
        int dy = y - center.getY();
        int radiusSquared = blastRadius * blastRadius - dy * dy;
        return radiusSquared <= 0 ? 0 : (int) Math.floor(Math.sqrt(radiusSquared));
    }

    private void spawnMushroomCloud(ServerLevel level) {
        if (cloudTicks % 2 != 0) return;
        double scale = blastRadius / (double) DEFAULT_RADIUS;
        double stemHeight = Math.min(46 * scale, cloudTicks * 0.9 * Math.max(0.45, scale));

        for (int i = 0; i < 8; i++) {
            double h = random.nextDouble() * stemHeight;
            double r = (2.5 + h * 0.04) * random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2;
            level.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.42f + random.nextFloat() * 0.22f, 0.06f), 3.2f),
                    center.getX() + Math.cos(angle) * r, center.getY() + h, center.getZ() + Math.sin(angle) * r,
                    2, 0.6, 0.9, 0.6, 0.01);
        }
        level.sendParticles(ParticleTypes.FLAME, center.getX(), center.getY() + 1.5, center.getZ(), 5, 1.6, 2.5, 1.6, 0.04);

        double capRadius = Math.min(17 * scale, stemHeight * 0.42);
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
        double damageRadius = Math.max(14, blastRadius * 0.7);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(damageRadius),
                e -> e.isAlive() && !e.isSpectator())) {
            double dist = target.position().distanceTo(c);
            if (dist > damageRadius) continue;
            float damage = (float) (Math.max(35, blastRadius * 1.2) * (1 - dist / damageRadius));
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
        if (tag.contains("CenterX")) {
            center = new BlockPos(tag.getInt("CenterX"), tag.getInt("CenterY"), tag.getInt("CenterZ"));
        }
        cursorY = tag.contains("CursorY") ? tag.getInt("CursorY") : tag.getInt("LayerY");
        if (tag.contains("CursorX")) {
            cursorX = tag.getInt("CursorX");
            cursorZ = tag.getInt("CursorZ");
        } else if (detonated) {
            resetCursorForLayer();
        }
        destructionComplete = tag.getBoolean("DestructionComplete");
        lingerTicks = tag.getInt("LingerTicks");
        cloudTicks = tag.getInt("CloudTicks");
        blastRadius = tag.contains("BlastRadius") ? tag.getInt("BlastRadius") : DEFAULT_RADIUS;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Detonated", detonated);
        tag.putInt("CenterX", center.getX());
        tag.putInt("CenterY", center.getY());
        tag.putInt("CenterZ", center.getZ());
        tag.putInt("CursorY", cursorY);
        tag.putInt("CursorX", cursorX);
        tag.putInt("CursorZ", cursorZ);
        tag.putBoolean("DestructionComplete", destructionComplete);
        tag.putInt("LingerTicks", lingerTicks);
        tag.putInt("CloudTicks", cloudTicks);
        tag.putInt("BlastRadius", blastRadius);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel serverLevel) {
            releaseChunkTickets(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
