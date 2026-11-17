package com.liam.wirelessredstone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ClientGuiAccess {
    private ClientGuiAccess() {
    }

    static void openScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    static Screen currentScreen(Minecraft client) {
        return client.screen;
    }

    static boolean isHudHidden(Minecraft client) {
        return client.options.hideGui;
    }
}
