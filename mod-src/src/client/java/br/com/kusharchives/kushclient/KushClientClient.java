package br.com.kusharchives.kushclient;

import br.com.kusharchives.kushclient.gui.KushMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KushClientClient implements ClientModInitializer {
    public static final String VERSION = "0.1.0";
    private static KeyMapping openMenu;
    private static boolean fpsEnabled = true;
    private static boolean coordinatesEnabled = true;

    @Override
    public void onInitializeClient() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.kushclient.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.kushclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.screen instanceof KushMenuScreen) client.setScreen(null);
                else client.setScreen(new KushMenuScreen(client.screen));
            }
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;

            if (fpsEnabled) {
                String text = "FPS  " + client.getFps();
                int x = 8, y = 8, width = client.font.width(text) + 12;
                graphics.fill(x, y, x + width, y + 18, 0xB009090B);
                graphics.fill(x, y, x + 2, y + 18, 0xFFFF3B30);
                graphics.drawString(client.font, text, x + 7, y + 5, 0xFFFFFFFF, false);
            }

            if (coordinatesEnabled) {
                String text = String.format("XYZ  %.1f  %.1f  %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
                int x = 8, y = 30, width = client.font.width(text) + 12;
                graphics.fill(x, y, x + width, y + 18, 0xB009090B);
                graphics.fill(x, y, x + 2, y + 18, 0xFFFF3B30);
                graphics.drawString(client.font, text, x + 7, y + 5, 0xFFFFFFFF, false);
            }
        });
    }

    public static boolean isFpsEnabled() { return fpsEnabled; }
    public static boolean isCoordinatesEnabled() { return coordinatesEnabled; }
    public static void toggleFps() { fpsEnabled = !fpsEnabled; }
    public static void toggleCoordinates() { coordinatesEnabled = !coordinatesEnabled; }
}
