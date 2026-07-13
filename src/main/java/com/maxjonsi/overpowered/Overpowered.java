package com.maxjonsi.overpowered;

import com.maxjonsi.overpowered.network.ModNetworking;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModItems;
import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.EnergyService;
import com.maxjonsi.overpowered.server.ServerAbilityHandler;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.slf4j.Logger;

public class Overpowered implements ModInitializer {
    public static final String MODID = "overpowered";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("overpowered"));

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModDataComponents.init();
        ModItems.init();
        ModEntities.init();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.overpowered"))
                .icon(() -> ModItems.SIX_EYES.getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.YAMATO);
                    output.accept(ModItems.ROCKET_LAUNCHER);
                    output.accept(ModItems.SIX_EYES);
                    output.accept(ModItems.GOJO_MASK);
                    output.accept(ModItems.VOID_ORB);
                    output.accept(ModItems.STONE_MASK);
                    output.accept(ModItems.KYOKA_SUIGETSU);
                    output.accept(ModItems.SHADOW_DAGGER);
                    output.accept(ModItems.INFINITY_CORE);
                }).build());

        ModNetworking.init();
        ServerAbilityHandler.init();
        EnergyService.init();
    }
}
