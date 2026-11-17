package com.liam.wirelessredstone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ClientGuiAccess {
    private ClientGuiAccess() {
    }

    static void openScreen(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }

    static Screen currentScreen(Minecraft client) {
        return client.gui.screen();
    }

    static boolean isHudHidden(Minecraft client) {
        return client.gui.hud.isHidden();
    }
}
