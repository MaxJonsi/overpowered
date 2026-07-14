package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.entity.DomainEntity;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.KyokaSuigetsuItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.ShadowDaggerItem;
import com.maxjonsi.overpowered.item.StoneMaskItem;
import com.maxjonsi.overpowered.item.VoidOrbItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.AbilityActionPayload;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ServerAbilityHandler {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("overpowered")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("cleanup")
                                .then(Commands.literal("radiation").executes(context -> {
                                    int count = NuclearAbilityManager.clearRadiation();
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Removed " + count + " radiation zones."), true);
                                    return count;
                                }))
                                .then(Commands.literal("void_shadows").executes(context -> {
                                    int count = VoidAbility.clearVoidShadows(context.getSource().getLevel());
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Removed " + count + " Void shadows."), true);
                                    return count;
                                })))));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                if (VoidAbility.isSilenced(serverPlayer.getUUID())) return InteractionResult.FAIL;
                if (DomainEntity.isTrapped(serverPlayer)) return InteractionResult.FAIL;
                if (VoidServerState.isActive(serverPlayer.getUUID())) {
                    if (entity instanceof LivingEntity living) VoidAbility.basicStrike(serverPlayer, living);
                    return InteractionResult.FAIL;
                }
                if (!(entity instanceof LivingEntity living)) return InteractionResult.PASS;
                ItemStack held = serverPlayer.getMainHandItem();
                if (held.getItem() instanceof StoneMaskItem) {
                    TimeAbilityManager.combo(serverPlayer, living);
                    return InteractionResult.SUCCESS;
                }
                if (held.getItem() instanceof SixEyesItem && GojoAbilityManager.hasMask(serverPlayer)) {
                    GojoAbilityManager.combo(serverPlayer, living);
                    return InteractionResult.SUCCESS;
                }
                if (held.getItem() instanceof YamatoItem) {
                    YamatoItem.comboSwing(serverPlayer, held);
                    return InteractionResult.SUCCESS;
                }
                if (held.getItem() instanceof KyokaSuigetsuItem) {
                    AizenAbilityManager.combo(serverPlayer, living);
                    return InteractionResult.SUCCESS;
                }
                if (held.getItem() instanceof ShadowDaggerItem) {
                    ShadowAbilityManager.combo(serverPlayer, living);
                    return InteractionResult.SUCCESS;
                }
                if (held.getItem() instanceof RocketLauncherItem) {
                    NuclearAbilityManager.basicBash(serverPlayer, living);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !DomainEntity.isTrapped(player));
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                DomainEntity.isTrapped(player) ? InteractionResult.FAIL : InteractionResult.PASS);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GojoAbilityManager::allowDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(VoidAbility::allowDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(AizenAbilityManager::allowDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(TimeAbilityManager::allowDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(YamatoAbilityManager::allowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(ShadowAbilityManager::onDeath);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerEnergyManager.tick(player);
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
            GojoAbilityManager.tick(server);
            YamatoAbilityManager.tick(server);
            VoidAbility.tick(server);
            TimeAbilityManager.tick(server);
            AizenAbilityManager.tick(server);
            ShadowAbilityManager.tick(server);
            NuclearAbilityManager.tick(server);
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
            PlayerEnergyManager.stateFor(handler.player);
            PlayerEnergyManager.sync(handler.player, true);
            sender.sendPacket(new VoidStatePayload(handler.player.getId(), false));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            server.execute(() -> {
                VoidServerState.deactivate(handler.player, false);
                PlayerEnergyManager.forgetClient(handler.player.getUUID());
                LegendaryCombat.clearPlayer(handler.player.getUUID());
                TimeAbilityManager.clearPlayer(server, handler.player.getUUID());
                AizenAbilityManager.clearPlayer(handler.player.getUUID());
                GojoAbilityManager.clearPlayer(handler.player.getUUID());
                ShadowAbilityManager.clearPlayer(server, handler.player.getUUID());
                NuclearAbilityManager.clearPlayer(handler.player.getUUID());
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
            VoidAbility.clear();
            PlayerEnergyManager.clear();
            GojoAbilityManager.clear();
            TimeAbilityManager.clear();
            AizenAbilityManager.clear();
            ShadowAbilityManager.clear();
            NuclearAbilityManager.clear();
            LegendaryCombat.clear();
            RocketLauncherItem.clearAllTransientState();
            YamatoItem.clearAllTransientState();
        });
    }

    public static void handleAction(ServerPlayer player, int action) {
        if (VoidAbility.isSilenced(player.getUUID()) || DomainEntity.isTrapped(player)) return;
        ItemStack main = player.getMainHandItem();
        switch (action) {
            case AbilityActionPayload.SWING -> {
                if (VoidServerState.isActive(player.getUUID())) {
                    LivingEntity target = VoidAbility.findTarget(player.serverLevel(), player, 5.5);
                    if (target != null) VoidAbility.basicStrike(player, target);
                } else if (main.getItem() instanceof YamatoItem) {
                    YamatoItem.comboSwing(player, main);
                }
            }
            case AbilityActionPayload.SPECIAL -> {
                if (main.getItem() instanceof YamatoItem yamato) {
                    if (player.isShiftKeyDown()) YamatoAbilityManager.toggleDevilTrigger(player);
                    else yamato.dash(player, main);
                }
                else if (main.getItem() instanceof RocketLauncherItem launcher) launcher.cycleMode(player, main);
                else if (main.getItem() instanceof SixEyesItem sixEyes) {
                    if (player.isShiftKeyDown()) GojoAbilityManager.toggleInfinity(player);
                    else sixEyes.cycleTechnique(player, main);
                }
                else if (main.getItem() instanceof StoneMaskItem stoneMask) stoneMask.cycleAbility(player, main);
                else if (main.getItem() instanceof KyokaSuigetsuItem kyokaSuigetsu) kyokaSuigetsu.cycleAbility(player, main);
                else if (main.getItem() instanceof ShadowDaggerItem shadowDagger) {
                    if (player.isShiftKeyDown()) ShadowAbilityManager.toggleMonarchForm(player);
                    else shadowDagger.cycleAbility(player, main);
                }
                else if (main.getItem() instanceof VoidOrbItem voidOrb
                        && VoidServerState.isActive(player.getUUID())) voidOrb.cycleAbility(player, main);
            }
            case AbilityActionPayload.MARK -> {
                if (main.getItem() instanceof RocketLauncherItem launcher) launcher.markTarget(player, main);
                else if (main.getItem() instanceof YamatoItem) YamatoAbilityManager.dimensionRift(player);
                else if (main.getItem() instanceof StoneMaskItem) TimeAbilityManager.timeDash(player);
            }
            case AbilityActionPayload.VOID_TOGGLE -> {
                // Void can only be toggled by using the orb on the server.
            }
            case AbilityActionPayload.VOID_KILL -> VoidAbility.kill(player);
            case AbilityActionPayload.ABILITY_ONE,
                    AbilityActionPayload.ABILITY_TWO,
                    AbilityActionPayload.ABILITY_THREE,
                    AbilityActionPayload.ABILITY_FOUR,
                    AbilityActionPayload.ABILITY_FIVE,
                    AbilityActionPayload.ULTIMATE -> handleAbilitySlot(player, main, action);
        }
    }

    private static void handleAbilitySlot(ServerPlayer player, ItemStack main, int action) {
        int slot = action == AbilityActionPayload.ULTIMATE
                ? 6
                : action - AbilityActionPayload.ABILITY_ONE + 1;

        if (VoidServerState.isActive(player.getUUID())) {
            switch (slot) {
                case 1 -> VoidAbility.step(player);
                case 2 -> VoidAbility.touch(player);
                case 3 -> VoidAbility.gaze(player);
                case 4 -> VoidAbility.grasp(player);
                case 5 -> VoidAbility.silence(player);
                case 6 -> VoidAbility.absoluteVoid(player);
                default -> {
                }
            }
        } else if (main.getItem() instanceof SixEyesItem sixEyes) {
            switch (slot) {
                case 1 -> {
                    if (player.isShiftKeyDown()) GojoAbilityManager.maximumBlue(player);
                    else sixEyes.castTechnique(player, SixEyesItem.TECH_BLUE);
                }
                case 2 -> sixEyes.castTechnique(player, SixEyesItem.TECH_RED);
                case 3 -> GojoAbilityManager.infinityFocus(player);
                case 4 -> GojoAbilityManager.teleport(player);
                case 5 -> sixEyes.castTechnique(player, SixEyesItem.TECH_PURPLE);
                case 6 -> sixEyes.castTechnique(player, SixEyesItem.TECH_DOMAIN);
                default -> {
                }
            }
        } else if (main.getItem() instanceof YamatoItem yamato) {
            switch (slot) {
                case 1 -> yamato.dash(player, main);
                case 2 -> yamato.performJudgementCut(player, main);
                case 3 -> YamatoAbilityManager.airTrick(player);
                case 4 -> YamatoAbilityManager.counter(player);
                case 5 -> YamatoAbilityManager.worldSplit(player);
                case 6 -> YamatoAbilityManager.startFinal(player);
                default -> {
                }
            }
        } else if (main.getItem() instanceof StoneMaskItem stoneMask) {
            stoneMask.performSlot(player, slot);
        } else if (main.getItem() instanceof KyokaSuigetsuItem kyokaSuigetsu) {
            kyokaSuigetsu.performSlot(player, slot);
        } else if (main.getItem() instanceof ShadowDaggerItem shadowDagger) {
            shadowDagger.performSlot(player, slot);
        } else if (main.getItem() instanceof RocketLauncherItem) {
            switch (slot) {
                case 1 -> NuclearAbilityManager.microNuke(player, main);
                case 2 -> NuclearAbilityManager.miniNuke(player, main);
                case 3 -> NuclearAbilityManager.mirv(player, main);
                case 4 -> NuclearAbilityManager.orbitalStrike(player);
                case 5 -> NuclearAbilityManager.laserBurst(player);
                case 6 -> NuclearAbilityManager.apocalypse(player);
                default -> {
                }
            }
        }
    }
}
