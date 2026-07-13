package com.maxjonsi.overpowered.client;

import com.maxjonsi.overpowered.client.render.DomainRenderer;
import com.maxjonsi.overpowered.client.render.HomingRocketRenderer;
import com.maxjonsi.overpowered.client.render.NoopRenderer;
import com.maxjonsi.overpowered.client.render.NukeRenderer;
import com.maxjonsi.overpowered.client.render.ShadowRemnantRenderer;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.AbilityActionPayload;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import com.maxjonsi.overpowered.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class OverpoweredClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.SPECIAL);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.MARK);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.VOID_KILL);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.VOID_TOGGLE);

        EntityRendererRegistry.register(ModEntities.HOMING_ROCKET, HomingRocketRenderer::new);
        EntityRendererRegistry.register(ModEntities.NUKE, NukeRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_REMNANT, ShadowRemnantRenderer::new);
        EntityRendererRegistry.register(ModEntities.DOMAIN, DomainRenderer::new);
        EntityRendererRegistry.register(ModEntities.JUDGEMENT_CUT, NoopRenderer::new);
        EntityRendererRegistry.register(ModEntities.JUDGEMENT_CUT_END, NoopRenderer::new);
        EntityRendererRegistry.register(ModEntities.HOLLOW_PURPLE, NoopRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLUE_VORTEX, NoopRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (ModKeyMappings.SPECIAL.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.SPECIAL));
            }
            while (ModKeyMappings.MARK.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.MARK));
            }
            while (ModKeyMappings.VOID_KILL.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.VOID_KILL));
            }
            while (ModKeyMappings.VOID_TOGGLE.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.VOID_TOGGLE));
            }
        });

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (clickCount > 0 && player.getMainHandItem().getItem() instanceof YamatoItem) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.SWING));
            }
            return false;
        });

        ClientPlayNetworking.registerGlobalReceiver(VoidStatePayload.TYPE, (payload, context) ->
                ClientVoidState.set(payload.entityId(), payload.active()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientVoidState.clear());
    }
}
