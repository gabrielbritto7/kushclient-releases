package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KushMenuScreen extends Screen {
    private final Screen parent;
    public KushMenuScreen(Screen parent) { super(Component.literal("KushClient Quick Menu")); this.parent = parent; }

    @Override protected void init() {
        int centerX = width / 2;
        int y = Math.max(110, height / 2 - 70);
        addRenderableWidget(new KushButton(centerX - 125, y, 250, 34, Component.literal("KUSH SETTINGS"), b -> minecraft.setScreen(new KushModsScreen(this)), true));
        int smallY = y + 42;
        KushButton store = new KushButton(centerX - 125, smallY, 76, 28, Component.literal("STORE"), b -> minecraft.setScreen(new KushPlaceholderScreen(this, "Store", "Loja ficará para uma etapa futura.")), false);
        store.active = false;
        addRenderableWidget(store);
        addRenderableWidget(new KushButton(centerX - 38, smallY, 76, 28, Component.literal("COSMETICS"), b -> minecraft.setScreen(new KushPlaceholderScreen(this, "Cosmetics", "Catálogo in-game em desenvolvimento.")), false));
        addRenderableWidget(new KushButton(centerX + 49, smallY, 76, 28, Component.literal("SOCIAL"), b -> minecraft.setScreen(new KushPlaceholderScreen(this, "Social", "Recursos sociais entram depois do núcleo.")), false));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x88000000);
        int centerX = width / 2;
        int y = Math.max(72, height / 2 - 118);
        graphics.drawCenteredString(font, "KUSHCLIENT", centerX, y, KushUi.WHITE);
        graphics.fill(centerX - 60, y + 17, centerX + 60, y + 19, KushUi.RED);
        graphics.drawCenteredString(font, "RIGHT SHIFT MENU", centerX, y + 28, KushUi.MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
