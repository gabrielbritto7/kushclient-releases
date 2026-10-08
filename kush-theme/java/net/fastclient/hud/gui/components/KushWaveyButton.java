package net.fastclient.hud.gui.components;

import net.fastclient.hud.gui.KushLanguage;
import net.fastclient.hud.gui.KushAssets;
import net.fastclient.client.render.KushWaveyBridge;
import net.minecraft.*;

public final class KushWaveyButton extends UIComponent {
    private final class_437 parent;
    public KushWaveyButton(int x,int y,int w,class_437 parent){super(x,y,w,32);this.parent=parent;}
    @Override public void render(class_332 g,int mx,int my,float delta){
        boolean hover=isHovered(mx,my);
        KushAssets.softRect(g,x,y,width,height,3,hover?0xFF63313E:0xFF39262F);
        class_310 mc=class_310.method_1551();
        drawUiText(g,mc,"Wavey Capes · "+KushLanguage.translate("Open official settings"),x+12,centeredTextY(mc,y,height),-1);
    }
    @Override public boolean mouseClicked(class_11909 event,boolean bl){
        return event.method_74245()==0 && super.mouseClicked(event,bl) && KushWaveyBridge.openSettings(parent);
    }
}
