package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.server.PlayerEnergyManager;
import com.maxjonsi.overpowered.server.PowerEventDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InfinityCoreItem extends Item {
    public InfinityCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerEnergyManager.grantInfinity(serverPlayer);
            PowerEventDispatcher.broadcast(serverPlayer, PowerEventPayload.POWER_INFINITY_CORE, 1,
                    PowerEventPayload.PHASE_STATE_START,
                    PlayerEnergyManager.INFINITY_DURATION_TICKS, 8);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.overpowered.infinity_core.active"), true);
            serverPlayer.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
