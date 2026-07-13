package com.maxjonsi.overpowered.registry;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Overpowered.MODID);

    public static final DeferredItem<YamatoItem> YAMATO = ITEMS.register("yamato",
            () -> new YamatoItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<RocketLauncherItem> ROCKET_LAUNCHER = ITEMS.register("rocket_launcher",
            () -> new RocketLauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredItem<SixEyesItem> SIX_EYES = ITEMS.register("six_eyes",
            () -> new SixEyesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
}
