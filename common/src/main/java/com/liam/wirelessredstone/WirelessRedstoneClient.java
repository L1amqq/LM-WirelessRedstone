package com.liam.wirelessredstone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class WirelessRedstoneClient {
    private static final Identifier RECEIVER_ICON = Identifier.fromNamespaceAndPath(
            WirelessRedstoneMod.MOD_ID, "textures/gui/hud/radio-receiver.png");
    private static final Identifier TRANSMITTER_ICON = Identifier.fromNamespaceAndPath(
            WirelessRedstoneMod.MOD_ID, "textures/gui/hud/radio-tower.png");
    private static final int CARD_HEIGHT = 53;
    private static final HudEntrance TOOLTIP_ENTRANCE = new HudEntrance();

    private WirelessRedstoneClient() {
    }

    static void openReceiverName(Minecraft client, WirelessRedstonePackets.OpenReceiverNamePayload payload) {
        ClientGuiAccess.openScreen(client, new ReceiverNameScreen(payload.pos(), payload.currentName()));
    }

    static void showNameResult(Minecraft client, WirelessRedstonePackets.ReceiverNameResultPayload payload) {
        if (ClientGuiAccess.currentScreen(client) instanceof ReceiverNameScreen screen && screen.isFor(payload.pos())) {
            screen.applyResult(payload);
        }
        WirelessNotifications.show(Component.translatable(payload.messageKey()), !payload.success());
    }

    static void reset() {
        WirelessNotifications.clear();
        TOOLTIP_ENTRANCE.reset();
    }

    static void renderHud(HudGraphics graphics) {
        Minecraft client = Minecraft.getInstance();

        if (ClientGuiAccess.isHudHidden(client) || ClientGuiAccess.currentScreen(client) != null
                || client.level == null || client.player == null) {
            TOOLTIP_ENTRANCE.reset();
            return;
        }

        WirelessNotifications.render(client.font, graphics.guiWidth(), graphics.guiHeight() / 2 - 4,
                graphics::fill, (text, x, y, color) -> graphics.text(client.font, text, x, y, color));

        if (!client.player.getMainHandItem().is(Items.AMETHYST_SHARD) && !client.player.getOffhandItem().is(Items.AMETHYST_SHARD)) {
            TOOLTIP_ENTRANCE.reset();
            return;
        }

        if (!(client.hitResult instanceof BlockHitResult blockHitResult) || client.hitResult.getType() != HitResult.Type.BLOCK) {
            TOOLTIP_ENTRANCE.reset();
            return;
        }

        var blockEntity = client.level.getBlockEntity(blockHitResult.getBlockPos());
        float opacity = TOOLTIP_ENTRANCE.sample(
                blockEntity instanceof ReceiverBlockEntity || blockEntity instanceof TransmitterBlockEntity ? blockEntity : null);
        if (!HudEntrance.isVisible(opacity)) {
            return;
        }

        if (blockEntity instanceof ReceiverBlockEntity receiver) {
            String name = receiver.getReceiverName().isBlank()
                    ? Component.translatable("message.lm_wireless_redstone.receiver_unnamed").getString()
                    : receiver.getReceiverName();
            drawTooltip(graphics, client.font, RECEIVER_ICON, WirelessHudCard.MINT, opacity,
                    Component.translatable("tooltip.lm_wireless_redstone.receiver_title"), name,
                    Component.translatable("tooltip.lm_wireless_redstone.transmitters"), Integer.toString(receiver.getConnectedTransmitters()));
        } else if (blockEntity instanceof TransmitterBlockEntity transmitter) {
            String link = transmitter.getLinkedReceiverName().isBlank()
                    ? Component.translatable("message.lm_wireless_redstone.transmitter_unlinked").getString()
                    : transmitter.getLinkedReceiverName();
            drawTooltip(graphics, client.font, TRANSMITTER_ICON, WirelessHudCard.COPPER, opacity,
                    Component.translatable("tooltip.lm_wireless_redstone.transmitter_title"), shortId(transmitter.getTransmitterId()),
                    Component.translatable("tooltip.lm_wireless_redstone.receiver_title"), link);
        }
    }

    private static void drawTooltip(HudGraphics graphics, Font font, Identifier icon, int accent, float opacity,
                                    Component title, String name, Component detail, String value) {
        Style nameStyle = Style.EMPTY.withBold(true);
        int headerWidth = 47 + Math.max(font.width(title), font.width(Component.literal(name).withStyle(nameStyle)));
        int footerWidth = 42 + font.width(detail) + font.width(value);
        int maxWidth = Math.min(188, graphics.guiWidth() / 2 - 20);
        int width = Math.min(maxWidth, Math.max(130, Math.max(headerWidth, footerWidth)));
        int x = graphics.guiWidth() / 2 + 14 + HudEntrance.offset(5, opacity);
        int y = Math.min(graphics.guiHeight() / 2 + 12, graphics.guiHeight() - CARD_HEIGHT - 28);
        int right = x + width;
        int tint = WirelessHudCard.tint(accent);
        int textAccent = HudEntrance.withOpacity(accent, opacity);
        WirelessHudCard.Fill fill = (left, top, end, bottom, color) ->
                graphics.fill(left, top, end, bottom, HudEntrance.withOpacity(color, opacity));

        WirelessHudCard.drawFrame(fill, x, y, width, CARD_HEIGHT);
        fill.rect(x, y + 7, x + 2, y + 29, accent);
        fill.rect(x + 10, y + 7, x + 32, y + 29, tint);
        graphics.icon(icon, x + 13, y + 10,
                HudEntrance.withOpacity(0xFFFFFFFF, opacity));

        graphics.text(font, fitText(font, title.getString(), width - 47, Style.EMPTY), x + 39, y + 8, textAccent);
        graphics.text(font, fitText(font, name, width - 47, nameStyle), x + 39, y + 20,
                HudEntrance.withOpacity(0xFFF4F7FA, opacity));
        fill.rect(x + 10, y + 34, right - 10, y + 35, 0xFF36454D);

        Component footerLabel = fitText(font, detail.getString(), width / 2 + 7, Style.EMPTY);
        Component footerValue = fitText(font, value, width - 40 - font.width(footerLabel), Style.EMPTY);
        int valueWidth = font.width(footerValue);
        int valueX = right - 15 - valueWidth;
        graphics.text(font, footerLabel, x + 10, y + 40, HudEntrance.withOpacity(0xFFB8C5CE, opacity));
        fill.rect(valueX - 5, y + 37, right - 10, y + 49, tint);
        graphics.text(font, footerValue, valueX, y + 40, textAccent);
    }

    private static Component fitText(Font font, String text, int maxWidth, Style style) {
        Component fullText = Component.literal(text).withStyle(style);
        if (font.width(fullText) <= maxWidth) {
            return fullText;
        }

        int ellipsisWidth = font.width(Component.literal("…").withStyle(style));
        String prefix = font.getSplitter().plainHeadByWidth(text, maxWidth - ellipsisWidth, style);
        return Component.literal(prefix + "…").withStyle(style);
    }

    private static String shortId(String transmitterId) {
        if (transmitterId == null || transmitterId.isBlank()) {
            return Component.translatable("message.lm_wireless_redstone.transmitter_unknown").getString();
        }

        return transmitterId.length() <= 8 ? transmitterId : transmitterId.substring(0, 8);
    }
}
