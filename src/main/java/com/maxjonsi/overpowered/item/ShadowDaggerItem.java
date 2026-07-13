package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.server.ShadowAbilityManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public class ShadowDaggerItem extends Item {
    private static final int[] SLOT_BY_MODE = {1, 2, 3, 4, 5, 6};
    private static final String[] MODE_KEYS = {
            "message.overpowered.shadow.step",
            "message.overpowered.shadow.exchange",
            "message.overpowered.shadow.extract",
            "message.overpowered.shadow.summon",
            "message.overpowered.shadow.monarch",
            "message.overpowered.shadow.domain"};

    public ShadowDaggerItem(Properties properties) {
        super(properties.attributes(createAttributes()));
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 12.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -1.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
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
            performSlot(serverPlayer, SLOT_BY_MODE[normalizedMode(stack)]);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) {
            target.hurt(player.damageSources().indirectMagic(player, player), 5f);
        }
        return true;
    }

    public void cycleAbility(ServerPlayer player, ItemStack stack) {
        int mode = (normalizedMode(stack) + 1) % SLOT_BY_MODE.length;
        stack.set(ModDataComponents.TECHNIQUE, mode);
        player.displayClientMessage(Component.translatable(MODE_KEYS[mode]), true);
    }

    public void performSlot(ServerPlayer player, int slot) {
        switch (slot) {
            case 1 -> ShadowAbilityManager.shadowStep(player);
            case 2 -> ShadowAbilityManager.shadowExchange(player);
            case 3 -> ShadowAbilityManager.extract(player);
            case 4 -> ShadowAbilityManager.summon(player);
            case 5 -> ShadowAbilityManager.toggleMonarchForm(player);
            case 6 -> ShadowAbilityManager.shadowDomain(player);
            default -> {
            }
        }
    }

    private static int normalizedMode(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(ModDataComponents.TECHNIQUE, 0), SLOT_BY_MODE.length);
    }
}
