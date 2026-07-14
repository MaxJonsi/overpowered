package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.server.TimeAbilityManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StoneMaskItem extends Item {
    private static final int[] SLOT_BY_MODE = {1, 2, 3, 4, 5, 6};
    private static final String[] MODE_KEYS = {
            "message.overpowered.dio.vampire_strike",
            "message.overpowered.dio.knives",
            "message.overpowered.dio.temporal_lock",
            "message.overpowered.dio.time_stop",
            "message.overpowered.dio.acceleration",
            "message.overpowered.dio.rewind"};

    public StoneMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (player.isShiftKeyDown()) {
            cycleAbility(serverPlayer, stack);
        } else {
            int mode = normalizedMode(stack);
            performSlot(serverPlayer, SLOT_BY_MODE[mode]);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public void cycleAbility(ServerPlayer player, ItemStack stack) {
        int mode = (normalizedMode(stack) + 1) % SLOT_BY_MODE.length;
        stack.set(ModDataComponents.TECHNIQUE, mode);
        player.displayClientMessage(Component.translatable(MODE_KEYS[mode]), true);
    }

    public void performSlot(ServerPlayer player, int slot) {
        switch (slot) {
            case 1 -> TimeAbilityManager.vampireStrike(player);
            case 2 -> TimeAbilityManager.throwKnives(player);
            case 3 -> TimeAbilityManager.temporalLock(player);
            case 4 -> TimeAbilityManager.timeStop(player);
            case 5 -> TimeAbilityManager.accelerate(player);
            case 6 -> TimeAbilityManager.rewind(player);
            default -> {
            }
        }
    }

    private static int normalizedMode(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(ModDataComponents.TECHNIQUE, 0), SLOT_BY_MODE.length);
    }
}
