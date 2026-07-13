package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.item.StoneMaskItem;
import com.maxjonsi.overpowered.network.EnergyPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative energy: 0-100, gradual regen, no long cooldowns (per MASTER_DESIGN).
 * Infinity Core grants a timed infinite-energy window.
 */
public final class EnergyService {
    public static final float MAX = 100f;
    private static final float REGEN_PER_TICK = 0.25f; // full bar in ~20s

    private static final Map<UUID, Float> ENERGY = new HashMap<>();
    private static final Map<UUID, Long> INFINITY_UNTIL = new HashMap<>();

    private EnergyService() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                float e = ENERGY.getOrDefault(player.getUUID(), MAX);
                if (e < MAX) {
                    ENERGY.put(player.getUUID(), Math.min(MAX, e + REGEN_PER_TICK));
                }
                if (player.tickCount % 10 == 0) {
                    sync(player);
                }
            }
            StoneMaskItem.tickTimeStop(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ENERGY.remove(handler.player.getUUID());
            INFINITY_UNTIL.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ENERGY.clear();
            INFINITY_UNTIL.clear();
        });
    }

    public static boolean isInfinite(ServerPlayer player) {
        Long until = INFINITY_UNTIL.get(player.getUUID());
        return until != null && player.server.overworld().getGameTime() < until;
    }

    /** Consume energy for an ability. Shows an actionbar warning and returns false when too low. */
    public static boolean tryUse(ServerPlayer player, float cost) {
        if (cost <= 0 || player.isCreative() || isInfinite(player)) return true;
        float e = ENERGY.getOrDefault(player.getUUID(), MAX);
        if (e < cost) {
            player.displayClientMessage(Component.translatable("message.overpowered.energy.low"), true);
            return false;
        }
        ENERGY.put(player.getUUID(), e - cost);
        sync(player);
        return true;
    }

    public static void activateInfinity(ServerPlayer player, int ticks) {
        INFINITY_UNTIL.put(player.getUUID(), player.server.overworld().getGameTime() + ticks);
        ENERGY.put(player.getUUID(), MAX);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ServerPlayNetworking.send(player,
                new EnergyPayload(ENERGY.getOrDefault(player.getUUID(), MAX), isInfinite(player)));
    }
}
