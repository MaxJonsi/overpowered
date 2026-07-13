package com.maxjonsi.overpowered.item;

import com.maxjonsi.overpowered.AbilityCosts;
import com.maxjonsi.overpowered.registry.ModDataComponents;
import com.maxjonsi.overpowered.registry.ModSounds;
import com.maxjonsi.overpowered.server.EnergyService;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Aizen — perception. Flash Step / Spiritual Pressure / Kyoka Suigetsu / Hogyoku Evolution. */
public class KyokaSuigetsuItem extends Item implements CyclingAbilityItem {
    public static final int FLASH_STEP = 0;
    public static final int PRESSURE = 1;
    public static final int HYPNOSIS = 2;
    public static final int HOGYOKU = 3;

    private static final String[] ABILITY_KEYS = {
            "ability.overpowered.aizen.step",
            "ability.overpowered.aizen.pressure",
            "ability.overpowered.aizen.hypnosis",
            "ability.overpowered.aizen.hogyoku"};

    public KyokaSuigetsuItem(Properties properties) {
        super(properties.attributes(createAttributes()));
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 10.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1.6, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        int ability = stack.getOrDefault(ModDataComponents.TECHNIQUE, FLASH_STEP);
        boolean cast = switch (ability) {
            case FLASH_STEP -> castFlashStep(serverLevel, serverPlayer);
            case PRESSURE -> castPressure(serverLevel, serverPlayer);
            case HYPNOSIS -> castHypnosis(serverLevel, serverPlayer);
            case HOGYOKU -> castHogyoku(serverLevel, serverPlayer);
            default -> false;
        };
        if (cast) player.getCooldowns().addCooldown(this, 15);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private boolean castFlashStep(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.AIZEN_STEP)) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(10)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 dest = hit.getType() == HitResult.Type.MISS
                ? eye.add(look.scale(10)) : hit.getLocation().subtract(look.scale(1.2));

        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        player.teleportTo(level, dest.x, dest.y - 1.2, dest.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.8f);
        return true;
    }

    private boolean castPressure(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.AIZEN_PRESSURE)) return false;
        Vec3 center = player.position().add(0, 1, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(12), e -> e != player && e.isAlive())) {
            target.hurt(player.damageSources().indirectMagic(player, player), 8f);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
            target.setDeltaMovement(target.getDeltaMovement().add(0, -0.6, 0));
            target.hurtMarked = true;
        }
        for (int ring = 2; ring <= 10; ring += 2) {
            for (int i = 0; i < ring * 6; i++) {
                double angle = 2 * Math.PI * i / (ring * 6);
                level.sendParticles(new DustParticleOptions(new Vector3f(0.65f, 0.3f, 0.85f), 1.5f),
                        center.x + ring * Math.cos(angle), center.y + 0.3, center.z + ring * Math.sin(angle), 1, 0.1, 0.2, 0.1, 0);
            }
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.MAGIC_FORCEFIELD, SoundSource.PLAYERS, 2.5f, 0.5f);
        return true;
    }

    private boolean castHypnosis(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.AIZEN_HYPNOSIS)) return false;
        Vec3 center = player.position();
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(16), e -> e != player && e.isAlive());
        for (LivingEntity target : victims) {
            if (target instanceof Player hypnotized) {
                hypnotized.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                hypnotized.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 260, 0));
            } else if (target instanceof Mob mob) {
                // false reality: mobs turn on each other
                LivingEntity decoy = victims.stream()
                        .filter(v -> v != mob && v instanceof LivingEntity)
                        .findAny().orElse(null);
                if (decoy != null) mob.setTarget(decoy);
            }
            level.sendParticles(ParticleTypes.WITCH,
                    target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.MAGIC_FORCEFIELD, SoundSource.PLAYERS, 2f, 1.6f);
        player.displayClientMessage(Component.translatable("message.overpowered.aizen.hypnosis"), true);
        return true;
    }

    private boolean castHogyoku(ServerLevel level, ServerPlayer player) {
        if (!EnergyService.tryUse(player, AbilityCosts.AIZEN_HOGYOKU)) return false;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
        level.sendParticles(new DustParticleOptions(new Vector3f(0.75f, 0.35f, 0.95f), 2f),
                player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 1, 0.5, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.AIZEN_THEME, SoundSource.RECORDS, 3f, 1f);
        player.displayClientMessage(Component.translatable("message.overpowered.aizen.hogyoku"), true);
        return true;
    }

    @Override
    public void cycleAbility(ServerPlayer player, ItemStack stack) {
        int ability = (stack.getOrDefault(ModDataComponents.TECHNIQUE, FLASH_STEP) + 1) % 4;
        stack.set(ModDataComponents.TECHNIQUE, ability);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.LAUNCHER_MODE, SoundSource.PLAYERS, 1f, 1.6f);
        player.displayClientMessage(Component.translatable(ABILITY_KEYS[ability]), true);
    }
}
