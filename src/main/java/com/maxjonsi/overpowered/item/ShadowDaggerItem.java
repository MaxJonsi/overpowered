package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.AbilityCosts;
import com.maxjonsi.overpowered.entity.ShadowSoldierEntity;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModEntities;
import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.EnergyService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shadow Monarch — shadows. Shadow Step / Arise / Shadow Wave / Monarch Form. */
public class ShadowDaggerItem extends Item implements CyclingAbilityItem {
    public static final int SHADOW_STEP = 0;
    public static final int ARISE = 1;
    public static final int SHADOW_WAVE = 2;
    public static final int MONARCH_FORM = 3;

    private static final String[] ABILITY_KEYS = {
            "ability.overpowered.shadow.step",
            "ability.overpowered.shadow.arise",
            "ability.overpowered.shadow.wave",
            "ability.overpowered.shadow.monarch"};

    public ShadowDaggerItem(Properties properties) {
        super(properties.attributes(createAttributes()));
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        int ability = stack.getOrDefault(ModDataComponents.TECHNIQUE, SHADOW_STEP);
        boolean cast = switch (ability) {
            case SHADOW_STEP -> castShadowStep(serverLevel, serverPlayer);
            case ARISE -> castArise(serverLevel, serverPlayer);
            case SHADOW_WAVE -> castShadowWave(serverLevel, serverPlayer);
            case MONARCH_FORM -> castMonarchForm(serverLevel, serverPlayer);
            default -> false;
        };
        if (cast) player.getCooldowns().addCooldown(this, 15);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private boolean castShadowStep(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.SHADOW_STEP)) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(14));
        EntityHitResult targetHit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1), e -> e instanceof LivingEntity && e != player && e.isAlive());

        Vec3 dest;
        float yaw = player.getYRot();
        if (targetHit != null) {
            LivingEntity target = (LivingEntity) targetHit.getEntity();
            dest = target.position().subtract(target.getLookAngle().scale(1.6));
            yaw = target.getYRot();
        } else {
            BlockHitResult hit = level.clip(new ClipContext(eye, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            dest = (hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(1.2)))
                    .subtract(0, 1.2, 0);
        }

        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        player.teleportTo(level, dest.x, dest.y, dest.z, yaw, player.getXRot());
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, false));
        level.sendParticles(ParticleTypes.SQUID_INK, dest.x, dest.y + 1, dest.z, 20, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, dest.x, dest.y, dest.z, ModSounds.SHADOW_PORTAL, SoundSource.PLAYERS, 1.4f, 1f);
        return true;
    }

    private boolean castArise(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.SHADOW_ARISE)) return false;
        Vec3 center = player.position();
        for (int i = 0; i < 3; i++) {
            float angle = (float) (i * Math.PI * 2 / 3) + player.getYRot() * Mth.DEG_TO_RAD;
            double x = center.x + Mth.cos(angle) * 2.2;
            double z = center.z + Mth.sin(angle) * 2.2;
            ShadowSoldierEntity soldier = new ShadowSoldierEntity(ModEntities.SHADOW_SOLDIER, level);
            soldier.moveTo(x, center.y, z, player.getYRot(), 0);
            level.addFreshEntity(soldier);
            level.sendParticles(ParticleTypes.SQUID_INK, x, center.y + 1, z, 25, 0.3, 0.8, 0.3, 0.03);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.SHADOW_ARISE, SoundSource.PLAYERS, 2f, 1f);
        player.displayClientMessage(Component.translatable("message.overpowered.shadow.arise"), true);
        return true;
    }

    private boolean castShadowWave(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.SHADOW_WAVE)) return false;
        Vec3 center = player.position().add(0, 1, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(10),
                e -> e != player && e.isAlive() && !(e instanceof ShadowSoldierEntity))) {
            target.hurt(player.damageSources().indirectMagic(player, player), 12f);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
        for (int ring = 2; ring <= 8; ring += 2) {
            for (int i = 0; i < ring * 6; i++) {
                double angle = 2 * Math.PI * i / (ring * 6);
                level.sendParticles(ParticleTypes.SQUID_INK,
                        center.x + ring * Math.cos(angle), center.y, center.z + ring * Math.sin(angle), 1, 0.1, 0.3, 0.1, 0.01);
            }
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.MAGIC_EXPLOSION, SoundSource.PLAYERS, 2f, 0.6f);
        return true;
    }

    private boolean castMonarchForm(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.SHADOW_MONARCH_FORM)) return false;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SHADOW_ARISE, SoundSource.PLAYERS, 2f, 0.7f);
        player.displayClientMessage(Component.translatable("message.overpowered.shadow.monarch"), true);
        return true;
    }

    @Override
    public void cycleAbility(ServerPlayer player, ItemStack stack) {
        int ability = (stack.getOrDefault(ModDataComponents.TECHNIQUE, SHADOW_STEP) + 1) % 4;
        stack.set(ModDataComponents.TECHNIQUE, ability);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LAUNCHER_MODE, SoundSource.PLAYERS, 1f, 1.6f);
        player.displayClientMessage(Component.translatable(ABILITY_KEYS[ability]), true);
    }
}
