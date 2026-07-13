package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.DomainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DomainRenderer extends EntityRenderer<DomainEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/domain_space.png");
    private static final int STACKS = 16;
    private static final int SLICES = 32;

    public DomainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(DomainEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(DomainEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float radius = (float) DomainEntity.RADIUS * Math.min(1f, (entity.tickCount + partialTick) / 25f);
        if (radius < 0.5f) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 0.12f));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        int light = LightTexture.FULL_BRIGHT;

        for (int i = 0; i < STACKS; i++) {
            float theta0 = (float) Math.PI * i / STACKS;
            float theta1 = (float) Math.PI * (i + 1) / STACKS;
            for (int j = 0; j < SLICES; j++) {
                float phi0 = (float) (2 * Math.PI) * j / SLICES;
                float phi1 = (float) (2 * Math.PI) * (j + 1) / SLICES;

                float u0 = (float) j / SLICES;
                float u1 = (float) (j + 1) / SLICES;
                float v0 = (float) i / STACKS;
                float v1 = (float) (i + 1) / STACKS;

                quad(consumer, pose, radius, theta0, theta1, phi0, phi1, u0, u1, v0, v1, light, false);
                quad(consumer, pose, radius, theta0, theta1, phi0, phi1, u0, u1, v0, v1, light, true);
            }
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float r,
                             float theta0, float theta1, float phi0, float phi1,
                             float u0, float u1, float v0, float v1, int light, boolean inside) {
        if (inside) {
            vertex(consumer, pose, r, theta0, phi0, u0, v0, light);
            vertex(consumer, pose, r, theta0, phi1, u1, v0, light);
            vertex(consumer, pose, r, theta1, phi1, u1, v1, light);
            vertex(consumer, pose, r, theta1, phi0, u0, v1, light);
        } else {
            vertex(consumer, pose, r, theta1, phi0, u0, v1, light);
            vertex(consumer, pose, r, theta1, phi1, u1, v1, light);
            vertex(consumer, pose, r, theta0, phi1, u1, v0, light);
            vertex(consumer, pose, r, theta0, phi0, u0, v0, light);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float r,
                               float theta, float phi, float u, float v, int light) {
        float x = r * Mth.sin(theta) * Mth.cos(phi);
        float y = r * Mth.cos(theta);
        float z = r * Mth.sin(theta) * Mth.sin(phi);
        float len = Math.max(0.001f, Mth.sqrt(x * x + y * y + z * z));
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 235)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, -x / len, -y / len, -z / len);
    }

    @Override
    public ResourceLocation getTextureLocation(DomainEntity entity) {
        return TEXTURE;
    }
}
