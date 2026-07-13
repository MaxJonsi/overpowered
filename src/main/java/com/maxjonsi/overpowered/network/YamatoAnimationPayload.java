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
    public static final int JUDGEMENT_CUT = 3;
    public static final int SHEATH = 4;
    public static final int UNLEASH = 5;
    public static final int DASH = 6;

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
