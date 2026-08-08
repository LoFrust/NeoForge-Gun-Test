package com.lofrust.lgm;

import com.lofrust.lgm.item.MaximumEffort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import static com.lofrust.lgm.item.MaximumEffort.*;

@EventBusSubscriber(modid = LGM.MODID, value = Dist.CLIENT)
public class WeaponInputHandler {

    public static final KeyMapping MELEE_KEY = new KeyMapping(
            "key.gunstest.melee",
            GLFW.GLFW_KEY_X,
            "key.categories.gunstest"
    );

    public static final KeyMapping RELOAD_KEY = new KeyMapping(
            "key.gunstest.reload",
            GLFW.GLFW_KEY_R,
            "key.categories.gunstest"
    );

    private static boolean isClientReloading = false;
    private static long clientReloadEndTime = 0L;

    private static boolean checkReloadBlock(Player player, ItemStack mainHand) {
        if (mainHand.getItem() instanceof MaximumEffort) {
            // Если текущее время компьютера всё ещё меньше, чем запланированное время окончания
            if (isClientReloading && System.currentTimeMillis() < clientReloadEndTime) {
                return true; // Блокировка активна!
            } else {
                isClientReloading = false; // Время вышло — сбрасываем флаг
            }
        } else {
            isClientReloading = false; // Если пушку убрали из рук — сбрасываем
        }
        return false;
    }

    private static boolean isRightClickHeld = false;
    private static boolean isSprinting = false;

    // Для отслеживания смены рук
    private static ItemStack lastMainHand = ItemStack.EMPTY;
    private static ItemStack lastOffHand = ItemStack.EMPTY;

    public static boolean isSprinting() {
        return isSprinting;
    }

    public static boolean isAiming() {
        return isRightClickHeld;
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isAiming = isRightClickHeld;

        // Проверяем, спринтует ли игрок через ванильную систему
        boolean isVanillaSprinting = mc.player.isSprinting();

        // Если зажат прицел - принудительно отключаем спринт
        if (isAiming) {
            if (isVanillaSprinting) {
                mc.player.setSprinting(false);
            }
            isSprinting = false;
        } else {
            // Используем ванильный спринт (Ctrl ИЛИ двойной W)
            isSprinting = isVanillaSprinting;
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack currentMain = mc.player.getMainHandItem();
        ItemStack currentOff = mc.player.getOffhandItem();

        if (checkReloadBlock(mc.player, mc.player.getMainHandItem())) {
            mc.player.setSprinting(false);
        }

        // Проверяем, изменилась ли ситуация с руками
        boolean mainChanged = !ItemStack.isSameItemSameComponents(lastMainHand, currentMain);
        boolean offChanged = !ItemStack.isSameItemSameComponents(lastOffHand, currentOff);

        if (mainChanged || offChanged) {
            // Проверяем, есть ли оружие вообще в руках
            boolean hasGunInMain = currentMain.getItem() instanceof MaximumEffort;
            boolean hasGunInOff = currentOff.getItem() instanceof MaximumEffort;
            boolean hadGunInMain = lastMainHand.getItem() instanceof MaximumEffort;
            boolean hadGunInOff = lastOffHand.getItem() instanceof MaximumEffort;

            // Если оружие было в руках, а теперь его нет - сбрасываем прицеливание
            if ((hadGunInMain || hadGunInOff) && !(hasGunInMain || hasGunInOff)) {
                isRightClickHeld = false;
            }

            // Если оружие переложили из одной руки в другую - прицеливание должно остаться
            // НО нужно проверить, что игрок всё ещё держит ПКМ
            // Используем GLFW для проверки состояния кнопки
            if (mc.getWindow().getWindow() != 0) {
                int rightButtonState = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_2);
                if (rightButtonState != GLFW.GLFW_PRESS) {
                    isRightClickHeld = false;
                }
            }

            lastMainHand = currentMain.copy();
            lastOffHand = currentOff.copy();
        }
    }

    // Таймер кулдауна удара (в миллисекундах)
    private static long lastMeleeTime = 0L;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MELEE_KEY);
        event.register(RELOAD_KEY);
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (mc.screen != null) {
            isRightClickHeld = false;
            return;
        }

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (checkReloadBlock(player, mainHand)) {
            isRightClickHeld = false; // Сбрасываем прицеливание
            event.setCanceled(true);  // Отменяем клик
            return;
        }

        // Фиксируем зажатие ПКМ
        if (event.getButton() == 1 && mainHand.getItem() instanceof MaximumEffort && offHand.isEmpty()) {
            if (event.getAction() == 1) {
                isRightClickHeld = true;
                if (player != null) {
                    player.setSprinting(false);
                }
            } else if (event.getAction() == 0) {
                isRightClickHeld = false;
            }
            event.setCanceled(true);
        }

        // ЛКМ - выстрел или меле
        if (mainHand.getItem() instanceof MaximumEffort gunItem) {
            if (event.getButton() == 0 && event.getAction() == 1) {
                // Проверяем, зажат ли ПКМ (прицеливание)
                boolean isAiming = isRightClickHeld;

                if (!offHand.isEmpty()) {
                    performShortMeleeAttack(player, InteractionHand.MAIN_HAND);
                } else {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ShootPacket(isAiming));
                }
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (checkReloadBlock(player, mainHand)) {
            MELEE_KEY.consumeClick(); // Очищаем клик, чтобы удар не "залип" и не ударил после перезарядки
            RELOAD_KEY.consumeClick();
            return;
        }

        if (MELEE_KEY.consumeClick()) {
            boolean hasGunInMain = mainHand.getItem() instanceof MaximumEffort;
            boolean hasGunInOff = offHand.getItem() instanceof MaximumEffort;

            if (isRightClickHeld) {
                return; // Игнорируем нажатие X, если зажат ПКМ
            }

            long currentTime = System.currentTimeMillis();
            if (hasGunInMain && (currentTime - lastMeleeTime < 1250L)) {
                return;
            }

            if (hasGunInOff) {
                performShortMeleeAttack(player, InteractionHand.OFF_HAND);
            } else if (hasGunInMain && offHand.isEmpty()) {
                performShortMeleeAttack(player, InteractionHand.MAIN_HAND);
            }
        }

        if (RELOAD_KEY.consumeClick()) {
            if (mainHand.getItem() instanceof MaximumEffort gunItem) {

                // === ДОБАВЛЯЕМ ПРОВЕРКУ НА ПАТРОНЫ ПЕРЕД БЛОКИРОВКОЙ ===
                // Замени методы getAmmo и getMaxAmmo на твои реальные методы из класса MaximumEffort!
                // Если у тебя патроны проверяются по-другому, напиши проверку: текущие < максимальных.
                boolean isMagazineFull = gunItem.getAmmo(mainHand) >= gunItem.getMaxAmmo(mainHand);

                // Если магазин ПОЛНЫЙ — мы просто игнорируем нажатие и НЕ включаем блокировку
                if (isMagazineFull) {
                    return;
                }

                // Проверяем, не идёт ли уже перезарядка на клиенте
                if (!checkReloadBlock(player, mainHand)) {

                    // ВКЛЮЧАЕМ БЛОКИРОВКУ НА КЛИЕНТЕ:
                    isClientReloading = true;
                    // Переводим тики оружия в миллисекунды (40 * 50мс = 2000мс)
                    clientReloadEndTime = System.currentTimeMillis() + ((long) gunItem.getReloadTime(mainHand) * 50);

                    // Насильно отключаем прицеливание и спринт сразу при нажатии R
                    isRightClickHeld = false;
                    player.setSprinting(false);

                    // Отправляем пакет на сервер (СТРОГО ОДИН РАЗ)
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ReloadPacket());
                }
            }
        }
    }

    private static void performShortMeleeAttack(Player player, InteractionHand swingingHand) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack gunStack = player.getItemInHand(swingingHand);

        if (!(gunStack.getItem() instanceof MaximumEffort gunItem)) return;

        // НЕ СБРАСЫВАЕМ ПРИЦЕЛИВАНИЕ!
        // isRightClickHeld = false; // УДАЛЕНО!

        // Обновляем таймер
        lastMeleeTime = System.currentTimeMillis();

        // Взмах и анимация
        player.swing(swingingHand);

        long id = software.bernie.geckolib.animatable.GeoItem.getId(gunStack);
        var manager = gunItem.getAnimatableInstanceCache().getManagerForId(id);
        if (manager != null) {
            if (player.isSprinting()) {
                gunItem.triggerAnim(player, id, SPRINT_CONTROLLER_NAME, SPRINT_MELEE_TRIGGER_NAME);
            } else {
                // Обычный выстрел от бедра в дефолтном контроллере выстрела
                gunItem.triggerAnim(player, id, CONTROLLER_NAME, MELEE_TRIGGER_NAME);
            }
        }

        player.level().playSound(
                player, player.getX(), player.getY(), player.getZ(),
                com.lofrust.lgm.sound.ModSounds.MAXIMUM_EFFORT_MELEE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

        // Проверка урона
        double shortReach = 2.5;
        HitResult hitResult = mc.hitResult;

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity target = entityHit.getEntity();

            if (player.distanceTo(target) <= shortReach) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new MeleeAttackPacket(target.getId()));
            }
        }
    }

}