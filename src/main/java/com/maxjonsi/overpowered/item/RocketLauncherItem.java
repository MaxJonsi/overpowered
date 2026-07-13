package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.entity.HomingRocketEntity;
import com.maxjonsi.overpowered.entity.NukeEntity;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.PlayerEnergyManager;
import com.maxjonsi.overpowered.server.NuclearAbilityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import com.maxjonsi.overpowered.client.render.RocketLauncherRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RocketLauncherItem extends Item implements GeoItem {
    public static final int MODE_HOMING = 0;
    public static final int MODE_NUKE = 1;
    public static final int MODE_LASER = 2;
    public static final int MODE_MIRV = 3;
    public static final int MODE_ORBITAL = 4;
    public static final int MODE_APOCALYPSE = 5;

    public static final int HOMING_COST = 8;
    public static final int NUKE_COST = 80;
    public static final int LASER_START_COST = 5;
    public static final int LASER_TICK_COST = 1;
    public static final int LASER_ENERGY_INTERVAL_TICKS = 5;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.overpowered.rocket_launcher.idle");
    private static final String[] MODE_KEYS = {
            "message.overpowered.mode.homing",
            "message.overpowered.mode.nuke",
            "message.overpowered.mode.laser",
            "message.overpowered.mode.mirv",
            "message.overpowered.mode.orbital",
            "message.overpowered.mode.apocalypse"};

    private static final Map<UUID, LaserState> LASER = new HashMap<>();

    private static class LaserState {
        BlockPos blockPos;
        int blockTicks;
        int firingTicks;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RocketLauncherItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 2, state -> state.setAndContinue(IDLE))
                .triggerableAnim("fire", RawAnimation.begin().thenPlay("animation.overpowered.rocket_launcher.fire"))
                .triggerableAnim("mode", RawAnimation.begin().thenPlay("animation.overpowered.rocket_launcher.mode"))
                .triggerableAnim("laser", RawAnimation.begin().thenPlay("animation.overpowered.rocket_launcher.laser")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private RocketLauncherRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new RocketLauncherRenderer();
                return renderer;
            }
        });
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int mode = stack.getOrDefault(ModDataComponents.MODE, MODE_HOMING);

        if (mode == MODE_LASER) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                if (!PlayerEnergyManager.tryConsumeOrNotify(serverPlayer, LASER_START_COST)) {
                    return InteractionResultHolder.fail(stack);
                }
                beginLaser(serverLevel, serverPlayer, stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            switch (mode) {
                case MODE_HOMING -> fireHomingRocket(serverLevel, serverPlayer, stack);
                case MODE_NUKE -> callNuclearStrike(serverLevel, serverPlayer, stack);
                case MODE_MIRV -> NuclearAbilityManager.mirv(serverPlayer, stack);
                case MODE_ORBITAL -> NuclearAbilityManager.orbitalStrike(serverPlayer);
                case MODE_APOCALYPSE -> NuclearAbilityManager.apocalypse(serverPlayer);
                default -> {
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void beginLaser(ServerLevel level, ServerPlayer player, ItemStack stack) {
        LASER.put(player.getUUID(), new LaserState());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LAUNCHER_LASER, SoundSource.PLAYERS, 0.8f, 1f);
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "base", "laser");
    }

    private void fireHomingRocket(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, HOMING_COST)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        HomingRocketEntity rocket = new HomingRocketEntity(ModEntities.HOMING_ROCKET, level);
        rocket.setOwner(player);
        Vec3 spawn = eye.add(look.scale(1.2)).subtract(0, 0.2, 0);
        rocket.setPos(spawn.x, spawn.y, spawn.z);
        rocket.setDeltaMovement(look.scale(1.4));

        UUID targetId = stack.get(ModDataComponents.TARGET);
        if (targetId != null && level.getEntity(targetId) instanceof LivingEntity target && target.isAlive()) {
            rocket.setTargetId(target.getId());
        }
        level.addFreshEntity(rocket);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LAUNCHER_FIRE, SoundSource.PLAYERS, 1.5f, 1f);
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "base", "fire");
        player.getCooldowns().addCooldown(this, 15);
    }

    private void callNuclearStrike(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(300)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, NUKE_COST)) return;

        Vec3 target = hit.getLocation();
        double spawnY = Math.min(level.getMaxBuildHeight() - 12, target.y + 90);
        NukeEntity nuke = new NukeEntity(ModEntities.NUKE, level);
        nuke.setPos(target.x, spawnY, target.z);
        nuke.prepareForLaunch(level);
        if (!level.addFreshEntity(nuke)) {
            nuke.discard();
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LAUNCHER_FIRE, SoundSource.PLAYERS, 1.5f, 0.7f);
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "base", "fire");
        player.displayClientMessage(Component.translatable(MODE_KEYS[MODE_NUKE]), true);
        player.getCooldowns().addCooldown(this, 1200);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel serverLevel) || !(living instanceof ServerPlayer player)) return;
        if (stack.getOrDefault(ModDataComponents.MODE, MODE_HOMING) != MODE_LASER) {
            LASER.remove(player.getUUID());
            player.stopUsingItem();
            return;
        }

        LaserState state = LASER.computeIfAbsent(player.getUUID(), ignored -> new LaserState());
        state.firingTicks++;
        if (state.firingTicks % LASER_ENERGY_INTERVAL_TICKS == 0
                && !PlayerEnergyManager.tryConsumeOrNotify(player, LASER_TICK_COST)) {
            LASER.remove(player.getUUID());
            player.stopUsingItem();
            return;
        }

        LaserBeamHelper.Trace trace = LaserBeamHelper.trace(
                serverLevel, player, player.getEyePosition(), player.getLookAngle());

        EntityHitResult entityHit = trace.entityHit();
        if (entityHit != null) {
            entityHit.getEntity().hurt(player.damageSources().indirectMagic(player, player), 4f);
            entityHit.getEntity().igniteForSeconds(2);
            resetBlockProgress(state);
        } else if (trace.blockHit() != null && trace.blockHit().getType() == HitResult.Type.BLOCK) {
            BlockPos pos = trace.blockHit().getBlockPos();
            if (pos.equals(state.blockPos)) {
                state.blockTicks++;
            } else {
                state.blockPos = pos.immutable();
                state.blockTicks = 1;
            }
            if (state.blockTicks >= 1 && serverLevel.getBlockState(pos).getDestroySpeed(serverLevel, pos) >= 0) {
                serverLevel.destroyBlock(pos, false, player);
                serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
                resetBlockProgress(state);
            }
        } else {
            resetBlockProgress(state);
        }

        if (entityHit != null || trace.blockHit() != null && trace.blockHit().getType() == HitResult.Type.BLOCK) {
            Vec3 impact = trace.end();
            serverLevel.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z, 5, 0.2, 0.2, 0.2, 0.01);
        }

        if (state.firingTicks % 190 == 0) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LAUNCHER_LASER, SoundSource.PLAYERS, 0.8f, 1f);
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "base", "laser");
        }
    }

    private static void resetBlockProgress(LaserState state) {
        state.blockPos = null;
        state.blockTicks = 0;
    }

    public static void clearTransientState(UUID playerId) {
        LASER.remove(playerId);
    }

    public static void clearAllTransientState() {
        LASER.clear();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeCharged) {
        if (living instanceof ServerPlayer player) {
            LASER.remove(player.getUUID());
        }
        super.releaseUsing(stack, level, living, timeCharged);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (living instanceof ServerPlayer player) {
            LASER.remove(player.getUUID());
        }
        return super.finishUsingItem(stack, level, living);
    }

    public void cycleMode(ServerPlayer player, ItemStack stack) {
        if (stack.getOrDefault(ModDataComponents.MODE, MODE_HOMING) == MODE_LASER) {
            LASER.remove(player.getUUID());
            if (player.isUsingItem()) player.stopUsingItem();
        }
        int mode = (stack.getOrDefault(ModDataComponents.MODE, MODE_HOMING) + 1) % MODE_KEYS.length;
        stack.set(ModDataComponents.MODE, mode);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LAUNCHER_MODE, SoundSource.PLAYERS, 1f, 1f);
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "base", "mode");
        player.displayClientMessage(Component.translatable(MODE_KEYS[mode]), true);
    }

    public void markTarget(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(64));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1.5), e -> e instanceof LivingEntity && e != player && e.isAlive());
        if (hit != null) {
            stack.set(ModDataComponents.TARGET, hit.getEntity().getUUID());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LAUNCHER_LOCK, SoundSource.PLAYERS, 1f, 1f);
            player.displayClientMessage(Component.translatable("message.overpowered.marked", hit.getEntity().getDisplayName()), true);
        }
    }
}
