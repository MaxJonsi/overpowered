package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.entity.JudgementCutEndEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * Client-side driver for the Judgement Cut End sky tear.
 * The JCE entity renderer feeds it while the entity lives; afterwards the
 * tear decays on its own (the "aftermath" stage — the world stays wounded a while).
 */
public final class SkyTearState {
    private static float intensity;
    private static long seed = 1L;
    private static long lastUpdateTime = Long.MIN_VALUE;

    private SkyTearState() {}

    public static void update(JudgementCutEndEntity entity, float partialTick) {
        float t = entity.tickCount + partialTick;
        float target = t < 30f ? t / 30f : 1f;
        intensity = Math.max(intensity, Mth.clamp(target, 0f, 1f));
        seed = entity.getId() * 341873128712L + 132897987541L;
        lastUpdateTime = entity.level().getGameTime();
    }

    /** Effective intensity, with slow decay once the entity is gone (~12s aftermath). */
    public static float intensity(ClientLevel level) {
        if (level == null || intensity <= 0f) return 0f;
        long now = level.getGameTime();
        long elapsed = now - lastUpdateTime;
        if (elapsed < 0) { // time jumped backwards (dimension change, world reload)
            clear();
            return 0f;
        }
        if (elapsed > 2) {
            return Math.max(0f, intensity - (elapsed - 2) * 0.004f);
        }
        return intensity;
    }

    public static long seed() {
        return seed;
    }

    public static void clear() {
        intensity = 0f;
        lastUpdateTime = Long.MIN_VALUE;
    }
}
