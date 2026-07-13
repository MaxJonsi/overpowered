package com.maxjonsi.overpowered.client.hud;

import java.util.List;

/**
 * Data the legendary power HUD renders. Energy comes from the server via EnergyPayload;
 * ability lists are defined per held item in LegendaryHudRenderer.
 */
public interface PowerHudData {

    CharacterTheme theme();

    float energy();

    float maxEnergy();

    List<AbilityEntry> abilities();

    List<String> activeBuffs();

    boolean isInfinityCore();

    /** selected marks the ability the R key currently has armed (for cycling items). */
    record AbilityEntry(String name, float cost, boolean available, String keybind, boolean selected) {}
}
