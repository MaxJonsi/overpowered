package com.maxjonsi.overpowered.client.hud;

import java.util.List;

/**
 * Data contract for the legendary power HUD. Codex's energy system provides this; the HUD renders it.
 */
public interface PowerHudData {

    CharacterTheme theme();

    float energy();

    float maxEnergy();

    List<AbilityEntry> abilities();

    String selectedAbility();

    List<String> activeBuffs();

    boolean isInfinityCore();

    record AbilityEntry(String name, float cost, boolean available, String keybind) {}
}
