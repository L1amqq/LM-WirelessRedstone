package com.liam.wirelessredstone;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = WirelessRedstoneMod.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClient {
    public NeoForgeClient(IEventBus modBus) {
        modBus.addListener(NeoForgeClient::registerPayloadHandlers);
        modBus.addListener(NeoForgeClient::registerHud);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> WirelessRedstoneClient.reset());
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(WirelessRedstonePackets.OpenReceiverNamePayload.ID,
                (payload, context) -> WirelessRedstoneClient.openReceiverName(Minecraft.getInstance(), payload));
        event.register(WirelessRedstonePackets.ReceiverNameResultPayload.ID,
                (payload, context) -> WirelessRedstoneClient.showNameResult(Minecraft.getInstance(), payload));
        event.register(NotificationPayload.ID, (payload, context) -> WirelessNotifications.show(payload.message(), payload.warning()));
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(WirelessRedstonePackets.id("wireless_tooltip"),
                (graphics, tickCounter) -> WirelessRedstoneClient.renderHud(new HudGraphics(graphics)));
    }
}
