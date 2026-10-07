package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.core.module.Module;
import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class KushModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final Module module;

    public KushModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName()));
        this.parent = parent;
        this.module = module;
    }

    @Override protected void init() {
        int w = Math.min(520, width - 40);
        int x = (width - w)/2;
        int y = height/2 + 70;
        if (module.isAvailable()) {
            addRenderableWidget(new KushButton(x+30, y, 180, 28,
                    Component.literal(module.isEnabled() ? "DESATIVAR" : "ATIVAR"), b -> {
                module.toggle();
                b.setMessage(Component.literal(module.isEnabled() ? "DESATIVAR" : "ATIVAR"));
            }, true));
        }
        addRenderableWidget(new KushButton(x+w-210, y, 180, 28, Component.literal("VOLTAR"), b -> onClose(), false));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KushUi.background(graphics, width, height);
        int w = Math.min(520, width - 40);
        int h = 230;
        int x = (width-w)/2;
        int y = (height-h)/2;
        KushUi.panel(graphics, x, y, w, h);
        graphics.drawString(font, module.getName().toUpperCase(), x+24, y+26, KushUi.WHITE, false);
        graphics.drawString(font, module.getCategory().getDisplayName(), x+24, y+44, KushUi.RED_HOVER, false);
        graphics.drawString(font, module.getDescription(), x+24, y+74, KushUi.MUTED, false);
        String status = module.isAvailable() ? (module.isEnabled() ? "ATIVADO" : "DESATIVADO") : "EM BREVE";
        int statusColor = module.isAvailable() ? (module.isEnabled() ? 0xFF6DEB93 : KushUi.MUTED) : 0xFFFFA24A;
        graphics.drawString(font, "Status: " + status, x+24, y+104, statusColor, false);
        graphics.drawString(font, "Configurações detalhadas entram conforme cada módulo for implementado.", x+24, y+132, KushUi.MUTED, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
