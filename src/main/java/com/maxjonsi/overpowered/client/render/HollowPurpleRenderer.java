package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.HollowPurpleEntity;
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
import org.joml.Quaternionf;

public class HollowPurpleRenderer extends EntityRenderer<HollowPurpleEntity> {
    private static final ResourceLocation CHURN = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/purple_churn.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/orb_glow.png");
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    public HollowPurpleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(HollowPurpleEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(HollowPurpleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float t = entity.tickCount + partialTick;
        float master = Math.min(1f, t / 4f) * Mth.clamp((90f - t) / 5f, 0f, 1f);
        if (master <= 0f) return;

        Quaternionf camRot = this.entityRenderDispatcher.cameraOrientation();
        VertexConsumer glow = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(GLOW));

        // dark halo — light bending into the mass
        billboard(poseStack, glow, camRot, 2.7f, 15, 5, 25, (int) (170 * master));
        // violet corona
        billboard(poseStack, glow, camRot, 2.2f, 190, 80, 255, (int) (140 * master));

        // churning shells (the "boiling" surface from the reference)
        VertexConsumer churn = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(CHURN));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 4.5f));
        poseStack.mulPose(Axis.XP.rotationDegrees(t * 1.7f));
        RenderShapes.uvSphere(churn, poseStack.last(), 0.95f, 12, 24,
                60, 20, 90, (int) (240 * master), false, LIGHT);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-t * 3.1f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(t * 2.3f));
        RenderShapes.uvSphere(churn, poseStack.last(), 1.4f, 12, 24,
                170, 90, 255, (int) (110 * master), false, LIGHT);
        poseStack.popPose();

        // bright violet inner ring + dark pupil (the "eye" from the reference)
        billboard(poseStack, glow, camRot, 1.15f, 240, 180, 255, (int) (200 * master));
        billboard(poseStack, glow, camRot, 0.45f, 10, 0, 20, (int) (220 * master));

        // precessing thin ring
        VertexConsumer ringBuf = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(GLOW));
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(70f + t * 2.6f));
        poseStack.mulPose(Axis.YP.rotationDegrees(t * 6.8f));
        ring(poseStack, ringBuf, 1.9f, 0.07f, 235, 200, 255, (int) (180 * master));
        poseStack.popPose();
    }

    private static void billboard(PoseStack poseStack, VertexConsumer c, Quaternionf camRot,
                                  float size, int r, int g, int b, int a) {
        poseStack.pushPose();
        poseStack.mulPose(camRot);
        RenderShapes.quad(poseStack, c, size, size, r, g, b, a, LIGHT);
        poseStack.popPose();
    }

    /** Flat ring in the local XZ plane built from short quads (double-sided). */
    private static void ring(PoseStack poseStack, VertexConsumer c, float radius, float halfWidth,
                             int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();
        int segments = 40;
        for (int k = 0; k < segments; k++) {
            float a0 = Mth.TWO_PI * k / segments;
            float a1 = Mth.TWO_PI * (k + 1) / segments;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float ri = radius - halfWidth, ro = radius + halfWidth;
            // use the glow texture's center (bright) band
            RenderShapes.vtx(c, pose, c0 * ri, 0, s0 * ri, 0.48f, 0.48f, r, g, b, a, LIGHT);
            RenderShapes.vtx(c, pose, c0 * ro, 0, s0 * ro, 0.52f, 0.48f, r, g, b, a, LIGHT);
            RenderShapes.vtx(c, pose, c1 * ro, 0, s1 * ro, 0.52f, 0.52f, r, g, b, a, LIGHT);
            RenderShapes.vtx(c, pose, c1 * ri, 0, s1 * ri, 0.48f, 0.52f, r, g, b, a, LIGHT);
            RenderShapes.vtx(c, pose, c1 * ri, 0, s1 * ri, 0.48f, 0.52f, r, g, b, a, LIGHT);
            RenderShapes.vtx(c, pose, c1 * ro, 0, s1 * ro, 0.52f, 0.52f, r, g, b, a, LIGHT);
            RenderShapes.vtx(c, pose, c0 * ro, 0, s0 * ro, 0.52f, 0.48f, r, g, b, a, LIGHT);
            RenderShapes.vtx(c, pose, c0 * ri, 0, s0 * ri, 0.48f, 0.48f, r, g, b, a, LIGHT);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(HollowPurpleEntity entity) {
        return CHURN;
    }
}
