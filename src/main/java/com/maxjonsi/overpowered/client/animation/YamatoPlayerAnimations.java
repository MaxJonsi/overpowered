package com.maxjonsi.overpowered.client.animation;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.YamatoAnimationPayload;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IActualAnimation;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.kosmx.playerAnim.minecraftApi.layers.LeftHandedHelperModifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class YamatoPlayerAnimations {
    private static final ResourceLocation LAYER_ID = Overpowered.id("yamato_player_layer");
    private static final ResourceLocation PLAYER_ANIMATOR_RELOAD =
            ResourceLocation.fromNamespaceAndPath("playeranimator", "animation");
    private static final String HOLD = "yamato.hold";
    private static final List<String> REQUIRED_ANIMATIONS = List.of(
            HOLD,
            "yamato.slash_1",
            "yamato.slash_2",
            "yamato.slash_3",
            "yamato.judgement_cut",
            "yamato.sheath",
            "yamato.unleash",
            "yamato.dash");
    private static final int PENDING_LIFETIME = 40;
    private static final Map<Integer, PendingAnimation> PENDING = new HashMap<>();
    private static boolean initialized;

    private YamatoPlayerAnimations() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                LAYER_ID, 1000, YamatoAnimationLayer::new);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return Overpowered.id("yamato_player_animations");
                    }

                    @Override
                    public List<ResourceLocation> getFabricDependencies() {
                        return List.of(PLAYER_ANIMATOR_RELOAD);
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        List<String> missing = REQUIRED_ANIMATIONS.stream()
                                .filter(name -> PlayerAnimationRegistry.getAnimation(Overpowered.id(name)) == null)
                                .toList();
                        if (missing.isEmpty()) {
                            Overpowered.LOGGER.info("Loaded {} Yamato player animations", REQUIRED_ANIMATIONS.size());
                        } else {
                            Overpowered.LOGGER.error("Missing Yamato player animations: {}", missing);
                        }
                    }
                });

        ClientPlayNetworking.registerGlobalReceiver(YamatoAnimationPayload.TYPE, (payload, context) ->
                context.client().execute(() -> receive(context.client(), payload)));

        ClientTickEvents.END_CLIENT_TICK.register(YamatoPlayerAnimations::retryPending);
    }

    private static void receive(Minecraft client, YamatoAnimationPayload payload) {
        String animation = animationName(payload.animation());
        if (animation == null) return;
        if (!play(client, payload.entityId(), animation)) {
            PENDING.put(payload.entityId(), new PendingAnimation(animation, PENDING_LIFETIME));
        }
    }

    private static void retryPending(Minecraft client) {
        if (client.level == null) {
            PENDING.clear();
            return;
        }

        Iterator<Map.Entry<Integer, PendingAnimation>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PendingAnimation> entry = iterator.next();
            PendingAnimation pending = entry.getValue();
            if (play(client, entry.getKey(), pending.animation()) || pending.ticksLeft() <= 1) {
                iterator.remove();
            } else {
                entry.setValue(new PendingAnimation(pending.animation(), pending.ticksLeft() - 1));
            }
        }
    }

    private static boolean play(Minecraft client, int entityId, String animation) {
        if (client.level == null
                || !(client.level.getEntity(entityId) instanceof AbstractClientPlayer player)) {
            return false;
        }

        IAnimation layer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
        if (!(layer instanceof YamatoAnimationLayer yamatoLayer)) return false;
        return yamatoLayer.play(animation);
    }

    private static String animationName(int animation) {
        return switch (animation) {
            case YamatoAnimationPayload.SLASH_1 -> "yamato.slash_1";
            case YamatoAnimationPayload.SLASH_2 -> "yamato.slash_2";
            case YamatoAnimationPayload.SLASH_3 -> "yamato.slash_3";
            case YamatoAnimationPayload.JUDGEMENT_CUT -> "yamato.judgement_cut";
            case YamatoAnimationPayload.SHEATH -> "yamato.sheath";
            case YamatoAnimationPayload.UNLEASH -> "yamato.unleash";
            case YamatoAnimationPayload.DASH -> "yamato.dash";
            default -> null;
        };
    }

    private record PendingAnimation(String animation, int ticksLeft) {
    }

    private static final class YamatoAnimationLayer implements IAnimation {
        private final AbstractClientPlayer player;
        private IAnimation animation;
        private String animationName;
        private boolean action;
        private int actionTicks;

        private YamatoAnimationLayer(AbstractClientPlayer player) {
            this.player = player;
        }

        private boolean play(String name) {
            IAnimation next = createAnimation(name);
            if (next == null) return false;
            animation = next;
            animationName = name;
            action = true;
            actionTicks = 0;
            return true;
        }

        @Override
        public void tick() {
            if (action) {
                actionTicks++;
                if (animation != null && animation.isActive()) {
                    animation.tick();
                }
                boolean cancelledSheath = "yamato.sheath".equals(animationName)
                        && actionTicks > 2 && (!isHoldingYamato() || !player.isUsingItem());
                if (!cancelledSheath && animation != null && animation.isActive()) return;
                action = false;
                animation = null;
                animationName = null;
            }

            if (!isHoldingYamato()) {
                animation = null;
                animationName = null;
                return;
            }

            if (animation == null || !animation.isActive() || !HOLD.equals(animationName)) {
                animation = createAnimation(HOLD);
                animationName = HOLD;
            }
            if (animation != null && animation.isActive()) {
                animation.tick();
            }
        }

        @Override
        public boolean isActive() {
            return action && animation != null && animation.isActive() || isHoldingYamato();
        }

        @Override
        public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value0) {
            return animation != null && animation.isActive()
                    ? animation.get3DTransform(modelName, type, tickDelta, value0)
                    : value0;
        }

        @Override
        public void setupAnim(float tickDelta) {
            if (animation != null && animation.isActive()) {
                animation.setupAnim(tickDelta);
            }
        }

        @Override
        public FirstPersonMode getFirstPersonMode(float tickDelta) {
            return animation != null && animation.isActive()
                    ? animation.getFirstPersonMode(tickDelta)
                    : FirstPersonMode.NONE;
        }

        @Override
        public FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
            return animation != null && animation.isActive()
                    ? animation.getFirstPersonConfiguration(tickDelta)
                    : new FirstPersonConfiguration();
        }

        private boolean isHoldingYamato() {
            return player.isAlive() && player.getMainHandItem().getItem() instanceof YamatoItem;
        }

        private IAnimation createAnimation(String name) {
            IPlayable playable = PlayerAnimationRegistry.getAnimation(Overpowered.id(name));
            if (playable == null) {
                Overpowered.LOGGER.warn("Missing Yamato player animation {}", name);
                return null;
            }

            IActualAnimation<?> animation = playable.playAnimation();
            animation.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            animation.setFirstPersonConfiguration(new FirstPersonConfiguration()
                    .setShowRightArm(true)
                    .setShowLeftArm(true)
                    .setShowRightItem(true)
                    .setShowLeftItem(false));
            ModifierLayer<IActualAnimation<?>> layer = new ModifierLayer<>(animation);
            layer.addModifierLast(new LeftHandedHelperModifier(player));
            return layer;
        }
    }
}
