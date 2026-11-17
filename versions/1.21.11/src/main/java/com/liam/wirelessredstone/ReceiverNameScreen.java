package com.liam.wirelessredstone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class ReceiverNameScreen extends Screen {
    private final BlockPos receiverPos;
    private final String initialName;
    private EditBox nameBox;

    public ReceiverNameScreen(BlockPos receiverPos, String initialName) {
        super(Component.translatable("screen.lm_wireless_redstone.receiver_name"));
        this.receiverPos = receiverPos;
        this.initialName = initialName;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int top = height / 2 - 54;

        nameBox = addRenderableWidget(new EditBox(font, centerX - 100, top + 32, 200, 20, Component.translatable("screen.lm_wireless_redstone.receiver_name_field")));
        nameBox.setMaxLength(WirelessLinkSavedData.MAX_RECEIVER_NAME_LENGTH);
        nameBox.setValue(initialName);
        setInitialFocus(nameBox);

        addRenderableWidget(Button.builder(Component.translatable("screen.lm_wireless_redstone.save"), button -> save())
                .pos(centerX - 100, top + 66)
                .size(96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.lm_wireless_redstone.cancel"), button -> onClose())
                .pos(centerX + 4, top + 66)
                .size(96, 20)
                .build());
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == 257 || keyEvent.key() == 335) {
            save();
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        int centerX = width / 2;
        int top = height / 2 - 54;

        drawContext.drawCenteredString(font, title, centerX, top, 0xFFFFFFFF);
        drawContext.drawString(font, Component.translatable("screen.lm_wireless_redstone.receiver_name_label"), centerX - 100, top + 20, 0xFFA0A0A0);

        WirelessNotifications.render(font, width, top - 6, drawContext::fill,
                (text, x, y, color) -> drawContext.drawString(font, text, x, y, color, false));
    }

    public boolean isFor(BlockPos pos) {
        return receiverPos.equals(pos);
    }

    public void applyResult(WirelessRedstonePackets.ReceiverNameResultPayload payload) {
        if (payload.success()) {
            onClose();
        } else if (nameBox != null) {
            nameBox.setValue(payload.currentName());
        }
    }

    private void save() {
        String requestedName = nameBox == null ? "" : nameBox.getValue();

        if (!PlatformNetworking.canSendToServer(WirelessRedstonePackets.SetReceiverNamePayload.ID)) {
            WirelessNotifications.show(Component.translatable("message.lm_wireless_redstone.network_unavailable"), true);
            return;
        }

        PlatformNetworking.sendToServer(new WirelessRedstonePackets.SetReceiverNamePayload(receiverPos, requestedName));
    }
}
