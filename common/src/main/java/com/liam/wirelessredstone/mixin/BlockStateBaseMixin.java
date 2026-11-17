package com.liam.wirelessredstone.mixin;

import com.liam.wirelessredstone.WirelessRedstoneMod;
import com.liam.wirelessredstone.WirelessRedstoneRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "isSignalSource", at = @At("HEAD"), cancellable = true)
    private void lmWirelessRedstone$isSignalSource(CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState) (Object) this;

        if (WirelessRedstoneMod.isTransmitterBlock(state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
    private void lmWirelessRedstone$getSignal(BlockGetter world, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        BlockState state = (BlockState) (Object) this;

        if (!WirelessRedstoneMod.isTransmitterBlock(state)) {
            return;
        }

        if (world instanceof Level level) {
            cir.setReturnValue(WirelessRedstoneRuntime.getPower(level, pos));
        }
    }

    @Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true)
    private void lmWirelessRedstone$getDirectSignal(BlockGetter world, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        BlockState state = (BlockState) (Object) this;

        if (!WirelessRedstoneMod.isTransmitterBlock(state)) {
            return;
        }

        if (world instanceof Level level) {
            cir.setReturnValue(WirelessRedstoneRuntime.getPower(level, pos));
        }
    }
}
