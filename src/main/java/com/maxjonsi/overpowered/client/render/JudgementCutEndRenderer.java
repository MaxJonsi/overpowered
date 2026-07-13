package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.entity.JudgementCutEndEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class JudgementCutEndRenderer extends EntityRenderer<JudgementCutEndEntity> {
    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("textures/misc/shadow.png");
    private static final float EFFECT_RADIUS = 24f;
    private static final int REFRESH_TICKS = 5;

    public JudgementCutEndRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(JudgementCutEndEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(JudgementCutEndEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        int epoch = entity.tickCount / REFRESH_TICKS;
        float refreshProgress = (entity.tickCount % REFRESH_TICKS + partialTick) / REFRESH_TICKS;
        float startFade = Mth.clamp((entity.tickCount + partialTick) / 8f, 0f, 1f);
        float endFade = Mth.clamp((130f - entity.tickCount - partialTick) / 16f, 0f, 1f);
        float pulse = 0.72f + Mth.sin(refreshProgress * Mth.PI) * 0.28f;
        int alpha = Mth.floor(235f * startFade * endFade * pulse);
        if (alpha <= 0) return;

        long epochSeed = ((long) entity.getId() << 32) ^ epoch * 0xD1342543DE82EF95L;
        int rayCount = 40 + Math.floorMod(mixToInt(entity.getId()), 31);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < rayCount; i++) {
            long seed = epochSeed ^ i * 0x9E3779B97F4A7C15L;
            float distance = Mth.sqrt(unit(seed + 1)) * EFFECT_RADIUS;
            float originAngle = unit(seed + 2) * Mth.TWO_PI;
            Vec3 origin = new Vec3(
                    Mth.cos(originAngle) * distance,
                    -1.5 + unit(seed + 3) * 10.0,
                    Mth.sin(originAngle) * distance);

            float yaw = unit(seed + 4) * Mth.TWO_PI;
            float vertical = -0.48f + unit(seed + 5) * 0.96f;
            float horizontal = Mth.sqrt(1f - vertical * vertical);
            Vec3 direction = new Vec3(
                    Mth.cos(yaw) * horizontal,
                    vertical,
                    Mth.sin(yaw) * horizontal);
            float length = 30f + unit(seed + 6) * 20f;
            Vec3 halfRay = direction.scale(length * 0.5);
            Vec3 from = origin.subtract(halfRay);
            Vec3 to = origin.add(halfRay);

            Vec3 side = direction.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0E-4) side = direction.cross(new Vec3(1, 0, 0));
            side = side.normalize();
            Vec3 secondSide = direction.cross(side).normalize();

            ribbon(consumer, pose, from, to, side, 0.15f,
                    74, 132, 255, Math.max(12, alpha / 3));
            ribbon(consumer, pose, from, to, secondSide, 0.15f,
                    88, 154, 255, Math.max(12, alpha / 3));
            ribbon(consumer, pose, from, to, side, 0.045f,
                    238, 249, 255, alpha);
            ribbon(consumer, pose, from, to, secondSide, 0.045f,
                    220, 241, 255, alpha);
        }
    }

    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 from, Vec3 to,
                               Vec3 side, float width, int red, int green, int blue, int alpha) {
        Vec3 offset = side.scale(width * 0.5);
        vertex(consumer, pose, from.subtract(offset), red, green, blue, alpha);
        vertex(consumer, pose, from.add(offset), red, green, blue, alpha);
        vertex(consumer, pose, to.add(offset), red, green, blue, alpha);
        vertex(consumer, pose, to.subtract(offset), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha);
    }

    private static int mixToInt(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        mixed *= 0x846CA68B;
        return mixed ^ mixed >>> 16;
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
    public ResourceLocation getTextureLocation(JudgementCutEndEntity entity) {
        return EMPTY;
    }
}
