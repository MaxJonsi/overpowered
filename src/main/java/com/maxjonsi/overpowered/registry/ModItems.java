package com.maxjonsi.overpowered.registry;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.item.GojoMaskItem;
import com.maxjonsi.overpowered.item.InfinityCoreItem;
import com.maxjonsi.overpowered.item.KyokaSuigetsuItem;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.ShadowDaggerItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.StoneMaskItem;
import com.maxjonsi.overpowered.item.VoidOrbItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public class ModItems {
    public static final YamatoItem YAMATO = register("yamato",
            new YamatoItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RocketLauncherItem ROCKET_LAUNCHER = register("rocket_launcher",
            new RocketLauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final SixEyesItem SIX_EYES = register("six_eyes",
            new SixEyesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final GojoMaskItem GOJO_MASK = register("gojo_mask",
            new GojoMaskItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final VoidOrbItem VOID_ORB = register("void_orb",
            new VoidOrbItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final StoneMaskItem STONE_MASK = register("stone_mask",
            new StoneMaskItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final KyokaSuigetsuItem KYOKA_SUIGETSU = register("kyoka_suigetsu",
            new KyokaSuigetsuItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final ShadowDaggerItem SHADOW_DAGGER = register("shadow_dagger",
            new ShadowDaggerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final InfinityCoreItem INFINITY_CORE = register("infinity_core",
            new InfinityCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final Item DIO_KNIFE = register("dio_knife",
            new Item(new Item.Properties().stacksTo(1).fireResistant()));

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(BuiltInRegistries.ITEM, Overpowered.id(name), item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(Overpowered.TAB_KEY).register(entries -> {
            entries.accept(STONE_MASK);
            entries.accept(KYOKA_SUIGETSU);
            entries.accept(SHADOW_DAGGER);
            entries.accept(INFINITY_CORE);
        });
    }
}
