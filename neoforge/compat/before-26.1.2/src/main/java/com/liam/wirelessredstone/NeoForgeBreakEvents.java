package com.liam.wirelessredstone;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

final class NeoForgeBreakEvents {
    private NeoForgeBreakEvents() {
    }

    static void register(NeoForgeMod.BreakListener listener) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (BlockEvent.BreakEvent event) ->
                listener.beforeBreak(event.getLevel(), event.getPos(), event.getState()));
    }
}
