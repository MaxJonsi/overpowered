package com.maxjonsi.overpowered.client.hud;

import com.maxjonsi.overpowered.item.GojoMaskItem;
import com.maxjonsi.overpowered.item.KyokaSuigetsuItem;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.ShadowDaggerItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.StoneMaskItem;
import com.maxjonsi.overpowered.item.VoidOrbItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import net.minecraft.world.item.Item;

public enum CharacterTheme {
    VERGIL("Vergil",
            0xFF4488FF, 0xFF2266CC, 0xCC0D1B2A,
            YamatoItem.class),

    GOJO("Gojo",
            0xFFAADDFF, 0xFF55AAEE, 0xCC0D1B2A,
            SixEyesItem.class, GojoMaskItem.class),

    VOID("Void",
            0xFF666666, 0xFF333333, 0xCC000000,
            VoidOrbItem.class),

    DIO("DIO",
            0xFFFFCC33, 0xFFCC9900, 0xCC2A2210,
            StoneMaskItem.class),

    AIZEN("Aizen",
            0xFFAA55DD, 0xFF7733AA, 0xCC1A0D2A,
            KyokaSuigetsuItem.class),

    SHADOW_MONARCH("Shadow Monarch",
            0xFF8833CC, 0xFF5511AA, 0xCC0D0A1E,
            ShadowDaggerItem.class),

    FAT_MAN("Fat Man",
            0xFFFFDD44, 0xFFCCAA00, 0xCC2A2A10,
            RocketLauncherItem.class);

    public final String displayName;
    public final int primaryColor;
    public final int secondaryColor;
    public final int backgroundColor;
    private final Class<?>[] itemClasses;

    CharacterTheme(String displayName, int primary, int secondary, int background,
                   Class<?>... itemClasses) {
        this.displayName = displayName;
        this.primaryColor = primary;
        this.secondaryColor = secondary;
        this.backgroundColor = background;
        this.itemClasses = itemClasses;
    }

    public static CharacterTheme fromItem(Item item) {
        if (item == null) return null;
        for (CharacterTheme theme : values()) {
            for (Class<?> itemClass : theme.itemClasses) {
                if (itemClass.isInstance(item)) return theme;
            }
        }
        return null;
    }

    public int primaryWithAlpha(int alpha) {
        return (primaryColor & 0x00FFFFFF) | (alpha << 24);
    }

    public int secondaryWithAlpha(int alpha) {
        return (secondaryColor & 0x00FFFFFF) | (alpha << 24);
    }
}
