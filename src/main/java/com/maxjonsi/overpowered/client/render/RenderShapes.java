package com.maxjonsi.overpowered.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public final class RenderShapes {
    private RenderShapes() {}

    public static void vtx(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
                           float u, float v, int r, int g, int b, int a, int light) {
        c.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0f, 1f, 0f);
    }

    /**
     * Double-sided quad in the local XY plane, centered on the pose origin.
     * Rotate/translate the pose stack before calling.
     */
    public static void quad(PoseStack poseStack, VertexConsumer c, float halfW, float halfH,
                            int r, int g, int b, int a, int light) {
        PoseStack.Pose pose = poseStack.last();
        vtx(c, pose, -halfW, -halfH, 0, 0, 1, r, g, b, a, light);
        vtx(c, pose, halfW, -halfH, 0, 1, 1, r, g, b, a, light);
        vtx(c, pose, halfW, halfH, 0, 1, 0, r, g, b, a, light);
        vtx(c, pose, -halfW, halfH, 0, 0, 0, r, g, b, a, light);
        vtx(c, pose, -halfW, halfH, 0, 0, 0, r, g, b, a, light);
        vtx(c, pose, halfW, halfH, 0, 1, 0, r, g, b, a, light);
        vtx(c, pose, halfW, -halfH, 0, 1, 1, r, g, b, a, light);
        vtx(c, pose, -halfW, -halfH, 0, 0, 1, r, g, b, a, light);
    }

    /** UV sphere centered on the pose origin. Set inside=true for interior faces. */
    public static void uvSphere(VertexConsumer consumer, PoseStack.Pose pose, float radius,
                                int stacks, int slices, int r, int g, int b, int a,
                                boolean inside, int light) {
        for (int i = 0; i < stacks; i++) {
            float theta0 = (float) Math.PI * i / stacks;
            float theta1 = (float) Math.PI * (i + 1) / stacks;
            for (int j = 0; j < slices; j++) {
                float phi0 = (float) (2 * Math.PI) * j / slices;
                float phi1 = (float) (2 * Math.PI) * (j + 1) / slices;
                float u0 = (float) j / slices;
                float u1 = (float) (j + 1) / slices;
                float v0 = (float) i / stacks;
                float v1 = (float) (i + 1) / stacks;
                if (inside) {
                    sphereVtx(consumer, pose, radius, theta0, phi0, u0, v0, r, g, b, a, light);
                    sphereVtx(consumer, pose, radius, theta0, phi1, u1, v0, r, g, b, a, light);
                    sphereVtx(consumer, pose, radius, theta1, phi1, u1, v1, r, g, b, a, light);
                    sphereVtx(consumer, pose, radius, theta1, phi0, u0, v1, r, g, b, a, light);
                } else {
                    sphereVtx(consumer, pose, radius, theta1, phi0, u0, v1, r, g, b, a, light);
                    sphereVtx(consumer, pose, radius, theta1, phi1, u1, v1, r, g, b, a, light);
                    sphereVtx(consumer, pose, radius, theta0, phi1, u1, v0, r, g, b, a, light);
                    sphereVtx(consumer, pose, radius, theta0, phi0, u0, v0, r, g, b, a, light);
                }
            }
        }
    }

    private static void sphereVtx(VertexConsumer consumer, PoseStack.Pose pose, float radius,
                                  float theta, float phi, float u, float v,
                                  int r, int g, int b, int a, int light) {
        float x = radius * Mth.sin(theta) * Mth.cos(phi);
        float y = radius * Mth.cos(theta);
        float z = radius * Mth.sin(theta) * Mth.sin(phi);
        float len = Math.max(0.001f, Mth.sqrt(x * x + y * y + z * z));
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, x / len, y / len, z / len);
    }
}
