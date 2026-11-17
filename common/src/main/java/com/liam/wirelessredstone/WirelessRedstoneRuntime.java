package com.liam.wirelessredstone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public final class WirelessRedstoneRuntime {
    private static final Map<ResourceKey<Level>, Map<Long, Integer>> POWER_BY_WORLD = new HashMap<>();

    private WirelessRedstoneRuntime() {
    }

    public static int getPower(Level world, BlockPos transmitterPos) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return 0;
        }

        Map<Long, Integer> poweredTransmitters = POWER_BY_WORLD.get(serverLevel.dimension());

        if (poweredTransmitters == null) {
            return 0;
        }

        return poweredTransmitters.getOrDefault(transmitterPos.asLong(), 0);
    }

    public static void tickWorld(ServerLevel world) {
        WirelessLinkSavedData savedData = WirelessLinkSavedData.get(world);
        Map<Long, Integer> previousPower = POWER_BY_WORLD.getOrDefault(world.dimension(), Map.of());
        Map<Long, Integer> currentPower = buildCurrentPowerMap(world, savedData);

        POWER_BY_WORLD.put(world.dimension(), currentPower);
        notifyChangedTransmitters(world, previousPower, currentPower);
    }

    public static void clearWorld(ServerLevel world) {
        POWER_BY_WORLD.remove(world.dimension());
    }

    private static Map<Long, Integer> buildCurrentPowerMap(ServerLevel world, WirelessLinkSavedData savedData) {
        Map<Long, Integer> currentPower = new HashMap<>();
        savedData.updateReceiverCounts(world);

        for (WirelessLinkSavedData.LinkEntry entry : Set.copyOf(savedData.getLinks())) {
            Long transmitterPacked = savedData.getTransmitterPos(entry.transmitterId());
            Long receiverPacked = savedData.getReceiverPos(entry.receiverName());

            if (transmitterPacked == null || receiverPacked == null) {
                continue;
            }

            BlockPos transmitterPos = resolveTransmitterPos(world, BlockPos.of(transmitterPacked.longValue()), entry.transmitterId());
            BlockPos receiverPos = resolveReceiverPos(world, BlockPos.of(receiverPacked.longValue()), entry.receiverName());

            if (transmitterPos == null || receiverPos == null) {
                continue;
            }

            if (transmitterPos.asLong() != transmitterPacked.longValue()) {
                savedData.updateTransmitterPosition(entry.transmitterId(), transmitterPos);
            }

            if (receiverPos.asLong() != receiverPacked.longValue()) {
                savedData.updateReceiverPosition(entry.receiverName(), receiverPos);
            }

            int power = world.getBestNeighborSignal(receiverPos);

            if (power > 0) {
                currentPower.merge(transmitterPos.asLong(), power, (left, right) -> Integer.valueOf(Math.max(left.intValue(), right.intValue())));
            }
        }

        savedData.updateReceiverCounts(world);

        return currentPower;
    }

    private static BlockPos resolveTransmitterPos(ServerLevel world, BlockPos pos, String transmitterId) {
        return resolveWirelessPos(world, pos, true, transmitterId);
    }

    private static BlockPos resolveReceiverPos(ServerLevel world, BlockPos pos, String receiverName) {
        return resolveWirelessPos(world, pos, false, receiverName);
    }

    private static BlockPos resolveWirelessPos(ServerLevel world, BlockPos pos, boolean transmitter, String expectedValue) {
        if (isExpectedWirelessBlock(world, pos, transmitter, expectedValue)) {
            return pos;
        }

        BlockPos foundPos = null;

        for (Direction direction : Direction.values()) {
            BlockPos candidatePos = pos.relative(direction);

            if (!isExpectedWirelessBlock(world, candidatePos, transmitter, expectedValue)) {
                continue;
            }

            if (foundPos != null) {
                return null;
            }

            foundPos = candidatePos;
        }

        return foundPos;
    }

    private static boolean isExpectedWirelessBlock(ServerLevel world, BlockPos pos, boolean transmitter, String expectedValue) {
        BlockState state = getLogicalBlockState(world, pos);

        if (transmitter) {
            if (!WirelessRedstoneMod.isTransmitterBlock(state)) {
                return false;
            }

            BlockEntity blockEntity = world.getBlockEntity(pos);
            return !(blockEntity instanceof TransmitterBlockEntity transmitterBlockEntity)
                    || WirelessLinkSavedData.normalizeTransmitterId(transmitterBlockEntity.getTransmitterId()).equals(WirelessLinkSavedData.normalizeTransmitterId(expectedValue));
        }

        if (!WirelessRedstoneMod.isReceiverBlock(state)) {
            return false;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        return !(blockEntity instanceof ReceiverBlockEntity receiverBlockEntity)
                || WirelessLinkSavedData.normalizeReceiverName(receiverBlockEntity.getReceiverName()).equals(WirelessLinkSavedData.normalizeReceiverName(expectedValue));
    }

    private static BlockState getLogicalBlockState(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (WirelessRedstoneMod.isTransmitterBlock(state) || WirelessRedstoneMod.isReceiverBlock(state)) {
            return state;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof PistonMovingBlockEntity movingBlockEntity) {
            return movingBlockEntity.getMovedState();
        }

        return state;
    }

    private static void notifyChangedTransmitters(ServerLevel world, Map<Long, Integer> previousPower, Map<Long, Integer> currentPower) {
        Set<Long> changedPositions = new HashSet<>(previousPower.keySet());
        changedPositions.addAll(currentPower.keySet());

        for (Long packedPos : changedPositions) {
            Integer oldPower = previousPower.get(packedPos);
            Integer newPower = currentPower.get(packedPos);

            if (oldPower == null && newPower == null) {
                continue;
            }

            if (oldPower != null && oldPower.equals(newPower)) {
                continue;
            }

            BlockPos transmitterPos = BlockPos.of(packedPos.longValue());
            updateNeighbors(world, transmitterPos);
        }
    }

    private static void updateNeighbors(ServerLevel world, BlockPos pos) {
        world.updateNeighborsAt(pos, WirelessRedstoneMod.TRANSMITTER_BLOCK);

        for (Direction direction : Direction.values()) {
            world.updateNeighborsAt(pos.relative(direction), WirelessRedstoneMod.TRANSMITTER_BLOCK);
        }
    }
}
