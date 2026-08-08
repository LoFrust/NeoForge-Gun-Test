package com.lofrust.lgm.client.model;

import com.lofrust.lgm.item.MaximumEffort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 1. Модель для обычных рук (Steve)
public class PlayerArmModel extends GeoModel<MaximumEffort> {
    @Override
    public ResourceLocation getModelResource(MaximumEffort animatable) {
        return ResourceLocation.fromNamespaceAndPath("lgm", "geo/player_arm_steve.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MaximumEffort animatable) {
        AbstractClientPlayer player = (AbstractClientPlayer) Minecraft.getInstance().player;
        if (player != null) {
            return player.getSkin().texture();
        }
        // Дефолтная широкая текстура Стива
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MaximumEffort animatable) {
        return null;
    }
}

