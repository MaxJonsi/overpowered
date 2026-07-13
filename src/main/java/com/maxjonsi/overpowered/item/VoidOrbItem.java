package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.client.render.VoidOrbRenderer;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.server.VoidAbility;
import com.maxjonsi.overpowered.server.VoidServerState;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class VoidOrbItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.overpowered.void_orb.idle");
    private static final int[] SLOT_BY_MODE = {1, 2, 3, 4, 6};
    private static final String[] MODE_KEYS = {
            "message.overpowered.void.touch",
            "message.overpowered.void.gaze",
            "message.overpowered.void.wave",
            "message.overpowered.void.silence",
            "message.overpowered.void.absolute"};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VoidOrbItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown() || !VoidServerState.isActive(player.getUUID())) {
                VoidAbility.toggle(serverPlayer);
            } else {
                performSelected(serverPlayer, stack);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public void cycleAbility(ServerPlayer player, ItemStack stack) {
        int mode = (normalizedMode(stack) + 1) % SLOT_BY_MODE.length;
        stack.set(ModDataComponents.TECHNIQUE, mode);
        player.displayClientMessage(Component.translatable(MODE_KEYS[mode]), true);
    }

    private static void performSelected(ServerPlayer player, ItemStack stack) {
        switch (SLOT_BY_MODE[normalizedMode(stack)]) {
            case 1 -> VoidAbility.touch(player);
            case 2 -> VoidAbility.kill(player);
            case 3 -> VoidAbility.wave(player);
            case 4 -> VoidAbility.silence(player);
            case 6 -> VoidAbility.absoluteVoid(player);
            default -> {
            }
        }
    }

    private static int normalizedMode(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(ModDataComponents.TECHNIQUE, 0), SLOT_BY_MODE.length);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private VoidOrbRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new VoidOrbRenderer();
                return renderer;
            }
        });
    }
}
