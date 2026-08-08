package com.lofrust.lgm;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// record принимает ID сущности (моба), по которой клиент успешно попал
public record MeleeAttackPacket(int targetId) implements CustomPacketPayload {

    // Уникальный сетевой ID пакета
    public static final Type<MeleeAttackPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LGM.MODID, "melee_attack_packet"));

    // Кодек для записи и чтения ID моба (формат Integer)
    public static final StreamCodec<FriendlyByteBuf, MeleeAttackPacket> CODEC = StreamCodec.of(
            (buf, val) -> buf.writeInt(val.targetId), // Запись на клиенте
            buf -> new MeleeAttackPacket(buf.readInt()) // Чтение на сервере
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Обработчик пакета на стороне Сервера
    public static void handle(final MeleeAttackPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // 1. Получаем игрока из контекста
            Player player = context.player();
            if (player == null) return; // Защита от Null, если игрок отключился во время отправки

            // 2. Ищем моба в серверном мире игрока
            Entity target = player.level().getEntity(payload.targetId);

            // 3. Защита: проверяем, что цель жива и дистанция в пределах нормы
            if (target instanceof LivingEntity livingTarget && player.distanceToSqr(target) < 16.0) {

                // ИСПРАВЛЕНИЕ: Используем альтернативный, самый стабильный метод получения источника урона игрока.
                // Вместо playerAttack(player) используем стандартный damageSources().mobAttack(player)
                // или напрямую хукаем через DamageSources.
                net.minecraft.world.damagesource.DamageSource source = player.damageSources().mobAttack(player);

                if (source != null) {
                    // Наносим урон
                    livingTarget.hurt(source, 4.0F);

                    // СБРОС ИНВУЛЬНЕРАБИЛЬНОСТИ: Если хотите, чтобы урон наносился мгновенно без ванильной задержки красного цвета
                    livingTarget.invulnerableTime = 0;

                    player.level().playSound(
                            null,                           // null = звук для всех в радиусе
                            player.getX(), player.getY(), player.getZ(),
                            com.lofrust.lgm.sound.ModSounds.MAXIMUM_EFFORT_MELEE.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            1.0F,
                            1.0F
                    );
                }
            }
        });
    }
}
