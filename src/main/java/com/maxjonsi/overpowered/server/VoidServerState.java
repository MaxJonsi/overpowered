package com.maxjonsi.overpowered.server;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

public final class VoidServerState {
    private static final String TAG_PREFIX = "overpowered.void.";
    private static final String ACTIVE_TAG = TAG_PREFIX + "active";
    private static final String MAYFLY_TAG = TAG_PREFIX + "restore_mayfly";
    private static final String FLYING_TAG = TAG_PREFIX + "restore_flying";
    private static final String GAME_TYPE_TAG_PREFIX = TAG_PREFIX + "restore_game_type.";
    private static final Map<UUID, FlightState> ACTIVE = new ConcurrentHashMap<>();

    private VoidServerState() {
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.containsKey(playerId);
    }

    public static boolean activate(ServerPlayer player) {
        FlightState state = FlightState.capture(player);
        if (ACTIVE.putIfAbsent(player.getUUID(), state) != null) {
            applyActiveFlight(player);
            return false;
        }
        state.storeRecoveryTags(player);
        applyActiveFlight(player);
        return true;
    }

    public static boolean deactivate(ServerPlayer player) {
        return deactivate(player, true);
    }

    public static boolean deactivate(ServerPlayer player, boolean syncAbilities) {
        FlightState state = ACTIVE.remove(player.getUUID());
        if (state == null) return false;

        state.restore(player, syncAbilities);
        clearRecoveryTags(player);
        return true;
    }

    public static boolean recoverOrphaned(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        Set<String> tags = player.getTags();
        if (!tags.contains(ACTIVE_TAG)) return false;

        String savedGameType = tags.stream()
                .filter(tag -> tag.startsWith(GAME_TYPE_TAG_PREFIX))
                .map(tag -> tag.substring(GAME_TYPE_TAG_PREFIX.length()))
                .findFirst()
                .orElse("");
        FlightState saved = new FlightState(
                tags.contains(MAYFLY_TAG),
                tags.contains(FLYING_TAG),
                savedGameType);
        saved.restore(player, true);
        clearRecoveryTags(player);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (!isActive(player.getUUID())) return;
        player.fallDistance = 0;
        applyActiveFlight(player);
    }

    public static void applyActiveFlight(ServerPlayer player) {
        FlightState state = ACTIVE.get(player.getUUID());
        if (state == null) return;

        if (!player.getTags().contains(ACTIVE_TAG)) {
            state.storeRecoveryTags(player);
        }

        Abilities abilities = player.getAbilities();
        boolean changed = !abilities.mayfly;
        abilities.mayfly = true;
        player.fallDistance = 0;
        if (changed) player.onUpdateAbilities();
    }

    public static void discard(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void clearRecoveryTags(ServerPlayer player) {
        player.getTags().stream()
                .filter(tag -> tag.startsWith(TAG_PREFIX))
                .toList()
                .forEach(player::removeTag);
    }

    private record FlightState(boolean mayfly, boolean flying, String gameType) {
        private static FlightState capture(ServerPlayer player) {
            Abilities abilities = player.getAbilities();
            return new FlightState(
                    abilities.mayfly,
                    abilities.flying,
                    player.gameMode.getGameModeForPlayer().getName());
        }

        private void restore(ServerPlayer player, boolean syncAbilities) {
            Abilities abilities = player.getAbilities();
            GameType currentGameType = player.gameMode.getGameModeForPlayer();
            if (currentGameType.getName().equals(gameType)) {
                abilities.mayfly = mayfly;
                abilities.flying = flying;
            } else {
                currentGameType.updatePlayerAbilities(abilities);
            }
            player.fallDistance = 0;
            if (syncAbilities) player.onUpdateAbilities();
        }

        private void storeRecoveryTags(ServerPlayer player) {
            clearRecoveryTags(player);
            player.addTag(ACTIVE_TAG);
            if (mayfly) player.addTag(MAYFLY_TAG);
            if (flying) player.addTag(FLYING_TAG);
            player.addTag(GAME_TYPE_TAG_PREFIX + gameType);
        }
    }
}
