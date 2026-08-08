package com.lofrust.lgm.client;

import com.lofrust.lgm.WeaponInputHandler;
import com.lofrust.lgm.item.MaximumEffort;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import software.bernie.geckolib.animatable.GeoItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = "lgm", bus = EventBusSubscriber.Bus.GAME, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class ClientAnimationHandler {

    private static int lastSelectedSlot = -1;

    // --- 1. НАБЛЮДАТЕЛЬ ЗА СЛОТАМИ И АНИМАЦИЕЙ ДОСТАВАНИЯ ---
    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.player == null) return;

        int currentSlot = mc.player.getInventory().selected;

        if (currentSlot != lastSelectedSlot) {
            ItemStack currentStack = mc.player.getMainHandItem();

            if (currentStack.getItem() instanceof MaximumEffort
                    && mc.options.getCameraType().isFirstPerson()
                    && mc.gameRenderer.itemInHandRenderer != null) {

                long instanceId = GeoItem.getId(currentStack);
                var manager = ((software.bernie.geckolib.animatable.GeoItem) currentStack.getItem())
                        .getAnimatableInstanceCache().getManagerForId(instanceId);

                if (manager != null) {
                    var equipController = manager.getAnimationControllers().get("EquipController");
                    if (equipController != null) {
                        equipController.forceAnimationReset();
                        equipController.tryTriggerAnimation(MaximumEffort.EQUIP_NAME);
                    }
                }
            }
            lastSelectedSlot = currentSlot;
        }
    }

    // --- 2. СКРЫТИЕ КРЕСТИКА ---
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.getItem() instanceof MaximumEffort && WeaponInputHandler.isAiming()) {
            if (VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
                event.setCanceled(true);
            }
        }
    }

    // --- 3. ЗАМЕДЛЕНИЕ МЫШИ ---
    @SubscribeEvent
    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.getItem() instanceof MaximumEffort && WeaponInputHandler.isAiming()) {
            double currentSensitivity = event.getMouseSensitivity();
            event.setMouseSensitivity(currentSensitivity * 0.4);
        }
    }

    private static boolean wasBobbingEnabled = true;
    private static boolean isBobbingOverridden = false;

    // --- 4. ГАРАНТИРОВАННОЕ ОТКЛЮЧЕНИЕ ТРЯСКИ НА УРОВНЕ ДВИЖКА ДЛЯ NEOFORGE 1.21.1 ---
    @SubscribeEvent
    public static void onRenderFramePre(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        ItemStack mainHand = mc.player.getMainHandItem();
        boolean isAimingNow = mainHand.getItem() instanceof MaximumEffort && WeaponInputHandler.isAiming();

        if (isAimingNow) {
            // Если игрок целится, временно убираем ванильную галочку "Покачивание камеры"
            if (!isBobbingOverridden) {
                wasBobbingEnabled = mc.options.bobView().get();
                isBobbingOverridden = true;
            }
            mc.options.bobView().set(false); // Полностью отключаем тряску рук и камеры
        } else {
            // Как только игрок отпустил ПКМ — мгновенно возвращаем его настройку обратно
            if (isBobbingOverridden) {
                mc.options.bobView().set(wasBobbingEnabled);
                isBobbingOverridden = false;
            }
        }
    }
    @SubscribeEvent
    public static void onLoggingOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (isBobbingOverridden && mc.options != null) {
            mc.options.bobView().set(wasBobbingEnabled);
            isBobbingOverridden = false;
        }
    }
}
