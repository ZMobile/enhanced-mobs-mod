package net.fabricmc.example.model;

import net.minecraft.entity.LivingEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class MobitoneProvision {
    private LivingEntity livingEntity;
    private LocalDateTime provisionTime;

    public MobitoneProvision(LivingEntity livingEntity) {
        this.provisionTime = LocalDateTime.now(ZoneOffset.UTC);
        this.livingEntity = livingEntity;
    }

    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    public void setLivingEntity(LivingEntity livingEntity) {
        this.livingEntity = livingEntity;
    }

    public LocalDateTime getProvisionTime() {
        return provisionTime;
    }

    public void setProvisionTime(LocalDateTime provisionTime) {
        this.provisionTime = provisionTime;
    }

    public void updateProvisionTime() {
        this.provisionTime = LocalDateTime.now(ZoneOffset.UTC);
    }
}
