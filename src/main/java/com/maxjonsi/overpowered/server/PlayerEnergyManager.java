package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.network.EnergyStatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central server-authoritative energy service. Energy is transient combat state:
 * it starts full, survives respawns and dimension changes through the player UUID,
 * and safely resets to full when a server restarts.
 */
public final class PlayerEnergyManager {
    public static final int MAX_ENERGY = 100;
    public static final int REGEN_INTERVAL_TICKS = 10;
    public static final int REGEN_PER_INTERVAL = 1;
    public static final int INFINITY_DURATION_TICKS = 20 * 60 * 5;

    private static final Map<UUID, PlayerEnergyState> STATES = new HashMap<>();
    private static final Map<UUID, EnergySnapshot> LAST_SENT = new HashMap<>();

    private PlayerEnergyManager() {
    }

    public static PlayerEnergyState stateFor(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerEnergyState(MAX_ENERGY));
    }

    public static int getEnergy(ServerPlayer player) {
        return stateFor(player).energy();
    }

    public static boolean isInfinite(ServerPlayer player) {
        return stateFor(player).isInfinite(player.serverLevel().getGameTime());
    }

    public static boolean tryConsume(ServerPlayer player, int amount) {
        boolean consumed = stateFor(player).spend(amount, player.serverLevel().getGameTime());
        if (consumed && amount > 0) sync(player, false);
        return consumed;
    }

    public static boolean tryConsumeOrNotify(ServerPlayer player, int amount) {
        if (tryConsume(player, amount)) return true;

        player.displayClientMessage(Component.translatable("message.overpowered.energy.insufficient"), true);
        return false;
    }

    public static boolean canConsume(ServerPlayer player, int amount) {
        PlayerEnergyState state = stateFor(player);
        return state.isInfinite(player.serverLevel().getGameTime()) || state.energy() >= amount;
    }

    public static void grantInfinity(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        stateFor(player).grantInfinity(gameTime, INFINITY_DURATION_TICKS, MAX_ENERGY);
        sync(player, true);
    }

    public static void setEnergy(ServerPlayer player, int energy) {
        stateFor(player).setEnergy(energy, MAX_ENERGY);
        sync(player, false);
    }

    /**
     * Runs once per server tick for each connected player. Returns whether a HUD
     * snapshot changed, ready for the client HUD integration.
     */
    public static boolean tick(ServerPlayer player) {
        PlayerEnergyState state = stateFor(player);
        long gameTime = player.serverLevel().getGameTime();
        boolean changed = state.expireInfinity(gameTime);

        if (!state.isInfinite(gameTime) && player.tickCount % REGEN_INTERVAL_TICKS == 0) {
            changed |= state.restore(REGEN_PER_INTERVAL, MAX_ENERGY);
        }
        if (state.isInfinite(gameTime) && player.tickCount % 20 == 0) {
            sync(player, false);
        }
        if (changed) sync(player, false);
        return changed;
    }

    public static EnergySnapshot snapshot(ServerPlayer player) {
        PlayerEnergyState state = stateFor(player);
        long gameTime = player.serverLevel().getGameTime();
        return new EnergySnapshot(
                state.energy(),
                state.isInfinite(gameTime),
                state.remainingInfinityTicks(gameTime));
    }

    public static void sync(ServerPlayer player, boolean force) {
        EnergySnapshot snapshot = snapshot(player);
        if (!force && snapshot.equals(LAST_SENT.get(player.getUUID()))) return;
        if (!ServerPlayNetworking.canSend(player, EnergyStatePayload.TYPE)) return;

        ServerPlayNetworking.send(player, new EnergyStatePayload(
                snapshot.energy(), snapshot.infinite(), snapshot.infinityTicks()));
        LAST_SENT.put(player.getUUID(), snapshot);
    }

    public static void forgetClient(UUID playerId) {
        LAST_SENT.remove(playerId);
    }

    public static void clear() {
        STATES.clear();
        LAST_SENT.clear();
    }

    public record EnergySnapshot(int energy, boolean infinite, int infinityTicks) {
    }
}
