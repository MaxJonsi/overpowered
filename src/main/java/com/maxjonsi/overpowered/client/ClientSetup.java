package com.maxjonsi.overpowered.client;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.client.render.DomainRenderer;
import com.maxjonsi.overpowered.client.render.HomingRocketRenderer;
import com.maxjonsi.overpowered.client.render.NoopRenderer;
import com.maxjonsi.overpowered.client.render.NukeRenderer;
import com.maxjonsi.overpowered.client.render.RocketLauncherRenderer;
import com.maxjonsi.overpowered.client.render.ShadowRemnantRenderer;
import com.maxjonsi.overpowered.client.render.YamatoRenderer;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Overpowered.MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.SPECIAL);
        event.register(ModKeyMappings.MARK);
        event.register(ModKeyMappings.VOID_KILL);
        event.register(ModKeyMappings.VOID_TOGGLE);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOMING_ROCKET.get(), HomingRocketRenderer::new);
        event.registerEntityRenderer(ModEntities.NUKE.get(), NukeRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADOW_REMNANT.get(), ShadowRemnantRenderer::new);
        event.registerEntityRenderer(ModEntities.DOMAIN.get(), DomainRenderer::new);
        event.registerEntityRenderer(ModEntities.JUDGEMENT_CUT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.JUDGEMENT_CUT_END.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.HOLLOW_PURPLE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.BLUE_VORTEX.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final YamatoRenderer renderer = new YamatoRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, ModItems.YAMATO.get());

        event.registerItem(new IClientItemExtensions() {
            private final RocketLauncherRenderer renderer = new RocketLauncherRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, ModItems.ROCKET_LAUNCHER.get());
    }
}
