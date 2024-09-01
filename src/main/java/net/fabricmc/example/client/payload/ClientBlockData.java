package net.fabricmc.example.client.payload;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.math.BlockPos;

public class ClientBlockData {
    @SerializedName("mob_id")
    private int mobId;

    @SerializedName("better_block_pos")
    private BlockPos blockPos;

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
