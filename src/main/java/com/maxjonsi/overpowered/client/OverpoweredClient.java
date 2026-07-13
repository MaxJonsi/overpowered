package com.maxjonsi.overpowered.client;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.client.animation.YamatoPlayerAnimations;
import com.maxjonsi.overpowered.client.gui.YamatoChargeHud;
import com.maxjonsi.overpowered.client.hud.LegendaryHudRenderer;
import com.maxjonsi.overpowered.client.render.BlueVortexRenderer;
import com.maxjonsi.overpowered.client.render.DomainRenderer;
import com.maxjonsi.overpowered.client.render.HollowPurpleRenderer;
import com.maxjonsi.overpowered.client.render.HomingRocketRenderer;
import com.maxjonsi.overpowered.client.render.JudgementCutEndRenderer;
import com.maxjonsi.overpowered.client.render.JudgementCutRenderer;
import com.maxjonsi.overpowered.client.render.LaserBeamRenderer;
import com.maxjonsi.overpowered.client.render.NukeRenderer;
import com.maxjonsi.overpowered.client.render.PowerEventPresentation;
import com.maxjonsi.overpowered.client.render.ShadowRemnantRenderer;
import com.maxjonsi.overpowered.client.render.SkyTearState;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.AbilityActionPayload;
import com.maxjonsi.overpowered.network.EnergyStatePayload;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.server.VoidAbility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class OverpoweredClient implements ClientModInitializer {
    private static final ResourceLocation VOID_CROSSHAIR = Overpowered.id("textures/gui/void_crosshair.png");

    @Override
    public void onInitializeClient() {
        YamatoPlayerAnimations.init();
        YamatoChargeHud.init();
        LaserBeamRenderer.init();
        PowerEventPresentation.init();

        KeyBindingHelper.registerKeyBinding(ModKeyMappings.SPECIAL);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.MARK);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.HUD_TOGGLE);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ABILITY_ONE);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ABILITY_TWO);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ABILITY_THREE);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ABILITY_FOUR);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ABILITY_FIVE);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ULTIMATE);

        HudRenderCallback.EVENT.register(LegendaryHudRenderer::render);

        EntityRendererRegistry.register(ModEntities.HOMING_ROCKET, HomingRocketRenderer::new);
        EntityRendererRegistry.register(ModEntities.NUKE, NukeRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_REMNANT, ShadowRemnantRenderer::new);
        EntityRendererRegistry.register(ModEntities.DOMAIN, DomainRenderer::new);
        EntityRendererRegistry.register(ModEntities.JUDGEMENT_CUT, JudgementCutRenderer::new);
        EntityRendererRegistry.register(ModEntities.JUDGEMENT_CUT_END, JudgementCutEndRenderer::new);
        EntityRendererRegistry.register(ModEntities.HOLLOW_PURPLE, HollowPurpleRenderer::new);
        EntityRendererRegistry.register(ModEntities.BLUE_VORTEX, BlueVortexRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            ClientEnergyState.tick();
            if (ClientVoidState.isActive(client.player.getId())) {
                client.player.fallDistance = 0;
            }
            while (ModKeyMappings.SPECIAL.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.SPECIAL));
            }
            while (ModKeyMappings.MARK.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.MARK));
            }
            while (ModKeyMappings.HUD_TOGGLE.consumeClick()) {
                LegendaryHudRenderer.toggleVisibility();
            }
            while (ModKeyMappings.ABILITY_ONE.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.ABILITY_ONE));
            }
            while (ModKeyMappings.ABILITY_TWO.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.ABILITY_TWO));
            }
            while (ModKeyMappings.ABILITY_THREE.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.ABILITY_THREE));
            }
            while (ModKeyMappings.ABILITY_FOUR.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.ABILITY_FOUR));
            }
            while (ModKeyMappings.ABILITY_FIVE.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.ABILITY_FIVE));
            }
            while (ModKeyMappings.ULTIMATE.consumeClick()) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.ULTIMATE));
            }
        });

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (ClientVoidState.isActive(player.getId())) {
                if (clickCount > 0) {
                    ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.VOID_KILL));
                }
                return true;
            }
            if (clickCount > 0 && player.getMainHandItem().getItem() instanceof YamatoItem) {
                ClientPlayNetworking.send(new AbilityActionPayload(AbilityActionPayload.SWING));
            }
            return false;
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null
                    || !ClientVoidState.isActive(client.player.getId())
                    || VoidAbility.findTarget(client.level, client.player) == null) {
                return;
            }

            int centerX = drawContext.guiWidth() / 2;
            int centerY = drawContext.guiHeight() / 2;
            drawContext.blit(VOID_CROSSHAIR, centerX - 7, centerY - 7,
                    0.0F, 0.0F, 15, 15, 15, 15);
        });

        ClientPlayNetworking.registerGlobalReceiver(VoidStatePayload.TYPE, (payload, context) ->
                ClientVoidState.set(payload.entityId(), payload.active()));
        ClientPlayNetworking.registerGlobalReceiver(EnergyStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientEnergyState.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(PowerEventPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PowerEventPresentation.receive(payload)));

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> ClientVoidState.remove(entity.getId()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientVoidState.clear();
            ClientEnergyState.clear();
            ClientPowerEventState.clear();
            SkyTearState.clear();
        }));
    }
}
