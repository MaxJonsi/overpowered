package com.maxjonsi.overpowered.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final String CATEGORY = "key.categories.overpowered";

    public static final KeyMapping SPECIAL = new KeyMapping("key.overpowered.special", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping MARK = new KeyMapping("key.overpowered.mark", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping HUD_TOGGLE = new KeyMapping("key.overpowered.hud_toggle", GLFW.GLFW_KEY_O, CATEGORY);
}
