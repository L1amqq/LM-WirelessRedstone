package com.liam.wirelessredstone;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class TransmitterBlockEntity extends BlockEntity {
    private static final String ID_KEY = "transmitter_id";
    private static final String LINKED_RECEIVER_NAME_KEY = "linked_receiver_name";

    private String transmitterId = "";
    private String linkedReceiverName = "";

    public TransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(WirelessRedstoneMod.TRANSMITTER_BLOCK_ENTITY_TYPE, pos, state);
    }

    public String getTransmitterId() {
        return transmitterId;
    }

    public String getOrCreateTransmitterId() {
        if (transmitterId.isBlank()) {
            setTransmitterId(UUID.randomUUID().toString());
        }

        return transmitterId;
    }

    public void setTransmitterId(String transmitterId) {
        String safeId = transmitterId == null ? "" : transmitterId.trim();

        if (this.transmitterId.equals(safeId)) {
            return;
        }

        this.transmitterId = safeId;
        markChangedAndSync();
    }

    public String getLinkedReceiverName() {
        return linkedReceiverName;
    }

    public void setLinkedReceiverName(String linkedReceiverName) {
        String safeName = WirelessLinkSavedData.normalizeReceiverName(linkedReceiverName);

        if (this.linkedReceiverName.equals(safeName)) {
            return;
        }

        this.linkedReceiverName = safeName;
        markChangedAndSync();
    }

    @Override
    public void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        transmitterId = data.getStringOr(ID_KEY, "").trim();
        linkedReceiverName = WirelessLinkSavedData.normalizeReceiverName(data.getStringOr(LINKED_RECEIVER_NAME_KEY, ""));
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        data.putString(ID_KEY, transmitterId);
        data.putString(LINKED_RECEIVER_NAME_KEY, linkedReceiverName);
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
