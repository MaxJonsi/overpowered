package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.DomainEntity;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.AbilityActionPayload;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ServerAbilityHandler {
    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer && entity instanceof LivingEntity living) {
                DomainEntity domain = DomainEntity.getActive(serverPlayer.getUUID());
                if (domain != null && domain.isInside(living)) {
                    living.hurt(serverPlayer.damageSources().playerAttack(serverPlayer), 10000f);
                }
            }
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (VoidServerState.isActive(player.getUUID()) && player.tickCount % 3 == 0) {
                    player.serverLevel().sendParticles(ParticleTypes.SMOKE,
                            player.getX(), player.getY() + 1, player.getZ(), 2, 0.25, 0.5, 0.25, 0.005);
                    if (player.tickCount % 30 == 0) {
                        player.serverLevel().sendParticles(ParticleTypes.SCULK_SOUL,
                                player.getX(), player.getY() + 1.5, player.getZ(), 1, 0.2, 0.3, 0.2, 0.01);
                    }
                }
            }
        });

        EntityTrackingEvents.START_TRACKING.register((tracked, player) -> {
            if (tracked instanceof ServerPlayer trackedPlayer && VoidServerState.isActive(trackedPlayer.getUUID())) {
                ServerPlayNetworking.send(player, new VoidStatePayload(trackedPlayer.getId(), true));
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (VoidServerState.isActive(handler.player.getUUID())) {
                sender.sendPacket(new VoidStatePayload(handler.player.getId(), true));
            }
        });
    }

    public static void handleAction(ServerPlayer player, int action) {
        ItemStack main = player.getMainHandItem();
        switch (action) {
            case AbilityActionPayload.SWING -> {
                if (main.getItem() instanceof YamatoItem) YamatoItem.comboSwing(player, main);
            }
            case AbilityActionPayload.SPECIAL -> {
                if (main.getItem() instanceof YamatoItem yamato) yamato.dash(player, main);
                else if (main.getItem() instanceof RocketLauncherItem launcher) launcher.cycleMode(player, main);
                else if (main.getItem() instanceof SixEyesItem sixEyes) sixEyes.cycleTechnique(player, main);
            }
            case AbilityActionPayload.MARK -> {
                if (main.getItem() instanceof RocketLauncherItem launcher) launcher.markTarget(player, main);
            }
            case AbilityActionPayload.VOID_TOGGLE -> VoidAbility.toggle(player);
            case AbilityActionPayload.VOID_KILL -> VoidAbility.kill(player);
        }
    }
}
