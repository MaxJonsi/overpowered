package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.network.PowerEventPayload;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class PowerEventDispatcher {
    private PowerEventDispatcher() {
    }

    public static void broadcast(
            ServerPlayer source,
            int power,
            int ability,
            int phase,
            int durationTicks,
            int radius) {
        broadcastDetailedAt(source, source.position(), power, ability, phase, durationTicks, radius, 0);
    }

    public static void broadcastDetailed(
            ServerPlayer source,
            int power,
            int ability,
            int phase,
            int durationTicks,
            int radius,
            int detail) {
        broadcastDetailedAt(source, source.position(), power, ability, phase, durationTicks, radius, detail);
    }

    public static void broadcastAt(
            ServerPlayer source,
            Vec3 origin,
            int power,
            int ability,
            int phase,
            int durationTicks,
            int radius) {
        broadcastDetailedAt(source, origin, power, ability, phase, durationTicks, radius, 0);
    }

    public static void broadcastDetailedAt(
            ServerPlayer source,
            Vec3 origin,
            int power,
            int ability,
            int phase,
            int durationTicks,
            int radius,
            int detail) {
        PowerEventPayload payload = new PowerEventPayload(
                source.getId(), power, ability, phase, durationTicks, radius, detail,
                net.minecraft.core.BlockPos.containing(origin).asLong());
        Set<ServerPlayer> recipients = new HashSet<>(PlayerLookup.tracking(source));
        recipients.addAll(PlayerLookup.around(
                source.serverLevel(), origin, Math.max(64, radius * 2.0)));
        recipients.add(source);
        recipients.forEach(player -> sendIfSupported(player, payload));
    }

    public static void sendIfSupported(ServerPlayer player, PowerEventPayload payload) {
        if (ServerPlayNetworking.canSend(player, PowerEventPayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
