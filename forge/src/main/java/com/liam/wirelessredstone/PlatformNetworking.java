package com.liam.wirelessredstone;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

final class PlatformNetworking {
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(WirelessRedstonePackets.id("main"))
            .networkProtocolVersion(1).simpleChannel();

    private PlatformNetworking() {
    }

    static void register() {
        var play = CHANNEL.play();
        play.clientbound()
                .addMain(WirelessRedstonePackets.OpenReceiverNamePayload.class,
                        WirelessRedstonePackets.OpenReceiverNamePayload.CODEC, (payload, context) -> ForgeClient.openReceiverName(payload))
                .addMain(WirelessRedstonePackets.ReceiverNameResultPayload.class,
                        WirelessRedstonePackets.ReceiverNameResultPayload.CODEC, (payload, context) -> ForgeClient.showNameResult(payload))
                .addMain(NotificationPayload.class, NotificationPayload.CODEC,
                        (payload, context) -> ForgeClient.showNotification(payload));
        play.serverbound()
                .addMain(WirelessRedstonePackets.SetReceiverNamePayload.class,
                        WirelessRedstonePackets.SetReceiverNamePayload.CODEC, (payload, context) -> {
                            ServerPlayer player = context.getSender();
                            if (player != null) {
                                WirelessRedstoneMod.handleSetReceiverName(payload, player);
                            }
                        });
        CHANNEL.build();
    }

    static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        var connection = player.connection.getConnection();
        if (CHANNEL.isRemotePresent(connection)) {
            CHANNEL.send(payload, connection);
        }
    }

    static boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        var connection = ForgeClient.connection();
        return WirelessRedstonePackets.SetReceiverNamePayload.ID.equals(type)
                && connection != null && CHANNEL.isRemotePresent(connection);
    }

    static void sendToServer(CustomPacketPayload payload) {
        var connection = ForgeClient.connection();
        if (connection != null && CHANNEL.isRemotePresent(connection)) {
            CHANNEL.send(payload, connection);
        }
    }
}
