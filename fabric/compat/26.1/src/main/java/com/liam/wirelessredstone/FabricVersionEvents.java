package com.liam.wirelessredstone;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.world.item.CreativeModeTabs;

final class FabricVersionEvents {
    private FabricVersionEvents() {
    }

    static void register() {
        PayloadTypeRegistry.clientboundPlay().register(WirelessRedstonePackets.OpenReceiverNamePayload.ID, WirelessRedstonePackets.OpenReceiverNamePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WirelessRedstonePackets.ReceiverNameResultPayload.ID, WirelessRedstonePackets.ReceiverNameResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NotificationPayload.ID, NotificationPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(WirelessRedstonePackets.SetReceiverNamePayload.ID, WirelessRedstonePackets.SetReceiverNamePayload.CODEC);
        ServerTickEvents.END_LEVEL_TICK.register(WirelessRedstoneRuntime::tickWorld);
        ServerLevelEvents.UNLOAD.register((server, world) -> WirelessRedstoneRuntime.clearWorld(world));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.accept(WirelessRedstoneMod.RECEIVER_BLOCK_ITEM);
            entries.accept(WirelessRedstoneMod.TRANSMITTER_BLOCK_ITEM);
        });
    }
}
