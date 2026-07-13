package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.DomainEntity;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.AbilityActionPayload;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ServerAbilityHandler {
    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                if (VoidServerState.isActive(serverPlayer.getUUID())) {
                    return InteractionResult.FAIL;
                }
                if (!(entity instanceof LivingEntity living)) return InteractionResult.PASS;
                DomainEntity domain = DomainEntity.getActive(serverPlayer.getUUID());
                if (domain != null && domain.isInside(living)) {
                    living.hurt(serverPlayer.damageSources().playerAttack(serverPlayer), 10000f);
                }
            }
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!VoidServerState.isActive(player.getUUID())) continue;

                VoidServerState.tick(player);
                if (player.tickCount % 3 == 0) {
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
            if (tracked instanceof ServerPlayer trackedPlayer) {
                VoidAbility.sendState(trackedPlayer, player, VoidServerState.isActive(trackedPlayer.getUUID()));
            }
        });

        EntityTrackingEvents.STOP_TRACKING.register((tracked, player) -> {
            if (tracked instanceof ServerPlayer trackedPlayer) {
                VoidAbility.sendState(trackedPlayer, player, false);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            VoidServerState.recoverOrphaned(handler.player);
            sender.sendPacket(new VoidStatePayload(handler.player.getId(), false));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            server.execute(() -> {
                VoidServerState.deactivate(handler.player, false);
                RocketLauncherItem.clearTransientState(handler.player.getUUID());
                YamatoItem.clearTransientState(handler.player.getUUID());
            });
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (VoidServerState.isActive(newPlayer.getUUID())) {
                VoidServerState.applyActiveFlight(newPlayer);
                VoidAbility.sendState(oldPlayer, newPlayer, false);
                VoidAbility.syncState(newPlayer);
            }
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (VoidServerState.isActive(player.getUUID())) {
                VoidServerState.applyActiveFlight(player);
                VoidAbility.syncState(player);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                server.getPlayerList().getPlayers().forEach(VoidServerState::deactivate));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            VoidServerState.clear();
            RocketLauncherItem.clearAllTransientState();
            YamatoItem.clearAllTransientState();
        });
    }

    public static void handleAction(ServerPlayer player, int action) {
        ItemStack main = player.getMainHandItem();
        switch (action) {
            case AbilityActionPayload.SWING -> {
                if (!VoidServerState.isActive(player.getUUID()) && main.getItem() instanceof YamatoItem) {
                    YamatoItem.comboSwing(player, main);
                }
            }
            case AbilityActionPayload.SPECIAL -> {
                if (main.getItem() instanceof YamatoItem yamato) yamato.dash(player, main);
                else if (main.getItem() instanceof RocketLauncherItem launcher) launcher.cycleMode(player, main);
                else if (main.getItem() instanceof SixEyesItem sixEyes) sixEyes.cycleTechnique(player, main);
            }
            case AbilityActionPayload.MARK -> {
                if (main.getItem() instanceof RocketLauncherItem launcher) launcher.markTarget(player, main);
            }
            case AbilityActionPayload.VOID_TOGGLE -> {
                // Void can only be toggled by using the orb on the server.
            }
            case AbilityActionPayload.VOID_KILL -> VoidAbility.kill(player);
        }
    }
}
