package br.com.kusharchives.kushclient.gui;

import net.minecraft.client.gui.GuiGraphics;

public final class KushUi {
    public static final int RED = 0xFFD5212C;
    public static final int RED_HOVER = 0xFFFF4B55;
    public static final int PANEL = 0xE80C090B;
    public static final int CARD = 0xE8171214;
    public static final int BORDER = 0xFF3A272A;
    public static final int MUTED = 0xFF9B8F92;
    public static final int WHITE = 0xFFF7F2F3;

    private KushUi() {}

    public static void background(GuiGraphics graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, 0xFF080608, 0xFF17070A);
        graphics.fill(0, 0, width, height, 0x72000000);
        int band = Math.max(100, width / 8);
        graphics.fill(width / 2 - band, 0, width / 2 - band + 3, height, 0x30D5212C);
        graphics.fill(width / 2 + band, 0, width / 2 + band + 2, height, 0x20D5212C);
        graphics.fill(0, height - 90, width, height, 0x45000000);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
    }

    public static void card(GuiGraphics graphics, int x, int y, int w, int h, boolean selected) {
        graphics.fill(x, y, x + w, y + h, selected ? RED : BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, CARD);
        if (selected) graphics.fill(x, y, x + w, y + 2, RED_HOVER);
    }
}
