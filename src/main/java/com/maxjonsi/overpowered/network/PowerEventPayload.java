package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.Overpowered;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A server-timed visual/audio event. Gameplay is already committed before this
 * packet is emitted; clients only present the event and never approve it.
 */
public record PowerEventPayload(
        int sourceEntityId,
        int power,
        int ability,
        int phase,
        int durationTicks,
        int radius,
        int detail,
        long origin) implements CustomPacketPayload {

    public static final int POWER_YAMATO = 0;
    public static final int POWER_GOJO = 1;
    public static final int POWER_VOID = 2;
    public static final int POWER_DIO = 3;
    public static final int POWER_AIZEN = 4;
    public static final int POWER_SHADOW = 5;
    public static final int POWER_NUCLEAR = 6;
    public static final int POWER_INFINITY_CORE = 7;

    public static final int PHASE_PREPARE = 0;
    public static final int PHASE_RELEASE = 1;
    public static final int PHASE_AFTERMATH = 2;
    public static final int PHASE_STATE_START = 3;
    public static final int PHASE_STATE_END = 4;

    public static final Type<PowerEventPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "power_event"));

    public static final StreamCodec<ByteBuf, PowerEventPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PowerEventPayload decode(ByteBuf buffer) {
            return new PowerEventPayload(
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    buffer.readLong());
        }

        @Override
        public void encode(ByteBuf buffer, PowerEventPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buffer, payload.sourceEntityId());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.power());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.ability());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.phase());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.radius());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.detail());
            buffer.writeLong(payload.origin());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
