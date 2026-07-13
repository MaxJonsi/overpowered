package com.maxjonsi.overpowered.registry;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ModItems {
    public static final YamatoItem YAMATO = register("yamato",
            new YamatoItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RocketLauncherItem ROCKET_LAUNCHER = register("rocket_launcher",
            new RocketLauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final SixEyesItem SIX_EYES = register("six_eyes",
            new SixEyesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(BuiltInRegistries.ITEM, Overpowered.id(name), item);
    }

    public static void init() {
    }
}
