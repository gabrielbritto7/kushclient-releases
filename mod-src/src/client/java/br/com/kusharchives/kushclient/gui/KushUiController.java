package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.KushClientClient;
import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class KushUiController {
    private KushUiController() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (KushClientClient.getInstance().getConfigManager().isUseKushUi()) return;
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                KushButton restore = new KushButton(scaledWidth - 48, 10, 38, 24, Component.literal("K"), b -> {
                    KushClientClient.getInstance().getConfigManager().setUseKushUi(true);
                    KushClientClient.getInstance().getConfigManager().save();
                    if (screen instanceof PauseScreen) client.setScreen(new KushPauseScreen());
                    else client.setScreen(new KushTitleScreen());
                }, true);
                Screens.getButtons(screen).add(restore);
            }
        });
    }

    public static void onClientTick(Minecraft client) {
        if (!KushClientClient.getInstance().getConfigManager().isUseKushUi()) return;
        if (client.screen instanceof TitleScreen) client.setScreen(new KushTitleScreen());
        else if (client.screen instanceof PauseScreen) client.setScreen(new KushPauseScreen());
    }
}
