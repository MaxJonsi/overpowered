package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.server.ServerAbilityHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModNetworking {
    public static void init() {
        PayloadTypeRegistry.playC2S().register(AbilityActionPayload.TYPE, AbilityActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(VoidStatePayload.TYPE, VoidStatePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AbilityActionPayload.TYPE, (payload, context) ->
                ServerAbilityHandler.handleAction(context.player(), payload.action()));
    }
}
