package com.maxjonsi.overpowered.client.hud;

import com.maxjonsi.overpowered.client.ClientVoidState;
import com.maxjonsi.overpowered.item.GojoMaskItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.List;

public class LegendaryHudRenderer {

    private static boolean expanded = true;
    private static float animatedEnergy = -1f;

    private static final int MARGIN = 6;
    private static final int PADDING = 5;
    private static final int PANEL_WIDTH = 142;
    private static final int BAR_HEIGHT = 6;
    private static final int LINE_HEIGHT = 11;

    public static void toggleVisibility() {
        expanded = !expanded;
    }

    public static boolean isVisible() {
        return expanded;
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PowerHudData data = resolveHudData(mc);
        if (data == null) return;

        if (animatedEnergy < 0) animatedEnergy = data.energy();
        animatedEnergy += (data.energy() - animatedEnergy) * 0.15f;

        renderPanel(graphics, data, mc.font, expanded);
    }

    private static PowerHudData resolveHudData(Minecraft mc) {
        if (ClientVoidState.isActive(mc.player.getId())) {
            return new LivePowerHudData(CharacterTheme.VOID);
        }
        if (mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GojoMaskItem) {
            return new LivePowerHudData(CharacterTheme.GOJO);
        }
        ItemStack mainHand = mc.player.getMainHandItem();
        CharacterTheme theme = CharacterTheme.fromItem(mainHand.getItem());
        if (theme != null) {
            return new LivePowerHudData(theme);
        }

        ItemStack offHand = mc.player.getOffhandItem();
        theme = CharacterTheme.fromItem(offHand.getItem());
        if (theme != null) {
            return new LivePowerHudData(theme);
        }

        return null;
    }

    private static void renderPanel(GuiGraphics g, PowerHudData data, Font font, boolean expanded) {
        CharacterTheme theme = data.theme();
        List<PowerHudData.AbilityEntry> abilities = data.abilities();
        List<String> buffs = data.activeBuffs();

        int contentHeight = PADDING
                + LINE_HEIGHT          // name
                + 2                    // gap
                + BAR_HEIGHT           // energy bar
                + 4                    // gap
                + LINE_HEIGHT           // selected ability
                + (expanded ? abilities.size() * LINE_HEIGHT : 0)
                + (expanded && !buffs.isEmpty() ? 4 + buffs.size() * LINE_HEIGHT : 0)
                + PADDING;

        int x = MARGIN;
        int y = MARGIN;
        int x2 = x + PANEL_WIDTH;
        int y2 = y + contentHeight;

        g.fill(x, y, x2, y2, theme.backgroundColor);

        int borderAlpha = 0x60;
        int borderColor = theme.primaryWithAlpha(borderAlpha);
        g.fill(x, y, x2, y + 1, borderColor);
        g.fill(x, y2 - 1, x2, y2, borderColor);
        g.fill(x, y, x + 1, y2, borderColor);
        g.fill(x2 - 1, y, x2, y2, borderColor);

        int cx = x + PADDING;
        int cy = y + PADDING;

        String nameStr = theme.displayName.toUpperCase();
        g.fill(cx, cy, cx + 8, cy + 8, theme.primaryWithAlpha(0x55));
        g.drawString(font, nameStr.substring(0, 1), cx + 1, cy, theme.primaryColor, false);
        g.drawString(font, nameStr, cx + 11, cy, theme.primaryColor, false);

        if (data.isInfinityCore()) {
            int infX = cx + 11 + font.width(nameStr) + 4;
            g.drawString(font, "\u221e", infX, cy, 0xFFFFDD44, false);
        }
        cy += LINE_HEIGHT + 2;

        renderEnergyBar(g, cx, cy, theme, data);
        cy += BAR_HEIGHT + 4;

        g.drawString(font, "> " + data.selectedAbility(), cx, cy, theme.secondaryColor, false);
        cy += LINE_HEIGHT;

        if (expanded) {
            for (PowerHudData.AbilityEntry ability : abilities) {
                boolean canAfford = ability.available() && (data.isInfinityCore() || data.energy() >= ability.cost());
                renderAbilityRow(g, font, cx, cy, ability, theme, canAfford);
                cy += LINE_HEIGHT;
            }
        }

        if (expanded && !buffs.isEmpty()) {
            cy += 4;
            for (String buff : buffs) {
                g.drawString(font, buff, cx, cy, theme.secondaryColor, false);
                cy += LINE_HEIGHT;
            }
        }
    }

    private static void renderEnergyBar(GuiGraphics g, int x, int y,
                                         CharacterTheme theme, PowerHudData data) {
        int barWidth = PANEL_WIDTH - PADDING * 2;

        g.fill(x, y, x + barWidth, y + BAR_HEIGHT, 0xFF111111);

        float ratio = Math.clamp(animatedEnergy / data.maxEnergy(), 0f, 1f);
        int fillWidth = (int) (barWidth * ratio);
        if (fillWidth > 0) {
            int fillColor = data.isInfinityCore() ? 0xFFFFDD44 : theme.primaryColor;
            g.fill(x, y, x + fillWidth, y + BAR_HEIGHT, fillColor);
        }

        g.fill(x, y, x + barWidth, y + 1, 0x33FFFFFF);

        String energyText = data.isInfinityCore() ? "∞" : String.valueOf((int) animatedEnergy);
        int textX = x + barWidth - Minecraft.getInstance().font.width(energyText) - 1;
        if (textX > x + fillWidth - 20) {
            g.drawString(Minecraft.getInstance().font, energyText, textX, y - 1, 0xAAFFFFFF, false);
        }
    }

    private static void renderAbilityRow(GuiGraphics g, Font font, int x, int y,
                                          PowerHudData.AbilityEntry ability,
                                          CharacterTheme theme, boolean canAfford) {
        int textColor = canAfford ? 0xFFDDDDDD : 0x55888888;
        int keyColor = canAfford ? theme.primaryColor : theme.primaryWithAlpha(0x55);
        int costColor = canAfford ? theme.secondaryColor : theme.secondaryWithAlpha(0x55);

        int keyBgColor = canAfford ? theme.primaryWithAlpha(0x30) : 0x20333333;
        int keyWidth = Math.max(9, font.width(ability.keybind()) + 4);
        g.fill(x, y, x + keyWidth, y + 9, keyBgColor);
        g.drawString(font, ability.keybind(), x + 2, y + 1, keyColor, false);

        g.drawString(font, ability.name(), x + keyWidth + 4, y + 1, textColor, false);

        String costStr = String.valueOf((int) ability.cost());
        int costX = x + PANEL_WIDTH - PADDING * 2 - font.width(costStr);
        g.drawString(font, costStr, costX, y + 1, costColor, false);
    }
}
