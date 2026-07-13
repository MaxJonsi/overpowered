package com.maxjonsi.overpowered.mixin;

import com.maxjonsi.overpowered.client.render.SkyTearRenderer;
import com.maxjonsi.overpowered.client.render.SkyTearState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(method = "renderSky", at = @At("TAIL"))
    private void overpowered$renderSkyTear(Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                           float partialTick, Camera camera, boolean isFoggy,
                                           Runnable skyFogSetup, CallbackInfo ci) {
        if (isFoggy || this.level == null) return;
        float intensity = SkyTearState.intensity(this.level);
        if (intensity <= 0.01f) return;
        SkyTearRenderer.render(frustumMatrix, partialTick, this.level, intensity);
    }
}
