package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.BlueVortexEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.joml.Vector3f;

public class BlueVortexRenderer extends EntityRenderer<BlueVortexEntity> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/orb_glow.png");
    private static final ResourceLocation STREAK = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/blue_streak.png");
    private static final int STREAKS = 13;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    public BlueVortexRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(BlueVortexEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(BlueVortexEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float t = entity.tickCount + partialTick;
        float master = Math.min(1f, t / 5f) * Mth.clamp((50f - t) / 6f, 0f, 1f);
        if (master <= 0f) return;

        Quaternionf camRot = this.entityRenderDispatcher.cameraOrientation();

        // damage pulses every 10 ticks
        float sincePulse = t % 10f;
        float pulse = 1f + 0.3f * Math.max(0f, 1f - sincePulse / 3f);

        VertexConsumer glow = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(GLOW));

        // lensing shadow disc — light being swallowed
        billboard(poseStack, glow, camRot, 1.6f * pulse, 8, 10, 30, (int) (200 * master));
        // outer blue corona
        billboard(poseStack, glow, camRot, 1.0f * pulse, 90, 160, 255, (int) (235 * master));
        // blinding core
        billboard(poseStack, glow, camRot, 0.5f * pulse, 255, 255, 255, (int) (255 * master));

        // infall streaks — matter spiraling inward and dying at the core
        VertexConsumer streak = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(STREAK));
        RandomSource rand = RandomSource.create(entity.getId() * 9871L + 31L);
        for (int i = 0; i < STREAKS; i++) {
            // per-streak fixed orbit plane
            Vector3f axis = randomUnit(rand);
            Vector3f u = perpendicular(axis);
            Vector3f v = new Vector3f(axis).cross(u).normalize();
            float speed = 0.035f + rand.nextFloat() * 0.02f;

            float phase = ((t * speed) + (float) i / STREAKS) % 1f; // 0 = spawn, 1 = at core
            float radius = 3.4f * (1f - phase * phase);
            float ang = i * 2.39996f + t * 0.1f + phase * 4.5f;

            Vector3f pos = new Vector3f(u).mul(Mth.cos(ang) * radius)
                    .add(new Vector3f(v).mul(Mth.sin(ang) * radius));

            // motion direction: tangent blended with inward pull
            Vector3f tangent = new Vector3f(u).mul(-Mth.sin(ang)).add(new Vector3f(v).mul(Mth.cos(ang)));
            Vector3f inward = new Vector3f(pos).mul(-1f).normalize();
            Vector3f motion = tangent.mul(0.55f).add(inward.mul(0.85f)).normalize();

            float env = (float) Math.pow(Math.sin(Math.PI * phase), 0.7);
            int alpha = (int) (220 * env * master);
            if (alpha <= 4) continue;

            float len = 0.5f + 0.9f * (1f - phase);
            float wid = 0.07f + 0.09f * (1f - phase);

            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y + 0.2f, pos.z);
            // orient local +Y along motion
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), motion));
            RenderShapes.quad(poseStack, streak, wid, len, 170, 210, 255, alpha, LIGHT);
            poseStack.popPose();
        }
    }

    private static void billboard(PoseStack poseStack, VertexConsumer c, Quaternionf camRot,
                                  float size, int r, int g, int b, int a) {
        poseStack.pushPose();
        poseStack.translate(0, 0.2f, 0);
        poseStack.mulPose(camRot);
        RenderShapes.quad(poseStack, c, size, size, r, g, b, a, LIGHT);
        poseStack.popPose();
    }

    private static Vector3f randomUnit(RandomSource rand) {
        while (true) {
            Vector3f v = new Vector3f(rand.nextFloat() * 2 - 1, rand.nextFloat() * 2 - 1, rand.nextFloat() * 2 - 1);
            float len = v.length();
            if (len > 0.01f && len <= 1f) return v.div(len);
        }
    }

    private static Vector3f perpendicular(Vector3f v) {
        Vector3f ref = Math.abs(v.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        return new Vector3f(v).cross(ref).normalize();
    }

    @Override
    public ResourceLocation getTextureLocation(BlueVortexEntity entity) {
        return GLOW;
    }
}
