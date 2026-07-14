package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.Overpowered;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record YamatoAnimationPayload(int entityId, int animation) implements CustomPacketPayload {
    public static final int SLASH_1 = 0;
    public static final int SLASH_2 = 1;
    public static final int SLASH_3 = 2;
    public static final int SLASH_4 = 3;
    public static final int SLASH_5 = 4;
    public static final int JUDGEMENT_CUT = 5;
    public static final int SHEATH = 6;
    public static final int UNLEASH = 7;
    public static final int DASH = 8;
    public static final int COUNTER = 9;
    public static final int WORLD_SPLIT = 10;

    public static final Type<YamatoAnimationPayload> TYPE = new Type<>(Overpowered.id("yamato_animation"));
    public static final StreamCodec<ByteBuf, YamatoAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, YamatoAnimationPayload::entityId,
            ByteBufCodecs.VAR_INT, YamatoAnimationPayload::animation,
            YamatoAnimationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
