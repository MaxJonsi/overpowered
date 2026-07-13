package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.Overpowered;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VoidStatePayload(int entityId, boolean active) implements CustomPacketPayload {
    public static final Type<VoidStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "void_state"));

    public static final StreamCodec<ByteBuf, VoidStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoidStatePayload::entityId,
            ByteBufCodecs.BOOL, VoidStatePayload::active,
            VoidStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
