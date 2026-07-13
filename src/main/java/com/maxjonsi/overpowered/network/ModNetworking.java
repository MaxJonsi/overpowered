package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.server.ServerAbilityHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(AbilityActionPayload.TYPE, AbilityActionPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        ServerAbilityHandler.handleAction(player, payload.action());
                    }
                }));

        registrar.playToClient(VoidStatePayload.TYPE, VoidStatePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientPayloadHandler.handleVoidState(payload)));
    }
}
