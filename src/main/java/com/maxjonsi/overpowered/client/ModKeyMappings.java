package com.maxjonsi.overpowered.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final String CATEGORY = "key.categories.overpowered";

    public static final KeyMapping SPECIAL = new KeyMapping("key.overpowered.special", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping MARK = new KeyMapping("key.overpowered.mark", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping HUD_TOGGLE = new KeyMapping("key.overpowered.hud_toggle", GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping ABILITY_ONE = new KeyMapping("key.overpowered.ability_1", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping ABILITY_TWO = new KeyMapping("key.overpowered.ability_2", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping ABILITY_THREE = new KeyMapping("key.overpowered.ability_3", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping ABILITY_FOUR = new KeyMapping("key.overpowered.ability_4", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ABILITY_FIVE = new KeyMapping("key.overpowered.ability_5", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping ULTIMATE = new KeyMapping("key.overpowered.ultimate", GLFW.GLFW_KEY_G, CATEGORY);
}
