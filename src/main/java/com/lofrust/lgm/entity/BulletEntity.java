package com.lofrust.lgm.entity;

import com.lofrust.lgm.item.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class BulletEntity extends ThrowableItemProjectile {

    private float baseDamage;        // Без значения по умолчанию!
    private int headshotMultiplier;
    private double maxRange;

    public BulletEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    private net.minecraft.world.phys.Vec3 startPos;

    public BulletEntity(Level level, LivingEntity shooter, float damage, int headshotMultiplier, double range) {
        super(ModEntities.BULLET.get(), shooter, level);
        this.setNoGravity(true);
        this.baseDamage = damage;
        this.headshotMultiplier = headshotMultiplier;
        this.maxRange = range;
        this.startPos = this.position();
    }

    public void setDamage(float damage) {
        this.baseDamage = damage;
    }
    public void setHeadshotMultiplier(int multiplier) {
        this.headshotMultiplier = multiplier;
    }
    public void setRange(double range) {
        this.maxRange = range;
    }
    public double getRange() {
        return this.maxRange;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BULLET_MODEL_ITEM.get();
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide) {
            if (result.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) result;
                net.minecraft.core.BlockPos blockPos = blockHit.getBlockPos();
                net.minecraft.world.phys.Vec3 hitPos = blockHit.getLocation();

                // ИСПРАВЛЕНО ДЛЯ ВЕРХА И НИЗА: Если ванильный face сбоит, рассчитываем грань честно по вектору движения пули
                net.minecraft.core.Direction face = blockHit.getDirection();
                if (this.getDeltaMovement().y > 0.1D && face == net.minecraft.core.Direction.UP) {
                    face = net.minecraft.core.Direction.DOWN; // Пуля летела снизу вверх и врезалась в потолок
                } else if (this.getDeltaMovement().y < -0.1D && face == net.minecraft.core.Direction.DOWN) {
                    face = net.minecraft.core.Direction.UP; // Пуля летела сверху вниз и врезалась в пол
                }

                // 1. Получаем и ИСПРАВЛЯЕМ ЗАТЕМНЕНИЕ: умножаем на 0.3F вместо 0.6F (цвет станет в 2 раза темнее!)
                net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(blockPos);
                int vanillaColor = state.getMapColor(this.level(), blockPos).col;
                int r = (int) (((vanillaColor >> 16) & 0xFF) * 0.3F);
                int g = (int) (((vanillaColor >> 8) & 0xFF) * 0.3F);
                int b = (int) ((vanillaColor & 0xFF) * 0.3F);
                int darkenedColor = (r << 16) | (g << 8) | b;

                // ==========================================
                // ИСПРАВЛЕНО: УМНЫЙ РАСЧЕТ РАССТОЯНИЯ (СДВИГА)
                // ==========================================
                double offsetDistance = 0.002D; // Для боковых стен (NORTH, SOUTH, EAST, WEST) — прижимаем почти вплотную, чтобы не висело в воздухе

                // Если пуля попала в ПОЛ (UP) или ПОТОЛОК (DOWN)
                if (face == net.minecraft.core.Direction.UP || face == net.minecraft.core.Direction.DOWN) {
                    offsetDistance = 0.012D; // Выдвигаем чуть сильнее, чтобы графический движок Майнкрафта гарантированно не рябил текстурами (Z-fighting)
                }

                // Смещаем точку дырки от стены на рассчитанное идеальное расстояние
                net.minecraft.world.phys.Vec3 normal = net.minecraft.world.phys.Vec3.atLowerCornerOf(face.getNormal()).scale(offsetDistance);
                net.minecraft.world.phys.Vec3 spawnPos = hitPos.add(normal);
                // ==========================================

                // Отправляем пакет на клиент
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(this,
                        new com.lofrust.lgm.BulletHolePacket(spawnPos, face.get3DDataValue(), darkenedColor));

                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // Получаем состояние блока, по которому попали


                    // Создаем параметры частиц разрушения для конкретного блока
                    net.minecraft.core.particles.BlockParticleOption particleOptions =
                            new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state);

                    // Спавним частицы через ServerLevel
                    serverLevel.sendParticles(
                            particleOptions,
                            hitPos.x, hitPos.y, hitPos.z, // Координаты попадания (с вашим оффсетом)
                            3,                           // Количество частиц
                            0.05D, 0.05D, 0.05D,          // Небольшой разброс, чтобы они летели в разные стороны
                            0.05D                         // Скорость разлета
                    );
                }

                // Звук щелчка пули о блок
                this.level().playSound(null, hitPos.x, hitPos.y, hitPos.z,
                        net.minecraft.sounds.SoundEvents.ARROW_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 1.6F);
            }
            this.discard(); // Удаляем пулю
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {

            net.minecraft.world.phys.Vec3 currentPos = this.position();
            net.minecraft.world.phys.Vec3 nextPos = currentPos.add(this.getDeltaMovement());

            // Клипаем луч пули об ОРИГИНАЛЬНЫЙ хитбокс моба (без инфляции)
            java.util.Optional<net.minecraft.world.phys.Vec3> strictClip =
                    target.getBoundingBox().clip(currentPos, nextPos);

            // Если луч траектории пули вообще не пересёк хитбокс моба — это промах!
            if (strictClip.isEmpty()) {
                return;
            }

            float finalDamage = this.baseDamage;
            boolean isHeadshot = false;

            double exactHitHeight = strictClip.get().y - target.getY();
            double headshotThreshold = target.getBbHeight() * 0.75;

            if (exactHitHeight >= headshotThreshold) {
                finalDamage = this.baseDamage * (this.headshotMultiplier / 100.0F);
                isHeadshot = true;
            }

            target.invulnerableTime = 0;

            if (this.getOwner() instanceof Player playerOwner) {
                target.hurt(this.damageSources().playerAttack(playerOwner), finalDamage);
                if (isHeadshot) {
                    this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.5F);
                }
            } else {
                target.hurt(this.damageSources().thrown(this, this.getOwner()), finalDamage);
            }
        }
    }

    @Override
    public void tick() {
        // ВАЖНО: НЕ вызываем super.tick(), чтобы ванильный движок не двигал пулю сам и не пропускал хитбоксы
        this.baseTick(); // Обновляет базовые параметры сущности (например, возраст)

        if (this.startPos == null) {
            this.startPos = this.position();
        }

        net.minecraft.world.phys.Vec3 currentPos = this.position();
        net.minecraft.world.phys.Vec3 movement = this.getDeltaMovement();
        double stepLength = movement.length();

        // 1. Проверяем ограничение по максимальной дальности
        double distanceTraveled = this.startPos.distanceTo(currentPos);
        double remainingRange = this.maxRange - distanceTraveled;

        if (remainingRange <= 0) {
            this.discard();
            return;
        }

        // Если пуле осталось лететь меньше, чем её скорость, урезаем шаг
        if (stepLength > remainingRange) {
            movement = movement.normalize().scale(remainingRange);
            stepLength = remainingRange;
        }

        net.minecraft.world.phys.Vec3 nextPos = currentPos.add(movement);

        // 2. Честный Raycast блоков на пути этого тика
        net.minecraft.world.phys.BlockHitResult blockHit = this.level().clip(
                new net.minecraft.world.level.ClipContext(
                        currentPos, nextPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        this
                )
        );

        // Если на пути блока есть стена, урезаем конечную точку траектории до неё
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            nextPos = blockHit.getLocation();
        }

        // 3. Честный Raycast мобов на этом же отрезке
        net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                this.level(), this, currentPos, nextPos,
                this.getBoundingBox().expandTowards(movement).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isPickable() && entity != this.getOwner()
        );

        // 4. Обработка результатов столкновения
        if (entityHit != null) {
            // Вызываем ВАШ существующий метод попадания по мобу
            this.onHitEntity(entityHit);
            this.discard();
            return;
        } else if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            // Вызываем ВАШ метод попадания по блоку (где спавнятся частицы разрушения)
            this.onHit(blockHit);
            this.discard();
            return;
        }

        // 5. Если никуда не попали — просто перемещаем пулю вперед
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // Если пуля после этого шага достигла лимита дальности — удаляем
        if (this.startPos.distanceTo(this.position()) >= this.maxRange) {
            this.discard();
        }
    }

}
