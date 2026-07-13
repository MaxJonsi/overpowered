package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.DomainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class DomainRenderer extends EntityRenderer<DomainEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/domain_space.png");
    private static final ResourceLocation BLACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/void_player.png");
    private static final int STACKS = 16;
    private static final int SLICES = 32;
    private static final float BLACK_HOLE_DISTANCE = 12f;
    private static final float BLACK_HOLE_HEIGHT = 5f;

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
        float progress = Math.min(1f, (entity.tickCount + partialTick) / 25f);
        float radius = (float) DomainEntity.RADIUS * progress;
        if (radius < 0.5f) return;

        poseStack.pushPose();
        renderBlackHole(entity, partialTick, poseStack, bufferSource, progress);
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

    private static void renderBlackHole(DomainEntity entity, float partialTick, PoseStack poseStack,
                                        MultiBufferSource bufferSource, float progress) {
        float reveal = Mth.clamp(progress * 1.4f, 0f, 1f);
        Vector3f cameraLook = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        float horizontalLength = Mth.sqrt(cameraLook.x() * cameraLook.x() + cameraLook.z() * cameraLook.z());
        float lookX = horizontalLength > 0.001f ? cameraLook.x() / horizontalLength : 0f;
        float lookZ = horizontalLength > 0.001f ? cameraLook.z() / horizontalLength : 1f;

        poseStack.pushPose();
        poseStack.translate(lookX * BLACK_HOLE_DISTANCE, BLACK_HOLE_HEIGHT,
                lookZ * BLACK_HOLE_DISTANCE);
        poseStack.scale(reveal, reveal, reveal);

        VertexConsumer black = bufferSource.getBuffer(RenderType.entitySolid(BLACK_TEXTURE));
        PoseStack.Pose blackPose = poseStack.last();
        int blackStacks = 12;
        int blackSlices = 24;
        for (int i = 0; i < blackStacks; i++) {
            float theta0 = (float) Math.PI * i / blackStacks;
            float theta1 = (float) Math.PI * (i + 1) / blackStacks;
            for (int j = 0; j < blackSlices; j++) {
                float phi0 = (float) (2 * Math.PI) * j / blackSlices;
                float phi1 = (float) (2 * Math.PI) * (j + 1) / blackSlices;
                quad(black, blackPose, 4f, theta0, theta1, phi0, phi1,
                        (float) j / blackSlices, (float) (j + 1) / blackSlices,
                        (float) i / blackStacks, (float) (i + 1) / blackStacks,
                        LightTexture.FULL_BRIGHT, false);
            }
        }

        float cameraFacingYaw = (float) (Mth.atan2(-lookX, -lookZ) * Mth.RAD_TO_DEG);
        poseStack.mulPose(Axis.YP.rotationDegrees(cameraFacingYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(82f));
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 2.4f));
        VertexConsumer ring = bufferSource.getBuffer(RenderType.lightning());
        torus(ring, poseStack.last(), 6f, 0.42f, 48, 8);
        poseStack.popPose();
    }

    private static void torus(VertexConsumer consumer, PoseStack.Pose pose, float majorRadius,
                              float tubeRadius, int segments, int tubeSegments) {
        for (int i = 0; i < segments; i++) {
            float u0 = (float) (2 * Math.PI) * i / segments;
            float u1 = (float) (2 * Math.PI) * (i + 1) / segments;
            for (int j = 0; j < tubeSegments; j++) {
                float v0 = (float) (2 * Math.PI) * j / tubeSegments;
                float v1 = (float) (2 * Math.PI) * (j + 1) / tubeSegments;
                int blue = 210 + (i + j) % 3 * 20;
                torusVertex(consumer, pose, majorRadius, tubeRadius, u0, v0, 235, 250, 255, 235);
                torusVertex(consumer, pose, majorRadius, tubeRadius, u1, v0, 210, blue, 255, 220);
                torusVertex(consumer, pose, majorRadius, tubeRadius, u1, v1, 235, 250, 255, 235);
                torusVertex(consumer, pose, majorRadius, tubeRadius, u0, v1, 185, 225, 255, 220);
            }
        }
    }

    private static void torusVertex(VertexConsumer consumer, PoseStack.Pose pose, float majorRadius,
                                    float tubeRadius, float u, float v,
                                    int red, int green, int blue, int alpha) {
        float ring = majorRadius + tubeRadius * Mth.cos(v);
        float x = ring * Mth.cos(u);
        float y = tubeRadius * Mth.sin(v);
        float z = ring * Mth.sin(u);
        consumer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
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
