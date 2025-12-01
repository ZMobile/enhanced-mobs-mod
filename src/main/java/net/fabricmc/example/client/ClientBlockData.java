package net.fabricmc.example.client;

import net.minecraft.util.math.BlockPos;

public class ClientBlockData {
    private final int mobId;
    private final BlockPos blockPos;

    public ClientBlockData(int mobId, BlockPos blockPos) {
        this.mobId = mobId;
        this.blockPos = blockPos;
    }

    public int getMobId() {
        return mobId;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }
}