package com.lofrust.lgm.item;

import com.lofrust.lgm.entity.BulletEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;
import com.lofrust.lgm.sound.ModSounds;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.constant.DataTickets;


public class MaximumEffort extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean isMeleeActive = false;

    public static final String CONTROLLER_NAME = "gun_controller";
    public static final String SHOOT_TRIGGER_NAME = "shoot_trigger";
    public static final String EQUIP_NAME = "equip_trigger";
    public static final String AIM_SHOOT_TRIGGER_NAME = "shoot_aim_trigger";
    public static final String MELEE_TRIGGER_NAME = "melee_trigger";
    public static final String SPRINT_CONTROLLER_NAME = "sprint_controller";
    public static final String SPRINT_SHOOT_TRIGGER_NAME = "sprint_shoot_trigger";
    public static final String SPRINT_MELEE_TRIGGER_NAME = "sprint_melee_trigger";
    public static final String RELOAD_TRIGGER_NAME = "reload_trigger";
    public static final String RELOAD_CONTROLLER_NAME = "reload_controller";

    public MaximumEffort(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 1. КОНТРОЛЛЕР ДЛЯ ДОСТАВАНИЯ (EQUIP)
        AnimationController<MaximumEffort> equipController = new AnimationController<>(this, "EquipController", 0, state -> {
            if (state.getController().getCurrentAnimation() == null) {
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        });
        equipController.triggerableAnim(EQUIP_NAME, RawAnimation.begin().then("equip", Animation.LoopType.HOLD_ON_LAST_FRAME));


        // 2. КОНТРОЛЛЕР ДЛЯ ВЫСТРЕЛА - ПРОСТОЙ И РАБОЧИЙ
        AnimationController<MaximumEffort> actionController = new AnimationController<>(this, CONTROLLER_NAME, 0, state -> {
            var currentAnim = state.getController().getCurrentAnimation();
            String currentAnimName = currentAnim != null ? currentAnim.animation().name() : "";

            // Если сейчас играет выстрел или удар - блокируем всё остальное
            if (currentAnimName.equals("shoot") || currentAnimName.equals("melee")) {
                return PlayState.CONTINUE;
            }

            return PlayState.CONTINUE;
        });
        actionController.triggerableAnim(SHOOT_TRIGGER_NAME,
                RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE)
        );
        actionController.triggerableAnim(MELEE_TRIGGER_NAME,
                RawAnimation.begin().then("melee", Animation.LoopType.PLAY_ONCE)
        );

        AnimationController<MaximumEffort> aimController = new AnimationController<>(this, "AimController", 0, state -> {
            boolean isAiming = com.lofrust.lgm.WeaponInputHandler.isAiming();
            var currentAnim = state.getController().getCurrentAnimation();
            String currentAnimName = currentAnim != null ? currentAnim.animation().name() : "";

            // 1. ПРИЦЕЛИВАНИЕ АКТИВНО
            if (isAiming) {
                // Если уже играет анимация вскидывания (aim) ИЛИ мы стоим в статичном прицеле (aim_static) после выстрела
                // ИЛИ прямо сейчас проигрывается сам выстрел (aim_shoot) — ПРОДОЛЖАЕМ играть и не перезапускаем!

                if (currentAnimName.equals("aim") || currentAnimName.equals("aim_static") || currentAnimName.equals("aim_shoot")) {
                    return PlayState.CONTINUE;
                }

                // Первичное вхождение в прицел (играет только один раз при зажатии ПКМ)
                return state.setAndContinue(RawAnimation.begin().then("aim", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
            // 2. ИГРОК ОТПУСТИЛ ПРИЦЕЛ
            else {
                // Если уже играет убирание прицела — продолжаем
                if (currentAnimName.equals("unaim")) {
                    return PlayState.CONTINUE;
                }
                // Если мы были в прицеле и отпустили — плавно играем unaim
                return state.setAndContinue(RawAnimation.begin().then("unaim", Animation.LoopType.PLAY_ONCE));
            }
        });
        aimController.triggerableAnim(AIM_SHOOT_TRIGGER_NAME,
                RawAnimation.begin()
                        .then("aim_shoot", Animation.LoopType.PLAY_ONCE)       // 1. Проигрываем выстрел в прицеле
                        .then("aim_static", Animation.LoopType.HOLD_ON_LAST_FRAME) // 2. Насильно и намертво встаем в aim_static
        );

        AnimationController<MaximumEffort> sprintController = new AnimationController<>(this, SPRINT_CONTROLLER_NAME, 0, state -> {
            boolean isSprinting = com.lofrust.lgm.WeaponInputHandler.isSprinting();
            var currentAnim = state.getController().getCurrentAnimation();
            String currentAnimName = currentAnim != null ? currentAnim.animation().name() : "";

            // ЕСЛИ ИГРОК ПРИЦЕЛИВАЕТСЯ - СПРИНТ СБРАСЫВАЕТСЯ
            if (com.lofrust.lgm.WeaponInputHandler.isAiming()) {
                if (currentAnimName.equals("sprint")) {
                    return state.setAndContinue(RawAnimation.begin().then("unsprint", Animation.LoopType.PLAY_ONCE));
                }
                return PlayState.CONTINUE;
            }

            // СПРИНТ АКТИВЕН
            if (isSprinting) {
                // Если уже играет анимация вскидывания (aim) ИЛИ мы стоим в статичном прицеле (aim_static) после выстрела
                // ИЛИ прямо сейчас проигрывается сам выстрел (aim_shoot) — ПРОДОЛЖАЕМ играть и не перезапускаем!

                if (currentAnimName.equals("sprint") ||  currentAnimName.equals("melee_sprint") ||  currentAnimName.equals("shoot_sprint")) {
                    return PlayState.CONTINUE;
                }

                // Первичное вхождение в прицел (играет только один раз при зажатии ПКМ)
                return state.setAndContinue(RawAnimation.begin().then("sprint", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
            // 2. ИГРОК ОТПУСТИЛ ПРИЦЕЛ
            else {
                // 1. Если unsprint или спринтовые действия уже играют (на экране или в фоне) — НЕ ТРОГАЕМ, пусть играют.
                if (currentAnimName.equals("unsprint") || currentAnimName.equals("melee_sprint") || currentAnimName.equals("shoot_sprint")) {
                    return PlayState.CONTINUE;
                }

                // 2. Запускаем unsprint ТОЛЬКО если мы реально переходим ИЗ анимации бега.
                if (currentAnimName.equals("sprint")) {
                    return state.setAndContinue(RawAnimation.begin().then("unsprint", Animation.LoopType.PLAY_ONCE));
                }

                // 3. Во всех остальных случаях (когда unsprint уже кончился или его и не должно быть) — просто гасим кости.
                return PlayState.STOP;
            }
        });
        sprintController.triggerableAnim(SPRINT_SHOOT_TRIGGER_NAME,
                RawAnimation.begin()
                        .then("shoot_sprint", Animation.LoopType.PLAY_ONCE)       // 1. Проигрываем выстрел в прицеле
                        .then("static", Animation.LoopType.HOLD_ON_LAST_FRAME) // 2. Насильно и намертво встаем в aim_static
        );
        sprintController.triggerableAnim(SPRINT_MELEE_TRIGGER_NAME,
                RawAnimation.begin()
                        .then("melee_sprint", Animation.LoopType.PLAY_ONCE)       // 1. Проигрываем выстрел в прицеле
                        .then("static", Animation.LoopType.HOLD_ON_LAST_FRAME) // 2. Насильно и намертво встаем в aim_static
        );
        // КОНТРОЛЛЕР ДЛЯ ПЕРЕЗАРЯДКИ
        AnimationController<MaximumEffort> reloadController = new AnimationController<>(this, RELOAD_CONTROLLER_NAME, 0, state -> {
            var currentAnim = state.getController().getCurrentAnimation();
            String currentAnimName = currentAnim != null ? currentAnim.animation().name() : "";

            // Если играет анимация перезарядки - блокируем всё остальное
            if (currentAnimName.equals("reload")) {
                return PlayState.CONTINUE;
            }

            return PlayState.STOP;
        });
        reloadController.triggerableAnim(RELOAD_TRIGGER_NAME,
                RawAnimation.begin().then("reload", Animation.LoopType.PLAY_ONCE)
        );

        controllers.add(equipController);
        controllers.add(actionController);
        controllers.add(aimController);
        controllers.add(sprintController);
        controllers.add(reloadController);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Полностью отключаем ванильную анимацию для этого предмета
        return false;
    }


    @SuppressWarnings("removal")
    @Override
    public boolean onEntitySwing(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return true; // Блокирует стандартный взмах руки Стива
    }



    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Перенаправляем регистрацию рендерера в изолированный клиентский класс
    @Override
    public void createGeoRenderer(java.util.function.Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.GeoRenderProvider() {
            private com.lofrust.lgm.client.renderer.MaximumEffortRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    // Используем ваш собственный класс рендерера!
                    this.renderer = new com.lofrust.lgm.client.renderer.MaximumEffortRenderer();
                }
                return this.renderer;
            }
        });
    }

    public void shoot(Level level, Player player, ItemStack gunStack, boolean isAiming) {
        // 1. ПРОВЕРКА ПАТРОНОВ В МАГАЗИНЕ
        int currentAmmo = getAmmo(gunStack);

        if (currentAmmo <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        // Звук выстрела
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.MAXIMUM_EFFORT_SHOOT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!level.isClientSide) {
            // Отдача
            com.lofrust.lgm.client.RecoilHandler.addRecoil(10.0f, 7.0f, 0);

            // Триггер анимации - ИСПРАВЛЕНО
            ServerLevel serverLevel = (ServerLevel) level; // <- ДОБАВЬТЕ ЭТУ СТРОКУ
            long id = GeoItem.getOrAssignId(gunStack, serverLevel);
            if (isAiming) {
                // Вместо сброса контроллера, мы активируем триггер testanim прямо ТАМ ЖЕ, где живет прицел
                // Это автоматически и полностью выгрузит анимацию 'aim' на время выстрела
                triggerAnim(player, id, "AimController", AIM_SHOOT_TRIGGER_NAME);
            } else if (player.isSprinting()) {

                // Показываем выстрел
                triggerAnim(player, id, SPRINT_CONTROLLER_NAME, SPRINT_SHOOT_TRIGGER_NAME);
            } else {
                // Обычный выстрел от бедра в дефолтном контроллере выстрела
                triggerAnim(player, id, CONTROLLER_NAME, SHOOT_TRIGGER_NAME);
            }

            // Создание пули
            BulletEntity bullet = new BulletEntity(
                    level,
                    player,
                    getGunDamage(gunStack),
                    getHeadshotMultiplier(gunStack),
                    getBulletRange(gunStack)
            );
            bullet.setPos(player.getX(), player.getEyeY(), player.getZ());
            float speed = getBulletSpeed(gunStack);
            float inaccuracy = getInaccuracy(gunStack, isAiming);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, inaccuracy);
            level.addFreshEntity(bullet);

            // Списываем патрон
            setAmmo(gunStack, currentAmmo - 1);
        }

        // 3. КУЛДАУН
        String cooldownKey = "gun_cooldown_" + this.getDescriptionId();
        player.getPersistentData().putInt(cooldownKey, getFireRate(gunStack));


    }


    public void reload(Player player, ItemStack gunStack) {
        String reloadKey = "gun_reloading_" + this.getDescriptionId();
        if (player.getPersistentData().getInt(reloadKey) > 0) {
            return;
        }

        int maxAmmo = getMaxAmmo(gunStack);
        int currentAmmo = getAmmo(gunStack);

        if (currentAmmo >= maxAmmo) return;

        int ammoNeeded = maxAmmo - currentAmmo;
        int ammoFound = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.HEAVY_AMMO.get())) {
                int count = stack.getCount();
                int take = Math.min(ammoNeeded, count);
                stack.shrink(take);
                ammoFound += take;
                ammoNeeded -= take;
                if (ammoNeeded <= 0) break;
            }
        }

        if (ammoFound > 0) {
            player.getPersistentData().putInt(reloadKey, getReloadTime(gunStack));
            long id = GeoItem.getId(gunStack);
            triggerAnim(player, id, RELOAD_CONTROLLER_NAME, RELOAD_TRIGGER_NAME);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    // ===== ГЕТТЕРЫ/СЕТТЕРЫ ДЛЯ NBT =====
    public int getAmmo(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Ammo")) {
                return tag.getInt("Ammo");
            }
        }
        return 0;
    }

    public void setAmmo(ItemStack stack, int ammo) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, customData -> {
            CompoundTag tag = customData.copyTag();
            tag.putInt("Ammo", ammo);
            return CustomData.of(tag);
        });
    }

    public boolean canShoot(Player player) {
        String cooldownKey = "gun_cooldown_" + this.getDescriptionId();
        return player.getPersistentData().getInt(cooldownKey) <= 0;
    }

    // ===== НАСТРОЙКИ ОРУЖИЯ =====
    public int getMaxAmmo(ItemStack stack) { return 7; }
    public int getGunDamage(ItemStack stack) { return 10; }
    public int getHeadshotMultiplier(ItemStack stack) { return 175; }
    public float getInaccuracy(ItemStack stack, boolean isAiming) {
        return isAiming ? 0.1F : 5.0F;
    }
    public float getBulletSpeed(ItemStack stack) { return 22.5F; }
    public double getBulletRange(ItemStack stack) { return 100.0D; }
    public int getFireRate(ItemStack stack) { return 15; }
    public float getMovementSlowdown(ItemStack stack) { return 0.52F; }
    public int getReloadTime(ItemStack stack) { return 45; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        int currentAmmo = getAmmo(stack);
        int maxAmmo = getMaxAmmo(stack);

        tooltip.add(net.minecraft.network.chat.Component.literal("")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(net.minecraft.network.chat.Component.literal(currentAmmo + " / " + maxAmmo)
                        .withStyle(ChatFormatting.DARK_GRAY)));

        tooltip.add(net.minecraft.network.chat.Component.literal("Урон: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(net.minecraft.network.chat.Component.literal(String.valueOf(getGunDamage(stack))).withStyle(ChatFormatting.AQUA)));

        super.appendHoverText(stack, context, tooltip, flag);
    }
}