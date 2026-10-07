package br.com.kusharchives.kushclient.input;

import br.com.kusharchives.kushclient.gui.KushMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KushKeybinds {
    private static KeyMapping openMenu;
    private KushKeybinds() {}
    public static void register() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kushclient.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.kushclient"));
    }
    public static void onClientTick(Minecraft client) {
        if (openMenu == null) return;
        while (openMenu.consumeClick()) {
            if (client.screen instanceof KushMenuScreen) client.setScreen(null);
            else client.setScreen(new KushMenuScreen(client.screen));
        }
    }
}
