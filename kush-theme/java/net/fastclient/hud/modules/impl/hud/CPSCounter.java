package net.fastclient.hud.modules.impl.hud;

import net.fastclient.hud.gui.KushModernKeys;
import net.fastclient.hud.modules.Module;
import net.fastclient.hud.modules.Category;
import net.minecraft.class_332;
import org.lwjgl.glfw.GLFW;

/** Modern Keystrokes CPS boxes and mouse event counter, integrated with Kush drag/scale. */
public class CPSCounter extends Module {
    public CPSCounter(){super("CPSCounter","Clicks per second counter",Category.HUD);}
    @Override public void onRender(class_332 g,float delta){
        if(!isInGame())return;
        g.method_51448().pushMatrix();g.method_51448().translate(getHudX(),getHudY());g.method_51448().scale(getHudScale(),getHudScale());
        long handle=mc.method_22683().method_4490();
        KushModernKeys.box(g,0,0,45,24,"LMB",KushModernKeys.cps(0)+" CPS",GLFW.glfwGetMouseButton(handle,0)==1,0xB5273C,0x191317);
        KushModernKeys.box(g,48,0,45,24,"RMB",KushModernKeys.cps(1)+" CPS",GLFW.glfwGetMouseButton(handle,1)==1,0xB5273C,0x191317);
        g.method_51448().popMatrix();
    }
    @Override public int getHudWidth(){return 93;}
    @Override public int getHudHeight(){return 24;}
}
