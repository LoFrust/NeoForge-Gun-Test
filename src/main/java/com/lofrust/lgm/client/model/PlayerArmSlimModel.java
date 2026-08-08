package com.lofrust.lgm.client.model;

import com.lofrust.lgm.item.MaximumEffort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// 2. ОТДЕЛЬНЫЙ КЛАСС для тонких рук (Slim)
public class PlayerArmSlimModel extends GeoModel<MaximumEffort> {
    @Override
    public ResourceLocation getModelResource(MaximumEffort animatable) {
        return ResourceLocation.fromNamespaceAndPath("lgm", "geo/player_arm_slim.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MaximumEffort animatable) {
        AbstractClientPlayer player = (AbstractClientPlayer) Minecraft.getInstance().player;
        if (player != null) {
            return player.getSkin().texture();
        }
        // Дефолтная тонкая текстура Алекс
        return ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MaximumEffort animatable) {
        return null;
    }
}
