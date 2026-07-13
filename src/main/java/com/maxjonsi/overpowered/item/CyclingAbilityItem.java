package com.maxjonsi.overpowered.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Items whose active ability is cycled with the SPECIAL key (R), like Six Eyes techniques. */
public interface CyclingAbilityItem {
    void cycleAbility(ServerPlayer player, ItemStack stack);
}
