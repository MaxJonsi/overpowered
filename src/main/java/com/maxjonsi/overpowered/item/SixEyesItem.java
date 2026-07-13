package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.entity.BlueVortexEntity;
import com.maxjonsi.overpowered.entity.DomainEntity;
import com.maxjonsi.overpowered.entity.HollowPurpleEntity;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.PlayerEnergyManager;
import com.maxjonsi.overpowered.server.GojoAbilityManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SixEyesItem extends Item {
    public static final int TECH_BLUE = 0;
    public static final int TECH_RED = 1;
    public static final int TECH_PURPLE = 2;
    public static final int TECH_DOMAIN = 3;
    public static final int TECH_INFINITY = 4;
    public static final int TECH_TELEPORT = 5;

    public static final int BLUE_COST = 15;
    public static final int RED_COST = 18;
    public static final int PURPLE_COST = 45;
    public static final int DOMAIN_COST = 75;

    private static final String[] TECH_KEYS = {
            "message.overpowered.technique.blue",
            "message.overpowered.technique.red",
            "message.overpowered.technique.purple",
            "message.overpowered.technique.domain",
            "message.overpowered.technique.infinity",
            "message.overpowered.technique.teleport"};

    public SixEyesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        int technique = stack.getOrDefault(ModDataComponents.TECHNIQUE, TECH_BLUE);
        if (!castTechnique(serverPlayer, technique)) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public boolean castTechnique(ServerPlayer player, int technique) {
        if (!hasRequiredMask(player)) {
            player.displayClientMessage(Component.translatable("message.overpowered.gojo.mask_required"), true);
            return false;
        }
        if (technique == TECH_INFINITY) {
            GojoAbilityManager.toggleInfinity(player);
            return true;
        }
        if (technique == TECH_TELEPORT) {
            GojoAbilityManager.teleport(player);
            return true;
        }
        if (technique == TECH_DOMAIN && DomainEntity.getActive(player.getUUID()) != null) return false;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, energyCostFor(technique))) return false;

        ServerLevel level = player.serverLevel();
        switch (technique) {
            case TECH_BLUE -> castBlue(level, player);
            case TECH_RED -> castRed(level, player);
            case TECH_PURPLE -> castPurple(level, player);
            case TECH_DOMAIN -> castDomain(level, player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void castBlue(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(18)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 center = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(14)) : hit.getLocation().subtract(look.scale(2));

        BlueVortexEntity vortex = new BlueVortexEntity(ModEntities.BLUE_VORTEX, level);
        vortex.setOwnerId(player.getUUID());
        vortex.setPos(center);
        level.addFreshEntity(vortex);

        player.getCooldowns().addCooldown(this, 80);
    }

    private void castRed(ServerLevel level, ServerPlayer player) {
        Vec3 center = player.position().add(0, 1, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(12),
                e -> e != player && e.isAlive())) {
            Vec3 away = target.position().subtract(center);
            double dist = Math.max(1, away.length());
            Vec3 push = away.normalize().scale(3.2 * (1 - dist / 16)).add(0, 0.6, 0);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true;
            target.hurt(player.damageSources().indirectMagic(player, player), 10f);
        }

        for (int ring = 2; ring <= 10; ring += 2) {
            for (int i = 0; i < ring * 6; i++) {
                double angle = 2 * Math.PI * i / (ring * 6);
                level.sendParticles(new DustParticleOptions(new Vector3f(0.95f, 0.1f, 0.15f), 1.6f),
                        center.x + ring * Math.cos(angle), center.y + 0.5, center.z + ring * Math.sin(angle), 1, 0.1, 0.3, 0.1, 0);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 1, 1, 1, 0);
        level.playSound(null, center.x, center.y, center.z, ModSounds.GOJO_RED, SoundSource.PLAYERS, 2.5f, 1f);
        player.getCooldowns().addCooldown(this, 80);
    }

    private void castPurple(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        HollowPurpleEntity purple = new HollowPurpleEntity(ModEntities.HOLLOW_PURPLE, level);
        purple.setOwnerId(player.getUUID());
        Vec3 spawn = eye.add(look.scale(2.5));
        purple.setPos(spawn.x, spawn.y, spawn.z);
        purple.setDeltaMovement(look.scale(1.2));
        level.addFreshEntity(purple);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GOJO_PURPLE, SoundSource.PLAYERS, 3f, 1f);
        player.getCooldowns().addCooldown(this, 400);
    }

    private void castDomain(ServerLevel level, ServerPlayer player) {
        if (DomainEntity.getActive(player.getUUID()) != null) return;

        DomainEntity domain = new DomainEntity(ModEntities.DOMAIN, level);
        domain.setOwnerId(player.getUUID());
        domain.setPos(player.position());
        level.addFreshEntity(domain);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GOJO_DOMAIN, SoundSource.MASTER, 8f, 1f);
        player.displayClientMessage(Component.translatable(TECH_KEYS[TECH_DOMAIN]), true);
        player.getCooldowns().addCooldown(this, 1800);
    }

    public void cycleTechnique(ServerPlayer player, ItemStack stack) {
        if (!hasRequiredMask(player)) {
            player.displayClientMessage(Component.translatable("message.overpowered.gojo.mask_required"), true);
            return;
        }

        int technique = (stack.getOrDefault(ModDataComponents.TECHNIQUE, TECH_BLUE) + 1) % TECH_KEYS.length;
        stack.set(ModDataComponents.TECHNIQUE, technique);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LAUNCHER_MODE, SoundSource.PLAYERS, 1f, 1.4f);
        player.displayClientMessage(Component.translatable(TECH_KEYS[technique]), true);
    }

    public static boolean hasRequiredMask(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GojoMaskItem;
    }

    private static int energyCostFor(int technique) {
        return switch (technique) {
            case TECH_BLUE -> BLUE_COST;
            case TECH_RED -> RED_COST;
            case TECH_PURPLE -> PURPLE_COST;
            case TECH_DOMAIN -> DOMAIN_COST;
            default -> 0;
        };
    }
}
