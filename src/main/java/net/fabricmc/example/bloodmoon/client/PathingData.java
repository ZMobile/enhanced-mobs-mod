package net.fabricmc.example.bloodmoon.client;

import com.google.gson.annotations.SerializedName;
import baritone.api.utils.BetterBlockPos;
import java.util.List;
import java.util.UUID;

public class PathingData {
    @SerializedName("mob_uuid")
    private UUID mobUuid;

    @SerializedName("path_positions")
    private List<BetterBlockPos> pathPositions;

    public PathingData(UUID mobUuid, List<BetterBlockPos> pathPositions) {
        this.mobUuid = mobUuid;
        this.pathPositions = pathPositions;
    }

    public UUID getMobUuid() {
        return mobUuid;
    }

    public List<BetterBlockPos> getPathPositions() {
        return pathPositions;
    }
}
