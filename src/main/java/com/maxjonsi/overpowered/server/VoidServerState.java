package com.maxjonsi.overpowered.server;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoidServerState {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }

    public static void set(UUID playerId, boolean active) {
        if (active) {
            ACTIVE.add(playerId);
        } else {
            ACTIVE.remove(playerId);
        }
    }
}
