package com.liam.wirelessredstone;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

@Mod(WirelessRedstoneMod.MOD_ID)
public final class ForgeMod {
    private final Map<ServerLevel, List<BrokenBlock>> pendingBreaks = new IdentityHashMap<>();

    public ForgeMod(FMLJavaModLoadingContext context) {
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgeMod::registerContent);
        BuildCreativeModeTabContentsEvent.BUS.addListener(ForgeMod::addCreativeItems);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(ForgeMod::onRightClick);
        BlockEvent.BreakEvent.BUS.addListener(this::onBreak);
        TickEvent.LevelTickEvent.Post.BUS.addListener(this::afterLevelTick);
        LevelEvent.Unload.BUS.addListener(this::onUnload);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event ->
                WirelessRedstoneMod.clearPendingLink(event.getEntity()));
        PlatformNetworking.register();
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

    private static boolean onRightClick(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = WirelessRedstoneMod.handleUseBlock(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result == InteractionResult.PASS) {
            return false;
        }
        event.setCancellationResult(result);
        return true;
    }

    private void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (event.getLevel() instanceof ServerLevel level
                && (WirelessRedstoneMod.isReceiverBlock(state) || WirelessRedstoneMod.isTransmitterBlock(state))) {
            pendingBreaks.computeIfAbsent(level, ignored -> new ArrayList<>())
                    .add(new BrokenBlock(event.getPos().immutable(), state));
        }
    }

    private void afterLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level() instanceof ServerLevel level) {
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
}
