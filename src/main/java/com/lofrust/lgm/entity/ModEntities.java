package com.lofrust.lgm.entity;

import com.lofrust.lgm.LGM;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LGM.MODID);

    // Ваша существующая пуля
    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET =
            ENTITIES.register("bullet", () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.02F, 0.02F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("bullet"));

    // ДОБАВЛЕНО: Регистрация сущности дырки от пули
    public static final DeferredHolder<EntityType<?>, EntityType<BulletHoleEntity>> BULLET_HOLE =
            ENTITIES.register("bullet_hole", () -> EntityType.Builder.<BulletHoleEntity>of(BulletHoleEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F) // Крошечный размер хитбокса
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("bullet_hole"));
}
