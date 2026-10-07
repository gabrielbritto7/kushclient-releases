package br.com.kusharchives.kushclient.gui;

import br.com.kusharchives.kushclient.KushClientClient;
import br.com.kusharchives.kushclient.gui.widget.KushButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public final class KushTitleScreen extends Screen {
    public KushTitleScreen() { super(Component.literal("KushClient")); }

    @Override protected void init() {
        int menuW = Math.min(300, width - 50);
        int x = (width - menuW) / 2;
        int y = Math.max(150, height / 2 - 54);
        int h = 30;
        int gap = 6;
        addRenderableWidget(new KushButton(x, y, menuW, h, Component.literal("UM JOGADOR"), b -> minecraft.setScreen(new SelectWorldScreen(this)), false));
        addRenderableWidget(new KushButton(x, y += h + gap, menuW, h, Component.literal("MULTIJOGADOR"), b -> minecraft.setScreen(new JoinMultiplayerScreen(this)), false));
        addRenderableWidget(new KushButton(x, y += h + gap, menuW, h, Component.literal("COSMETICS"), b -> minecraft.setScreen(new KushPlaceholderScreen(this, "Cosmetics", "Catálogo in-game entra nas próximas builds.")), false));
        addRenderableWidget(new KushButton(x, y += h + gap, menuW, h, Component.literal("MODS"), b -> minecraft.setScreen(new KushInstalledModsScreen(this)), false));
        addRenderableWidget(new KushButton(x + 35, y += h + 18, menuW - 70, h, Component.literal("SAIR DO JOGO"), b -> minecraft.stop(), true));
        int toolY = 18, toolW = 54, toolH = 28, toolGap = 6, right = width - 18;
        addRenderableWidget(new KushButton(right - toolW, toolY, toolW, toolH, Component.literal("MC"), b -> useMinecraftUi(), true));
        right -= toolW + toolGap;
        addRenderableWidget(new KushButton(right - 68, toolY, 68, toolH, Component.literal("PACKS"), b -> openPacks(), false));
        right -= 68 + toolGap;
        addRenderableWidget(new KushButton(right - 70, toolY, 70, toolH, Component.literal("AJUSTES"), b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)), false));
        right -= 70 + toolGap;
        addRenderableWidget(new KushButton(right - 82, toolY, 82, toolH, Component.literal("KUSH MODS"), b -> minecraft.setScreen(new KushModsScreen(this)), false));
    }

    private void useMinecraftUi() {
        KushClientClient.getInstance().getConfigManager().setUseKushUi(false);
        KushClientClient.getInstance().getConfigManager().save();
        minecraft.setScreen(new TitleScreen());
    }

    private void openPacks() {
        minecraft.setScreen(new PackSelectionScreen(minecraft.getResourcePackRepository(), repository -> {
            minecraft.options.updateResourcePacks(repository);
            minecraft.setScreen(this);
        }, minecraft.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KushUi.background(graphics, width, height);
        int logoY = Math.max(72, height / 2 - 150);
        graphics.drawCenteredString(font, "K U S H C L I E N T", width / 2, logoY, KushUi.WHITE);
        graphics.fill(width / 2 - 90, logoY + 18, width / 2 + 90, logoY + 20, KushUi.RED);
        graphics.drawCenteredString(font, "MINECRAFT • FABRIC • KUSH MOD v0.2.0", width / 2, logoY + 30, KushUi.MUTED);
        String user = minecraft != null && minecraft.getUser() != null ? minecraft.getUser().getName() : "Player";
        graphics.drawString(font, user, 18, 27, KushUi.WHITE, false);
        graphics.drawString(font, "KUSH UI", 18, 43, KushUi.RED_HOVER, false);
        graphics.drawString(font, "KushClient Mod 0.2.0 • Minecraft 1.21.1", 18, height - 20, KushUi.MUTED, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
