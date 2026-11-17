package com.liam.wirelessredstone;

final class WirelessHudCard {
    static final int MINT = 0xFF79E6C7;
    static final int COPPER = 0xFFFFAA89;
    private static final int BORDER = 0xFF36454D;

    private WirelessHudCard() {
    }

    static void drawFrame(Fill fill, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        fill.rect(x + 1, y + 1, right + 1, bottom + 1, 0x38000000);
        fill.rect(x, y, right, bottom, 0xF2111A21);
        fill.rect(x, y, right, y + 1, BORDER);
        fill.rect(x, bottom - 1, right, bottom, BORDER);
        fill.rect(x, y + 1, x + 1, bottom - 1, BORDER);
        fill.rect(right - 1, y + 1, right, bottom - 1, BORDER);
    }

    static int tint(int accent) {
        return (accent & 0x00FFFFFF) | 0x24000000;
    }

    @FunctionalInterface
    interface Fill {
        void rect(int x, int y, int right, int bottom, int color);
    }
}
