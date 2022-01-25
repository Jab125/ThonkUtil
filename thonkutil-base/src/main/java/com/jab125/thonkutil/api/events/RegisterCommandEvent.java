package com.jab125.thonkutil.api.events;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class RegisterCommandEvent extends EventTaxiEvent {
    private final CommandDispatcher<ServerCommandSource> source;
    private final boolean dedicated;
    public RegisterCommandEvent(CommandDispatcher<ServerCommandSource> source, boolean dedicated) {
        this.source = source;
        this.dedicated = dedicated;
    }

    public CommandDispatcher<ServerCommandSource> getSource() {
        return source;
    }

    public boolean isDedicated() {
        return dedicated;
    }
}