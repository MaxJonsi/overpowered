package com.maxjonsi.overpowered.client;

import com.maxjonsi.overpowered.network.EnergyStatePayload;

/** Client-side mirror of the server-authoritative energy snapshot. */
public final class ClientEnergyState {
    public static final int MAX_ENERGY = 100;

    private static int energy = MAX_ENERGY;
    private static boolean infinite;
    private static int infinityTicks;

    private ClientEnergyState() {
    }

    public static void accept(EnergyStatePayload payload) {
        energy = Math.clamp(payload.energy(), 0, MAX_ENERGY);
        infinite = payload.infinite();
        infinityTicks = Math.max(0, payload.infinityTicks());
    }

    public static void tick() {
        if (infinite && infinityTicks > 0) infinityTicks--;
        if (infinite && infinityTicks == 0) infinite = false;
    }

    public static int energy() {
        return infinite ? MAX_ENERGY : energy;
    }

    public static boolean infinite() {
        return infinite;
    }

    public static int infinityTicks() {
        return infinityTicks;
    }

    public static void clear() {
        energy = MAX_ENERGY;
        infinite = false;
        infinityTicks = 0;
    }
}
