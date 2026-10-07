package net.fastclient.hud.launcher;

import java.util.LinkedHashMap;
import java.util.Map;
import net.fastclient.hud.gui.FastClientFonts;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.gui.KushAssets;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_5348;

/** Native title/pause chrome; signatures retain base compatibility. */
public final class LauncherRenderer {
    private static final Map<String, int[]> REGIONS = new LinkedHashMap<>();
    private static final Map<String, Float> HOVER = new LinkedHashMap<>();
    private static final int TEXT = 0xFFF5EFF1;
    private static final String CREDIT = "base do FastClient feito por fã";
    private static float uiColor = 1;
    private static long lastToggleTime = System.nanoTime();
    private LauncherRenderer() {}
    public static String getClickedButton(int mx, int my) {
        for (var entry : REGIONS.entrySet()) {
            int[] r = entry.getValue();
            if (hit(r[0], r[1], r[2], r[3], mx, my)) return entry.getKey();
        }
        return null;
    }
    public static boolean isSkinToggleClicked(int w, int h, int mx, int my) {
        int[] r = toggleBounds(w); return hit(r[0], r[1], r[2], r[3], mx, my);
    }
    public static boolean isDiscordClicked(int w, int h, int mx, int my) { return false; }
    public static void render(class_332 g, class_327 font, int w, int h, int mx, int my) {
        REGIONS.clear(); g.method_25294(0, 0, w, h, 0xA509070A);
        int cx=w/2, bh=Math.max(34,Math.min(44,h/17)), gap=8, bw=Math.min(310,w-40);
        int start=Math.max(166,h/2-54),logoW=Math.min(286,Math.round(h*0.40f)),logoH=Math.round(logoW*1080f/1920f);
        KushAssets.image(g,KushAssets.SKATE,cx-logoW/2,start-logoH-22,logoW,logoH,1920,1080,-1);
        button(g,font,"singleplayer","Um jogador",cx-bw/2,start,bw,bh,mx,my,false);
        button(g,font,"multiplayer","Multijogador",cx-bw/2,start+(bh+gap),bw,bh,mx,my,false);
        button(g,font,"skins","Cosméticos",cx-bw/2,start+(bh+gap)*2,bw,bh,mx,my,false);
        button(g,font,"modmenu","Mods",cx-bw/2,start+(bh+gap)*3,bw,bh,mx,my,false);
        button(g,font,"quit","Sair do jogo",cx-114,start+(bh+gap)*4+12,228,bh,mx,my,true);
        topTools(g,font,w,h,mx,my);credit(g,font,w,h);
    }
    private static void topTools(class_332 g,class_327 font,int w,int h,int mx,int my) {
        renderSkinToggle(g,font,w,h,mx,my);int x=toggleBounds(w)[0]-10-48;
        icon(g,font,"box","\ue2c8","Resource packs",null,x,18,48,mx,my);x-=58;
        icon(g,font,"settings","\ue8b8","Configurações do Minecraft",null,x,18,48,mx,my);x-=58;
        icon(g,font,"window",null,"Kush Mods",KushAssets.RED_K,x,18,48,mx,my);
    }
    public static void renderSkinToggle(class_332 g,class_327 font,int w,int h,int mx,int my) {
        int[] r=toggleBounds(w);int x=r[0],y=r[1];surface(g,"toggle",x,y,r[2],r[3],hit(x,y,r[2],r[3],mx,my),false);
        long now=System.nanoTime();float dt=Math.min(0.1f,(now-lastToggleTime)/1_000_000_000f);lastToggleTime=now;
        float target=LauncherSkinPreference.isFastClientSkinEnabled()?1:0;
        uiColor+=(target-uiColor)*(1-(float)Math.exp(-dt/0.075f));
        int iw=50,ih=28,ix=x+(r[2]-iw)/2,iy=y+(r[3]-ih)/2;
        KushAssets.image(g,KushAssets.grayWordmark(),ix,iy,iw,ih,1672,941,-1);
        KushAssets.image(g,KushAssets.WORDMARK,ix,iy,iw,ih,1672,941,FastClientUI.withAlpha(-1,Math.round(uiColor*255)));
        if(hit(x,y,r[2],r[3],mx,my))tooltip(g,font,target>0?"Usar interface do Minecraft":"Usar interface Kush",x+r[2]/2,y+r[3]+8);
    }
    public static void renderVanillaOverlay(class_332 g,class_327 font,int w,int h,int mx,int my) {
        REGIONS.clear();renderSkinToggle(g,font,w,h,mx,my);credit(g,font,w,h);
    }
    private static int[] toggleBounds(int w) { return new int[]{w-84,18,66,48}; }
    public static void renderPause(class_332 g,class_327 font,int w,int h,int mx,int my) {
        REGIONS.clear();g.method_25294(0,0,w,h,0x7D0B080C);
        int cx=w/2,bh=Math.max(32,Math.min(40,h/19)),gap=6,start=Math.max(148,h/2-130),bw=Math.min(310,w-40);
        int lw=Math.min(220,h/3),lh=Math.round(lw*1080f/1920f);
        KushAssets.image(g,KushAssets.SKATE,cx-lw/2,start-lh-16,lw,lh,1920,1080,-1);
        String[][] rows={{"pause_backtogame","Voltar ao jogo"},{"pause_fastclient_settings","Kush Settings"},
            {"pause_options","Opções"},{"pause_open_to_lan","Abrir para LAN"},{"pause_modmenu","Mods"},{"pause_disconnect","Desconectar"}};
        int y=start;
        for(var row:rows){
            if(row[0].equals("pause_open_to_lan")&&class_310.method_1551().method_1576()==null)continue;
            button(g,font,row[0],row[1],cx-bw/2,y,bw,bh,mx,my,row[0].equals("pause_fastclient_settings"));y+=bh+gap;
        }
        String[][] bottom={{"pause_advancements","Avanços"},{"pause_statistics","Estatísticas"},{"pause_minecraftfolder","Pasta do jogo"}};
        int smallW=124,bx=cx-(smallW*3+16)/2;
        for(var b:bottom){button(g,font,b[0],b[1],bx,h-60,smallW,32,mx,my,false);bx+=smallW+8;}
        credit(g,font,w,h);
    }
    private static void button(class_332 g,class_327 font,String id,String label,int x,int y,int w,int h,int mx,int my,boolean accent) {
        REGIONS.put(id,new int[]{x,y,w,h});surface(g,id,x,y,w,h,hit(x,y,w,h,mx,my),accent);
        text(g,font,FastClientFonts.strong(label),x+w/2,y+h/2,1.65f,TEXT);
    }
    private static void icon(class_332 g,class_327 font,String id,String glyph,String label,class_2960 texture,int x,int y,int s,int mx,int my){
        REGIONS.put(id,new int[]{x,y,s,s});boolean hover=hit(x,y,s,s,mx,my);surface(g,id,x,y,s,s,hover,false);
        if(texture!=null)KushAssets.image(g,texture,x+11,y+8,s-22,s-16,1254,1254,-1);
        else text(g,font,FastClientFonts.filledMaterialSymbol(glyph),x+s/2,y+s/2+3,2f,hover?0xFFFFCDD5:0xFFB4A4AC);
        if(hover)tooltip(g,font,label,x+s/2,y+s+8);
    }
    private static void surface(class_332 g,String id,int x,int y,int w,int h,boolean hovered,boolean accent){
        float progress=HOVER.getOrDefault(id,0f);progress+=(hovered?1-progress:-progress)*0.15f;HOVER.put(id,progress);
        int fill=accent?FastClientUI.blend(0xE58A2136,0xFFAE2F46,progress):FastClientUI.blend(0xA6181116,0xD43B1C28,progress);
        int border=FastClientUI.blend(accent?0xCCCA5369:0x606B3745,0xCCBD536B,progress);
        FastClientUI.roundedRect(g,x,y+3,w,h,8,0x28000000);
        FastClientUI.borderedRoundedRect(g,x,y,w,h,8,fill,border);
        g.method_25294(x+8,y+1,x+w-8,y+2,accent?0x35FFD6DE:0x16FFD6DE);
    }
    private static void tooltip(class_332 g,class_327 font,String label,int cx,int y){
        class_2561 c=FastClientFonts.body(label);float scale=1.25f;int w=Math.round(font.method_27525((class_5348)c)*scale)+20;
        int x=Math.max(4,Math.min(cx-w/2,net.fastclient.hud.gui.DisplaySpace.width()-w-4));
        FastClientUI.borderedRoundedRect(g,x,y,w,26,5,0xE8181118,0x607B3A44);text(g,font,c,x+w/2,y+13,scale,TEXT);
    }
    private static void credit(class_332 g,class_327 font,int w,int h){
        class_2561 c=FastClientFonts.body(CREDIT);float s=0.9f;int width=Math.round(font.method_27525((class_5348)c)*s);
        text(g,font,c,w-14-width/2,h-14,s,0xFF877780);
    }
    private static void text(class_332 g,class_327 font,class_2561 c,int cx,int cy,float scale,int color){
        float x=cx-font.method_27525((class_5348)c)*scale/2f,y=cy-9*scale/2f;
        g.method_51448().pushMatrix();g.method_51448().translate(x,y);g.method_51448().scale(scale,scale);
        g.method_51439(font,c,0,0,color,false);g.method_51448().popMatrix();
    }
    private static boolean hit(int x,int y,int w,int h,int mx,int my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
}
