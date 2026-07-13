package com.maxjonsi.overpowered.client;

public final class ClientEnergyState {
    private static float energy = 100f;
    private static boolean infinite;

    private ClientEnergyState() {}

    public static void set(float newEnergy, boolean newInfinite) {
        energy = newEnergy;
        infinite = newInfinite;
    }

    public static float energy() {
        return energy;
    }

    public static boolean infinite() {
        return infinite;
    }

    public static void clear() {
        energy = 100f;
        infinite = false;
    }
}
