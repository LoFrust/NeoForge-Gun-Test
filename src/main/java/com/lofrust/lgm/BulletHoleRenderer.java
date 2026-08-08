package com.lofrust.lgm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = LGM.MODID, value = Dist.CLIENT)
public class BulletHoleRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LGM.MODID, "textures/particle/bullet_hole.png");

    // Наш собственный, абсолютно изолированный и безопасный список дырок
    private static final List<BulletHole> HOLES = new CopyOnWriteArrayList<>();

    public static void addHole(Vec3 pos, Direction face, int color) {
        HOLES.add(new BulletHole(pos, face, color));
    }

    // 1. ТАЙМЕР ЖИЗНИ КЛИЕНТА: Уменьшаем время жизни дырок каждую миллисекунду
    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            for (BulletHole hole : HOLES) {
                hole.ticksLeft--;
                if (hole.ticksLeft <= 0) {
                    HOLES.remove(hole);
                }
            }
        }
    }

    // 2. СТАБИЛЬНЫЙ МИРОВОЙ РЕНДЕРИНГ
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Используем стадию AFTER_ENTITIES, она гарантирует точные матрицы проекции в 1.21.1
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || HOLES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Используем честный полупрозрачный слой
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

        for (BulletHole hole : HOLES) {
            poseStack.pushPose();

            // Смещаем матрицу относительно камеры игрока
            poseStack.translate(hole.pos.x - cameraPos.x, hole.pos.y - cameraPos.y, hole.pos.z - cameraPos.z);

            switch (hole.face) {
                case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                case NORTH -> {}
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }

            // Выдвигаем пиксель на сантиметр вперед от стены блока
            poseStack.translate(0.0D, 0.0D, -0.01D);

            float size = 0.04F;
            Matrix4f matrix = poseStack.last().pose();

            // Прозрачность: 2 сек висит (80-40 тиков) + 2 сек плавно тает (40-0 тиков)
            // ИСПРАВЛЕНО НА 80% ПЛОТНОСТИ: Стартовое значение альфы выставляем на 200 вместо 255
            int alpha = 200;

            if (hole.ticksLeft < 40) { // Последние 2 секунды запускаем плавное растворение
                float fadeProgress = (float) hole.ticksLeft / 40.0F;
                // Таем плавно от 200 до 0
                alpha = (int) (fadeProgress * 200);
            }

            if (alpha < 0) alpha = 0;
            if (alpha > 200) alpha = 200;


            // Цвет пробитого блока
            int r = (hole.color >> 16) & 0xFF;
            int g = (hole.color >> 8) & 0xFF;
            int b = hole.color & 0xFF;

            // Максимальное освещение, чтобы дырка не чернела в темноте
            int fullLight = 15728880;

            // ИСПРАВЛЕНО ДЛЯ 1.21.1: Каждой вершине добавлены обязательные методы .setOverlay() и .setNormal()
            // Вершина 1
            builder.addVertex(matrix, -size, -size, 0.0F)
                    .setColor(r, g, b, alpha)
                    .setUv(0.0F, 0.0F)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(fullLight)
                    .setNormal(0.0F, 0.0F, -1.0F);
            // Вершина 2
            builder.addVertex(matrix, size, -size, 0.0F)
                    .setColor(r, g, b, alpha)
                    .setUv(1.0F, 0.0F)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(fullLight)
                    .setNormal(0.0F, 0.0F, -1.0F);
            // Вершина 3
            builder.addVertex(matrix, size, size, 0.0F)
                    .setColor(r, g, b, alpha)
                    .setUv(1.0F, 1.0F)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(fullLight)
                    .setNormal(0.0F, 0.0F, -1.0F);
            // Вершина 4
            builder.addVertex(matrix, -size, size, 0.0F)
                    .setColor(r, g, b, alpha)
                    .setUv(0.0F, 1.0F)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(fullLight)
                    .setNormal(0.0F, 0.0F, -1.0F);

            poseStack.popPose();
        }

        bufferSource.endBatch(RenderType.entityTranslucent(TEXTURE));
    }

}
