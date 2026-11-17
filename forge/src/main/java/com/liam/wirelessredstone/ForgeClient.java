package com.liam.wirelessredstone;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WirelessRedstoneMod.MOD_ID, value = Dist.CLIENT)
public final class ForgeClient {
    private ForgeClient() {
    }

    @SubscribeEvent
    public static void addHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(WirelessRedstonePackets.id("wireless_tooltip"),
                (graphics, tickCounter) -> WirelessRedstoneClient.renderHud(new HudGraphics(graphics)));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        WirelessRedstoneClient.reset();
    }

    static Connection connection() {
        var listener = Minecraft.getInstance().getConnection();
        return listener == null ? null : listener.getConnection();
    }

    static void openReceiverName(WirelessRedstonePackets.OpenReceiverNamePayload payload) {
        WirelessRedstoneClient.openReceiverName(Minecraft.getInstance(), payload);
    }

    static void showNameResult(WirelessRedstonePackets.ReceiverNameResultPayload payload) {
        WirelessRedstoneClient.showNameResult(Minecraft.getInstance(), payload);
    }

    static void showNotification(NotificationPayload payload) {
        WirelessNotifications.show(payload.message(), payload.warning());
    }
}
