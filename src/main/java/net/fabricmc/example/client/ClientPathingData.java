package net.fabricmc.example.client;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.UUID;

public class ClientPathingData {
    @SerializedName("mob_uuid")
    private UUID mobUuid;

    @SerializedName("path_positions")
    private List<BetterBlockPos> pathPositions;

    public ClientPathingData(UUID mobUuid, List<BetterBlockPos> pathPositions) {
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