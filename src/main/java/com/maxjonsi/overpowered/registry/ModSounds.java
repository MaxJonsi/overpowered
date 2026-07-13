package com.maxjonsi.overpowered.registry;

import com.maxjonsi.overpowered.Overpowered;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent YAMATO_WHOOSH = register("yamato.whoosh");
    public static final SoundEvent YAMATO_SLICE = register("yamato.slice");
    public static final SoundEvent YAMATO_SHEATH = register("yamato.sheath");
    public static final SoundEvent YAMATO_DASH = register("yamato.dash");
    public static final SoundEvent YAMATO_JUDGEMENT_END = register("yamato.judgement_end");
    public static final SoundEvent BURY_THE_LIGHT = register("yamato.bury_the_light");
    public static final SoundEvent LAUNCHER_FIRE = register("launcher.fire");
    public static final SoundEvent LAUNCHER_MODE = register("launcher.mode");
    public static final SoundEvent LAUNCHER_LOCK = register("launcher.lock");
    public static final SoundEvent LAUNCHER_LASER = register("launcher.laser");
    public static final SoundEvent LAUNCHER_NUKE = register("launcher.nuke");
    public static final SoundEvent LAUNCHER_SIREN = register("launcher.siren");
    public static final SoundEvent GOJO_BLUE = register("gojo.blue");
    public static final SoundEvent GOJO_RED = register("gojo.red");
    public static final SoundEvent GOJO_PURPLE = register("gojo.purple");
    public static final SoundEvent GOJO_DOMAIN = register("gojo.domain");
    public static final SoundEvent VOID_KILL = register("void.kill");
    public static final SoundEvent IMPACT_METAL = register("impact.metal");
    public static final SoundEvent MAGIC_EXPLOSION = register("magic.explosion");
    public static final SoundEvent MAGIC_FORCEFIELD = register("magic.forcefield");

    private static SoundEvent register(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Overpowered.id(name),
                SoundEvent.createVariableRangeEvent(Overpowered.id(name)));
    }

    public static void init() {
    }
}
