package com.liam.wirelessredstone;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

record NotificationPayload(Component message, boolean warning) implements CustomPacketPayload {
    static final Type<NotificationPayload> ID = new Type<>(WirelessRedstonePackets.id("notification"));
    static final StreamCodec<RegistryFriendlyByteBuf, NotificationPayload> CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, NotificationPayload::message,
            ByteBufCodecs.BOOL, NotificationPayload::warning,
            NotificationPayload::new
    );

    static void send(Player player, Component message, boolean warning) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlatformNetworking.sendToPlayer(serverPlayer, new NotificationPayload(message, warning));
        }
    }

    @Override
    public Type<NotificationPayload> type() {
        return ID;
    }
}
