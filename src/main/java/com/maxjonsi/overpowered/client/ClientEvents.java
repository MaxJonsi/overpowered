package com.maxjonsi.overpowered.client;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.AbilityActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Overpowered.MODID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) return;

        while (ModKeyMappings.SPECIAL.consumeClick()) {
            PacketDistributor.sendToServer(new AbilityActionPayload(AbilityActionPayload.SPECIAL));
        }
        while (ModKeyMappings.MARK.consumeClick()) {
            PacketDistributor.sendToServer(new AbilityActionPayload(AbilityActionPayload.MARK));
        }
        while (ModKeyMappings.VOID_KILL.consumeClick()) {
            PacketDistributor.sendToServer(new AbilityActionPayload(AbilityActionPayload.VOID_KILL));
        }
        while (ModKeyMappings.VOID_TOGGLE.consumeClick()) {
            PacketDistributor.sendToServer(new AbilityActionPayload(AbilityActionPayload.VOID_TOGGLE));
        }
    }

    @SubscribeEvent
    static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getItemStack().getItem() instanceof YamatoItem) {
            PacketDistributor.sendToServer(new AbilityActionPayload(AbilityActionPayload.SWING));
        }
    }
}
