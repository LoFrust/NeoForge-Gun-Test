package com.lofrust.lgm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class RecoilHandler {
    private static float targetRecoilUp = 0.0f;
    private static float targetRecoilRecovery = 0.0f;
    private static long hoverEndTime = 0; // Время окончания зависания в мс

    // НАСТРОЙКА СКОРОСТИ (в градусах за ОДНУ МИЛЛИСЕКУНДУ)
    // 0.01f означает: за 100 мс камера поднимется на 1 градус.
    private static final float UP_SPEED_PER_MS = 0.08f;
    private static final float DOWN_SPEED_PER_MS = 0.03f;

    // Переменная для отслеживания времени между кадрами рендера
    private static long lastFrameTime = 0;

    public static void addRecoil(float upForce, float recoveryForce, int hoverDurationMs) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        float currentXRot = player.getXRot();
        float maxPossibleUp = currentXRot - (-90.0f);

        float realUpForce = Math.min(upForce, maxPossibleUp);
        float compensationFactor = upForce > 0 ? (realUpForce / upForce) : 1.0f;
        float realRecoveryForce = recoveryForce * compensationFactor;

        targetRecoilUp += realUpForce;
        targetRecoilRecovery += realRecoveryForce;

        // Устанавливаем точное системное время, когда зависание должно закончиться
        hoverEndTime = System.currentTimeMillis() + hoverDurationMs;
    }

    // ЭТОТ МЕТОД ВЫЗЫВАЕТСЯ КАЖДЫЙ КАДР РЕНДЕРА (FPS)
    public static void onRenderFrame() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastFrameTime = 0;
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Если это самый первый кадр, инициализируем таймер и выходим
        if (lastFrameTime == 0) {
            lastFrameTime = currentTime;
            return;
        }

        // Вычисляем, сколько РЕАЛЬНЫХ миллисекунд прошло с предыдущего кадра рендера
        long deltaTimeMs = currentTime - lastFrameTime;
        lastFrameTime = currentTime;

        // Защита от огромного скачка (например, при фризе игры во время загрузки чанков)
        if (deltaTimeMs > 100) {
            deltaTimeMs = 16; // Считаем как дефолтный кадр при 60 FPS
        }

        // 1. ПЛАВНАЯ ФАЗА ВВЕРХ (на основе реального времени)
        if (targetRecoilUp > 0.0f) {
            float step = Math.min(UP_SPEED_PER_MS * deltaTimeMs, targetRecoilUp);
            player.setXRot(Mth.clamp(player.getXRot() - step, -90.0f, 90.0f));
            targetRecoilUp -= step;
        }

        // ПРОВЕРКА ЗАВИСАНИЯ (сравниваем с системными часами)
        if (currentTime < hoverEndTime) {
            return; // Камера зависает, фаза вниз ждет
        }

        // 2. ПЛАВНАЯ ФАЗА ВНИЗ (на основе реального времени)
        if (targetRecoilRecovery > 0.0f && targetRecoilUp <= 0.0f) {
            float step = Math.min(DOWN_SPEED_PER_MS * deltaTimeMs, targetRecoilRecovery);
            player.setXRot(Mth.clamp(player.getXRot() + step, -90.0f, 90.0f));
            targetRecoilRecovery -= step;
        }
    }

    // Сброс таймера при перезаходе в мир
    public static void reset() {
        lastFrameTime = 0;
        targetRecoilUp = 0;
        targetRecoilRecovery = 0;
        hoverEndTime = 0;
    }
}
