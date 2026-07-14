package com.maxjonsi.overpowered.client;

/** Display-only record of the last direct ability key selected by the local player. */
public final class ClientAbilitySelection {
    private static String key = "Z";

    private ClientAbilitySelection() {
    }

    public static void select(String keybind) {
        key = keybind;
    }

    public static String key() {
        return key;
    }

    public static void clear() {
        key = "Z";
    }
}
