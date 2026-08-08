package com.lofrust.lgm;

import com.lofrust.lgm.item.MaximumEffort;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ReloadPacket() implements CustomPacketPayload {
    // Уникальный ID сетевого пакета перезарядки
    public static final Type<ReloadPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LGM.MODID, "reload_packet"));

    // ИСПРАВЛЕНО: Для пустых пакетов в 1.21.1 используется StreamCodec.unit, который не ломает сетевой поток
    public static final StreamCodec<FriendlyByteBuf, ReloadPacket> CODEC = StreamCodec.unit(new ReloadPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Этот метод выполняется на стороне Сервера, когда игрок нажимает R на клиенте
    public static void handle(final ReloadPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player(); // Получаем игрока
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND); // Смотрим предмет в правой руке

            // Если игрок удерживает наше кастомное оружие — запускаем перезарядку
            if (heldItem.getItem() instanceof MaximumEffort gunItem) {
                gunItem.reload(player, heldItem);
            }
        });
    }
}
