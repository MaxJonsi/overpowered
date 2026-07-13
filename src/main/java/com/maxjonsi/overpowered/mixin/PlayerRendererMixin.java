package com.maxjonsi.overpowered.mixin;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.client.ClientVoidState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Unique
    private static final ResourceLocation OVERPOWERED$VOID_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/void_player.png");

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void overpowered$voidTexture(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        if (ClientVoidState.isActive(player.getId())) {
            cir.setReturnValue(OVERPOWERED$VOID_TEXTURE);
        }
    }
}
