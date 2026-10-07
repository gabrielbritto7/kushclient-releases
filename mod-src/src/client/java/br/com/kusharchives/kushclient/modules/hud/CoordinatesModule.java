package br.com.kusharchives.kushclient.modules.hud;

import br.com.kusharchives.kushclient.core.module.Category;
import br.com.kusharchives.kushclient.core.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class CoordinatesModule extends Module {
    public CoordinatesModule() { super("coordinates", "Coordenadas", "Mostra X, Y e Z do jogador.", Category.HUD); }
    @Override public void onHudRender(Minecraft client, GuiGraphics graphics) {
        if (client.options.hideGui || client.player == null) return;
        String text = String.format("XYZ  %.1f  %.1f  %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
        int x=8,y=30,width=client.font.width(text)+12;
        graphics.fill(x,y,x+width,y+18,0xB009090B);
        graphics.fill(x,y,x+2,y+18,0xFFFF3B30);
        graphics.drawString(client.font,text,x+7,y+5,0xFFFFFFFF,false);
    }
}
