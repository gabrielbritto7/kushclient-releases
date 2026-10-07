package br.com.kusharchives.kushclient.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class KushButton extends Button {
    private final boolean accent;

    public KushButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean accent) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHoveredOrFocused();
        int border = accent ? (hover ? 0xFFFF5961 : 0xFFD5212C) : (hover ? 0xFF6D3035 : 0xFF3A272A);
        int fill = accent ? (hover ? 0xFFE02A34 : 0xFFC51B25) : (hover ? 0xFF25191B : 0xE5151113);
        if (!active) {
            border = 0xFF303030;
            fill = 0xFF151515;
        }
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
        int color = active ? 0xFFF5F5F5 : 0xFF777777;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2,
                getY() + (height - 8) / 2, color);
    }
}
