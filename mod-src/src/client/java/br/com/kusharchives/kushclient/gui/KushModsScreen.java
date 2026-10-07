package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.KushClientClient;
import br.com.kusharchives.kushclient.core.module.Category;
import br.com.kusharchives.kushclient.core.module.Module;
import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class KushModsScreen extends Screen {
    private static final int COLS = 4;
    private static final int ROWS = 3;
    private final Screen parent;
    private Category category = Category.HUD;
    private int page;

    public KushModsScreen(Screen parent) { super(Component.literal("Kush Mods")); this.parent = parent; }

    @Override protected void init() {
        int panelW = Math.min(1040, width - 32);
        int left = (width - panelW) / 2;
        int top = Math.max(22, (height - 650) / 2);
        int tabY = top + 58;
        int tabX = left + 18;
        for (Category c : Category.values()) {
            int w = c == Category.MOVEMENT ? 92 : 76;
            addRenderableWidget(new KushButton(tabX, tabY, w, 26, Component.literal(c.getDisplayName().toUpperCase()), b -> {
                category = c; page = 0; rebuildWidgets();
            }, category == c));
            tabX += w + 6;
        }
        addRenderableWidget(new KushButton(left + panelW - 172, tabY, 72, 26, Component.literal("RESET"), b -> resetCategory(), false));
        addRenderableWidget(new KushButton(left + panelW - 92, tabY, 74, 26, Component.literal("FECHAR"), b -> onClose(), true));

        List<Module> modules = KushClientClient.getInstance().getModuleManager().getModules(category);
        int perPage = COLS * ROWS, start = page * perPage, end = Math.min(modules.size(), start + perPage), gap = 10;
        int gridTop = tabY + 42;
        int cardW = (panelW - 36 - gap * (COLS - 1)) / COLS;
        int cardH = 132;
        for (int i = start; i < end; i++) {
            Module module = modules.get(i);
            int local = i - start, col = local % COLS, row = local / COLS;
            int x = left + 18 + col * (cardW + gap), y = gridTop + row * (cardH + gap);
            addRenderableWidget(new KushButton(x + 10, y + cardH - 31, 46, 21, Component.literal("CFG"), b -> minecraft.setScreen(new KushModuleSettingsScreen(this, module)), false));
            KushButton toggle = new KushButton(x + 65, y + cardH - 31, cardW - 75, 21, Component.literal(module.isAvailable() ? (module.isEnabled() ? "ATIVADO" : "DESATIVADO") : "EM BREVE"), b -> {
                if (!module.isAvailable()) return;
                module.toggle();
                b.setMessage(Component.literal(module.isEnabled() ? "ATIVADO" : "DESATIVADO"));
            }, module.isAvailable() && module.isEnabled());
            toggle.active = module.isAvailable();
            addRenderableWidget(toggle);
        }
        int pages = Math.max(1, (modules.size() + perPage - 1) / perPage);
        if (pages > 1) {
            addRenderableWidget(new KushButton(left + panelW/2 - 92, top + 594, 70, 24, Component.literal("<"), b -> { page = Math.max(0, page - 1); rebuildWidgets(); }, false));
            addRenderableWidget(new KushButton(left + panelW/2 + 22, top + 594, 70, 24, Component.literal(">"), b -> { page = Math.min(pages - 1, page + 1); rebuildWidgets(); }, false));
        }
    }

    private void resetCategory() {
        for (Module module : KushClientClient.getInstance().getModuleManager().getModules(category)) if (module.isAvailable()) module.setEnabled(false);
        rebuildWidgets();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KushUi.background(graphics, width, height);
        int panelW = Math.min(1040, width - 32), panelH = Math.min(640, height - 30);
        int left = (width - panelW) / 2, top = (height - panelH) / 2;
        KushUi.panel(graphics, left, top, panelW, panelH);
        graphics.drawString(font, "KUSH MODS", left + 18, top + 20, KushUi.WHITE, false);
        graphics.drawString(font, "Right Shift • HUD / Render / Movement / Player / Utility", left + 18, top + 36, KushUi.MUTED, false);
        List<Module> modules = KushClientClient.getInstance().getModuleManager().getModules(category);
        int perPage = COLS * ROWS, start = page * perPage, end = Math.min(modules.size(), start + perPage), gap = 10;
        int gridTop = top + 100, cardW = (panelW - 36 - gap * (COLS - 1)) / COLS, cardH = 132;
        for (int i = start; i < end; i++) {
            Module module = modules.get(i);
            int local = i - start, col = local % COLS, row = local / COLS;
            int x = left + 18 + col * (cardW + gap), y = gridTop + row * (cardH + gap);
            KushUi.card(graphics, x, y, cardW, cardH, module.isAvailable() && module.isEnabled());
            graphics.drawString(font, module.getName(), x + 12, y + 12, KushUi.WHITE, false);
            graphics.drawString(font, module.isAvailable() ? "KUSH MODULE" : "PLANEJADO", x + 12, y + 31, module.isAvailable() ? KushUi.RED_HOVER : 0xFFFFA24A, false);
            String desc = module.getDescription();
            if (font.width(desc) > cardW - 24) {
                while (desc.length() > 3 && font.width(desc + "...") > cardW - 24) desc = desc.substring(0, desc.length()-1);
                desc += "...";
            }
            graphics.drawString(font, desc, x + 12, y + 53, KushUi.MUTED, false);
        }
        int pages = Math.max(1, (modules.size() + perPage - 1) / perPage);
        graphics.drawCenteredString(font, category.getDisplayName().toUpperCase() + "  •  " + (page+1) + "/" + pages, width/2, top + panelH - 28, KushUi.MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
