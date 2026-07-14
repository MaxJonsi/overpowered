package com.maxjonsi.overpowered.client;

import com.maxjonsi.overpowered.network.PowerEventPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Timed client presentation state derived exclusively from server events. */
public final class ClientPowerEventState {
    private static final int INDEFINITE = Integer.MAX_VALUE;
    private static final Map<EventKey, ActiveEvent> ACTIVE = new HashMap<>();

    private static int illusionTicks;
    private static int illusionSourceId = -1;
    private static int illusionStrength;
    private static long illusionSeed;

    private ClientPowerEventState() {
    }

    public static void accept(PowerEventPayload payload) {
        EventKey key = new EventKey(payload.sourceEntityId(), payload.power(), payload.ability());
        if (payload.phase() == PowerEventPayload.PHASE_STATE_END) {
            ACTIVE.remove(key);
            if (payload.power() == PowerEventPayload.POWER_AIZEN && payload.ability() == 2
                    && payload.sourceEntityId() == illusionSourceId) {
                clearIllusion();
            }
            return;
        }

        int lifetime = payload.durationTicks() > 0 ? payload.durationTicks()
                : payload.phase() == PowerEventPayload.PHASE_STATE_START ? INDEFINITE : 10;
        ACTIVE.put(key, new ActiveEvent(payload, lifetime));

        // Only the victim receives Aizen's detail 1/2 packets. Observers get detail 0.
        if (payload.power() == PowerEventPayload.POWER_AIZEN && payload.ability() == 2
                && payload.phase() == PowerEventPayload.PHASE_STATE_START && payload.detail() > 0) {
            illusionTicks = lifetime == INDEFINITE ? 20 * 30 : lifetime;
            illusionSourceId = payload.sourceEntityId();
            illusionStrength = payload.detail();
            illusionSeed = payload.origin() ^ ((long) payload.sourceEntityId() << 32);
        }
    }

    public static void tick() {
        Iterator<Map.Entry<EventKey, ActiveEvent>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveEvent event = iterator.next().getValue();
            if (event.ticks == INDEFINITE) continue;
            if (--event.ticks <= 0) iterator.remove();
        }
        if (illusionTicks > 0 && --illusionTicks == 0) clearIllusion();
    }

    public static boolean isActive(int power, int ability) {
        return ACTIVE.keySet().stream().anyMatch(key -> key.power == power && key.ability == ability);
    }

    public static ActiveEvent strongest(int power, int ability) {
        return ACTIVE.entrySet().stream()
                .filter(entry -> entry.getKey().power == power && entry.getKey().ability == ability)
                .map(Map.Entry::getValue)
                .max((left, right) -> Integer.compare(left.payload.detail(), right.payload.detail()))
                .orElse(null);
    }

    public static List<String> buffsFor(int sourceEntityId) {
        List<String> buffs = new ArrayList<>();
        for (Map.Entry<EventKey, ActiveEvent> entry : ACTIVE.entrySet()) {
            EventKey key = entry.getKey();
            if (key.sourceEntityId != sourceEntityId
                    || entry.getValue().payload.phase() != PowerEventPayload.PHASE_STATE_START) continue;
            String label = buffLabel(entry.getValue().payload);
            if (label != null && !buffs.contains(label)) buffs.add(label);
        }
        return buffs;
    }

    private static String buffLabel(PowerEventPayload event) {
        return switch (event.power()) {
            case PowerEventPayload.POWER_YAMATO -> event.ability() == 4 ? "Devil Trigger" : null;
            case PowerEventPayload.POWER_GOJO -> event.ability() == 3 ? "Infinity: ACTIVE"
                    : event.ability() == 6 ? "Infinity Focus" : null;
            case PowerEventPayload.POWER_VOID -> event.ability() == 5 ? "Absolute Silence" : null;
            case PowerEventPayload.POWER_DIO -> event.ability() == 3 ? "Time Stop"
                    : event.ability() == 4 ? "Time Acceleration" : null;
            case PowerEventPayload.POWER_AIZEN -> event.ability() == 4
                    ? "Hogyoku: Stage " + Math.max(1, event.detail()) : null;
            case PowerEventPayload.POWER_SHADOW -> event.ability() == 5 ? "Monarch Form" : null;
            case PowerEventPayload.POWER_NUCLEAR -> null;
            case PowerEventPayload.POWER_INFINITY_CORE -> "Infinity Core";
            default -> null;
        };
    }

    public static boolean hasIllusion() {
        return illusionTicks > 0 && illusionSourceId >= 0;
    }

    public static int illusionSourceId() {
        return illusionSourceId;
    }

    public static int illusionStrength() {
        return illusionStrength;
    }

    public static long illusionSeed() {
        return illusionSeed;
    }

    public static int illusionTicks() {
        return illusionTicks;
    }

    private static void clearIllusion() {
        illusionTicks = 0;
        illusionSourceId = -1;
        illusionStrength = 0;
        illusionSeed = 0;
    }

    public static void clear() {
        ACTIVE.clear();
        clearIllusion();
    }

    private record EventKey(int sourceEntityId, int power, int ability) {
    }

    public static final class ActiveEvent {
        private final PowerEventPayload payload;
        private int ticks;

        private ActiveEvent(PowerEventPayload payload, int ticks) {
            this.payload = payload;
            this.ticks = ticks;
        }

        public PowerEventPayload payload() {
            return payload;
        }

        public int ticks() {
            return ticks;
        }
    }
}
