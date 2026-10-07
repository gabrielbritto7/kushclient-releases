package br.com.kusharchives.kushclient.modules.hud;

import br.com.kusharchives.kushclient.core.module.Category;
import br.com.kusharchives.kushclient.core.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class FpsModule extends Module {
    public FpsModule() { super("fps", "FPS", "Mostra a taxa de quadros atual.", Category.HUD); }
    @Override public void onHudRender(Minecraft client, GuiGraphics graphics) {
        if (client.options.hideGui || client.player == null) return;
        String text = "FPS  " + client.getFps();
        int x=8,y=8,width=client.font.width(text)+12;
        graphics.fill(x,y,x+width,y+18,0xB009090B);
        graphics.fill(x,y,x+2,y+18,0xFFFF3B30);
        graphics.drawString(client.font,text,x+7,y+5,0xFFFFFFFF,false);
    }
}
