package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.KushClientClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KushMenuScreen extends Screen {
    private final Screen parent;
    private Button fpsButton;
    private Button coordsButton;

    public KushMenuScreen(Screen parent) {
        super(Component.literal("KushClient"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        int top = Math.max(28, (height - 270) / 2);

        fpsButton = addRenderableWidget(Button.builder(fpsLabel(), button -> {
            KushClientClient.toggleFps();
            button.setMessage(fpsLabel());
        }).bounds(left + panelWidth - 130, top + 92, 108, 20).build());

        coordsButton = addRenderableWidget(Button.builder(coordsLabel(), button -> {
            KushClientClient.toggleCoordinates();
            button.setMessage(coordsLabel());
        }).bounds(left + panelWidth - 130, top + 140, 108, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Fechar"), button -> onClose())
                .bounds(left + panelWidth - 130, top + 222, 108, 20).build());
    }

    private static Component fpsLabel() {
        return Component.literal(KushClientClient.isFpsEnabled() ? "ATIVADO" : "DESATIVADO");
    }

    private static Component coordsLabel() {
        return Component.literal(KushClientClient.isCoordinatesEnabled() ? "ATIVADO" : "DESATIVADO");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(520, width - 40);
        int panelHeight = 270;
        int left = (width - panelWidth) / 2;
        int top = Math.max(28, (height - panelHeight) / 2);
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, 0xF20A0A0C);
        graphics.fill(left, top, right, top + 3, 0xFFFF3B30);
        graphics.drawString(font, "KUSHCLIENT", left + 20, top + 18, 0xFFFFFFFF, false);
        graphics.drawString(font, "Minecraft 1.21.1 • Fabric • v0.1.0", left + 20, top + 36, 0xFF8F8F96, false);
        graphics.drawString(font, "RIGHT SHIFT  •  MOD MENU", left + 20, top + 56, 0xFFFF5A50, false);

        drawModule(graphics, left, right, top + 92, "FPS", "Mostra a taxa de quadros atual.");
        drawModule(graphics, left, right, top + 140, "Coordenadas", "Mostra X, Y e Z do jogador.");

        graphics.drawString(font, "Primeira build funcional do KushClient Mod.", left + 20, bottom - 26, 0xFF8F8F96, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawModule(GuiGraphics graphics, int left, int right, int y, String name, String description) {
        graphics.fill(left + 18, y - 10, right - 18, y + 28, 0xFF131316);
        graphics.drawString(font, name, left + 32, y - 1, 0xFFFFFFFF, false);
        graphics.drawString(font, description, left + 32, y + 13, 0xFF8F8F96, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
