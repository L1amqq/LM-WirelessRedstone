package com.liam.wirelessredstone;

import java.util.concurrent.TimeUnit;

import net.minecraft.world.level.block.entity.BlockEntity;

final class HudEntrance {
    private static final long DURATION = TimeUnit.MILLISECONDS.toNanos(200);
    private BlockEntity target;
    private long startedAt;

    float sample(BlockEntity blockEntity) {
        long now = System.nanoTime();
        if (target != blockEntity) {
            target = blockEntity;
            startedAt = now;
        }
        return blockEntity == null ? 0.0F : ease(now - startedAt);
    }

    void reset() {
        target = null;
    }

    static float ease(long elapsed) {
        float progress = Math.clamp((float) elapsed / DURATION, 0.0F, 1.0F);
        return progress * progress * (3.0F - 2.0F * progress);
    }

    static boolean isVisible(float opacity) {
        // Minecraft fonts may treat an alpha below 4 as fully opaque.
        return Math.round(opacity * 255.0F) >= 4;
    }

    static int withOpacity(int color, float opacity) {
        int alpha = Math.round((color >>> 24) * opacity);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    static int offset(int distance, float progress) {
        return Math.round(distance * (1.0F - progress));
    }
}
