package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders the Judgement Cut End sky tear: the sky rips open along a great arc,
 * cosmos visible through the gap, edges burning gold (see assets/yamato/sky fall ref).
 * Called from LevelRendererMixin at the tail of renderSky.
 */
public final class SkyTearRenderer {
    private static final ResourceLocation COSMOS = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/domain_space.png");
    private static final ResourceLocation CRACK = ResourceLocation.fromNamespaceAndPath(Overpowered.MODID, "textures/entity/sky_crack.png");
    private static final int SEGMENTS = 28;
    private static final float RADIUS = 90f;

    private SkyTearRenderer() {}

    public static void render(Matrix4f frustumMatrix, float partialTick, ClientLevel level, float intensity) {
        long seed = SkyTearState.seed();
        RandomSource rand = RandomSource.create(seed);

        // tear arc: great circle segment across the sky, seeded orientation
        float yaw = rand.nextFloat() * Mth.TWO_PI;
        Vector3f start = new Vector3f(Mth.cos(yaw), 0.18f, Mth.sin(yaw)).normalize();
        Vector3f planeNormal = new Vector3f(start).cross(new Vector3f(0, 1, 0)).normalize();

        Vector3f[] centers = new Vector3f[SEGMENTS + 1];
        Vector3f[] lefts = new Vector3f[SEGMENTS + 1];
        Vector3f[] rights = new Vector3f[SEGMENTS + 1];
        float open = intensity * intensity; // tear widens as it powers up

        for (int i = 0; i <= SEGMENTS; i++) {
            float s = (float) i / SEGMENTS;
            float ang = (float) Math.toRadians(-78 + 156 * s);
            Vector3f c = new Quaternionf().fromAxisAngleRad(planeNormal, ang).transform(new Vector3f(start));
            centers[i] = c;

            RandomSource jag = RandomSource.create(seed + i * 7919L);
            float jagL = jag.nextFloat() * 0.045f;
            float jagR = jag.nextFloat() * 0.045f;
            float width = (0.02f + 0.14f * (float) Math.pow(Math.sin(Math.PI * s), 0.8)) * open;

            lefts[i] = new Vector3f(c).add(new Vector3f(planeNormal).mul(width + jagL * open)).normalize().mul(RADIUS);
            rights[i] = new Vector3f(c).sub(new Vector3f(planeNormal).mul(width + jagR * open)).normalize().mul(RADIUS);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        Tesselator tess = Tesselator.getInstance();

        // --- cosmos strip (the void behind the sky) ---
        RenderSystem.setShaderTexture(0, COSMOS);
        int cosmosAlpha = (int) (240 * intensity);
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < SEGMENTS; i++) {
            float u0 = (float) i / SEGMENTS * 3f;
            float u1 = (float) (i + 1) / SEGMENTS * 3f;
            buf.addVertex(frustumMatrix, lefts[i].x, lefts[i].y, lefts[i].z).setUv(u0, 0f).setColor(255, 255, 255, cosmosAlpha);
            buf.addVertex(frustumMatrix, lefts[i + 1].x, lefts[i + 1].y, lefts[i + 1].z).setUv(u1, 0f).setColor(255, 255, 255, cosmosAlpha);
            buf.addVertex(frustumMatrix, rights[i + 1].x, rights[i + 1].y, rights[i + 1].z).setUv(u1, 1f).setColor(255, 255, 255, cosmosAlpha);
            buf.addVertex(frustumMatrix, rights[i].x, rights[i].y, rights[i].z).setUv(u0, 1f).setColor(255, 255, 255, cosmosAlpha);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());

        // --- burning edges (additive gold fire) ---
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderTexture(0, CRACK);
        float time = (level.getGameTime() % 24000L) + partialTick;
        float scroll = time * 0.0025f;
        int edgeAlpha = (int) (255 * intensity);

        buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        drawEdge(buf, frustumMatrix, centers, lefts, planeNormal, 1f, scroll, edgeAlpha, seed);
        drawEdge(buf, frustumMatrix, centers, rights, planeNormal, -1f, scroll + 0.37f, edgeAlpha, seed + 555L);
        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }

    private static void drawEdge(BufferBuilder buf, Matrix4f mat, Vector3f[] centers, Vector3f[] edge,
                                 Vector3f planeNormal, float sideSign, float scroll, int alpha, long seed) {
        for (int i = 0; i < SEGMENTS; i++) {
            float s0 = (float) i / SEGMENTS;
            float s1 = (float) (i + 1) / SEGMENTS;
            RandomSource flick = RandomSource.create(seed + i * 131L);
            float edgeW = 0.05f + flick.nextFloat() * 0.03f;

            Vector3f out0 = new Vector3f(edge[i]).normalize()
                    .add(new Vector3f(planeNormal).mul(sideSign * edgeW)).normalize().mul(RADIUS);
            Vector3f out1 = new Vector3f(edge[i + 1]).normalize()
                    .add(new Vector3f(planeNormal).mul(sideSign * edgeW)).normalize().mul(RADIUS);

            float u0 = s0 * 2f + scroll;
            float u1 = s1 * 2f + scroll;
            // v: 0 at inner (tear side), 1 at outer — fire line sits mid-strip
            buf.addVertex(mat, edge[i].x, edge[i].y, edge[i].z).setUv(u0, 0f).setColor(255, 205, 100, alpha);
            buf.addVertex(mat, edge[i + 1].x, edge[i + 1].y, edge[i + 1].z).setUv(u1, 0f).setColor(255, 205, 100, alpha);
            buf.addVertex(mat, out1.x, out1.y, out1.z).setUv(u1, 1f).setColor(255, 205, 100, alpha);
            buf.addVertex(mat, out0.x, out0.y, out0.z).setUv(u0, 1f).setColor(255, 205, 100, alpha);
        }
    }
}
