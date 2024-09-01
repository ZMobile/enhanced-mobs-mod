package net.fabricmc.example.client.payload;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.UUID;

public class ClientPathingData {
    @SerializedName("mob_id")
    private int mobId;

    @SerializedName("path_positions")
    private List<BetterBlockPos> pathPositions;

    public ClientPathingData(int mobId, List<BetterBlockPos> pathPositions) {
        this.mobId = mobId;
        this.pathPositions = pathPositions;
    }

    public int getMobId() {
        return mobId;
    }

    public List<BetterBlockPos> getPathPositions() {
        return pathPositions;
    }
}
