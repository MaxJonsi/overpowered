package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.JudgementCutEndEntity;
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

public class JudgementCutEndRenderer extends EntityRenderer<JudgementCutEndEntity> {
    private static final ResourceLocation SLASH = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/slash_plane.png");
    private static final ResourceLocation SLASH_DASHED = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/slash_dashed.png");
    private static final ResourceLocation SHARD = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/glass_shard.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/orb_glow.png");
    private static final ResourceLocation COSMOS = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/domain_space.png");

    private static final float DOME_RADIUS = 24f;
    private static final int STORM_SLOTS = 12;
    private static final float SLASH_LIFE = 6f;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    public JudgementCutEndRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(JudgementCutEndEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(JudgementCutEndEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float t = entity.tickCount + partialTick;
        SkyTearState.update(entity, partialTick);

        float endFade = Mth.clamp((130f - t) / 10f, 0f, 1f);

        // --- stage 1: the dome — a bubble of cut-open space ---
        float domeGrow = Math.min(1f, t / 20f);
        int domeAlpha = (int) (120 * domeGrow * endFade);
        if (domeAlpha > 3) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(t * 0.05f));
            VertexConsumer dome = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(COSMOS));
            RenderShapes.uvSphere(dome, poseStack.last(), DOME_RADIUS * domeGrow, 16, 32,
                    255, 255, 255, domeAlpha, true, LIGHT);
            poseStack.popPose();
        }

        // --- stage 2: the slash storm ---
        if (t >= 10f && t <= 110f) {
            VertexConsumer slash = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(SLASH));
            VertexConsumer dashed = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(SLASH_DASHED));
            float stormT = t - 10f;
            for (int s = 0; s < STORM_SLOTS; s++) {
                float slotPhase = stormT + s * (SLASH_LIFE / STORM_SLOTS);
                int gen = (int) (slotPhase / SLASH_LIFE);
                float age = slotPhase - gen * SLASH_LIFE;
                float env = age < 1f ? age : (SLASH_LIFE - age) / (SLASH_LIFE - 1f);
                int alpha = (int) (210 * env);
                if (alpha <= 4) continue;

                RandomSource rs = RandomSource.create(entity.getId() * 31L + s * 97L + gen * 7919L);
                float ox = (rs.nextFloat() * 2 - 1) * 18f;
                float oy = rs.nextFloat() * 8f;
                float oz = (rs.nextFloat() * 2 - 1) * 18f;
                Quaternionf orient = new Quaternionf().rotationXYZ(
                        rs.nextFloat() * Mth.TWO_PI, rs.nextFloat() * Mth.TWO_PI, rs.nextFloat() * Mth.TWO_PI);
                float len = 9f + rs.nextFloat() * 6f;

                poseStack.pushPose();
                poseStack.translate(ox, oy, oz);
                poseStack.mulPose(orient);
                RenderShapes.quad(poseStack, s % 3 == 0 ? dashed : slash, 0.35f, len,
                        255, 255, 255, alpha, LIGHT);
                poseStack.popPose();
            }
        }

        // --- stage 3: the climax flash ---
        float flashEnv = Mth.clamp(1f - Math.abs(t - 120f) / 4f, 0f, 1f);
        if (flashEnv > 0f) {
            Quaternionf camRot = this.entityRenderDispatcher.cameraOrientation();
            VertexConsumer glow = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(GLOW));
            poseStack.pushPose();
            poseStack.translate(0, 2, 0);
            poseStack.mulPose(camRot);
            RenderShapes.quad(poseStack, glow, 60f * flashEnv, 60f * flashEnv,
                    255, 255, 255, (int) (255 * flashEnv), LIGHT);
            poseStack.popPose();
        }

        // --- stage 4: the world falls apart ---
        if (t >= 118f) {
            float st = t - 118f;
            float shardAlpha = Mth.clamp(1f - st / 12f, 0f, 1f);
            if (shardAlpha > 0f) {
                VertexConsumer shard = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(SHARD));
                RandomSource rs = RandomSource.create(entity.getId() * 31L + 4242L);
                for (int i = 0; i < 40; i++) {
                    float dx = rs.nextFloat() * 2 - 1;
                    float dy = rs.nextFloat() * 1.4f;
                    float dz = rs.nextFloat() * 2 - 1;
                    float dlen = Math.max(0.05f, Mth.sqrt(dx * dx + dy * dy + dz * dz));
                    float dist = 4f + st * 1.2f + rs.nextFloat() * 10f;
                    float size = 0.8f + rs.nextFloat() * 1.2f;
                    float spin = rs.nextFloat() * 360f;

                    poseStack.pushPose();
                    poseStack.translate(dx / dlen * dist, dy / dlen * dist + 3f, dz / dlen * dist);
                    poseStack.mulPose(new Quaternionf().rotationXYZ(spin + st * 0.3f, spin * 0.6f, st * 0.5f));
                    RenderShapes.quad(poseStack, shard, size, size, 255, 255, 255, (int) (190 * shardAlpha), LIGHT);
                    poseStack.popPose();
                }
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(JudgementCutEndEntity entity) {
        return SLASH;
    }
}
