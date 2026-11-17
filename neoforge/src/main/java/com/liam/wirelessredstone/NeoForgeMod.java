package com.liam.wirelessredstone;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(WirelessRedstoneMod.MOD_ID)
public final class NeoForgeMod {
    private final Map<ServerLevel, List<BrokenBlock>> pendingBreaks = new IdentityHashMap<>();

    public NeoForgeMod(IEventBus modBus) {
        modBus.addListener(NeoForgeMod::registerContent);
        modBus.addListener(NeoForgeMod::registerPayloads);
        modBus.addListener(NeoForgeMod::addCreativeItems);
        NeoForge.EVENT_BUS.addListener(NeoForgeMod::onRightClick);
        NeoForgeBreakEvents.register(this::onBreak);
        NeoForge.EVENT_BUS.addListener(this::afterLevelTick);
        NeoForge.EVENT_BUS.addListener(this::onUnload);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) ->
                WirelessRedstoneMod.clearPendingLink(event.getEntity()));
    }

    private static void registerContent(RegisterEvent event) {
        event.register(Registries.BLOCK, registry -> {
            registry.register(WirelessRedstoneMod.RECEIVER_ID, WirelessRedstoneMod.RECEIVER_BLOCK);
            registry.register(WirelessRedstoneMod.TRANSMITTER_ID, WirelessRedstoneMod.TRANSMITTER_BLOCK);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, registry -> {
            registry.register(WirelessRedstoneMod.RECEIVER_ID, WirelessRedstoneMod.RECEIVER_BLOCK_ENTITY_TYPE);
            registry.register(WirelessRedstoneMod.TRANSMITTER_ID, WirelessRedstoneMod.TRANSMITTER_BLOCK_ENTITY_TYPE);
        });
        event.register(Registries.ITEM, registry -> {
            registry.register(WirelessRedstoneMod.RECEIVER_ID, WirelessRedstoneMod.RECEIVER_BLOCK_ITEM);
            registry.register(WirelessRedstoneMod.TRANSMITTER_ID, WirelessRedstoneMod.TRANSMITTER_BLOCK_ITEM);
        });
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
            event.accept(WirelessRedstoneMod.RECEIVER_BLOCK_ITEM);
            event.accept(WirelessRedstoneMod.TRANSMITTER_BLOCK_ITEM);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(WirelessRedstonePackets.OpenReceiverNamePayload.ID, WirelessRedstonePackets.OpenReceiverNamePayload.CODEC);
        registrar.playToClient(WirelessRedstonePackets.ReceiverNameResultPayload.ID, WirelessRedstonePackets.ReceiverNameResultPayload.CODEC);
        registrar.playToClient(NotificationPayload.ID, NotificationPayload.CODEC);
        registrar.playToServer(WirelessRedstonePackets.SetReceiverNamePayload.ID, WirelessRedstonePackets.SetReceiverNamePayload.CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        WirelessRedstoneMod.handleSetReceiverName(payload, player);
                    }
                });
    }

    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = WirelessRedstoneMod.handleUseBlock(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private void onBreak(LevelAccessor world, BlockPos pos, BlockState state) {
        if (world instanceof ServerLevel level
                && (WirelessRedstoneMod.isReceiverBlock(state) || WirelessRedstoneMod.isTransmitterBlock(state))) {
            pendingBreaks.computeIfAbsent(level, ignored -> new ArrayList<>())
                    .add(new BrokenBlock(pos.immutable(), state));
        }
    }

    private void afterLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            List<BrokenBlock> brokenBlocks = pendingBreaks.remove(level);
            if (brokenBlocks != null) {
                for (BrokenBlock broken : brokenBlocks) {
                    if (!level.getBlockState(broken.pos()).is(broken.state().getBlock())) {
                        WirelessRedstoneMod.handleBlockBreak(level, broken.pos(), broken.state());
                    }
                }
            }
            WirelessRedstoneRuntime.tickWorld(level);
        }
    }

    private void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            pendingBreaks.remove(level);
            WirelessRedstoneRuntime.clearWorld(level);
        }
    }

    private record BrokenBlock(BlockPos pos, BlockState state) {
    }

    @FunctionalInterface
    interface BreakListener {
        void beforeBreak(LevelAccessor world, BlockPos pos, BlockState state);
    }
}
