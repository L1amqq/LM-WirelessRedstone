package com.liam.wirelessredstone;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.world.item.CreativeModeTabs;

final class FabricVersionEvents {
    private FabricVersionEvents() {
    }

    static void register() {
        PayloadTypeRegistry.playS2C().register(WirelessRedstonePackets.OpenReceiverNamePayload.ID, WirelessRedstonePackets.OpenReceiverNamePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WirelessRedstonePackets.ReceiverNameResultPayload.ID, WirelessRedstonePackets.ReceiverNameResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NotificationPayload.ID, NotificationPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WirelessRedstonePackets.SetReceiverNamePayload.ID, WirelessRedstonePackets.SetReceiverNamePayload.CODEC);
        ServerTickEvents.END_WORLD_TICK.register(WirelessRedstoneRuntime::tickWorld);
        ServerWorldEvents.UNLOAD.register((server, world) -> WirelessRedstoneRuntime.clearWorld(world));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.accept(WirelessRedstoneMod.RECEIVER_BLOCK_ITEM);
            entries.accept(WirelessRedstoneMod.TRANSMITTER_BLOCK_ITEM);
        });
    }
}
