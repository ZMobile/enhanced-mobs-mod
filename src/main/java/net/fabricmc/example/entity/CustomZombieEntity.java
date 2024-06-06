package net.fabricmc.example.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CustomZombieEntity extends ZombieEntity {
    private World world;

    public CustomZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
        this.world = world;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.world.isClient && this.isAlive() && !this.isSpectator()) {
            BlockPos blockPos = this.getBlockPos().down();
            BlockState blockState = world.getBlockState(blockPos);

            if (!blockState.isAir() && this.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
                ItemStack blockItemStack = new ItemStack(blockState.getBlock().asItem());
                this.equipStack(EquipmentSlot.MAINHAND, blockItemStack);
                world.breakBlock(blockPos, false);
            }
        }
    }
}