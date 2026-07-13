package com.maxjonsi.overpowered.server;

/**
 * Mutable server-side energy for one player. Instances are owned by
 * {@link PlayerEnergyManager}; clients only ever receive snapshots.
 */
public final class PlayerEnergyState {
    private static final long NO_INFINITY = Long.MIN_VALUE;

    private int energy;
    private long infinityUntilTick = NO_INFINITY;

    PlayerEnergyState(int initialEnergy) {
        energy = initialEnergy;
    }

    public int energy() {
        return energy;
    }

    public boolean isInfinite(long gameTime) {
        return infinityUntilTick != NO_INFINITY && gameTime < infinityUntilTick;
    }

    public int remainingInfinityTicks(long gameTime) {
        if (!isInfinite(gameTime)) return 0;
        return (int) Math.min(Integer.MAX_VALUE, infinityUntilTick - gameTime);
    }

    boolean spend(int amount, long gameTime) {
        if (amount <= 0 || isInfinite(gameTime)) return true;
        if (energy < amount) return false;

        energy -= amount;
        return true;
    }

    boolean restore(int amount, int maximum) {
        if (amount <= 0 || energy >= maximum) return false;

        energy = Math.min(maximum, energy + amount);
        return true;
    }

    boolean expireInfinity(long gameTime) {
        if (infinityUntilTick == NO_INFINITY || gameTime < infinityUntilTick) return false;

        infinityUntilTick = NO_INFINITY;
        return true;
    }

    void grantInfinity(long gameTime, int durationTicks, int maximumEnergy) {
        energy = maximumEnergy;
        infinityUntilTick = Math.max(infinityUntilTick, gameTime + durationTicks);
    }

    void setEnergy(int value, int maximumEnergy) {
        energy = Math.max(0, Math.min(maximumEnergy, value));
    }
}
