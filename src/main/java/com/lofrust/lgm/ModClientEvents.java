package com.lofrust.lgm;

import com.lofrust.lgm.client.RecoilHandler;
import com.lofrust.lgm.item.MaximumEffort;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = LGM.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // AFTER_SKY срабатывает каждый кадр рендера перед выводом остального мира на экран
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            RecoilHandler.onRenderFrame();
        }
    }

    // Уникальный ID модификатора скорости на клиенте
    private static final ResourceLocation CLIENT_AIM_SLOWDOWN_ID = ResourceLocation.fromNamespaceAndPath(LGM.MODID, "client_aim_slowdown");

    // Модификатор скорости: уменьшает скорость на 35% (-0.35)
    private static final AttributeModifier SLOWDOWN_MODIFIER = new AttributeModifier(
            CLIENT_AIM_SLOWDOWN_ID, -0.35D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
    );

    // 1. ЭФФЕКТ ПРИБЛИЖЕНИЯ КАМЕРЫ (ЗУМ)
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (mainHand.getItem() instanceof MaximumEffort && offHand.isEmpty()) {
            if (WeaponInputHandler.isAiming()) {
                event.setNewFovModifier(event.getFovModifier() * 0.7F);
            }
        }
    }

    // 2. ИСПРАВЛЕНО: ЭФФЕКТ ЗАМЕДЛЕНИЯ ХОДЬБЫ НА КЛИЕНТЕ
    // Обрабатываем каждый тик на клиенте, чтобы скорость менялась мгновенно и без пинга
    @SubscribeEvent
    public static void onClientPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Выполняем код только для локального игрока (для себя) и только на клиенте
        if (player == null || !player.level().isClientSide() || player != Minecraft.getInstance().player) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (speedAttribute != null) {
            // Если в руке пушка, левая рука пуста и зажат прицел (ПКМ)
            if (mainHand.getItem() instanceof MaximumEffort && offHand.isEmpty() && WeaponInputHandler.isAiming()) {
                // Если модификатора еще нет — накладываем его
                if (!speedAttribute.hasModifier(CLIENT_AIM_SLOWDOWN_ID)) {
                    speedAttribute.addTransientModifier(SLOWDOWN_MODIFIER);
                }
            } else {
                // Если игрок отпустил ПКМ, или убрал пушку — мгновенно удаляем замедление
                if (speedAttribute.hasModifier(CLIENT_AIM_SLOWDOWN_ID)) {
                    speedAttribute.removeModifier(CLIENT_AIM_SLOWDOWN_ID);
                }
            }
        }
    }
}
