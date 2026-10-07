/* UI revision of the user-provided FastClient HUD base; original notices retained. */
package net.fastclient.hud.gui.components;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.hud.gui.DisplaySpace;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.gui.KushAssets;
import net.fastclient.hud.render.AnimationUtils;
import net.fastclient.hud.render.Theme;
import net.minecraft.class_11909;
import net.minecraft.class_310;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class Dropdown extends UIComponent {
    private final String label;
    private final String[] options;
    private int selectedIndex;
    private final Consumer<String> onChange;
    private boolean expanded;
    private float hoverProgress, expandProgress;
    private long lastUpdate=System.currentTimeMillis();

    public Dropdown(int x,int y,int width,int height,String label,String[] options,String selected,Consumer<String> onChange) {
        super(x,y,width,height);this.label=label;this.options=options;this.onChange=onChange;
        for(int i=0;i<options.length;i++)if(options[i].equals(selected)){selectedIndex=i;break;}
    }
    private int dropWidth(class_310 mc) {
        int widest=0;
        for(String option:options)widest=Math.max(widest,this.uiTextWidth(mc,option));
        return Math.min(Math.max(80,this.width/2),Math.max(136,widest+38));
    }
    private int optionsY() {
        int h=options.length*this.height;
        return this.y+this.height+2+h>DisplaySpace.height()-10?Math.max(10,this.y-h-2):this.y+this.height+2;
    }
    private String fit(class_310 mc,String text,int available) {
        if(this.uiTextWidth(mc,text)<=available)return text;
        while(text.length()>1 && this.uiTextWidth(mc,text+"…")>available)text=text.substring(0,text.length()-1);
        return text+"…";
    }
    @Override public void render(class_332 g,int mouseX,int mouseY,float delta) {
        this.hovered=this.isHovered(mouseX,mouseY);
        long now=System.currentTimeMillis();float dt=(now-lastUpdate)/1000f;lastUpdate=now;
        hoverProgress=AnimationUtils.smoothDelta(hoverProgress,this.hovered?1:0,.4f,dt*60);
        expandProgress=AnimationUtils.smoothDelta(expandProgress,this.expanded?1:0,.35f,dt*60);
        class_310 mc=class_310.method_1551();
        int dw=dropWidth(mc),dx=this.x+this.width-dw-12,dy=this.y+3,dh=this.height-6;
        String display=fit(mc,Theme.formatSettingName(label),Math.max(20,dx-this.x-24));
        this.drawUiText(g,mc,display,this.x+12,this.centeredTextY(mc,this.y,this.height),FastClientUI.blend(0xFFB5AEB5,0xFFF4EFF2,hoverProgress));
        int border=this.expanded?0xFFDE3542:0xFF363038;
        KushAssets.softRect(g,dx,dy,dw,dh,3,border);
        KushAssets.softRect(g,dx+1,dy+1,dw-2,dh-2,2,this.hovered?0xFF282228:0xFF1D191F);
        this.drawUiText(g,mc,fit(mc,options[selectedIndex],dw-36),dx+10,this.centeredTextY(mc,dy,dh),0xFFF4EFF2);
        KushAssets.symbol(g,this.expanded?"chevron-up":"chevron-down",dx+dw-22,dy+(dh-12)/2,12,0xFFAAA2AC);
        if(expandProgress>.01f) {
            int oy=optionsY(),full=options.length*this.height,total=Math.max(1,Math.round(full*expandProgress));
            KushAssets.softRect(g,dx,oy,dw,total,3,0xFF363038);
            KushAssets.softRect(g,dx+1,oy+1,dw-2,Math.max(1,total-2),2,0xFA18151B);
            for(int i=0;i<options.length;i++) {
                int py=oy+i*this.height;
                if(py+this.height>oy+total)continue;
                boolean over=mouseX>=dx && mouseX<dx+dw && mouseY>=py && mouseY<py+this.height;
                if(i==selectedIndex || over)g.method_25294(dx+1,py+1,dx+dw-1,py+this.height-1,i==selectedIndex?0xFF382028:0xFF2A232B);
                if(i==selectedIndex)g.method_25294(dx+1,py+5,dx+3,py+this.height-5,0xFFE53542);
                this.drawUiText(g,mc,fit(mc,options[i],dw-20),dx+10,this.centeredTextY(mc,py,this.height),i==selectedIndex?0xFFFF6871:0xFFEDE6EB);
            }
        }
    }
    @Override public boolean mouseClicked(class_11909 event,boolean bl) {
        if(event.method_74245()!=0)return false;
        class_310 mc=class_310.method_1551();int dw=dropWidth(mc),dx=this.x+this.width-dw-12;
        double mx=event.comp_4798(),my=event.comp_4799();
        if(expanded && expandProgress>.9f) {
            int oy=optionsY();
            if(mx>=dx && mx<dx+dw && my>=oy && my<oy+options.length*this.height) {
                selectedIndex=(int)(my-oy)/this.height;expanded=false;
                if(onChange!=null)onChange.accept(options[selectedIndex]);return true;
            }
        }
        if(mx>=dx && mx<dx+dw && my>=this.y && my<this.y+this.height){expanded=!expanded;return true;}
        if(expanded){expanded=false;return true;}
        return false;
    }
    public String getSelected(){return options[selectedIndex];}
    public boolean isExpanded(){return expanded;}
    @Override public int getHeight(){return expanded && expandProgress>.5f?this.height+options.length*this.height+4:this.height;}
}
