package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.entity.JudgementCutEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class JudgementCutRenderer extends EntityRenderer<JudgementCutEntity> {
    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("textures/misc/shadow.png");
    private static final int LIFETIME = 24;
    private static final int SEGMENTS = 18;

    public JudgementCutRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(JudgementCutEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(JudgementCutEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        if (age >= LIFETIME) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        int arcCount = 8 + Math.floorMod(entity.getId(), 5);
        float fadeOut = Mth.clamp((LIFETIME - age) / 5f, 0f, 1f);

        for (int i = 0; i < arcCount; i++) {
            long seed = ((long) entity.getId() << 32) ^ i * 0x9E3779B97F4A7C15L;
            int group = i % 3;
            float groupAge = age - group * 3f;
            float reveal = Mth.clamp(groupAge / 4f, 0f, 1f);
            if (reveal <= 0f) continue;

            float radius = 2.15f + unit(seed + 1) * 0.7f;
            float start = -170f + unit(seed + 2) * 130f;
            float span = 125f + unit(seed + 3) * 95f;
            int visibleSegments = Math.max(1, Mth.ceil(SEGMENTS * reveal));
            int alpha = Mth.floor(235f * fadeOut * Mth.clamp(groupAge / 2f, 0f, 1f));

            poseStack.pushPose();
            poseStack.translate(0, -0.35f + unit(seed + 4) * 0.7f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(unit(seed + 5) * 360f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-70f + unit(seed + 6) * 140f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(unit(seed + 7) * 360f));

            PoseStack.Pose pose = poseStack.last();
            arc(consumer, pose, radius, 0.18f, start, span, visibleSegments,
                    116, 78, 255, Math.max(20, alpha / 3));
            arc(consumer, pose, radius, 0.055f, start, span, visibleSegments,
                    245, 248, 255, alpha);
            poseStack.popPose();
        }
    }

    private static void arc(VertexConsumer consumer, PoseStack.Pose pose, float radius, float width,
                            float startDegrees, float spanDegrees, int segments,
                            int red, int green, int blue, int alpha) {
        for (int segment = 0; segment < segments; segment++) {
            float t0 = (float) segment / SEGMENTS;
            float t1 = (float) (segment + 1) / SEGMENTS;
            float a0 = (startDegrees + spanDegrees * t0) * Mth.DEG_TO_RAD;
            float a1 = (startDegrees + spanDegrees * t1) * Mth.DEG_TO_RAD;
            float edgeFade = Mth.sin((t0 + t1) * 0.5f * Mth.PI);
            int segmentAlpha = Math.max(1, Mth.floor(alpha * (0.35f + edgeFade * 0.65f)));

            vertex(consumer, pose, radius - width, a0, red, green, blue, segmentAlpha);
            vertex(consumer, pose, radius + width, a0, red, green, blue, segmentAlpha);
            vertex(consumer, pose, radius + width, a1, red, green, blue, segmentAlpha);
            vertex(consumer, pose, radius - width, a1, red, green, blue, segmentAlpha);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float radius, float angle,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, Mth.cos(angle) * radius, Mth.sin(angle) * radius, 0)
                .setColor(red, green, blue, alpha);
    }

    private static float unit(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 40) / (float) (1 << 24);
    }

    @Override
    public ResourceLocation getTextureLocation(JudgementCutEntity entity) {
        return EMPTY;
    }
}
