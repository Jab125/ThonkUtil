package com.jab125.thonkutil.api.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class ServerWorldLoadEvent extends EventTaxiEvent {
    private final MinecraftServer server;
    private final ServerWorld world;
    public ServerWorldLoadEvent(MinecraftServer server, ServerWorld world) {
        this.server = server;
        this.world = world;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerWorld getWorld() {
        return world;
    }
}
