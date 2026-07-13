package com.maxjonsi.overpowered.client;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public final class ClientVoidState {
    private static final IntSet ACTIVE = new IntOpenHashSet();

    private ClientVoidState() {
    }

    public static boolean isActive(int entityId) {
        return ACTIVE.contains(entityId);
    }

    public static void set(int entityId, boolean active) {
        if (active) {
            ACTIVE.add(entityId);
        } else {
            ACTIVE.remove(entityId);
        }
    }

    public static void remove(int entityId) {
        ACTIVE.remove(entityId);
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
