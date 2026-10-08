package net.fastclient.hud.gui;

import java.util.ArrayDeque;
import net.minecraft.class_332;
import net.minecraft.class_310;

/** Adapted from Modern Keystrokes (MIT), arsrillon/modern-keystrokes.
 * Copyright (c) 2023 pseudonym-2669, (c) 2026 Marblock.
 * Kush adapter: main-thread input events, monotonic time, no polling thread.
 */
public final class KushModernKeys {
    @SuppressWarnings("unchecked") private static final ArrayDeque<Long>[] CLICKS=new ArrayDeque[]{new ArrayDeque<>(),new ArrayDeque<>()};
    private KushModernKeys() {}
    public static void press(int button) {
        if(button<0 || button>1)return;
        long now=System.nanoTime();expire(button,now);
        if(CLICKS[button].size()<512)CLICKS[button].addLast(now);
    }
    private static void expire(int button,long now){while(!CLICKS[button].isEmpty() && now-CLICKS[button].getFirst()>=1_000_000_000L)CLICKS[button].pollFirst();}
    public static int cps(int button){expire(button,System.nanoTime());return CLICKS[button].size();}
    public static void clear(){CLICKS[0].clear();CLICKS[1].clear();}
    /** Modern Keystrokes' centered key/CPS boxes, mapped to the Kush HUD settings. */
    public static void box(class_332 g,int x,int y,int w,int h,String label,String count,boolean pressed,int pressedColor,int normalColor) {
        int bg=pressed?0xFF000000|pressedColor:0xB0000000|normalColor;
        int edge=pressed?0xFFF4D8DF:0xFF7B3648;
        g.method_25294(x,y,x+w,y+h,bg);
        g.method_25294(x,y,x+w,y+1,edge);g.method_25294(x,y+h-1,x+w,y+h,edge);
        g.method_25294(x,y+1,x+1,y+h-1,edge);g.method_25294(x+w-1,y+1,x+w,y+h-1,edge);
        var font=class_310.method_1551().field_1772;
        float scale=count==null?1f:.75f;
        float textY=count==null?(h-8)/2f:4f;
        g.method_51448().pushMatrix();g.method_51448().translate(x+(w-font.method_1727(label)*scale)/2f,y+textY);g.method_51448().scale(scale,scale);
        g.method_51433(font,label,0,0,-1,false);g.method_51448().popMatrix();
        if(count!=null){float cs=.7f;g.method_51448().pushMatrix();g.method_51448().translate(x+(w-font.method_1727(count)*cs)/2f,y+h-10);g.method_51448().scale(cs,cs);g.method_51433(font,count,0,0,0xFFD9C8CF,false);g.method_51448().popMatrix();}
    }
}
