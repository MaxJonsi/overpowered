package com.maxjonsi.overpowered.client.gui;

import com.maxjonsi.overpowered.item.YamatoItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class YamatoChargeHud {
    private static final int CHARGE_TICKS = 30;
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 8;
    private static boolean initialized;

    private YamatoChargeHud() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        HudRenderCallback.EVENT.register(YamatoChargeHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || !client.player.isUsingItem()) return;

        ItemStack stack = client.player.getUseItem();
        if (!(stack.getItem() instanceof YamatoItem)) return;

        int heldTicks = stack.getUseDuration(client.player) - client.player.getUseItemRemainingTicks();
        float partialTick = tickCounter.getGameTimeDeltaPartialTick(true);
        float progress = Mth.clamp((heldTicks + partialTick) / CHARGE_TICKS, 0f, 1f);

        int x = (graphics.guiWidth() - BAR_WIDTH) / 2;
        int y = graphics.guiHeight() - 58;
        int filled = Mth.floor((BAR_WIDTH - 4) * progress);

        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xB0000000);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xD0181028);
        graphics.fill(x + 2, y + 2, x + 2 + filled, y + BAR_HEIGHT - 2, 0xFF7867E8);
        if (filled > 3) {
            graphics.fill(x + 2, y + 2, x + 2 + filled, y + 3, 0xFFE7F5FF);
        }
    }
}
