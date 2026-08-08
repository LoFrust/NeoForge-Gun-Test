package com.lofrust.lgm.client.model;

import com.lofrust.lgm.item.MaximumEffort;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MaximumEffortModel extends GeoModel<MaximumEffort> {
    @SuppressWarnings("removal")
    @Override
    public ResourceLocation getModelResource(MaximumEffort animatable) {
        return ResourceLocation.fromNamespaceAndPath("lgm", "geo/item/maximum_effort.geo.json");
    }
    @SuppressWarnings("removal")
    @Override
    public ResourceLocation getTextureResource(MaximumEffort animatable) {
        return ResourceLocation.fromNamespaceAndPath("lgm", "textures/item/maximum_effort.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MaximumEffort animatable) {
        return ResourceLocation.fromNamespaceAndPath("lgm", "animations/item/model.animation.json");
    }
}

