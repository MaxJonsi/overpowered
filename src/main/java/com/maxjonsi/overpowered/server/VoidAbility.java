package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.ShadowRemnantEntity;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class VoidAbility {
    public static void toggle(ServerPlayer player) {
        boolean active = !VoidServerState.isActive(player.getUUID());
        VoidServerState.set(player.getUUID(), active);

        VoidStatePayload payload = new VoidStatePayload(player.getId(), active);
        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, payload);
        }

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1, player.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MAGIC_FORCEFIELD, SoundSource.PLAYERS, 1.5f, 0.55f);
        player.displayClientMessage(Component.translatable(active ? "message.overpowered.void.on" : "message.overpowered.void.off"), true);
    }

    public static void kill(ServerPlayer player) {
        if (!VoidServerState.isActive(player.getUUID())) return;

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(32));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1), e -> e instanceof LivingEntity && e != player && e.isAlive() && !e.isSpectator());
        if (hit == null || !(hit.getEntity() instanceof LivingEntity victim)) return;

        BlockPos ground = victim.blockPosition();
        for (int i = 0; i < 6 && level.getBlockState(ground.below()).isAir(); i++) {
            ground = ground.below();
        }

        ShadowRemnantEntity shadow = new ShadowRemnantEntity(ModEntities.SHADOW_REMNANT, level);
        shadow.setPos(victim.getX(), ground.getY() + 0.05, victim.getZ());
        shadow.setSize(Mth.clamp(victim.getBbWidth() * 2.4f, 1.2f, 5f));
        shadow.setYRot(victim.getYRot());
        level.addFreshEntity(shadow);

        level.sendParticles(ParticleTypes.SCULK_SOUL, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
                25, 0.3, victim.getBbHeight() / 3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
                40, 0.3, victim.getBbHeight() / 3, 0.3, 0.03);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.VOID_KILL, SoundSource.PLAYERS, 2f, 1f);

        victim.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
    }
}
