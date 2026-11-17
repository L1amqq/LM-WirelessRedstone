package com.liam.wirelessredstone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class WirelessRedstonePackets {
    private WirelessRedstonePackets() {
    }

    public record OpenReceiverNamePayload(BlockPos pos, String currentName) implements CustomPacketPayload {
        public static final Type<OpenReceiverNamePayload> ID = new Type<>(id("open_receiver_name"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenReceiverNamePayload> CODEC = CustomPacketPayload.codec(OpenReceiverNamePayload::write, OpenReceiverNamePayload::new);

        public OpenReceiverNamePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readUtf(WirelessLinkSavedData.MAX_RECEIVER_NAME_LENGTH));
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(currentName, WirelessLinkSavedData.MAX_RECEIVER_NAME_LENGTH);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record SetReceiverNamePayload(BlockPos pos, String requestedName) implements CustomPacketPayload {
        public static final Type<SetReceiverNamePayload> ID = new Type<>(id("set_receiver_name"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetReceiverNamePayload> CODEC = CustomPacketPayload.codec(SetReceiverNamePayload::write, SetReceiverNamePayload::new);

        public SetReceiverNamePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readUtf(WirelessLinkSavedData.MAX_RECEIVER_NAME_LENGTH));
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(requestedName, WirelessLinkSavedData.MAX_RECEIVER_NAME_LENGTH);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ReceiverNameResultPayload(BlockPos pos, boolean success, String messageKey, String currentName) implements CustomPacketPayload {
        public static final Type<ReceiverNameResultPayload> ID = new Type<>(id("receiver_name_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReceiverNameResultPayload> CODEC = CustomPacketPayload.codec(ReceiverNameResultPayload::write, ReceiverNameResultPayload::new);

        public ReceiverNameResultPayload(RegistryFriendlyByteBuf buf) {
            this(
                    buf.readBlockPos(),
                    buf.readBoolean(),
                    buf.readUtf(128),
                    buf.readUtf(WirelessLinkSavedData.MAX_RECEIVER_NAME_LENGTH)
            );
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeBoolean(success);
            buf.writeUtf(messageKey, 128);
            buf.writeUtf(currentName, WirelessLinkSavedData.MAX_RECEIVER_NAME_LENGTH);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WirelessRedstoneMod.MOD_ID, path);
    }
}
