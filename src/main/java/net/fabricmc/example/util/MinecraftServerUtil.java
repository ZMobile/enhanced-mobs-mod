package net.fabricmc.example.util;

import net.minecraft.server.MinecraftServer;

public class MinecraftServerUtil {
    private static MinecraftServer minecraftServer;

    public static void setMinecraftServer(MinecraftServer server) {
        minecraftServer = server;
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}
