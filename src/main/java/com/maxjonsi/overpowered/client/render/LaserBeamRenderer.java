package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.item.LaserBeamHelper;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class LaserBeamRenderer {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static boolean initialized;

    private LaserBeamRenderer() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldRenderEvents.AFTER_ENTITIES.register(LaserBeamRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MultiBufferSource buffers = context.consumers();
        if (buffers == null) return;

        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.camera().getPosition();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));

        for (Player player : context.world().players()) {
            if (!LaserBeamHelper.isFiring(player)) continue;

            Vec3 eye = player.getEyePosition(partialTick);
            Vec3 direction = player.getViewVector(partialTick).normalize();
            LaserBeamHelper.Trace trace = LaserBeamHelper.trace(context.world(), player, eye, direction);
            Vec3 start = muzzlePosition(player, eye, direction);
            Vec3 end = trace.end();
            if (end.distanceToSqr(start) < 1.0E-4) continue;

            drawBeamLayer(consumer, start, end, camera, 0.25, 112, 48, 255, 105);
            drawBeamLayer(consumer, start, end, camera, 0.15, 68, 146, 255, 165);
            drawBeamLayer(consumer, start, end, camera, 0.08, 255, 255, 255, 255);
        }
    }

    private static Vec3 muzzlePosition(Player player, Vec3 eye, Vec3 direction) {
        Vec3 right = direction.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-8) right = new Vec3(1, 0, 0);
        right = right.normalize();

        boolean rightHand = player.getMainArm() == HumanoidArm.RIGHT;
        if (player.getUsedItemHand() == InteractionHand.OFF_HAND) rightHand = !rightHand;
        double sideOffset = rightHand ? 0.34 : -0.34;
        return eye.add(direction.scale(0.72)).add(right.scale(sideOffset)).add(0, -0.28, 0);
    }

    private static void drawBeamLayer(VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 camera,
                                      double halfWidth, int red, int green, int blue, int alpha) {
        Vec3 axis = end.subtract(start).normalize();
        Vec3 midpoint = start.add(end).scale(0.5);
        Vec3 side = camera.subtract(midpoint).cross(axis);
        if (side.lengthSqr() < 1.0E-8) {
            Vec3 fallback = Math.abs(axis.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            side = fallback.cross(axis);
        }
        side = side.normalize().scale(halfWidth);
        Vec3 normal = axis.cross(side).normalize();

        Vec3 startMinus = start.subtract(side).subtract(camera);
        Vec3 endMinus = end.subtract(side).subtract(camera);
        Vec3 endPlus = end.add(side).subtract(camera);
        Vec3 startPlus = start.add(side).subtract(camera);

        vertex(consumer, startMinus, 0, 0, normal, red, green, blue, alpha);
        vertex(consumer, endMinus, 0, 1, normal, red, green, blue, alpha);
        vertex(consumer, endPlus, 1, 1, normal, red, green, blue, alpha);
        vertex(consumer, startPlus, 1, 0, normal, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Vec3 position, float u, float v, Vec3 normal,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex((float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
