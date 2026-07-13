package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.Overpowered;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EnergyPayload(float energy, boolean infinite) implements CustomPacketPayload {
    public static final Type<EnergyPayload> TYPE = new Type<>(Overpowered.id("energy"));
    public static final StreamCodec<FriendlyByteBuf, EnergyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, EnergyPayload::energy,
            ByteBufCodecs.BOOL, EnergyPayload::infinite,
            EnergyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
