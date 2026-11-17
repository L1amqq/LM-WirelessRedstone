package com.liam.wirelessredstone;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("null")
public final class WirelessRedstoneMod {
    public static final String MOD_ID = "lm_wireless_redstone";

    public static final Identifier RECEIVER_ID = id("receiver");
    public static final Identifier TRANSMITTER_ID = id("transmitter");

    public static final ResourceKey<Block> RECEIVER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, RECEIVER_ID);
    public static final ResourceKey<Block> TRANSMITTER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, TRANSMITTER_ID);

    public static final ResourceKey<Item> RECEIVER_ITEM_KEY = ResourceKey.create(Registries.ITEM, RECEIVER_ID);
    public static final ResourceKey<Item> TRANSMITTER_ITEM_KEY = ResourceKey.create(Registries.ITEM, TRANSMITTER_ID);

    public static final ReceiverBlock RECEIVER_BLOCK = new ReceiverBlock(copperProperties("waxed_oxidized_chiseled_copper").setId(RECEIVER_BLOCK_KEY));
    public static final TransmitterBlock TRANSMITTER_BLOCK = new TransmitterBlock(copperProperties("waxed_chiseled_copper").setId(TRANSMITTER_BLOCK_KEY));

    public static final BlockEntityType<ReceiverBlockEntity> RECEIVER_BLOCK_ENTITY_TYPE = PlatformRegistries.blockEntityType(ReceiverBlockEntity::new, RECEIVER_BLOCK);
    public static final BlockEntityType<TransmitterBlockEntity> TRANSMITTER_BLOCK_ENTITY_TYPE = PlatformRegistries.blockEntityType(TransmitterBlockEntity::new, TRANSMITTER_BLOCK);

    public static final BlockItem RECEIVER_BLOCK_ITEM = new ReceiverBlockItem(RECEIVER_BLOCK, new Item.Properties().setId(RECEIVER_ITEM_KEY));
    public static final BlockItem TRANSMITTER_BLOCK_ITEM = new GlintBlockItem(TRANSMITTER_BLOCK, new Item.Properties().setId(TRANSMITTER_ITEM_KEY));

    private static final Map<UUID, PendingLink> PENDING_LINKS = new HashMap<>();

    private static BlockBehaviour.Properties copperProperties(String blockId) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(blockId));
        return BlockBehaviour.Properties.ofFullCopy(BuiltInRegistries.BLOCK.getValueOrThrow(key));
    }

    private WirelessRedstoneMod() {
    }

    static void clearPendingLink(Player player) {
        PENDING_LINKS.remove(player.getUUID());
    }

    static InteractionResult handleUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide() || !(world instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        boolean amethystInCurrentHand = stack.is(Items.AMETHYST_SHARD);
        boolean amethystInEitherHand = player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.AMETHYST_SHARD)
                || player.getItemInHand(InteractionHand.OFF_HAND).is(Items.AMETHYST_SHARD);
        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = world.getBlockState(clickedPos);

        if (!amethystInCurrentHand) {
            if (amethystInEitherHand) {
                return InteractionResult.PASS;
            }

            if (isReceiverBlock(clickedState) && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
                String receiverName = ensureReceiverRegistered(serverLevel, clickedPos);
                openReceiverNameScreen(serverPlayer, clickedPos, receiverName);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (isTransmitterBlock(clickedState)) {
                unlinkTransmitter(player, serverLevel, clickedPos);
                return InteractionResult.SUCCESS;
            }

            if (isReceiverBlock(clickedState)) {
                showReceiverStatus(player, serverLevel, clickedPos);
                return InteractionResult.SUCCESS;
            }
        }

        if (isTransmitterBlock(clickedState)) {
            selectTransmitter(player, serverLevel, clickedPos);
            return InteractionResult.SUCCESS;
        }

        if (isReceiverBlock(clickedState)) {
            linkSelectedTransmitter(player, serverLevel, clickedPos);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void selectTransmitter(Player player, ServerLevel world, BlockPos transmitterPos) {
        String transmitterId = ensureTransmitterRegistered(world, transmitterPos);

        if (transmitterId.isBlank()) {
            sendWarning(player, "message.lm_wireless_redstone.transmitter_missing");
            return;
        }

        PENDING_LINKS.put(player.getUUID(), new PendingLink(world.dimension(), transmitterId));
        playTransmitterSelectSound(world, transmitterPos);
        sendInfo(player, "message.lm_wireless_redstone.transmitter_selected", shortId(transmitterId));
    }

    private static void linkSelectedTransmitter(Player player, ServerLevel world, BlockPos receiverPos) {
        PendingLink pendingLink = PENDING_LINKS.get(player.getUUID());

        if (pendingLink == null) {
            sendWarning(player, "message.lm_wireless_redstone.select_transmitter_first");
            return;
        }

        if (!pendingLink.dimension().equals(world.dimension())) {
            sendWarning(player, "message.lm_wireless_redstone.other_dimension");
            return;
        }

        WirelessLinkSavedData data = WirelessLinkSavedData.get(world);
        Long transmitterPacked = data.getTransmitterPos(pendingLink.transmitterId());

        if (transmitterPacked == null || !isTransmitterIdAt(world, BlockPos.of(transmitterPacked.longValue()), pendingLink.transmitterId())) {
            PENDING_LINKS.remove(player.getUUID());
            sendWarning(player, "message.lm_wireless_redstone.transmitter_missing");
            return;
        }

        String receiverName = ensureReceiverRegistered(world, receiverPos);

        if (receiverName.isBlank()) {
            sendWarning(player, "message.lm_wireless_redstone.receiver_missing");
            return;
        }

        data.setLink(pendingLink.transmitterId(), receiverName);
        data.updateReceiverCounts(world);
        PENDING_LINKS.remove(player.getUUID());
        WirelessRedstoneRuntime.tickWorld(world);
        playLinkSound(world, receiverPos);

        sendInfo(player, "message.lm_wireless_redstone.link_created", shortId(pendingLink.transmitterId()), receiverName);
    }

    private static void unlinkTransmitter(Player player, ServerLevel world, BlockPos transmitterPos) {
        String transmitterId = ensureTransmitterRegistered(world, transmitterPos);
        WirelessLinkSavedData data = WirelessLinkSavedData.get(world);

        if (!transmitterId.isBlank() && data.removeLinksFromTransmitter(transmitterPos) > 0) {
            data.updateReceiverCounts(world);
            WirelessRedstoneRuntime.tickWorld(world);
            playUnlinkSound(world, transmitterPos);
            sendInfo(player, "message.lm_wireless_redstone.link_removed", shortId(transmitterId));
        } else {
            sendWarning(player, "message.lm_wireless_redstone.no_link");
        }
    }

    private static void showReceiverStatus(Player player, ServerLevel world, BlockPos receiverPos) {
        String receiverName = ensureReceiverRegistered(world, receiverPos);
        WirelessLinkSavedData data = WirelessLinkSavedData.get(world);

        sendInfo(
                player,
                "message.lm_wireless_redstone.receiver_status",
                receiverName,
                data.countTransmittersForReceiverName(receiverName)
        );
    }

    static void handleBlockBreak(Level world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        WirelessLinkSavedData data = WirelessLinkSavedData.get(serverLevel);
        boolean changed = false;

        if (isTransmitterBlock(state)) {
            changed = data.unregisterTransmitter(pos);
        } else if (isReceiverBlock(state)) {
            changed = data.unregisterReceiver(pos);
        }

        if (changed) {
            data.updateReceiverCounts(serverLevel);
            WirelessRedstoneRuntime.tickWorld(serverLevel);
        }
    }

    static void handleSetReceiverName(WirelessRedstonePackets.SetReceiverNamePayload payload, ServerPlayer player) {

        if (!(player.level() instanceof ServerLevel world)) {
            return;
        }

        BlockPos receiverPos = payload.pos();

        if (player.blockPosition().distManhattan(receiverPos) > 16) {
            sendReceiverNameResult(player, receiverPos, false, "message.lm_wireless_redstone.receiver_too_far", "");
            return;
        }

        if (!isReceiverBlock(world.getBlockState(receiverPos)) || !(world.getBlockEntity(receiverPos) instanceof ReceiverBlockEntity receiver)) {
            sendReceiverNameResult(player, receiverPos, false, "message.lm_wireless_redstone.receiver_missing", "");
            return;
        }

        WirelessLinkSavedData data = WirelessLinkSavedData.get(world);
        String currentName = ensureReceiverRegistered(world, receiverPos);
        WirelessLinkSavedData.RenameResult result = data.renameReceiver(receiverPos, payload.requestedName());

        if (result.success()) {
            receiver.setReceiverName(result.receiverName());
            receiver.setConnectedTransmitters(data.countTransmittersForReceiverName(result.receiverName()));
            data.updateReceiverCounts(world);
            WirelessRedstoneRuntime.tickWorld(world);
            playRenameSound(world, receiverPos);
        } else if (!currentName.isBlank()) {
            receiver.setReceiverName(currentName);
        }

        sendReceiverNameResult(player, receiverPos, result.success(), result.messageKey(), result.receiverName());
    }

    public static void openReceiverNameScreen(ServerPlayer player, BlockPos receiverPos, String receiverName) {
        PlatformNetworking.sendToPlayer(player, new WirelessRedstonePackets.OpenReceiverNamePayload(receiverPos, receiverName));
    }

    private static String ensureReceiverRegistered(ServerLevel world, BlockPos receiverPos) {
        if (!(world.getBlockEntity(receiverPos) instanceof ReceiverBlockEntity receiver)) {
            return "";
        }

        WirelessLinkSavedData data = WirelessLinkSavedData.get(world);
        String receiverName = data.getReceiverName(receiverPos).orElse(receiver.getReceiverName());

        if (receiverName.isBlank()) {
            receiverName = data.registerReceiver(receiverPos, receiverName);
            receiver.setReceiverName(receiverName);
        } else if (data.getReceiverPos(receiverName) == null) {
            receiverName = data.registerReceiver(receiverPos, receiverName);
        }

        receiver.setReceiverName(receiverName);
        receiver.setConnectedTransmitters(data.countTransmittersForReceiverName(receiverName));
        return receiverName;
    }

    private static String ensureTransmitterRegistered(ServerLevel world, BlockPos transmitterPos) {
        if (!(world.getBlockEntity(transmitterPos) instanceof TransmitterBlockEntity transmitter)) {
            return "";
        }

        String transmitterId = WirelessLinkSavedData.normalizeTransmitterId(transmitter.getTransmitterId());

        if (transmitterId.isBlank()) {
            transmitterId = transmitter.getOrCreateTransmitterId();
        }

        WirelessLinkSavedData.get(world).registerTransmitter(transmitterPos, transmitterId);
        return transmitterId;
    }

    private static boolean isTransmitterIdAt(ServerLevel world, BlockPos transmitterPos, String transmitterId) {
        if (!(world.getBlockEntity(transmitterPos) instanceof TransmitterBlockEntity transmitter)) {
            return false;
        }

        return WirelessLinkSavedData.normalizeTransmitterId(transmitter.getTransmitterId()).equals(WirelessLinkSavedData.normalizeTransmitterId(transmitterId));
    }

    private static void sendReceiverNameResult(ServerPlayer player, BlockPos receiverPos, boolean success, String messageKey, String receiverName) {
        PlatformNetworking.sendToPlayer(player, new WirelessRedstonePackets.ReceiverNameResultPayload(receiverPos, success, messageKey, receiverName));
    }

    public static boolean isReceiverBlock(BlockState state) {
        return state.is(RECEIVER_BLOCK);
    }

    public static boolean isTransmitterBlock(BlockState state) {
        return state.is(TRANSMITTER_BLOCK);
    }

    private static String shortId(String transmitterId) {
        return transmitterId.length() <= 8 ? transmitterId : transmitterId.substring(0, 8);
    }

    private static void sendInfo(Player player, String key, Object... args) {
        NotificationPayload.send(player, Component.translatable(key, args), false);
    }

    private static void sendWarning(Player player, String key, Object... args) {
        NotificationPayload.send(player, Component.translatable(key, args), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6F, 1.0F);
    }

    private static void playLinkSound(ServerLevel world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.1F);
    }

    private static void playTransmitterSelectSound(ServerLevel world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 1.2F);
    }

    private static void playUnlinkSound(ServerLevel world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static void playRenameSound(ServerLevel world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private record PendingLink(ResourceKey<Level> dimension, String transmitterId) {
    }
}
