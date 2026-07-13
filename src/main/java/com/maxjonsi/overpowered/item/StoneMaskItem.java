package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.AbilityCosts;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.EnergyService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** DIO — time. Knife Throw / Time Dash / Time Stop / Time Acceleration. */
public class StoneMaskItem extends Item implements CyclingAbilityItem {
    public static final int KNIFE = 0;
    public static final int TIME_DASH = 1;
    public static final int TIME_STOP = 2;
    public static final int TIME_ACCEL = 3;

    private static final String[] ABILITY_KEYS = {
            "ability.overpowered.dio.knife",
            "ability.overpowered.dio.dash",
            "ability.overpowered.dio.stop",
            "ability.overpowered.dio.accel"};

    private record FrozenMob(Mob mob, boolean hadNoAi, long until) {}
    private static final List<FrozenMob> FROZEN = new ArrayList<>();

    public StoneMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        int ability = stack.getOrDefault(ModDataComponents.TECHNIQUE, KNIFE);
        boolean cast = switch (ability) {
            case KNIFE -> castKnives(serverLevel, serverPlayer);
            case TIME_DASH -> castTimeDash(serverLevel, serverPlayer);
            case TIME_STOP -> castTimeStop(serverLevel, serverPlayer);
            case TIME_ACCEL -> castTimeAccel(serverLevel, serverPlayer);
            default -> false;
        };
        if (cast) player.getCooldowns().addCooldown(this, 15);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private boolean castKnives(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.DIO_KNIFE)) return false;
        for (int i = -1; i <= 1; i++) {
            Arrow knife = new Arrow(level, player, new ItemStack(Items.ARROW), null);
            knife.setBaseDamage(7);
            knife.pickup = AbstractArrow.Pickup.DISALLOWED;
            knife.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 7f, 0f, 2.8f, 0.5f);
            level.addFreshEntity(knife);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2f, 0.8f);
        return true;
    }

    private boolean castTimeDash(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.DIO_DASH)) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(12)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 dest = hit.getType() == HitResult.Type.MISS
                ? eye.add(look.scale(12)) : hit.getLocation().subtract(look.scale(1.2));

        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        player.teleportTo(level, dest.x, dest.y - 1.2, dest.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y, dest.z, 30, 0.4, 0.8, 0.4, 0.1);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);
        return true;
    }

    private boolean castTimeStop(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.DIO_STOP)) return false;
        long until = player.server.overworld().getGameTime() + 100; // 5 seconds
        Vec3 center = player.position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(24), e -> e != player && e.isAlive())) {
            if (target instanceof Mob mob) {
                FROZEN.add(new FrozenMob(mob, mob.isNoAi(), until));
                mob.setNoAi(true);
                mob.setDeltaMovement(Vec3.ZERO);
            } else if (target instanceof Player frozen) {
                frozen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 10, false, false));
                frozen.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
                frozen.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 4, false, false));
            }
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.DIO_TIME_STOP, SoundSource.MASTER, 3f, 1f);
        player.displayClientMessage(Component.translatable("message.overpowered.dio.time_stop"), true);
        return true;
    }

    private boolean castTimeAccel(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.DIO_ACCEL)) return false;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 1));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 1));
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 25, 0.5, 0.8, 0.5, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.5f, 0.6f);
        return true;
    }

    @Override
    public void cycleAbility(ServerPlayer player, ItemStack stack) {
        int ability = (stack.getOrDefault(ModDataComponents.TECHNIQUE, KNIFE) + 1) % 4;
        stack.set(ModDataComponents.TECHNIQUE, ability);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LAUNCHER_MODE, SoundSource.PLAYERS, 1f, 1.6f);
        player.displayClientMessage(Component.translatable(ABILITY_KEYS[ability]), true);
    }

    /** Called every server tick from EnergyService — unfreezes mobs when time resumes. */
    public static void tickTimeStop(MinecraftServer server) {
        if (FROZEN.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Iterator<FrozenMob> it = FROZEN.iterator();
        while (it.hasNext()) {
            FrozenMob frozen = it.next();
            if (!frozen.mob().isAlive()) {
                it.remove();
            } else if (now >= frozen.until()) {
                frozen.mob().setNoAi(frozen.hadNoAi());
                it.remove();
            }
        }
    }
}
