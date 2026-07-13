package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.Overpowered;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client snapshot for the future power HUD. The client receives its
 * own energy only; gameplay never trusts a value supplied by the client.
 */
public record EnergyStatePayload(int energy, boolean infinite, int infinityTicks) implements CustomPacketPayload {
    public static final Type<EnergyStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "energy_state"));

    public static final StreamCodec<ByteBuf, EnergyStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnergyStatePayload::energy,
            ByteBufCodecs.BOOL, EnergyStatePayload::infinite,
            ByteBufCodecs.VAR_INT, EnergyStatePayload::infinityTicks,
            EnergyStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
