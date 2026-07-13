package com.maxjonsi.overpowered.client.hud;

import java.util.List;

public class MockPowerHudData implements PowerHudData {

    private final CharacterTheme theme;

    public MockPowerHudData(CharacterTheme theme) {
        this.theme = theme;
    }

    @Override
    public CharacterTheme theme() {
        return theme;
    }

    @Override
    public float energy() {
        return 73f;
    }

    @Override
    public float maxEnergy() {
        return 100f;
    }

    @Override
    public List<AbilityEntry> abilities() {
        return switch (theme) {
            case VERGIL -> List.of(
                    new AbilityEntry("Judgment Cut", 15, true, "R"),
                    new AbilityEntry("Air Trick", 10, true, "H"),
                    new AbilityEntry("Dimension Rift", 30, true, "F"),
                    new AbilityEntry("Devil Trigger", 50, true, "V")
            );
            case GOJO -> List.of(
                    new AbilityEntry("Blue", 15, true, "R"),
                    new AbilityEntry("Red", 20, true, "H"),
                    new AbilityEntry("Infinity", 0, true, "F"),
                    new AbilityEntry("Hollow Purple", 40, true, "V")
            );
            case VOID -> List.of(
                    new AbilityEntry("Void Touch", 10, true, "R"),
                    new AbilityEntry("Void Gaze", 20, true, "H"),
                    new AbilityEntry("Void Wave", 25, true, "F"),
                    new AbilityEntry("Absolute Silence", 35, true, "V")
            );
            case DIO -> List.of(
                    new AbilityEntry("Knife Throw", 10, true, "R"),
                    new AbilityEntry("Time Dash", 15, true, "H"),
                    new AbilityEntry("Time Stop", 40, true, "F"),
                    new AbilityEntry("Time Accel", 30, true, "V")
            );
            case AIZEN -> List.of(
                    new AbilityEntry("Flash Step", 10, true, "R"),
                    new AbilityEntry("Kyoka Suigetsu", 25, true, "H"),
                    new AbilityEntry("Spirit Pressure", 20, true, "F"),
                    new AbilityEntry("Hogyoku", 50, true, "V")
            );
            case SHADOW_MONARCH -> List.of(
                    new AbilityEntry("Shadow Step", 10, true, "R"),
                    new AbilityEntry("Shadow Exchange", 15, true, "H"),
                    new AbilityEntry("Extraction", 20, true, "F"),
                    new AbilityEntry("Summon Shadow", 30, true, "V")
            );
            case FAT_MAN -> List.of(
                    new AbilityEntry("Mini Nuke", 20, true, "R"),
                    new AbilityEntry("MIRV", 35, true, "H"),
                    new AbilityEntry("Orbital Strike", 50, true, "F")
            );
        };
    }

    @Override
    public List<String> activeBuffs() {
        return List.of();
    }

    @Override
    public boolean isInfinityCore() {
        return false;
    }
}
