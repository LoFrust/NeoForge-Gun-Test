package com.lofrust.lgm.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class BulletHoleEntity extends Entity {

    private int lifeTime = 80; // 80 тиков = 4 секунды (2 сек висит + 2 сек тает)
    private int colorRGB = 0x444444;
    private int faceId = 2; // По умолчанию NORTH

    public BulletHoleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.lifeTime--;
            if (this.lifeTime <= 0) {
                this.discard(); // Самоудаление через 4 секунды
            }
        }
    }

    public int getLifeTime() { return this.lifeTime; }
    public void setHoleColor(int rgb) { this.colorRGB = rgb; }
    public int getHoleColor() { return this.colorRGB; }
    public void setFaceId(int id) { this.faceId = id; }
    public int getFaceId() { return this.faceId; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("HoleColor")) this.colorRGB = tag.getInt("HoleColor");
        if (tag.contains("FaceId")) this.faceId = tag.getInt("FaceId");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("HoleColor", this.colorRGB);
        tag.putInt("FaceId", this.faceId);
    }
}
