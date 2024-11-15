package net.fabricmc.example.client.path;

import com.google.gson.annotations.SerializedName;
import baritone.api.utils.BetterBlockPos;
import java.util.List;
import java.util.UUID;

public class PathingData {
    @SerializedName("mob_id")
    private int mobId;

    @SerializedName("type")
    private String type;

    @SerializedName("path_positions")
    private List<BetterBlockPos> pathPositions;

    public PathingData(int mobId, String type, List<BetterBlockPos> pathPositions) {
        this.mobId = mobId;
        this.type = type;
        this.pathPositions = pathPositions;
    }

    public int getMobId() {
        return mobId;
    }

    public List<BetterBlockPos> getPathPositions() {
        return pathPositions;
    }
}
