package com.liam.wirelessredstone;

import java.util.List;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class WirelessNotifications {
    private static final long DURATION = TimeUnit.SECONDS.toNanos(5);
    private static Notice currentNotice;

    private WirelessNotifications() {
    }

    static void show(Component message, boolean warning) {
        long now = System.nanoTime();
        Notice previous = currentNotice;
        boolean repeat = previous != null && previous.warning() == warning && previous.message().equals(message)
                && now - previous.shownAt() < DURATION;
        currentNotice = new Notice(message, warning, repeat ? previous.animationStartedAt() : now, now);
    }

    static void clear() {
        currentNotice = null;
    }

    static void render(Font font, int screenWidth, int maxBottom, WirelessHudCard.Fill fill, DrawText drawText) {
        Notice notice = currentNotice;
        if (notice == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - notice.shownAt() >= DURATION) {
            clear();
            return;
        }

        float opacity = HudEntrance.ease(now - notice.animationStartedAt());
        if (!HudEntrance.isVisible(opacity)) {
            return;
        }

        int maxWidth = Math.min(252, screenWidth - 16);
        if (maxWidth <= 42) {
            return;
        }
        int top = 10;
        int y = top - HudEntrance.offset(6, opacity);
        int width = Math.min(maxWidth, Math.max(134, font.width(notice.message()) + 42));
        List<FormattedCharSequence> lines = font.split(notice.message(), width - 42);
        int lineHeight = font.lineHeight + 2;
        int textHeight = lines.size() * lineHeight - 2;
        int height = Math.max(29, textHeight + 12);
        if (top + height > maxBottom) {
            return;
        }

        int x = (screenWidth - width) / 2;
        int accent = notice.warning() ? WirelessHudCard.COPPER : WirelessHudCard.MINT;
        int textAccent = HudEntrance.withOpacity(accent, opacity);
        WirelessHudCard.Fill fadedFill = (left, upper, right, bottom, color) ->
                fill.rect(left, upper, right, bottom, HudEntrance.withOpacity(color, opacity));
        WirelessHudCard.drawFrame(fadedFill, x, y, width, height);
        fadedFill.rect(x, y + 6, x + 2, y + height - 6, accent);

        int iconX = x + 10;
        int iconY = y + (height - 18) / 2;
        fadedFill.rect(iconX, iconY, iconX + 18, iconY + 18, WirelessHudCard.tint(accent));
        Component icon = Component.literal(notice.warning() ? "!" : "✓");
        drawText.line(icon.getVisualOrderText(), iconX + (18 - font.width(icon)) / 2, iconY + 5, textAccent);

        int textY = y + (height - textHeight) / 2;
        for (FormattedCharSequence line : lines) {
            drawText.line(line, x + 34, textY, HudEntrance.withOpacity(0xFFF4F7FA, opacity));
            textY += lineHeight;
        }
    }

    @FunctionalInterface
    interface DrawText {
        void line(FormattedCharSequence text, int x, int y, int color);
    }

    private record Notice(Component message, boolean warning, long animationStartedAt, long shownAt) {
    }
}
