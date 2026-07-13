package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.JudgementCutEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

public class JudgementCutRenderer extends EntityRenderer<JudgementCutEntity> {
    private static final ResourceLocation SLASH = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/slash_plane.png");
    private static final ResourceLocation SHARD = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/glass_shard.png");
    private static final ResourceLocation CHURN = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/purple_churn.png");
    private static final int[] BURSTS = {1, 8, 16};
    private static final int SLASHES_PER_BURST = 5;
    private static final int SHARDS = 12;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    public JudgementCutRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(JudgementCutEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(JudgementCutEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float t = entity.tickCount + partialTick;

        // space "tensing": faint violet sphere, grows fast, dies before the shatter
        float sphereAlpha = Mth.clamp((18f - t) / 3f, 0f, 1f) * Math.min(1f, t / 3f);
        if (sphereAlpha > 0f) {
            float radius = 2.5f * Math.min(1f, t / 4f);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 1.5f));
            VertexConsumer sphere = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(CHURN));
            RenderShapes.uvSphere(sphere, poseStack.last(), radius, 12, 24,
                    200, 170, 255, (int) (45 * sphereAlpha), false, LIGHT);
            RenderShapes.uvSphere(sphere, poseStack.last(), radius, 12, 24,
                    200, 170, 255, (int) (45 * sphereAlpha), true, LIGHT);
            poseStack.popPose();
        }

        // slash planes, synced to the damage ticks
        RandomSource rand = RandomSource.create(entity.getId() * 31L + 7L);
        VertexConsumer slash = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(SLASH));
        for (int burst : BURSTS) {
            for (int j = 0; j < SLASHES_PER_BURST; j++) {
                Quaternionf orient = new Quaternionf().rotationXYZ(
                        rand.nextFloat() * Mth.TWO_PI, rand.nextFloat() * Mth.TWO_PI, rand.nextFloat() * Mth.TWO_PI);
                float len = 2.6f + rand.nextFloat() * 1.0f;
                float age = t - burst;
                if (age < 0f || age > 4f) continue;
                float env = age < 1f ? age : (4f - age) / 3f;
                int alpha = (int) (235 * env);
                if (alpha <= 4) continue;

                poseStack.pushPose();
                poseStack.mulPose(orient);
                RenderShapes.quad(poseStack, slash, 0.24f, len, 255, 255, 255, alpha, LIGHT);
                poseStack.popPose();
            }
        }

        // glass shatter — the sphere breaks apart
        if (t >= 17f && t <= 24f) {
            float st = t - 17f;
            float shardAlpha = 1f - st / 7f;
            VertexConsumer shard = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(SHARD));
            RandomSource shardRand = RandomSource.create(entity.getId() * 31L + 99L);
            for (int i = 0; i < SHARDS; i++) {
                float dx = shardRand.nextFloat() * 2 - 1;
                float dy = shardRand.nextFloat() * 2 - 1;
                float dz = shardRand.nextFloat() * 2 - 1;
                float dlen = Math.max(0.05f, Mth.sqrt(dx * dx + dy * dy + dz * dz));
                float dist = 1.5f + st * 0.5f;
                float size = 0.35f + shardRand.nextFloat() * 0.3f;
                float spin = shardRand.nextFloat() * 360f;

                poseStack.pushPose();
                poseStack.translate(dx / dlen * dist, dy / dlen * dist, dz / dlen * dist);
                poseStack.mulPose(new Quaternionf().rotationXYZ(spin + st * 0.4f, spin * 0.7f, st * 0.6f));
                RenderShapes.quad(poseStack, shard, size, size, 255, 255, 255, (int) (200 * shardAlpha), LIGHT);
                poseStack.popPose();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(JudgementCutEntity entity) {
        return SLASH;
    }
}
