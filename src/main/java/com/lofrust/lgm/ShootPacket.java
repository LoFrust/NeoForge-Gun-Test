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

public record ShootPacket(boolean isAiming) implements CustomPacketPayload {

    public static final Type<ShootPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LGM.MODID, "shoot_packet"));

    public static final StreamCodec<FriendlyByteBuf, ShootPacket> CODEC = StreamCodec.of(
            (buf, val) -> buf.writeBoolean(val.isAiming),
            buf -> new ShootPacket(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ShootPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (heldItem.getItem() instanceof MaximumEffort gunItem) {
                if (gunItem.canShoot(player)) {
                    int currentAmmo = gunItem.getAmmo(heldItem);

                    if (currentAmmo > 0) {
                        // Выстрел
                        gunItem.shoot(player.level(), player, heldItem, payload.isAiming());
                    }
                }
            }
        });
    }
}