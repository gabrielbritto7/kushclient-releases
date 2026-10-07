package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

public final class KushPauseScreen extends Screen {
    public KushPauseScreen() { super(Component.literal("KushClient Pause")); }

    @Override protected void init() {
        int w = Math.min(300, width - 50);
        int x = (width - w)/2;
        int y = Math.max(105, height/2 - 108);
        int h = 30, gap = 6;
        addRenderableWidget(new KushButton(x, y, w, h, Component.literal("VOLTAR AO JOGO"), b -> minecraft.setScreen(null), true));
        addRenderableWidget(new KushButton(x, y += h+gap, w, h, Component.literal("KUSH SETTINGS"), b -> minecraft.setScreen(new KushModsScreen(this)), false));
        addRenderableWidget(new KushButton(x, y += h+gap, w, h, Component.literal("OPÇÕES"), b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)), false));
        addRenderableWidget(new KushButton(x, y += h+gap, w, h, Component.literal("MODS"), b -> minecraft.setScreen(new KushInstalledModsScreen(this)), false));
        addRenderableWidget(new KushButton(x, y += h+gap, w, h, Component.literal("DESCONECTAR"), b -> minecraft.disconnect(new TitleScreen()), false));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA5000000);
        int y = Math.max(45, height/2 - 170);
        graphics.drawCenteredString(font, "KUSHCLIENT", width/2, y, KushUi.WHITE);
        graphics.fill(width/2-70, y+18, width/2+70, y+20, KushUi.RED);
        graphics.drawCenteredString(font, "PAUSADO", width/2, y+30, KushUi.MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return true; }
}
