package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

public final class KushInstalledModsScreen extends Screen {
    private final Screen parent;
    private List<ModContainer> mods;

    public KushInstalledModsScreen(Screen parent) {
        super(Component.literal("Mods"));
        this.parent = parent;
    }

    @Override protected void init() {
        mods = FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(m -> m.getMetadata().getName().toLowerCase()))
                .toList();
        addRenderableWidget(new KushButton(width/2-80, height-44, 160, 26, Component.literal("CONCLUÍDO"), b -> onClose(), true));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KushUi.background(graphics, width, height);
        int panelW = Math.min(780, width - 48);
        int left = (width-panelW)/2;
        int top = 30;
        int bottom = height - 58;
        KushUi.panel(graphics, left, top, panelW, bottom-top);
        graphics.drawString(font, "MODS INSTALADOS", left+20, top+18, KushUi.WHITE, false);
        graphics.drawString(font, mods.size()+" mods detectados pelo Fabric Loader", left+20, top+36, KushUi.MUTED, false);
        int y = top+62;
        int max = Math.max(0, (bottom-y-12)/28);
        for (int i=0; i<mods.size() && i<max; i++) {
            ModContainer mod = mods.get(i);
            String name = mod.getMetadata().getName();
            String version = mod.getMetadata().getVersion().getFriendlyString();
            graphics.fill(left+18, y, left+panelW-18, y+24, i%2==0 ? 0xA8171214 : 0xA8110D0F);
            graphics.drawString(font, name, left+30, y+7, KushUi.WHITE, false);
            int vw = font.width(version);
            graphics.drawString(font, version, left+panelW-30-vw, y+7, KushUi.MUTED, false);
            y += 28;
        }
        if (mods.size() > max) {
            graphics.drawString(font, "+ "+(mods.size()-max)+" outros mods", left+20, bottom-22, KushUi.MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
