package com.lofrust.lgm;

import com.lofrust.lgm.entity.ModEntities;
import com.lofrust.lgm.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;



@Mod(LGM.MODID)
public class LGM {
    public static final String MODID = "lgm";
    public LGM(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        com.lofrust.lgm.sound.ModSounds.register(modEventBus);

        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerPackets);

        // ТА САМАЯ СТРОКА: Подключаем наш новый мировой рендерер дырок к шине NeoForge!
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(BulletHoleRenderer.class);
    }


    private void registerRenderers(final net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        // Рендерер летящей пули (остается без изменений)
        event.registerEntityRenderer(ModEntities.BULLET.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

        // ИСПРАВЛЕНО: Вместо ThrownItemRenderer подключаем наш кастомный BulletHoleEntityRenderer!

    }


    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MODID);

        registrar.playToServer(
                ShootPacket.TYPE,
                ShootPacket.CODEC,
                ShootPacket::handle
        );

        registrar.playToServer(
                ReloadPacket.TYPE,
                ReloadPacket.CODEC,
                ReloadPacket::handle
        );

        registrar.playToClient(
                BulletHolePacket.TYPE,
                BulletHolePacket.CODEC,
                BulletHolePacket::handle
        );
        registrar.playToServer(
                MeleeAttackPacket.TYPE,
                MeleeAttackPacket.CODEC,
                MeleeAttackPacket::handle
        );

    }

}
