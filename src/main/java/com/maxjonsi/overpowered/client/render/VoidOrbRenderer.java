package com.maxjonsi.overpowered.client.render;

import com.maxjonsi.overpowered.Overpowered;
import com.maxjonsi.overpowered.item.VoidOrbItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class VoidOrbRenderer extends GeoItemRenderer<VoidOrbItem> {
    public VoidOrbRenderer() {
        super(new DefaultedItemGeoModel<>(Overpowered.id("void_orb")));
    }
}
