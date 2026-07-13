package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.item.GojoMaskItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GojoMaskRenderer extends GeoArmorRenderer<GojoMaskItem> {
    public GojoMaskRenderer() {
        super(new DefaultedItemGeoModel<>(Overpowered.id("gojo_mask")));
    }
}
