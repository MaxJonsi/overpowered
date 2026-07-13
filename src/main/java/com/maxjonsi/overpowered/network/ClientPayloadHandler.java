package com.maxjonsi.overpowered.network;

import com.maxjonsi.overpowered.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class ClientPayloadHandler {
    public static void handleVoidState(VoidStatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(payload.entityId());
        if (entity != null) {
            entity.setData(ModAttachments.VOID_ACTIVE.get(), payload.active());
        }
    }
}
