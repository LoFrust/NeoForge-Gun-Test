/*
package com.lofrust.gunstest.item;

import com.lofrust.gunstest.Gunstest;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gunstest.MOD_ID);

    public static final DeferredItem<Item> PATRON = ITEMS.register("patron", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> GUN = ITEMS.register("gun", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
*/
package com.lofrust.lgm.item;

import com.lofrust.lgm.LGM;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;


public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LGM.MODID);

    // Само оружие
    public static final DeferredItem<Item> MAXIMUM_EFFORT = ITEMS.register("maximum_effort",
            () -> new MaximumEffort(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> HEAVY_AMMO = ITEMS.register("heavy_ammo",
            () -> new Item(new Item.Properties().stacksTo(60)));
    public static final DeferredItem<Item> BULLET_MODEL_ITEM = ITEMS.register("bullet_model_item",
            () -> new Item(new Item.Properties()));
}
