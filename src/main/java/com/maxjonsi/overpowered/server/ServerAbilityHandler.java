package com.maxjonsi.overpowered.server;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.entity.DomainEntity;
import com.maxjonsi.overpowered.item.RocketLauncherItem;
import com.maxjonsi.overpowered.item.SixEyesItem;
import com.maxjonsi.overpowered.item.YamatoItem;
import com.maxjonsi.overpowered.network.AbilityActionPayload;
import com.maxjonsi.overpowered.network.VoidStatePayload;
import com.maxjonsi.overpowered.registry.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Overpowered.MODID)
public class ServerAbilityHandler {
    public static void handleAction(ServerPlayer player, int action) {
        ItemStack main = player.getMainHandItem();
        switch (action) {
            case AbilityActionPayload.SWING -> {
                if (main.getItem() instanceof YamatoItem) YamatoItem.comboSwing(player, main, null);
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

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getMainHandItem().getItem() instanceof YamatoItem) {
            YamatoItem.comboSwing(player, player.getMainHandItem(), event.getTarget());
        }
    }

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            DomainEntity domain = DomainEntity.getActive(attacker.getUUID());
            if (domain != null && domain.isInside(event.getEntity())) {
                event.setAmount(10000f);
            }
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getData(ModAttachments.VOID_ACTIVE.get())
                && player.tickCount % 3 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1, player.getZ(), 2, 0.25, 0.5, 0.25, 0.005);
            if (player.tickCount % 30 == 0) {
                player.serverLevel().sendParticles(ParticleTypes.SCULK_SOUL,
                        player.getX(), player.getY() + 1.5, player.getZ(), 1, 0.2, 0.3, 0.2, 0.01);
            }
        }
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player tracked
                && tracked.getData(ModAttachments.VOID_ACTIVE.get())
                && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new VoidStatePayload(tracked.getId(), true));
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getData(ModAttachments.VOID_ACTIVE.get())) {
            PacketDistributor.sendToPlayer(player, new VoidStatePayload(player.getId(), true));
        }
    }
}
