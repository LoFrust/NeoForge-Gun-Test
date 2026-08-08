package com.lofrust.lgm;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BulletHolePacket(Vec3 pos, int faceId, int color) implements CustomPacketPayload {
    public static final Type<BulletHolePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LGM.MODID, "bullet_hole_packet"));

    public static final StreamCodec<FriendlyByteBuf, BulletHolePacket> CODEC = StreamCodec.of(
            (buf, val) -> {
                buf.writeDouble(val.pos.x);
                buf.writeDouble(val.pos.y);
                buf.writeDouble(val.pos.z);
                buf.writeInt(val.faceId);
                buf.writeInt(val.color); // Записываем цвет в буфер сети
            },
            buf -> new BulletHolePacket(
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readInt(),
                    buf.readInt() // Читаем цвет из буфера сети
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final BulletHolePacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Direction face = Direction.from3DDataValue(payload.faceId);
            // Передаем координаты, грань и цвет блока в наш безопасный рендерер дырок!
            BulletHoleRenderer.addHole(payload.pos, face, payload.color);
        });
    }
}
