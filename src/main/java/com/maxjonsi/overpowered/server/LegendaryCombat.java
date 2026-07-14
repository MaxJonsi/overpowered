package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.item.KyokaSuigetsuItem;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.ShadowDaggerItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.StoneMaskItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/** Shared server-side rules for legendary combat, action commitments and damage scaling. */
public final class LegendaryCombat {
    private static final Map<UUID, Long> COMMITTED_UNTIL = new HashMap<>();
    private static final Set<UUID> INFINITY_BYPASS = new HashSet<>();
    private static final Set<UUID> VOID_BYPASS = new HashSet<>();

    private LegendaryCombat() {
    }

    public enum AttackKind {
        PHYSICAL(false, false),
        ENERGY(false, true),
        SPATIAL(true, true),
        TEMPORAL(true, true),
        CONCEPTUAL(true, true),
        WORLD_LEVEL(true, true);

        private final boolean bypassInfinity;
        private final boolean bypassVoid;

        AttackKind(boolean bypassInfinity, boolean bypassVoid) {
            this.bypassInfinity = bypassInfinity;
            this.bypassVoid = bypassVoid;
        }
    }

    public static boolean begin(ServerPlayer player, int energyCost, int commitmentTicks) {
        long now = player.serverLevel().getGameTime();
        if (now < COMMITTED_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE)) return false;
        if (!PlayerEnergyManager.tryConsumeOrNotify(player, energyCost)) return false;
        COMMITTED_UNTIL.put(player.getUUID(), now + Math.max(1, commitmentTicks));
        return true;
    }

    public static boolean beginFree(ServerPlayer player, int commitmentTicks) {
        return begin(player, 0, commitmentTicks);
    }

    public static boolean isCommitted(ServerPlayer player) {
        return player.serverLevel().getGameTime()
                < COMMITTED_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE);
    }

    public static boolean isLegendary(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return false;
        if (VoidServerState.isActive(player.getUUID()) || GojoAbilityManager.hasGojoLoadout(player)) return true;
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        return isLegendaryArtifact(main) || isLegendaryArtifact(off);
    }

    private static boolean isLegendaryArtifact(Item item) {
        return item instanceof YamatoItem
                || item instanceof SixEyesItem
                || item instanceof StoneMaskItem
                || item instanceof KyokaSuigetsuItem
                || item instanceof ShadowDaggerItem
                || item instanceof RocketLauncherItem;
    }

    public static boolean damage(ServerPlayer attacker, LivingEntity target,
            float ordinaryDamage, float legendaryMaxHealthFraction, AttackKind kind) {
        DamageSource source = attacker.damageSources().indirectMagic(attacker, attacker);
        return damage(target, source, ordinaryDamage, legendaryMaxHealthFraction, kind);
    }

    public static boolean damage(LivingEntity target, DamageSource source,
            float ordinaryDamage, float legendaryMaxHealthFraction, AttackKind kind) {
        float amount = isLegendary(target)
                ? Math.max(0.5f, target.getMaxHealth() * legendaryMaxHealthFraction)
                : ordinaryDamage;
        UUID targetId = target.getUUID();
        if (kind.bypassInfinity) INFINITY_BYPASS.add(targetId);
        if (kind.bypassVoid) VOID_BYPASS.add(targetId);
        try {
            return target.hurt(source, amount);
        } finally {
            INFINITY_BYPASS.remove(targetId);
            VOID_BYPASS.remove(targetId);
        }
    }

    public static boolean bypassesInfinity(LivingEntity target) {
        return INFINITY_BYPASS.contains(target.getUUID());
    }

    public static boolean bypassesVoid(LivingEntity target) {
        return VOID_BYPASS.contains(target.getUUID());
    }

    public static void stagger(LivingEntity target, int ticks, int strength) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                ticks, Math.max(0, strength), false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                Math.max(10, ticks / 2), Math.max(0, strength - 1), false, false));
        target.setDeltaMovement(target.getDeltaMovement().scale(0.25));
        target.hurtMarked = true;
    }

    public static void clearPlayer(UUID playerId) {
        COMMITTED_UNTIL.remove(playerId);
        INFINITY_BYPASS.remove(playerId);
        VOID_BYPASS.remove(playerId);
    }

    public static void clear() {
        COMMITTED_UNTIL.clear();
        INFINITY_BYPASS.clear();
        VOID_BYPASS.clear();
    }
}
