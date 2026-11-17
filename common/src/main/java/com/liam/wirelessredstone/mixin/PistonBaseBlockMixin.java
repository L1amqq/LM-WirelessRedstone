package com.liam.wirelessredstone.mixin;

import java.util.HashMap;
import java.util.Map;

import com.liam.wirelessredstone.WirelessLinkSavedData;
import com.liam.wirelessredstone.WirelessPistonMoveData;
import com.liam.wirelessredstone.WirelessRedstoneMod;
import com.liam.wirelessredstone.WirelessRedstoneRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
    @Unique
    private static final ThreadLocal<WirelessPistonMoveData> lmWirelessRedstone$pistonMoveData = new ThreadLocal<>();

    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private void lmWirelessRedstone$captureWirelessMoves(Level world, BlockPos pistonPos, Direction direction, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        lmWirelessRedstone$pistonMoveData.set(lmWirelessRedstone$collectMoveData(world, pistonPos, direction, extending));
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private void lmWirelessRedstone$applyWirelessMoves(Level world, BlockPos pistonPos, Direction direction, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!Boolean.TRUE.equals(cir.getReturnValue()) || !(world instanceof ServerLevel serverLevel)) {
                return;
            }

            WirelessPistonMoveData moveData = lmWirelessRedstone$pistonMoveData.get();

            if (moveData == null || moveData.isEmpty()) {
                return;
            }

            WirelessLinkSavedData savedData = WirelessLinkSavedData.get(serverLevel);

            if (savedData.moveBlockPositions(moveData.movedTransmitters(), moveData.movedReceivers())) {
                WirelessRedstoneRuntime.tickWorld(serverLevel);
            }
        } finally {
            lmWirelessRedstone$pistonMoveData.remove();
        }
    }

    @Redirect(
            method = "isPushable",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z")
    )
    private static boolean lmWirelessRedstone$allowWirelessBlockEntityPush(BlockState state) {
        if (WirelessRedstoneMod.isTransmitterBlock(state) || WirelessRedstoneMod.isReceiverBlock(state)) {
            return false;
        }

        return state.hasBlockEntity();
    }

    @Unique
    private static WirelessPistonMoveData lmWirelessRedstone$collectMoveData(Level world, BlockPos pistonPos, Direction direction, boolean extending) {
        if (!(world instanceof ServerLevel)) {
            return WirelessPistonMoveData.EMPTY;
        }

        PistonStructureResolver resolver = new PistonStructureResolver(world, pistonPos, direction, extending);

        if (!resolver.resolve()) {
            return WirelessPistonMoveData.EMPTY;
        }

        Direction pushDirection = extending ? direction : direction.getOpposite();
        Map<Long, Long> movedTransmitters = new HashMap<>();
        Map<Long, Long> movedReceivers = new HashMap<>();

        for (Object object : resolver.getToPush()) {
            BlockPos sourcePos = (BlockPos) object;
            BlockState state = world.getBlockState(sourcePos);
            long sourcePacked = sourcePos.asLong();
            long targetPacked = sourcePos.relative(pushDirection).asLong();

            if (WirelessRedstoneMod.isTransmitterBlock(state)) {
                movedTransmitters.put(sourcePacked, targetPacked);
            } else if (WirelessRedstoneMod.isReceiverBlock(state)) {
                movedReceivers.put(sourcePacked, targetPacked);
            }
        }

        if (movedTransmitters.isEmpty() && movedReceivers.isEmpty()) {
            return WirelessPistonMoveData.EMPTY;
        }

        return new WirelessPistonMoveData(movedTransmitters, movedReceivers);
    }
}
