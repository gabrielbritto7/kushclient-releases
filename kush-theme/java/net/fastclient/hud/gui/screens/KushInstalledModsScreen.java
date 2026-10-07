package net.fastclient.hud.gui.screens;

import java.util.Comparator;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fastclient.hud.gui.DisplaySpace;
import net.fastclient.hud.gui.FastClientFonts;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.gui.KushAssets;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

/** Installed Fabric mods, independent of whether Mod Menu is installed. */
public final class KushInstalledModsScreen extends class_437 {
    private final class_437 parent;
    private final List<ModContainer> mods;
    private double scroll;
    private int panelX, panelY, panelW, panelH;
    public KushInstalledModsScreen(class_437 parent) {
        super(class_2561.method_43470("Mods instalados"));this.parent=parent;
        this.mods=FabricLoader.getInstance().getAllMods().stream()
            .filter(m->m.getContainingMod().isEmpty())
            .sorted(Comparator.comparing(m->m.getMetadata().getName(),String.CASE_INSENSITIVE_ORDER)).toList();
    }
    @Override public void method_25394(class_332 g,int mx,int my,float delta) {
        g.method_71278();DisplaySpace.push(g);int w=DisplaySpace.width(),h=DisplaySpace.height();
        KushAssets.backdrop(g,w,h);g.method_25294(0,0,w,h,0x60000000);
        panelW=Math.min(740,w-40);panelH=Math.max(180,h-140);panelX=(w-panelW)/2;panelY=(h-panelH)/2;
        KushAssets.glass(g,panelX,panelY,panelW,panelH,255);
        draw(g,"Mods instalados",panelX+24,panelY+22,0xFFF5EFF1,1.75f,true);
        draw(g,mods.size()+" mods · Fabric",panelX+24,panelY+52,0xFFAB94A0,1.1f,false);
        draw(g,"×",panelX+panelW-34,panelY+22,0xFFFFAFBD,1.75f,true);
        int top=panelY+88,bottom=panelY+panelH-20;
        DisplaySpace.enableScissor(g,panelX+15,top,panelX+panelW-15,bottom);
        int y=top-(int)scroll;
        for(ModContainer mod:mods) {
            if(y+66>top&&y<bottom) {
                FastClientUI.borderedRoundedRect(g,panelX+20,y,panelW-40,58,7,0x891E161E,0x3066404B);
                draw(g,mod.getMetadata().getName(),panelX+36,y+10,0xFFF2E8ED,1.5f,true);
                draw(g,mod.getMetadata().getId()+" · "+mod.getMetadata().getVersion().getFriendlyString(),panelX+36,y+34,0xFFAF94A0,1.1f,false);
            }
            y+=66;
        }
        DisplaySpace.disableScissor(g);
        int available=bottom-top,total=mods.size()*66;
        if(total>available) {
            int thumb=Math.max(28,available*available/total);
            int sy=top+(int)(scroll/Math.max(1,total-available)*(available-thumb));
            FastClientUI.roundedRect(g,panelX+panelW-10,sy,3,thumb,1,0xCC9C4056);
        }
        DisplaySpace.pop(g);
    }
    private void draw(class_332 g,String text,int x,int y,int color,float scale,boolean strong) {
        g.method_51448().pushMatrix();g.method_51448().translate(x,y);g.method_51448().scale(scale,scale);
        g.method_51439(this.field_22793,strong?FastClientFonts.strong(text):FastClientFonts.body(text),0,0,color,false);g.method_51448().popMatrix();
    }
    @Override public boolean method_25401(double mx,double my,double dx,double dy) {
        scroll=Math.max(0,Math.min(Math.max(0,mods.size()*66-(panelH-108)),scroll-dy*44));return true;
    }
    @Override public boolean method_25402(class_11909 e,boolean doubleClick) {
        int x=DisplaySpace.mouseX(e.comp_4798()),y=DisplaySpace.mouseY(e.comp_4799());
        if(x>=panelX+panelW-52&&x<=panelX+panelW-12&&y>=panelY+12&&y<=panelY+60){method_25419();return true;}
        return super.method_25402(e,doubleClick);
    }
    @Override public void method_25419(){this.field_22787.method_1507(parent);}
    @Override public boolean method_25421(){return false;}
}
