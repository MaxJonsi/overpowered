package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.client.ClientPowerEventState;
import com.maxjonsi.overpowered.network.PowerEventPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shared presentation layer for server-timed power events. It deliberately owns
 * no gameplay decisions: the server has already committed every event it sees.
 */
public final class PowerEventPresentation {
    private static boolean initialized;

    private PowerEventPresentation() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        HudRenderCallback.EVENT.register(PowerEventPresentation::renderOverlay);
        WorldRenderEvents.AFTER_ENTITIES.register(PowerEventPresentation::renderIllusionClones);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPowerEventState.tick();
            tickIllusionParticles(client);
            tickInfinityCoreAura(client);
        });
    }

    private static void tickInfinityCoreAura(Minecraft client) {
        if (client.level == null || client.level.getGameTime() % 3 != 0) return;
        ClientPowerEventState.ActiveEvent event = ClientPowerEventState.strongest(
                PowerEventPayload.POWER_INFINITY_CORE, 1);
        if (event == null) return;
        Entity source = client.level.getEntity(event.payload().sourceEntityId());
        if (source == null) return;
        double angle = client.level.getGameTime() * 0.18;
        client.level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.86f, 0.35f), 1.0f),
                source.getX() + Math.cos(angle) * 0.7,
                source.getY() + 0.35 + client.level.random.nextDouble() * 1.4,
                source.getZ() + Math.sin(angle) * 0.7, 0, 0.02, 0);
        client.level.addParticle(ParticleTypes.END_ROD,
                source.getX() - Math.cos(angle) * 0.7,
                source.getY() + 0.3 + client.level.random.nextDouble() * 1.5,
                source.getZ() - Math.sin(angle) * 0.7, 0, 0.01, 0);
    }

    public static void receive(PowerEventPayload payload) {
        ClientPowerEventState.accept(payload);
        spawnArrivalBurst(payload);
    }

    private static void spawnArrivalBurst(PowerEventPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockPos blockPos = BlockPos.of(payload.origin());
        Vec3 center = Vec3.atCenterOf(blockPos);
        Vector3f color = color(payload.power());
        int count = Mth.clamp(8 + payload.radius() / 2, 8, 34);
        double spread = Mth.clamp(payload.radius() * 0.035, 0.25, 1.8);
        for (int i = 0; i < count; i++) {
            client.level.addParticle(new DustParticleOptions(color, 1.1f),
                    center.x + client.level.random.nextGaussian() * spread,
                    center.y + client.level.random.nextDouble() * Math.max(1.0, spread),
                    center.z + client.level.random.nextGaussian() * spread,
                    0, 0.015, 0);
        }
    }

    private static Vector3f color(int power) {
        return switch (power) {
            case PowerEventPayload.POWER_YAMATO -> new Vector3f(0.12f, 0.55f, 1f);
            case PowerEventPayload.POWER_GOJO -> new Vector3f(0.55f, 0.82f, 1f);
            case PowerEventPayload.POWER_VOID -> new Vector3f(0.12f, 0.12f, 0.16f);
            case PowerEventPayload.POWER_DIO -> new Vector3f(1f, 0.72f, 0.12f);
            case PowerEventPayload.POWER_AIZEN -> new Vector3f(0.62f, 0.14f, 0.9f);
            case PowerEventPayload.POWER_SHADOW -> new Vector3f(0.32f, 0.08f, 0.55f);
            case PowerEventPayload.POWER_NUCLEAR -> new Vector3f(1f, 0.32f, 0.05f);
            default -> new Vector3f(1f, 0.85f, 0.2f);
        };
    }

    private static void tickIllusionParticles(Minecraft client) {
        if (!ClientPowerEventState.hasIllusion() || client.level == null || client.player == null
                || client.player.tickCount % 3 != 0) return;
        int count = ClientPowerEventState.illusionStrength() >= 2 ? 5 : 2;
        for (int i = 0; i < count; i++) {
            double angle = (client.player.tickCount * 0.08) + i * Mth.TWO_PI / count;
            double radius = 2.2 + (i & 1) * 0.9;
            client.level.addParticle(ParticleTypes.REVERSE_PORTAL,
                    client.player.getX() + Math.cos(angle) * radius,
                    client.player.getY() + 0.25 + client.level.random.nextDouble() * 1.8,
                    client.player.getZ() + Math.sin(angle) * radius,
                    0, 0.02, 0);
        }
    }

    private static void renderOverlay(GuiGraphics graphics, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        if (ClientPowerEventState.hasIllusion()) {
            renderAizenIllusion(graphics, width, height, client.level.getGameTime());
        }
        if (ClientPowerEventState.isActive(PowerEventPayload.POWER_DIO, 3)) {
            renderTimeStop(graphics, width, height, client.level.getGameTime());
        }
        if (ClientPowerEventState.isActive(PowerEventPayload.POWER_VOID, 6)) {
            renderVoidVignette(graphics, width, height);
        }
        if (ClientPowerEventState.isActive(PowerEventPayload.POWER_NUCLEAR, 6)) {
            renderNuclearWarning(graphics, width, height, client.level.getGameTime());
        }
        if (ClientPowerEventState.isActive(PowerEventPayload.POWER_GOJO, 3)) {
            renderInfinityEdge(graphics, width, height, client.level.getGameTime());
        }
    }

    private static void renderAizenIllusion(GuiGraphics g, int width, int height, long time) {
        boolean perfect = ClientPowerEventState.illusionStrength() >= 2;
        g.fill(0, 0, width, height, perfect ? 0x45100020 : 0x29160726);
        int edge = perfect ? 18 : 10;
        g.fill(0, 0, width, edge, 0x5A7D24A8);
        g.fill(0, height - edge, width, height, 0x5A3E0C60);
        g.fill(0, 0, edge, height, 0x4A5A1588);
        g.fill(width - edge, 0, width, height, 0x4A5A1588);

        long seed = ClientPowerEventState.illusionSeed();
        int shardCount = perfect ? 18 : 10;
        for (int i = 0; i < shardCount; i++) {
            long mixed = seed + i * 0x9E3779B97F4A7C15L;
            int x = Math.floorMod((int) (mixed ^ (mixed >>> 32)) + (int) time * (i % 3 - 1), Math.max(1, width));
            int y = Math.floorMod((int) (mixed >>> 17) + (int) time * ((i + 1) % 3 - 1), Math.max(1, height));
            int length = 9 + Math.floorMod((int) mixed, 24);
            g.fill(x, y, Math.min(width, x + 1), Math.min(height, y + length), 0x88DCA8FF);
            if ((i & 1) == 0) g.fill(x, y, Math.min(width, x + length / 2), Math.min(height, y + 1), 0x667B2BA8);
        }

        int cx = width / 2;
        int cy = height / 2;
        int drift = (int) (Math.sin(time * 0.17) * (perfect ? 32 : 18));
        drawFalseReticle(g, cx + drift, cy - 18, 0x99C86CFF);
        drawFalseReticle(g, cx - drift / 2 - 44, cy + 24, 0x66752BA5);
        if (perfect) drawFalseReticle(g, cx + 62, cy + drift / 2, 0x88F0CAFF);
    }

    private static void drawFalseReticle(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 8, y, x - 2, y + 1, color);
        g.fill(x + 3, y, x + 9, y + 1, color);
        g.fill(x, y - 8, x + 1, y - 2, color);
        g.fill(x, y + 3, x + 1, y + 9, color);
    }

    private static void renderTimeStop(GuiGraphics g, int width, int height, long time) {
        g.fill(0, 0, width, height, 0x20D7A928);
        int scan = Math.floorMod((int) time * 3, Math.max(1, height));
        g.fill(0, scan, width, Math.min(height, scan + 1), 0x55FFE89A);
        g.fill(0, 0, width, 3, 0x88D9B33A);
        g.fill(0, height - 3, width, height, 0x88D9B33A);
    }

    private static void renderVoidVignette(GuiGraphics g, int width, int height) {
        int border = Math.max(18, Math.min(width, height) / 12);
        g.fill(0, 0, width, border, 0xA8000000);
        g.fill(0, height - border, width, height, 0xA8000000);
        g.fill(0, border, border, height - border, 0x90000000);
        g.fill(width - border, border, width, height - border, 0x90000000);
    }

    private static void renderNuclearWarning(GuiGraphics g, int width, int height, long time) {
        int alpha = 18 + (int) (10 * (1 + Math.sin(time * 0.35)));
        g.fill(0, 0, width, height, (alpha << 24) | 0x00FF4300);
        int stripe = Math.floorMod((int) time * 5, 36);
        for (int x = -height + stripe; x < width; x += 36) {
            g.fill(Math.max(0, x), 0, Math.min(width, x + 3), 4, 0xCCFFB000);
            g.fill(Math.max(0, x + height - 4), height - 4, Math.min(width, x + height), height, 0xCCFFB000);
        }
    }

    private static void renderInfinityEdge(GuiGraphics g, int width, int height, long time) {
        int pulse = 70 + (int) (25 * Math.sin(time * 0.18));
        int color = (Mth.clamp(pulse, 30, 120) << 24) | 0x0088DFFF;
        g.fill(0, 0, width, 1, color);
        g.fill(0, height - 1, width, height, color);
        g.fill(0, 0, 1, height, color);
        g.fill(width - 1, 0, width, height, color);
    }

    /** Renders actual caster-model afterimages only on the hypnotized client. */
    private static void renderIllusionClones(WorldRenderContext context) {
        if (!ClientPowerEventState.hasIllusion()) return;
        Minecraft client = Minecraft.getInstance();
        PoseStack poseStack = context.matrixStack();
        MultiBufferSource buffers = context.consumers();
        if (client.player == null || poseStack == null || buffers == null) return;

        Entity caster = context.world().getEntity(ClientPowerEventState.illusionSourceId());
        if (caster == null || caster == client.player) return;

        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.camera().getPosition();
        int clones = ClientPowerEventState.illusionStrength() >= 2 ? 5 : 2;
        double phase = (context.world().getGameTime() + partialTick) * 0.035;
        for (int i = 0; i < clones; i++) {
            double angle = phase + i * Mth.TWO_PI / clones;
            double radius = 4.0 + (i & 1) * 1.4;
            Vec3 falsePosition = client.player.position().add(
                    Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            float yaw = (float) (Mth.atan2(client.player.getZ() - falsePosition.z,
                    client.player.getX() - falsePosition.x) * Mth.RAD_TO_DEG) - 90f;
            poseStack.pushPose();
            client.getEntityRenderDispatcher().render(caster,
                    falsePosition.x - camera.x,
                    falsePosition.y - camera.y,
                    falsePosition.z - camera.z,
                    yaw, partialTick, poseStack, buffers, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
    }
}
