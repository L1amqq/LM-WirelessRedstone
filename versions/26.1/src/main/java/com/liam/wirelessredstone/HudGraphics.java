package com.liam.wirelessredstone;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

record HudGraphics(GuiGraphicsExtractor graphics) {
    int guiWidth() {
        return graphics.guiWidth();
    }

    int guiHeight() {
        return graphics.guiHeight();
    }

    void fill(int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, bottom, color);
    }

    void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, false);
    }

    void text(Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, false);
    }

    void icon(Identifier texture, int x, int y, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 16, 16, 16, 16, color);
    }
}
