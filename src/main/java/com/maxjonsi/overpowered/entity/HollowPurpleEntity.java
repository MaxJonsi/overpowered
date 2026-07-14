package com.maxjonsi.overpowered.entity;

import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.LegendaryCombat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class HollowPurpleEntity extends EffectEntity {
    private static final int ERASE_RADIUS = 3;
    private final Set<UUID> damaged = new HashSet<>();

    public HollowPurpleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 delta = getDeltaMovement();
        setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);

        if (!(level() instanceof ServerLevel level)) return;

        eraseBlocks(level);

        Player owner = getOwnerPlayer();
        DamageSource source = owner != null
                ? level.damageSources().indirectMagic(owner, owner)
                : level.damageSources().magic();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(position(), position()).inflate(3.5),
                target -> isVictim(target) && damaged.add(target.getUUID()))) {
            LegendaryCombat.damage(target, source, 10000f, 0.80f,
                    LegendaryCombat.AttackKind.CONCEPTUAL);
            LegendaryCombat.stagger(target, 80, 4);
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                new AABB(position(), position()).inflate(3.5), Entity::isAlive)) {
            item.discard();
        }

        for (int i = 0; i < 14; i++) {
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            double r = 2.2;
            level.sendParticles(new DustParticleOptions(new Vector3f(0.62f, 0.12f, 0.94f), 2f),
                    getX() + r * Math.sin(phi) * Math.cos(theta),
                    getY() + r * Math.cos(phi),
                    getZ() + r * Math.sin(phi) * Math.sin(theta),
                    1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 5, 1, 1, 1, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 3, 0.5, 0.5, 0.5, 0.02);

        if (tickCount % 10 == 0) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.MAGIC_FORCEFIELD, SoundSource.PLAYERS, 1.5f, 0.7f);
        }

        if (tickCount >= 90) {
            level.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 20, 2, 2, 2, 0.02);
            discard();
        }
    }

    private void eraseBlocks(ServerLevel level) {
        Player owner = getOwnerPlayer();
        boolean mayEraseProtected = owner instanceof ServerPlayer serverPlayer
                && serverPlayer.hasPermissions(2);
        BlockPos base = blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -ERASE_RADIUS; dx <= ERASE_RADIUS; dx++) {
            for (int dy = -ERASE_RADIUS; dy <= ERASE_RADIUS; dy++) {
                for (int dz = -ERASE_RADIUS; dz <= ERASE_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > ERASE_RADIUS * ERASE_RADIUS) continue;
                    cursor.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() && level.getBlockEntity(cursor) == null
                            && (state.getDestroySpeed(level, cursor) >= 0 || mayEraseProtected)) {
                        level.setBlock(cursor, air, 2 | 16);
                    }
                }
            }
        }
    }
}
