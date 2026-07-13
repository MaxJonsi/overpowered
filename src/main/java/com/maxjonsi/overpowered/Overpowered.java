package com.maxjonsi.overpowered;

import com.maxjonsi.overpowered.network.ModNetworking;
import com.maxjonsi.overpowered.registry.ModAttachments;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModItems;
import com.maxjonsi.overpowered.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Overpowered.MODID)
public class Overpowered {
    public static final String MODID = "overpowered";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OVERPOWERED_TAB = CREATIVE_MODE_TABS.register("overpowered", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.overpowered"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.SIX_EYES.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.YAMATO.get());
                output.accept(ModItems.ROCKET_LAUNCHER.get());
                output.accept(ModItems.SIX_EYES.get());
            }).build());

    public Overpowered(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
    }
}
