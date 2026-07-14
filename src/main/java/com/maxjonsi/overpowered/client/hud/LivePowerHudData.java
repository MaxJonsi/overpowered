package com.maxjonsi.overpowered.client.hud;

import com.maxjonsi.overpowered.client.ClientEnergyState;
import com.maxjonsi.overpowered.client.ClientAbilitySelection;
import com.maxjonsi.overpowered.client.ClientPowerEventState;
import com.maxjonsi.overpowered.client.ClientVoidState;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.server.AizenAbilityManager;
import com.maxjonsi.overpowered.server.GojoAbilityManager;
import com.maxjonsi.overpowered.server.NuclearAbilityManager;
import com.maxjonsi.overpowered.server.ShadowAbilityManager;
import com.maxjonsi.overpowered.server.TimeAbilityManager;
import com.maxjonsi.overpowered.server.VoidAbility;
import com.maxjonsi.overpowered.server.YamatoAbilityManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class LivePowerHudData implements PowerHudData {
    private final CharacterTheme theme;

    public LivePowerHudData(CharacterTheme theme) {
        this.theme = theme;
    }

    @Override
    public CharacterTheme theme() {
        return theme;
    }

    @Override
    public float energy() {
        return ClientEnergyState.energy();
    }

    @Override
    public float maxEnergy() {
        return ClientEnergyState.MAX_ENERGY;
    }

    @Override
    public List<AbilityEntry> abilities() {
        return switch (theme) {
            case VERGIL -> List.of(
                    ability("Rapid Slash", YamatoItem.DASH_COST, "Z"),
                    ability("Judgment Cut", YamatoItem.JUDGEMENT_CUT_COST, "X"),
                    ability("Air Trick", YamatoAbilityManager.AIR_TRICK_COST, "C"),
                    ability("Yamato Counter", YamatoAbilityManager.COUNTER_COST, "V"),
                    ability("World Split", YamatoAbilityManager.WORLD_SPLIT_COST, "B"),
                    ability("Dimension Rift", YamatoAbilityManager.DIMENSION_RIFT_COST, "H"),
                    ability("Devil Trigger", YamatoAbilityManager.DEVIL_TRIGGER_COST, "Shift+R"),
                    ability("Final Judgment Cut", YamatoAbilityManager.FINAL_COST, "G"));
            case GOJO -> List.of(
                    ability("Blue", SixEyesItem.BLUE_COST, "Z"),
                    ability("Maximum Blue", GojoAbilityManager.MAXIMUM_BLUE_COST, "Shift+Z"),
                    ability("Red", SixEyesItem.RED_COST, "X"),
                    ability("Infinity Focus", GojoAbilityManager.INFINITY_FOCUS_COST, "C"),
                    ability("Teleport", GojoAbilityManager.TELEPORT_COST, "V"),
                    ability("Hollow Purple", SixEyesItem.PURPLE_COST, "B"),
                    ability("Infinity Toggle", 0, "Shift+R"),
                    ability("Unlimited Void", SixEyesItem.DOMAIN_COST, "G"));
            case VOID -> List.of(
                    ability("Void Step", VoidAbility.STEP_COST, "Z"),
                    ability("Void Touch", VoidAbility.TOUCH_COST, "X"),
                    ability("Void Gaze", VoidAbility.ERASE_COST, "C"),
                    ability("Void Grasp", VoidAbility.WAVE_COST, "V"),
                    ability("Absolute Silence", VoidAbility.SILENCE_COST, "B"),
                    ability("Absolute Void", VoidAbility.ABSOLUTE_VOID_COST, "G"));
            case DIO -> List.of(
                    ability("Vampire Strike", TimeAbilityManager.VAMPIRE_STRIKE_COST, "Z"),
                    ability("Knife Throw", TimeAbilityManager.KNIFE_COST, "X"),
                    ability("Temporal Lock", TimeAbilityManager.TEMPORAL_LOCK_COST, "C"),
                    ability("Time Stop", TimeAbilityManager.STOP_COST, "V"),
                    ability("Time Acceleration", TimeAbilityManager.ACCELERATION_COST, "B"),
                    ability("Time Dash", TimeAbilityManager.DASH_COST, "H"),
                    ability("Time Rewind", TimeAbilityManager.REWIND_COST, "G"));
            case AIZEN -> List.of(
                    ability("Flash Step", AizenAbilityManager.FLASH_STEP_COST, "Z"),
                    ability("Kyoka Suigetsu", AizenAbilityManager.HYPNOSIS_COST, "X"),
                    ability("Spiritual Pressure", AizenAbilityManager.PRESSURE_COST, "C"),
                    ability("Kurohitsugi", AizenAbilityManager.KUROHITSUGI_COST, "V"),
                    ability("Hogyoku Evolution", AizenAbilityManager.EVOLUTION_BASE_COST, "B"),
                    ability("Perfect Hypnosis", AizenAbilityManager.PERFECT_HYPNOSIS_COST, "G"));
            case SHADOW_MONARCH -> List.of(
                    ability("Shadow Step", ShadowAbilityManager.STEP_COST, "Z"),
                    ability("Ruler's Authority", ShadowAbilityManager.AUTHORITY_COST, "X"),
                    ability("Shadow Exchange", ShadowAbilityManager.EXCHANGE_COST, "C"),
                    ability("Shadow Extraction", ShadowAbilityManager.EXTRACTION_COST, "V"),
                    ability("Summon Shadow", ShadowAbilityManager.SUMMON_COST, "B"),
                    ability("Monarch Form", ShadowAbilityManager.MONARCH_FORM_COST, "Shift+R"),
                    ability("Shadow Domain", ShadowAbilityManager.DOMAIN_COST, "G"));
            case FAT_MAN -> List.of(
                    ability("Micro-Nuke", NuclearAbilityManager.MICRO_NUKE_COST, "Z"),
                    ability("Mini Nuke", NuclearAbilityManager.MINI_NUKE_COST, "X"),
                    ability("MIRV", NuclearAbilityManager.MIRV_COST, "C"),
                    ability("Orbital Strike", NuclearAbilityManager.ORBITAL_COST, "V"),
                    ability("Nuclear Apocalypse", NuclearAbilityManager.APOCALYPSE_COST, "G"));
            case INFINITY_CORE -> List.of();
        };
    }

    private static AbilityEntry ability(String name, float cost, String key) {
        return new AbilityEntry(name, cost, true, key);
    }

    @Override
    public String selectedAbility() {
        String selectedKey = ClientAbilitySelection.key();
        return abilities().stream()
                .filter(ability -> ability.keybind().equals(selectedKey))
                .map(AbilityEntry::name)
                .findFirst()
                .orElseGet(() -> abilities().isEmpty() ? "Ready" : abilities().getFirst().name());
    }

    @Override
    public List<String> activeBuffs() {
        Minecraft client = Minecraft.getInstance();
        List<String> buffs = client.player == null
                ? new ArrayList<>()
                : new ArrayList<>(ClientPowerEventState.buffsFor(client.player.getId()));
        if (client.player != null && ClientVoidState.isActive(client.player.getId())) buffs.add("Void Form");
        if (ClientEnergyState.infinite()) {
            int seconds = ClientEnergyState.infinityTicks() / 20;
            buffs.add(String.format("Infinity Core  %d:%02d", seconds / 60, seconds % 60));
        }
        return buffs;
    }

    @Override
    public boolean isInfinityCore() {
        return ClientEnergyState.infinite();
    }
}
