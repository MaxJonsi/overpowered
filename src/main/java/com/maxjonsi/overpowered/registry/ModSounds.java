package com.maxjonsi.overpowered.registry;

import com.maxjonsi.overpowered.Overpowered;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Overpowered.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> YAMATO_WHOOSH = register("yamato.whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> YAMATO_SLICE = register("yamato.slice");
    public static final DeferredHolder<SoundEvent, SoundEvent> YAMATO_SHEATH = register("yamato.sheath");
    public static final DeferredHolder<SoundEvent, SoundEvent> YAMATO_DASH = register("yamato.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> YAMATO_JUDGEMENT_END = register("yamato.judgement_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> BURY_THE_LIGHT = register("yamato.bury_the_light");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCHER_FIRE = register("launcher.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCHER_MODE = register("launcher.mode");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCHER_LOCK = register("launcher.lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCHER_LASER = register("launcher.laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCHER_NUKE = register("launcher.nuke");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOJO_BLUE = register("gojo.blue");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOJO_RED = register("gojo.red");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOJO_PURPLE = register("gojo.purple");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOJO_DOMAIN = register("gojo.domain");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_KILL = register("void.kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT_METAL = register("impact.metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_EXPLOSION = register("magic.explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_FORCEFIELD = register("magic.forcefield");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, name)));
    }
}
