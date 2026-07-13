package com.maxjonsi.overpowered.client;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class ClientVoidState {
    private static final IntSet ACTIVE = new IntOpenHashSet();

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

    public static void clear() {
        ACTIVE.clear();
    }
}
