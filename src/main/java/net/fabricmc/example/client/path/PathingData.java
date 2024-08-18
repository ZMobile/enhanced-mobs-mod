package net.fabricmc.example.client.path;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PathingData {
    @SerializedName("mob_id")
    private int mobId;

    @SerializedName("path_positions")
    private List<BetterBlockPos> pathPositions;

    public PathingData(int mobId, List<BetterBlockPos> pathPositions) {
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
