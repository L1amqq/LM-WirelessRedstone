package com.liam.wirelessredstone;

import java.util.Map;

public record WirelessPistonMoveData(Map<Long, Long> movedTransmitters, Map<Long, Long> movedReceivers) {
    public static final WirelessPistonMoveData EMPTY = new WirelessPistonMoveData(Map.of(), Map.of());

    public boolean isEmpty() {
        return movedTransmitters.isEmpty() && movedReceivers.isEmpty();
    }
}
