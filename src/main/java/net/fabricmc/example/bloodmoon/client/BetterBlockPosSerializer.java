package net.fabricmc.example.bloodmoon.client;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.*;

import java.lang.reflect.Type;

public class BetterBlockPosSerializer implements JsonSerializer<BetterBlockPos>, JsonDeserializer<BetterBlockPos> {
    @Override
    public JsonElement serialize(BetterBlockPos src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", src.getX());
        obj.addProperty("y", src.getY());
        obj.addProperty("z", src.getZ());
        return obj;
    }

    @Override
    public BetterBlockPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        int x = obj.get("x").getAsInt();
        int y = obj.get("y").getAsInt();
        int z = obj.get("z").getAsInt();
        return new BetterBlockPos(x, y, z);
    }
}
