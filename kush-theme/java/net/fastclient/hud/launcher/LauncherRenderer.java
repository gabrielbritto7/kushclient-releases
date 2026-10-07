package net.fastclient.hud.launcher;

import java.util.LinkedHashMap;
import java.util.Map;
import net.fastclient.hud.gui.FastClientFonts;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.gui.KushAssets;
import net.fastclient.hud.gui.KushLanguage;
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
    private static final Map<String, Long> HOVER_TIME = new LinkedHashMap<>();
    private static final int TEXT = 0xFFF5EFF1;
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
        topTools(g,font,w,h,mx,my);
    }
    private static void topTools(class_332 g,class_327 font,int w,int h,int mx,int my) {
        renderSkinToggle(g,font,w,h,mx,my);int x=toggleBounds(w)[0]-10-48;
        icon(g,font,"box","\ue2c8","Resource packs",null,x,18,48,mx,my);x-=58;
        icon(g,font,"settings","\ue8b8","Configurações do Minecraft",null,x,18,48,mx,my);x-=58;
        icon(g,font,"window",null,"Kush Mods",KushAssets.PIXEL_RED_K,x,18,48,mx,my);x-=58;
        REGIONS.put("kush_language",new int[]{x,18,48,48});KushLanguage.drawButton(g,font,x,18,48,48,mx,my);
    }
    public static void renderSkinToggle(class_332 g,class_327 font,int w,int h,int mx,int my) {
        int[] r=toggleBounds(w);int x=r[0],y=r[1];
        long now=System.nanoTime();float dt=Math.min(0.1f,(now-lastToggleTime)/1_000_000_000f);lastToggleTime=now;
        float target=LauncherSkinPreference.isFastClientSkinEnabled()?1:0;
        uiColor+=(target-uiColor)*(1-(float)Math.exp(-dt/0.075f));
        surface(g,"toggle",x,y,r[2],r[3],hit(x,y,r[2],r[3],mx,my),false);
        int iw=50,ih=28,ix=x+(r[2]-iw)/2,iy=y+(r[3]-ih)/2;
        KushAssets.image(g,KushAssets.grayWordmark(),ix,iy,iw,ih,1672,941,-1);
        KushAssets.image(g,KushAssets.WORDMARK,ix,iy,iw,ih,1672,941,FastClientUI.withAlpha(-1,Math.round(uiColor*255)));
        if(hit(x,y,r[2],r[3],mx,my))tooltip(g,font,target>0?"Usar interface do Minecraft":"Usar interface Kush",x+r[2]/2,y+r[3]+8);
    }
    public static void renderVanillaOverlay(class_332 g,class_327 font,int w,int h,int mx,int my) {
        REGIONS.clear();renderSkinToggle(g,font,w,h,mx,my);
    }
    private static int[] toggleBounds(int w) { return new int[]{w-84,18,66,48}; }
    public static void renderPause(class_332 g,class_327 font,int w,int h,int mx,int my) {
        REGIONS.clear();g.method_25294(0,0,w,h,0x7D0B080C);
        topTools(g,font,w,h,mx,my);
        float s=Math.min(w/1920f,h/1080f);
        int cx=w/2,bh=Math.max(32,Math.round(69*s)),gap=Math.max(4,Math.round(6*s));
        int start=Math.max(156,h/2-Math.round(174*s)),bw=Math.min(w-40,Math.max(240,Math.round(360*s)));
        int lw=Math.round(324*s),lh=Math.round(lw*1080f/1920f);
        KushAssets.image(g,KushAssets.SKATE,cx-lw/2,start-lh-16,lw,lh,1920,1080,-1);
        String[][] rows={{"pause_backtogame","Voltar ao jogo","back"},{"pause_fastclient_settings","Kush Settings","kush"},
            {"pause_options","Opções","gear"},{"pause_open_to_lan","Abrir para LAN","lan"},
            {"pause_modmenu","Mods","mods"},{"pause_disconnect","Desconectar","exit"}};
        int y=start;
        for(var row:rows){
            if(row[0].equals("pause_open_to_lan")&&class_310.method_1551().method_1576()==null)continue;
            pauseButton(g,font,row[0],row[1],row[2],cx-bw/2,y,bw,bh,mx,my);y+=bh+gap;
        }
        String[][] bottom={{"pause_minecraftfolder","Pasta do jogo","folder"},{"pause_statistics","Estatísticas","chart"},{"pause_advancements","Avanços","award"}};
        int bx=w-32;
        for(var b:bottom){int smallW=font.method_1727(KushLanguage.translate(b[1]))*2+46;bx-=smallW;
            pauseButton(g,font,b[0],b[1],b[2],bx,h-70,smallW,38,mx,my);bx-=10;}
    }
    private static void pauseButton(class_332 g,class_327 font,String id,String label,String icon,int x,int y,int w,int h,int mx,int my) {
        label=KushLanguage.translate(label);
        REGIONS.put(id,new int[]{x,y,w,h});boolean hovered=hit(x,y,w,h,mx,my);
        surface(g,id,x,y,w,h,hovered,false);
        class_2561 c=class_2561.method_43470(label);
        int tw=font.method_27525((class_5348)c)*2, iw=14, gap=8, contentX=x+(w-tw-iw-gap)/2;
        int inkTop=y+h/2-7;
        if(icon.equals("kush"))KushAssets.pixelLabelK(g,contentX,inkTop,iw,hovered);
        else KushAssets.symbol(g,icon,contentX,inkTop,iw,TEXT);
        text(g,font,c,contentX+iw+gap+tw/2,y+h/2,2f,TEXT);
    }
    private static void button(class_332 g,class_327 font,String id,String label,int x,int y,int w,int h,int mx,int my,boolean accent) {
        label=KushLanguage.translate(label);REGIONS.put(id,new int[]{x,y,w,h});surface(g,id,x,y,w,h,hit(x,y,w,h,mx,my),accent);
        text(g,font,class_2561.method_43470(label),x+w/2,y+h/2,w<160?1f:2f,TEXT);
    }
    private static void icon(class_332 g,class_327 font,String id,String glyph,String label,class_2960 texture,int x,int y,int s,int mx,int my){
        REGIONS.put(id,new int[]{x,y,s,s});boolean hover=hit(x,y,s,s,mx,my);surface(g,id,x,y,s,s,hover,false);
        if(texture!=null)KushAssets.image(g,hover?KushAssets.PIXEL_RED_K:KushAssets.PIXEL_GRAY_K,x+8,y+7,s-16,s-14,1254,1254,-1);
        else KushAssets.symbol(g,id.equals("box")?"folder":"gear",x+12,y+12,s-24,hover?0xFFFFCDD5:0xFFB4A4AC);
        if(hover)tooltip(g,font,label,x+s/2,y+s+8);
    }
    private static void surface(class_332 g,String id,int x,int y,int w,int h,boolean hovered,boolean accent){
        long now=System.nanoTime(),last=HOVER_TIME.getOrDefault(id,now-16_000_000);
        float dt=Math.min(.1f,(now-last)/1_000_000_000f);HOVER_TIME.put(id,now);
        float progress=HOVER.getOrDefault(id,0f);
        progress+=((hovered?1f:0f)-progress)*(1-(float)Math.exp(-dt/.10f));HOVER.put(id,progress);
        int fill=accent?FastClientUI.blend(0xF5CA2634,0xFFED3C48,progress):FastClientUI.blend(0xD81D1A1E,0xF0352228,progress);
        int border=FastClientUI.blend(accent?0xFF841B24:0xFF110F12,0xFFA52A37,progress);
        int line=FastClientUI.withAlpha(0xFFE53542,Math.round(progress*220));
        if(id.equals("toggle")){
            fill=0xFF000000;
            border=saturate(border,uiColor);line=saturate(line,uiColor);
        }
        g.method_25294(x,y+2,x+w,y+h+2,0x28000000);
        FastClientUI.borderedRoundedRect(g,x,y,w,h,0,fill,border);
        if(progress>.01f && !id.equals("toggle"))g.method_25296(x+1,y+1,x+w-1,y+h-1,
            FastClientUI.withAlpha(0xFFE53542,Math.round(progress*8)),
            FastClientUI.withAlpha(0xFFE53542,Math.round(progress*32)));
        if(progress>.01f)g.method_25294(x+1,y+h-2,x+w-1,y+h-1,line);
    }
    private static void tooltip(class_332 g,class_327 font,String label,int cx,int y){
        class_2561 c=class_2561.method_43470(KushLanguage.translate(label));float scale=1f;int w=Math.round(font.method_27525((class_5348)c)*scale)+20;
        int x=Math.max(4,Math.min(cx-w/2,net.fastclient.hud.gui.DisplaySpace.width()-w-4));
        FastClientUI.borderedRoundedRect(g,x,y,w,26,0,0xE8181118,0xFF110E12);text(g,font,c,x+w/2,y+13,scale,TEXT);
    }
    private static int saturate(int color,float saturation) {
        int gray=Math.round(.2126f*((color>>>16)&255)+.7152f*((color>>>8)&255)+.0722f*(color&255));
        return FastClientUI.blend((color&0xFF000000)|gray<<16|gray<<8|gray,color,saturation);
    }
    private static void text(class_332 g,class_327 font,class_2561 c,int cx,int cy,float scale,int color){
        float x=cx-font.method_27525((class_5348)c)*scale/2f,y=cy-9*scale/2f;
        g.method_51448().pushMatrix();g.method_51448().translate(x,y);g.method_51448().scale(scale,scale);
        g.method_51439(font,c,0,0,color,false);g.method_51448().popMatrix();
    }
    private static boolean hit(int x,int y,int w,int h,int mx,int my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
}

