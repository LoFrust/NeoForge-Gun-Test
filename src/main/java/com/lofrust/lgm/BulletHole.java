package com.lofrust.lgm;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class BulletHole {
    public final Vec3 pos;
    public final Direction face;
    public final int color;
    public int ticksLeft;

    public BulletHole(Vec3 pos, Direction face, int color) {
        this.pos = pos;
        this.face = face;
        this.color = color;
        this.ticksLeft = 80; // 4 секунды жизни (2 висит + 2 тает)
    }
}
