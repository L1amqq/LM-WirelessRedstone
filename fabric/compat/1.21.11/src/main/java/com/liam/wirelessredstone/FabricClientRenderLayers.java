package com.liam.wirelessredstone;

import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

final class FabricClientRenderLayers {
    private FabricClientRenderLayers() {
    }

    static void register() {
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
                WirelessRedstoneMod.RECEIVER_BLOCK, WirelessRedstoneMod.TRANSMITTER_BLOCK);
    }
}
