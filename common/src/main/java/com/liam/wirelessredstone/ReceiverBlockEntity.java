package com.liam.wirelessredstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ReceiverBlockEntity extends BlockEntity {
    private static final String NAME_KEY = "receiver_name";
    private static final String CONNECTED_TRANSMITTERS_KEY = "connected_transmitters";

    private String receiverName = "";
    private int connectedTransmitters;

    public ReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(WirelessRedstoneMod.RECEIVER_BLOCK_ENTITY_TYPE, pos, state);
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        String normalizedName = WirelessLinkSavedData.normalizeReceiverName(receiverName);

        if (this.receiverName.equals(normalizedName)) {
            return;
        }

        this.receiverName = normalizedName;
        markChangedAndSync();
    }

    public int getConnectedTransmitters() {
        return connectedTransmitters;
    }

    public void setConnectedTransmitters(int connectedTransmitters) {
        int safeValue = Math.max(0, connectedTransmitters);

        if (this.connectedTransmitters == safeValue) {
            return;
        }

        this.connectedTransmitters = safeValue;
        markChangedAndSync();
    }

    @Override
    public void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        receiverName = WirelessLinkSavedData.normalizeReceiverName(data.getStringOr(NAME_KEY, ""));
        connectedTransmitters = Math.max(0, data.getIntOr(CONNECTED_TRANSMITTERS_KEY, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        data.putString(NAME_KEY, receiverName);
        data.putInt(CONNECTED_TRANSMITTERS_KEY, connectedTransmitters);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider wrapperLookup) {
        return saveCustomOnly(wrapperLookup);
    }

    private void markChangedAndSync() {
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(worldPosition);
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
