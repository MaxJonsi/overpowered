package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.entity.JudgementCutEndEntity;
import com.maxjonsi.overpowered.entity.JudgementCutEntity;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import com.maxjonsi.overpowered.client.render.YamatoRenderer;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class YamatoItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.overpowered.yamato.idle");
    private static final Map<UUID, long[]> COMBO = new HashMap<>();
    private static final Map<UUID, Long> DASH_COOLDOWN = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public YamatoItem(Properties properties) {
        super(properties.attributes(createAttributes()));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 13.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 2, state -> state.setAndContinue(IDLE))
                .triggerableAnim("slash_1", RawAnimation.begin().thenPlay("animation.overpowered.yamato.slash_1"))
                .triggerableAnim("slash_2", RawAnimation.begin().thenPlay("animation.overpowered.yamato.slash_2"))
                .triggerableAnim("slash_3", RawAnimation.begin().thenPlay("animation.overpowered.yamato.slash_3"))
                .triggerableAnim("judgement_cut", RawAnimation.begin().thenPlay("animation.overpowered.yamato.judgement_cut"))
                .triggerableAnim("sheath", RawAnimation.begin().thenPlayAndHold("animation.overpowered.yamato.sheath"))
                .triggerableAnim("unleash", RawAnimation.begin().thenPlay("animation.overpowered.yamato.unleash"))
                .triggerableAnim("dash", RawAnimation.begin().thenPlay("animation.overpowered.yamato.dash")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private YamatoRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new YamatoRenderer();
                return renderer;
            }
        });
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            if (level instanceof ServerLevel serverLevel) {
                triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "base", "sheath");
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.YAMATO_SHEATH, SoundSource.PLAYERS, 1.2f, 1f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BURY_THE_LIGHT, SoundSource.RECORDS, 4f, 1f);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (level instanceof ServerLevel serverLevel) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = eye.add(look.scale(24));
            BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 target = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation().subtract(look.scale(1.5));
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, target,
                    new AABB(eye, target).inflate(1), e -> e instanceof LivingEntity && e != player && e.isAlive());
            if (entityHit != null) {
                Entity hit = entityHit.getEntity();
                target = hit.position().add(0, hit.getBbHeight() / 2, 0);
            }

            JudgementCutEntity cut = new JudgementCutEntity(ModEntities.JUDGEMENT_CUT, level);
            cut.setOwnerId(player.getUUID());
            cut.setPos(target);
            serverLevel.addFreshEntity(cut);

            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "base", "judgement_cut");
            player.getCooldowns().addCooldown(this, 15);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel serverLevel) || !(living instanceof ServerPlayer player)) return;
        int held = getUseDuration(stack, living) - remaining;
        if (held == 30) {
            player.stopUsingItem();
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "base", "unleash");

            JudgementCutEndEntity end = new JudgementCutEndEntity(ModEntities.JUDGEMENT_CUT_END, level);
            end.setOwnerId(player.getUUID());
            end.setPos(player.position());
            serverLevel.addFreshEntity(end);

            player.getCooldowns().addCooldown(this, 600);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        int held = getUseDuration(stack, living) - timeLeft;
        if (held < 30 && level instanceof ServerLevel serverLevel && living instanceof Player player) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "base", "unleash");
        }
    }

    public static void comboSwing(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long[] combo = COMBO.computeIfAbsent(player.getUUID(), k -> new long[]{-1, 0});
        if (now - combo[1] < 5 && combo[0] >= 0) return;

        Vec3 pickEye = player.getEyePosition();
        Vec3 pickEnd = pickEye.add(player.getLookAngle().scale(4.5));
        EntityHitResult picked = ProjectileUtil.getEntityHitResult(level, player, pickEye, pickEnd,
                new AABB(pickEye, pickEnd).inflate(0.5), e -> e instanceof LivingEntity && e != player && e.isAlive());
        Entity primaryTarget = picked != null ? picked.getEntity() : null;

        int stage = (now - combo[1] < 14) ? (int) ((combo[0] + 1) % 3) : 0;
        combo[0] = stage;
        combo[1] = now;

        ((YamatoItem) stack.getItem()).triggerAnim(player, GeoItem.getOrAssignId(stack, level), "base", "slash_" + (stage + 1));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.YAMATO_WHOOSH, SoundSource.PLAYERS, 1f, 0.9f + stage * 0.15f);

        Vec3 look = player.getLookAngle();
        Vec3 center = player.position().add(look.scale(2.5)).add(0, 1, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, 0.6, 0.4, 0.6, 0);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(2.8),
                e -> e != player && e.isAlive() && e != primaryTarget)) {
            Vec3 toTarget = target.position().subtract(player.position()).normalize();
            if (toTarget.dot(look) > 0.35) {
                target.hurt(player.damageSources().playerAttack(player), 12f);
            }
        }
    }

    public void dash(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long last = DASH_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < 50) return;
        DASH_COOLDOWN.put(player.getUUID(), now);

        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, Mth.clamp(look.y, -0.2, 0.35), look.z).normalize();
        player.setDeltaMovement(dir.scale(2.8));
        player.hurtMarked = true;

        Vec3 start = player.position().add(0, 1, 0);
        Vec3 end = start.add(dir.scale(9));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.5),
                e -> e != player && e.isAlive())) {
            target.hurt(player.damageSources().playerAttack(player), 16f);
        }

        for (int i = 0; i < 9; i++) {
            Vec3 p = start.add(dir.scale(i));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.3, 0.3, 0.3, 0);
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.2, 0.2, 0.2, 0.01);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.YAMATO_DASH, SoundSource.PLAYERS, 1.2f, 1f);
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "base", "dash");
    }
}
