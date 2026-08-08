package com.lofrust.lgm.client.renderer;

import com.lofrust.lgm.client.model.MaximumEffortModel;
import com.lofrust.lgm.client.model.PlayerArmModel;
import com.lofrust.lgm.client.model.PlayerArmSlimModel;
import com.lofrust.lgm.item.MaximumEffort;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public class MaximumEffortRenderer extends GeoItemRenderer<MaximumEffort> {
    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("lgm", "textures/item/maximum_effort_e.png");

    private boolean isReloadingNow = false;
    private final PlayerArmModel steveArmModel = new PlayerArmModel();
    private final PlayerArmSlimModel slimArmModel = new PlayerArmSlimModel();
    private String getCurrentAnimationName(MaximumEffort animatable) {
        long id = GeoItem.getId(animatable.getDefaultInstance());
        var manager = animatable.getAnimatableInstanceCache().getManagerForId(id);
        if (manager != null) {
            // Проверяем ТОЛЬКО контроллер перезарядки
            var reloadController = manager.getAnimationControllers().get(MaximumEffort.RELOAD_CONTROLLER_NAME);
            if (reloadController != null && reloadController.getCurrentAnimation() != null) {
                return reloadController.getCurrentAnimation().animation().name();
            }
        }
        return "";
    }

    public MaximumEffortRenderer() {
        super(new MaximumEffortModel());


        this.addRenderLayer(new GeoRenderLayer<MaximumEffort>(this) {
            @Override
            public void render(PoseStack poseStack, MaximumEffort animatable,
                               BakedGeoModel weaponModel, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer,
                               float partialTick, int packedLight, int packedOverlay) {

                ItemDisplayContext currentContext = MaximumEffortRenderer.this.renderPerspective;
                if (currentContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND &&
                        currentContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                    return;
                }

                AbstractClientPlayer player = (AbstractClientPlayer) Minecraft.getInstance().player;
                if (player == null) return;

                ResourceLocation skinTexture = player.getSkin().texture();

                // Проверенное рабочее условие проверки типа скина через Enum
                boolean isSlim = player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
                boolean isReloading = false;
                long instanceId = software.bernie.geckolib.animatable.GeoItem.getId(MaximumEffortRenderer.this.currentItemStack);
                var manager = animatable.getAnimatableInstanceCache().getManagerForId(instanceId);
                if (manager != null) {
                    var reloadController = manager.getAnimationControllers().get(MaximumEffort.RELOAD_CONTROLLER_NAME);
                    if (reloadController != null && reloadController.getCurrentAnimation() != null) {
                        // Анимация активна, если состояние НЕ STOPPED И имя "reload"
                        if (reloadController.getAnimationState() != software.bernie.geckolib.animation.AnimationController.State.STOPPED &&
                                "reload".equals(reloadController.getCurrentAnimation().animation().name())) {
                            isReloading = true;
                        }
                    }
                }

                // Получаем запеченную модель СТРОГО в зависимости от типа скина
                BakedGeoModel armBakedModel;
                if (isSlim) {
                    ResourceLocation geo = MaximumEffortRenderer.this.slimArmModel.getModelResource(animatable);
                    armBakedModel = MaximumEffortRenderer.this.slimArmModel.getBakedModel(geo);
                } else {
                    ResourceLocation geo = MaximumEffortRenderer.this.steveArmModel.getModelResource(animatable);
                    armBakedModel = MaximumEffortRenderer.this.steveArmModel.getBakedModel(geo);
                }

                if (armBakedModel == null) return;

                RenderType armRenderType = RenderType.entityCutoutNoCull(skinTexture);
                VertexConsumer armBuffer = bufferSource.getBuffer(armRenderType);

                // Рендерим правую руку
                renderHand(poseStack, weaponModel, "right_hand_anchor", armBakedModel,
                        armRenderType, armBuffer, bufferSource, animatable,
                        partialTick, packedLight, packedOverlay, false, isSlim, isReloading);

                // Рендерим левую руку
                renderHand(poseStack, weaponModel, "left_hand_anchor", armBakedModel,
                        armRenderType, armBuffer, bufferSource, animatable,
                        partialTick, packedLight, packedOverlay, true, isSlim, isReloading);

            }

            private void renderHand(PoseStack poseStack, BakedGeoModel weaponModel, String boneName,
                                    BakedGeoModel armModel, RenderType renderType,
                                    VertexConsumer buffer, MultiBufferSource bufferSource,
                                    MaximumEffort animatable,
                                    float partialTick, int packedLight, int packedOverlay,
                                    boolean isLeft, boolean isSlim, boolean isReloading) {
                java.util.Optional<GeoBone> boneOpt = weaponModel.getBone(boneName);
                if (boneOpt.isEmpty()) return;

                GeoBone bone = boneOpt.get();

                if (isLeft && bone.getScaleX() <= 0.01f) {
                    return;
                }

                // === СКРЫТИЕ ЛИШНИХ КОСТЕЙ ИЗ ФАЙЛА РУК ===
                // ВАЖНО: Проверь в Blockbench, чтобы главные папки (кости) рук в твоих .geo.json
                // назывались именно "right_arm" и "left_arm". Если там заглавные буквы (например, "RightArm"), измени строки ниже!
                java.util.Optional<GeoBone> armFileRightBone = armModel.getBone("right_arm");
                java.util.Optional<GeoBone> armFileLeftBone = armModel.getBone("left_arm");

                if (isLeft) {
                    // Рендерим левый якорь оружия -> скрываем правую руку из файла, показываем левую
                    armFileRightBone.ifPresent(b -> b.setHidden(true));
                    armFileLeftBone.ifPresent(b -> b.setHidden(false));
                } else {
                    // Рендерим правый якорь оружия -> показываем правую руку из файла, скрываем левую
                    armFileRightBone.ifPresent(b -> b.setHidden(false));
                    armFileLeftBone.ifPresent(b -> b.setHidden(true));
                }

                poseStack.pushPose();
                RenderUtil.prepMatrixForBone(poseStack, bone);

                if (!isLeft) {
                    // Твои настройки для правой руки
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(0));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(5));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(0));

                    poseStack.translate(0.0, 0.7, -0.9);
                    poseStack.scale(5.0f, 5.0f, 5.0f);

                } else {
                    // ТОЧЕЧНО: Полностью заменить внутренности блока else (левая рука) на этот код
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(0));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(0));

                    if (isReloading) {
                        poseStack.translate(0.0, 0.7, -0.9); // Смещение левой руки во время перезарядки
                    } else {
                        poseStack.translate(0.0, 0.7, -10.9);   // Обычное положение левой руки в покое
                    }

                    poseStack.scale(5.0f, 5.0f, 5.0f);
                }

                // Рендерим через GeckoLib. Теперь на экране появится только та рука, которую мы не скрыли выше!
                MaximumEffortRenderer.this.reRender(armModel, poseStack, bufferSource, animatable,
                        renderType, buffer, partialTick, packedLight, packedOverlay,
                        0xFFFFFFFF);

                poseStack.popPose();
            }
        });
    }

    public static void stopReloadAnimation(ItemStack stack) {
        if (stack.getItem() instanceof MaximumEffort gunItem) {
            long id = GeoItem.getId(stack);
            var manager = gunItem.getAnimatableInstanceCache().getManagerForId(id);
            if (manager != null) {
                var reloadController = manager.getAnimationControllers().get(MaximumEffort.RELOAD_CONTROLLER_NAME);
                if (reloadController != null) {
                    reloadController.stop();
                    reloadController.forceAnimationReset();
                    // Возвращаем скорость на нормальную для других анимаций
                    reloadController.setAnimationSpeed(1.0f);
                }
            }
        }
    }


    @Override
    public void preRender(PoseStack poseStack, MaximumEffort animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {

        ItemDisplayContext renderContext = this.renderPerspective;
        ItemStack currentStack = this.currentItemStack;

        if (!isReRender && currentStack != null) {
            long instanceId = GeoItem.getId(currentStack);
            var manager = animatable.getAnimatableInstanceCache().getManagerForId(instanceId);

            if (manager != null) {
                boolean isFirstPerson = renderContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        || renderContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
                boolean isGuiContext = renderContext == ItemDisplayContext.GUI || renderContext == ItemDisplayContext.FIXED;

                var shootController = manager.getAnimationControllers().get(MaximumEffort.CONTROLLER_NAME);
                if (shootController != null) {
                    if (isGuiContext || !isFirstPerson) {
                        shootController.setAnimationSpeed(0.0f);
                        shootController.stop();
                    } else {
                        shootController.setAnimationSpeed(1.0f);
                    }
                }

                var equipController = manager.getAnimationControllers().get("EquipController");
                if (equipController != null) {
                    if (isGuiContext || !isFirstPerson) {
                        equipController.setAnimationSpeed(0.0f);
                        equipController.stop();
                    } else {
                        equipController.setAnimationSpeed(1.0f);
                    }
                }

                var aimController = manager.getAnimationControllers().get("AimController");
                if (aimController != null) {
                    if (isGuiContext || !isFirstPerson) {
                        aimController.setAnimationSpeed(0.0f);
                        aimController.stop();
                    } else {
                        aimController.setAnimationSpeed(1.0f);
                    }
                }

                var sprintController = manager.getAnimationControllers().get(MaximumEffort.SPRINT_CONTROLLER_NAME);
                if (sprintController != null) {
                    if (isGuiContext || !isFirstPerson) {
                        sprintController.setAnimationSpeed(0.0f);
                        sprintController.stop();
                    } else {
                        sprintController.setAnimationSpeed(1.0f);
                    }
                }


            }
        }

        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void postRender(PoseStack poseStack, MaximumEffort animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer,
                           boolean isReRender, float partialTick, int packedLight,
                           int packedOverlay, int colour) {

        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        if (!isReRender) {
            RenderType glowRenderType = RenderType.eyes(GLOW_TEXTURE);
            VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);

            this.reRender(model, poseStack, bufferSource, animatable, glowRenderType,
                    glowBuffer, partialTick, 15728880, packedOverlay, colour);
        }
    }
}