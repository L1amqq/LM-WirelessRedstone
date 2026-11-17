package com.liam.wirelessredstone;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientRenderLayers.register();
        ClientPlayNetworking.registerGlobalReceiver(WirelessRedstonePackets.OpenReceiverNamePayload.ID,
                (payload, context) -> WirelessRedstoneClient.openReceiverName(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(WirelessRedstonePackets.ReceiverNameResultPayload.ID,
                (payload, context) -> WirelessRedstoneClient.showNameResult(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(NotificationPayload.ID,
                (payload, context) -> WirelessNotifications.show(payload.message(), payload.warning()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WirelessRedstoneClient.reset());
        HudElementRegistry.addLast(WirelessRedstonePackets.id("wireless_tooltip"),
                (graphics, tickCounter) -> WirelessRedstoneClient.renderHud(new HudGraphics(graphics)));
    }
}
