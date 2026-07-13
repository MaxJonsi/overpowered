package com.maxjonsi.overpowered.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class NoopRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("textures/misc/shadow.png");

    public NoopRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return EMPTY;
    }
}
