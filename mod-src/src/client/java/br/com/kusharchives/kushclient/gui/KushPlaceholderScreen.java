package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KushPlaceholderScreen extends Screen {
    private final Screen parent;
    private final String heading;
    private final String detail;

    public KushPlaceholderScreen(Screen parent, String heading, String detail) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.detail = detail;
    }

    @Override protected void init() {
        addRenderableWidget(new KushButton(width/2-90, height/2+42, 180, 28, Component.literal("VOLTAR"), b -> onClose(), true));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KushUi.background(graphics, width, height);
        int w = Math.min(520, width - 40);
        int x = (width - w)/2;
        int y = height/2 - 90;
        KushUi.panel(graphics, x, y, w, 180);
        graphics.drawCenteredString(font, heading.toUpperCase(), width/2, y+35, KushUi.WHITE);
        graphics.drawCenteredString(font, detail, width/2, y+65, KushUi.MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
