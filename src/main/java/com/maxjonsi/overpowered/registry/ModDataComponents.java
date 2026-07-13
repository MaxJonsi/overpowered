package com.maxjonsi.overpowered.registry;

import com.maxjonsi.overpowered.Overpowered;
import com.mojang.serialization.Codec;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public class ModDataComponents {
    public static final DataComponentType<Integer> MODE = register("mode",
            DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DataComponentType<Integer> TECHNIQUE = register("technique",
            DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DataComponentType<UUID> TARGET = register("target",
            DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Overpowered.id(name), type);
    }

    public static void init() {
    }
}
