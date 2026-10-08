package net.fastclient.hud.modules.impl.hud;

import net.fastclient.hud.gui.KushModernKeys;
import net.fastclient.hud.modules.Module;
import net.fastclient.hud.modules.Category;
import net.fastclient.hud.modules.settings.*;
import net.minecraft.class_332;
import org.lwjgl.glfw.GLFW;

/** Integrated Modern Keystrokes renderer. See licenses/Modern-Keystrokes-MIT.txt. */
public class Keystrokes extends Module {
    private final NumberSetting keySize=register(new NumberSetting("key_size","Size of each key",22,16,40,2));
    private final NumberSetting spacing=register(new NumberSetting("spacing","Gap between keys",2,0,10,1));
    private final BooleanSetting showMouse=register(new BooleanSetting("show_mouse","Show mouse buttons",true));
    private final BooleanSetting showSpace=register(new BooleanSetting("show_space","Show spacebar",true));
    private final BooleanSetting showCps=register(new BooleanSetting("show_cps","Show CPS inside mouse buttons",true));
    private final BooleanSetting rgbBorder=register(new BooleanSetting("rgb_border","Animated RGB border",false));
    private final NumberSetting rgbPeriod=register(new NumberSetting("rgb_period","Seconds per RGB cycle",6,1,15,1));
    private final ColorSetting pressedColor=register(new ColorSetting("pressed_color","Pressed key color",181,39,60));
    private final ColorSetting normalColor=register(new ColorSetting("normal_color","Normal key color",25,19,23));
    public Keystrokes(){super("Keystrokes","Shows pressed keys visually",Category.HUD);showCps.visibleWhen(showMouse::isEnabled);rgbPeriod.visibleWhen(rgbBorder::isEnabled);}
    private void box(class_332 g,int x,int y,int w,int h,String label,String count,boolean pressed){KushModernKeys.box(g,x,y,w,h,label,count,pressed,pressedColor.getRGB(),normalColor.getRGB(),rgbBorder.isEnabled(),rgbPeriod.getFloatValue());}
    @Override public void onRender(class_332 g,float delta) {
        if(!isInGame())return;
        g.method_51448().pushMatrix();g.method_51448().translate(getHudX(),getHudY());g.method_51448().scale(getHudScale(),getHudScale());
        int size=keySize.getIntValue(),gap=spacing.getIntValue(),step=size+gap,total=getHudWidth();
        box(g,step,0,size,size,"W",null,mc.field_1690.field_1894.method_1434());
        box(g,0,step,size,size,"A",null,mc.field_1690.field_1913.method_1434());
        box(g,step,step,size,size,"S",null,mc.field_1690.field_1881.method_1434());
        box(g,2*step,step,size,size,"D",null,mc.field_1690.field_1849.method_1434());
        int y=step+size;
        if(showMouse.isEnabled()) {
            y+=gap;int half=(total-gap)/2,height=size+(showCps.isEnabled()?7:0);long handle=mc.method_22683().method_4490();
            box(g,0,y,half,height,"LMB",showCps.isEnabled()?KushModernKeys.cps(0)+" CPS":null,GLFW.glfwGetMouseButton(handle,0)==1);
            box(g,half+gap,y,total-half-gap,height,"RMB",showCps.isEnabled()?KushModernKeys.cps(1)+" CPS":null,GLFW.glfwGetMouseButton(handle,1)==1);y+=height;
        }
        if(showSpace.isEnabled())box(g,0,y+gap,total,Math.max(8,size/2),"—",null,mc.field_1690.field_1903.method_1434());
        g.method_51448().popMatrix();
    }
    @Override public int getHudWidth(){return keySize.getIntValue()*3+spacing.getIntValue()*2;}
    @Override public int getHudHeight(){int size=keySize.getIntValue(),gap=spacing.getIntValue(),h=2*size+gap;if(showMouse.isEnabled())h+=gap+size+(showCps.isEnabled()?7:0);if(showSpace.isEnabled())h+=gap+Math.max(8,size/2);return h;}
}
