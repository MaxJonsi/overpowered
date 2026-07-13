package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.EnergyService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Grants 5 minutes of infinite energy — the absolute power mode from MASTER_DESIGN. */
public class InfinityCoreItem extends Item {
    private static final int DURATION_TICKS = 6000; // 5 minutes

    public InfinityCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            EnergyService.activateInfinity(serverPlayer, DURATION_TICKS);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.1);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.MAGIC_FORCEFIELD, SoundSource.PLAYERS, 2f, 1.8f);
            serverPlayer.displayClientMessage(Component.translatable("message.overpowered.infinity_core"), true);
            player.getCooldowns().addCooldown(this, DURATION_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
