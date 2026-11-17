package com.liam.wirelessredstone;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.saveddata.SavedDataType;

final class SavedNetworkType {
    private SavedNetworkType() {
    }

    static SavedDataType<WirelessLinkSavedData> create(Supplier<WirelessLinkSavedData> factory, Codec<WirelessLinkSavedData> codec) {
        return new SavedDataType<>(WirelessRedstoneMod.MOD_ID + "_network", factory, codec, null);
    }
}
