package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.Overpowered;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AbilityActionPayload(int action) implements CustomPacketPayload {
    public static final int SWING = 0;
    public static final int SPECIAL = 1;
    public static final int MARK = 2;
    public static final int VOID_KILL = 3;
    public static final int VOID_TOGGLE = 4;
    public static final int ABILITY_ONE = 5;
    public static final int ABILITY_TWO = 6;
    public static final int ABILITY_THREE = 7;
    public static final int ABILITY_FOUR = 8;
    public static final int ABILITY_FIVE = 9;
    public static final int ULTIMATE = 10;

    public static final Type<AbilityActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "ability_action"));

    public static final StreamCodec<ByteBuf, AbilityActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityActionPayload::action,
            AbilityActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
