package com.liam.wirelessredstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TransmitterBlock extends Block implements EntityBlock {
    public TransmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransmitterBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);

        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(world.getBlockEntity(pos) instanceof TransmitterBlockEntity transmitter)) {
            return;
        }

        WirelessLinkSavedData.get(serverLevel).registerTransmitter(pos, transmitter.getOrCreateTransmitterId());
    }
}
