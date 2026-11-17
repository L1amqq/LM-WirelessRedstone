package com.liam.wirelessredstone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK, WirelessRedstoneMod.RECEIVER_BLOCK_KEY, WirelessRedstoneMod.RECEIVER_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, WirelessRedstoneMod.TRANSMITTER_BLOCK_KEY, WirelessRedstoneMod.TRANSMITTER_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, WirelessRedstoneMod.RECEIVER_ID, WirelessRedstoneMod.RECEIVER_BLOCK_ENTITY_TYPE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, WirelessRedstoneMod.TRANSMITTER_ID, WirelessRedstoneMod.TRANSMITTER_BLOCK_ENTITY_TYPE);
        Registry.register(BuiltInRegistries.ITEM, WirelessRedstoneMod.RECEIVER_ITEM_KEY, WirelessRedstoneMod.RECEIVER_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, WirelessRedstoneMod.TRANSMITTER_ITEM_KEY, WirelessRedstoneMod.TRANSMITTER_BLOCK_ITEM);

        FabricVersionEvents.register();
        ServerPlayNetworking.registerGlobalReceiver(WirelessRedstonePackets.SetReceiverNamePayload.ID,
                (payload, context) -> WirelessRedstoneMod.handleSetReceiverName(payload, context.player()));
        UseBlockCallback.EVENT.register(WirelessRedstoneMod::handleUseBlock);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) ->
                WirelessRedstoneMod.handleBlockBreak(world, pos, state));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> WirelessRedstoneMod.clearPendingLink(handler.player));
    }
}
