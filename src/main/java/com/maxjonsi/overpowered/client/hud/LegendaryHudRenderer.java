package com.maxjonsi.overpowered.client.hud;

import com.maxjonsi.overpowered.AbilityCosts;
import com.maxjonsi.overpowered.client.ClientEnergyState;
import com.maxjonsi.overpowered.client.ClientVoidState;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class LegendaryHudRenderer {

    private static boolean visible = true;
    private static float animatedEnergy = -1f;

    private static final int MARGIN = 6;
    private static final int PADDING = 5;
    private static final int PANEL_WIDTH = 150;
    private static final int BAR_HEIGHT = 6;
    private static final int LINE_HEIGHT = 11;

    public static void toggleVisibility() {
        visible = !visible;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        HudInfo info = resolveHudInfo(mc);
        if (info == null) return;

        if (animatedEnergy < 0) animatedEnergy = ClientEnergyState.energy();
        animatedEnergy += (ClientEnergyState.energy() - animatedEnergy) * 0.15f;

        renderPanel(graphics, info, mc);
    }

    private record HudInfo(CharacterTheme theme, ItemStack stack) {}

    private static HudInfo resolveHudInfo(Minecraft mc) {
        ItemStack mainHand = mc.player.getMainHandItem();
        CharacterTheme theme = CharacterTheme.fromItem(mainHand.getItem());
        if (theme != null) return new HudInfo(theme, mainHand);

        ItemStack offHand = mc.player.getOffhandItem();
        theme = CharacterTheme.fromItem(offHand.getItem());
        if (theme != null) return new HudInfo(theme, offHand);

        return null;
    }

    /** Real ability lists — names, energy costs, and the actual controls of each item. */
    private static List<PowerHudData.AbilityEntry> abilitiesFor(CharacterTheme theme, ItemStack stack) {
        int cycled = stack.getOrDefault(ModDataComponents.TECHNIQUE, 0);
        int mode = stack.getOrDefault(ModDataComponents.MODE, 0);
        List<PowerHudData.AbilityEntry> list = new ArrayList<>();
        switch (theme) {
            case VERGIL -> {
                list.add(entry("ability.overpowered.vergil.combo", 0, "LMB", false));
                list.add(entry("ability.overpowered.vergil.judgement_cut", AbilityCosts.YAMATO_JUDGEMENT_CUT, "RMB", false));
                list.add(entry("ability.overpowered.vergil.dash", AbilityCosts.YAMATO_DASH, "R", false));
                list.add(entry("ability.overpowered.vergil.jce", AbilityCosts.YAMATO_JCE, "⇧RMB", false));
            }
            case GOJO -> {
                list.add(entry("ability.overpowered.gojo.blue", AbilityCosts.GOJO_BLUE, "RMB", cycled == 0));
                list.add(entry("ability.overpowered.gojo.red", AbilityCosts.GOJO_RED, "RMB", cycled == 1));
                list.add(entry("ability.overpowered.gojo.purple", AbilityCosts.GOJO_PURPLE, "RMB", cycled == 2));
                list.add(entry("ability.overpowered.gojo.domain", AbilityCosts.GOJO_DOMAIN, "RMB", cycled == 3));
            }
            case VOID -> {
                list.add(entry("ability.overpowered.void.transform", 0, "RMB", false));
                list.add(entry("ability.overpowered.void.erase", AbilityCosts.VOID_ERASE, "LMB", false));
            }
            case DIO -> {
                list.add(entry("ability.overpowered.dio.knife", AbilityCosts.DIO_KNIFE, "RMB", cycled == 0));
                list.add(entry("ability.overpowered.dio.dash", AbilityCosts.DIO_DASH, "RMB", cycled == 1));
                list.add(entry("ability.overpowered.dio.stop", AbilityCosts.DIO_STOP, "RMB", cycled == 2));
                list.add(entry("ability.overpowered.dio.accel", AbilityCosts.DIO_ACCEL, "RMB", cycled == 3));
            }
            case AIZEN -> {
                list.add(entry("ability.overpowered.aizen.step", AbilityCosts.AIZEN_STEP, "RMB", cycled == 0));
                list.add(entry("ability.overpowered.aizen.pressure", AbilityCosts.AIZEN_PRESSURE, "RMB", cycled == 1));
                list.add(entry("ability.overpowered.aizen.hypnosis", AbilityCosts.AIZEN_HYPNOSIS, "RMB", cycled == 2));
                list.add(entry("ability.overpowered.aizen.hogyoku", AbilityCosts.AIZEN_HOGYOKU, "RMB", cycled == 3));
            }
            case SHADOW_MONARCH -> {
                list.add(entry("ability.overpowered.shadow.step", AbilityCosts.SHADOW_STEP, "RMB", cycled == 0));
                list.add(entry("ability.overpowered.shadow.arise", AbilityCosts.SHADOW_ARISE, "RMB", cycled == 1));
                list.add(entry("ability.overpowered.shadow.wave", AbilityCosts.SHADOW_WAVE, "RMB", cycled == 2));
                list.add(entry("ability.overpowered.shadow.monarch", AbilityCosts.SHADOW_MONARCH_FORM, "RMB", cycled == 3));
            }
            case FAT_MAN -> {
                list.add(entry("ability.overpowered.fatman.homing", AbilityCosts.LAUNCHER_HOMING, "RMB", mode == 0));
                list.add(entry("ability.overpowered.fatman.nuke", AbilityCosts.LAUNCHER_NUKE, "RMB", mode == 1));
                list.add(entry("ability.overpowered.fatman.laser", AbilityCosts.LAUNCHER_LASER, "RMB", mode == 2));
                list.add(entry("ability.overpowered.fatman.mark", 0, "H", false));
            }
        }
        return list;
    }

    private static PowerHudData.AbilityEntry entry(String key, float cost, String keybind, boolean selected) {
        boolean available = ClientEnergyState.infinite() || ClientEnergyState.energy() >= cost;
        return new PowerHudData.AbilityEntry(Component.translatable(key).getString(), cost, available, keybind, selected);
    }

    private static List<String> buffsFor(Minecraft mc) {
        List<String> buffs = new ArrayList<>();
        if (ClientEnergyState.infinite()) {
            buffs.add(Component.translatable("buff.overpowered.infinity").getString());
        }
        if (mc.player != null && ClientVoidState.isActive(mc.player.getId())) {
            buffs.add(Component.translatable("buff.overpowered.void_form").getString());
        }
        return buffs;
    }

    private static void renderPanel(GuiGraphics g, HudInfo info, Minecraft mc) {
        Font font = mc.font;
        CharacterTheme theme = info.theme();
        boolean infinite = ClientEnergyState.infinite();
        List<PowerHudData.AbilityEntry> abilities = abilitiesFor(theme, info.stack());
        List<String> buffs = buffsFor(mc);

        int contentHeight = PADDING
                + LINE_HEIGHT
                + 2
                + BAR_HEIGHT
                + 4
                + abilities.size() * LINE_HEIGHT
                + (buffs.isEmpty() ? 0 : 4 + buffs.size() * LINE_HEIGHT)
                + PADDING;

        int x = MARGIN;
        int y = MARGIN;
        int x2 = x + PANEL_WIDTH;
        int y2 = y + contentHeight;

        g.fill(x, y, x2, y2, theme.backgroundColor);

        int borderColor = theme.primaryWithAlpha(0x60);
        g.fill(x, y, x2, y + 1, borderColor);
        g.fill(x, y2 - 1, x2, y2, borderColor);
        g.fill(x, y, x + 1, y2, borderColor);
        g.fill(x2 - 1, y, x2, y2, borderColor);

        int cx = x + PADDING;
        int cy = y + PADDING;

        String nameStr = theme.displayName.toUpperCase();
        g.drawString(font, nameStr, cx, cy, theme.primaryColor, false);
        if (infinite) {
            g.drawString(font, "∞", cx + font.width(nameStr) + 4, cy, 0xFFFFDD44, false);
        }
        cy += LINE_HEIGHT + 2;

        renderEnergyBar(g, font, cx, cy, theme, infinite);
        cy += BAR_HEIGHT + 4;

        for (PowerHudData.AbilityEntry ability : abilities) {
            renderAbilityRow(g, font, cx, cy, ability, theme);
            cy += LINE_HEIGHT;
        }

        if (!buffs.isEmpty()) {
            cy += 4;
            for (String buff : buffs) {
                g.drawString(font, buff, cx, cy, theme.secondaryColor, false);
                cy += LINE_HEIGHT;
            }
        }
    }

    private static void renderEnergyBar(GuiGraphics g, Font font, int x, int y,
                                        CharacterTheme theme, boolean infinite) {
        int barWidth = PANEL_WIDTH - PADDING * 2;

        g.fill(x, y, x + barWidth, y + BAR_HEIGHT, 0xFF111111);

        float ratio = infinite ? 1f : Mth.clamp(animatedEnergy / 100f, 0f, 1f);
        int fillWidth = (int) (barWidth * ratio);
        if (fillWidth > 0) {
            int fillColor = infinite ? 0xFFFFDD44 : theme.primaryColor;
            g.fill(x, y, x + fillWidth, y + BAR_HEIGHT, fillColor);
        }

        g.fill(x, y, x + barWidth, y + 1, 0x33FFFFFF);

        String energyText = infinite ? "∞" : String.valueOf((int) animatedEnergy);
        int textX = x + barWidth - font.width(energyText) - 1;
        g.drawString(font, energyText, textX, y - 1, 0xAAFFFFFF, false);
    }

    private static void renderAbilityRow(GuiGraphics g, Font font, int x, int y,
                                         PowerHudData.AbilityEntry ability,
                                         CharacterTheme theme) {
        boolean canAfford = ability.available();
        int textColor = canAfford ? 0xFFDDDDDD : 0x55888888;
        int keyColor = canAfford ? theme.primaryColor : theme.primaryWithAlpha(0x55);
        int costColor = canAfford ? theme.secondaryColor : theme.secondaryWithAlpha(0x55);

        if (ability.selected()) {
            g.fill(x - 2, y - 1, x + PANEL_WIDTH - PADDING * 2 + 2, y + 9, theme.primaryWithAlpha(0x28));
            textColor = canAfford ? 0xFFFFFFFF : textColor;
        }

        int keyWidth = font.width(ability.keybind()) + 4;
        int keyBgColor = canAfford ? theme.primaryWithAlpha(0x30) : 0x20333333;
        g.fill(x, y, x + keyWidth, y + 9, keyBgColor);
        g.drawString(font, ability.keybind(), x + 2, y + 1, keyColor, false);

        g.drawString(font, ability.name(), x + keyWidth + 4, y + 1, textColor, false);

        if (ability.cost() > 0) {
            String costStr = String.valueOf((int) ability.cost());
            int costX = x + PANEL_WIDTH - PADDING * 2 - font.width(costStr);
            g.drawString(font, costStr, costX, y + 1, costColor, false);
        }
    }
}
