package net.fastclient.hud.launcher;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class LauncherRenderer {
    private static final Map<String, int[]> REGIONS = new LinkedHashMap<>();
    private static final int RED = 0xFFE32636;
    private static final int RED_HOVER = 0xFFFF4351;
    private static final int PANEL = 0xE8141012;
    private static final int PANEL_HOVER = 0xEF251719;
    private static final int BORDER = 0xFF5A252A;
    private static final int TEXT = 0xFFF4F0F0;
    private static final int MUTED = 0xFFAAA0A2;
    private static int[] toggleBounds = new int[]{0,0,0,0};

    private LauncherRenderer() {}

    public static String getClickedButton(int mouseX, int mouseY) {
        for (Map.Entry<String, int[]> entry : REGIONS.entrySet()) {
            int[] r = entry.getValue();
            if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) return entry.getKey();
        }
        return null;
    }

    public static boolean isSkinToggleClicked(int screenW, int screenH, int mouseX, int mouseY) {
        int[] r = toggleBounds(screenW);
        return mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
    }

    public static boolean isDiscordClicked(int screenW, int screenH, int mouseX, int mouseY) { return false; }

    public static void render(GuiGraphics g, Font font, int w, int h, int mouseX, int mouseY) {
        REGIONS.clear();
        g.fill(0, 0, w, h, 0xA6080506);
        g.fill(0, 0, w, 2, RED);

        int cx = w / 2;
        int logoY = Math.max(42, h / 2 - 190);
        g.drawCenteredString(font, "KUSHCLIENT", cx, logoY, 0xFFFFFFFF);
        g.drawCenteredString(font, "FAN EDITION", cx, logoY + 14, RED);

        int bw = 304, bh = 38, gap = 6;
        int x = cx - bw / 2;
        int y = Math.max(110, h / 2 - 95);
        button(g, font, "singleplayer", "Um jogador", x, y, bw, bh, mouseX, mouseY);
        y += bh + gap;
        button(g, font, "multiplayer", "Multijogador", x, y, bw, bh, mouseX, mouseY);
        y += bh + gap;
        button(g, font, "skins", "Cosmetics", x, y, bw, bh, mouseX, mouseY);
        y += bh + gap;
        button(g, font, "modmenu", "Mods", x, y, bw, bh, mouseX, mouseY);
        y += bh + 24;
        accentButton(g, font, "quit", "Sair do jogo", cx - 116, y, 232, 40, mouseX, mouseY);

        renderTopTools(g, font, w, mouseX, mouseY);

        String fan = "Kush Fan Edition • base FastClient HUD";
        g.drawString(font, fan, w - font.width(fan) - 8, h - 14, 0xFF8E8587, false);
    }

    private static void renderTopTools(GuiGraphics g, Font font, int w, int mouseX, int mouseY) {
        int y = 18, h = 36, gap = 6;
        int uiW = 50, packsW = 58, settingsW = 58, modsW = 82;
        int right = w - 18;
        int uiX = right - uiW;
        toggleBounds = new int[]{uiX, y, uiW, h};
        small(g, font, "MC", uiX, y, uiW, h, mouseX, mouseY, true);
        right = uiX - gap;
        int packsX = right - packsW;
        smallRegion(g, font, "box", "Packs", packsX, y, packsW, h, mouseX, mouseY);
        right = packsX - gap;
        int settingsX = right - settingsW;
        smallRegion(g, font, "settings", "Ajustes", settingsX, y, settingsW, h, mouseX, mouseY);
        right = settingsX - gap;
        int modsX = right - modsW;
        smallRegion(g, font, "window", "Kush Mods", modsX, y, modsW, h, mouseX, mouseY);
    }

    public static void renderPause(GuiGraphics g, Font font, int w, int h, int mouseX, int mouseY) {
        REGIONS.clear();
        g.fill(0, 0, w, h, 0xB5070506);
        int cx = w / 2;
        int titleY = Math.max(48, h / 2 - 205);
        g.drawCenteredString(font, "KUSHCLIENT", cx, titleY, 0xFFFFFFFF);
        g.drawCenteredString(font, "FAN EDITION", cx, titleY + 14, RED);
        int bw = 306, bh = 35, gap = 5, x = cx - bw/2, y = Math.max(110, h/2 - 145);
        button(g,font,"pause_backtogame","Voltar ao jogo",x,y,bw,bh,mouseX,mouseY); y += bh+gap;
        accentButton(g,font,"pause_fastclient_settings","Kush Settings",x,y,bw,bh,mouseX,mouseY); y += bh+gap;
        button(g,font,"pause_options","Opções",x,y,bw,bh,mouseX,mouseY); y += bh+gap;
        button(g,font,"pause_open_to_lan","Abrir para LAN",x,y,bw,bh,mouseX,mouseY); y += bh+gap;
        button(g,font,"pause_modmenu","Mods",x,y,bw,bh,mouseX,mouseY); y += bh+gap;
        button(g,font,"pause_disconnect","Desconectar",x,y,bw,bh,mouseX,mouseY);

        int bottomY = h - 42;
        int miniW = 112, miniGap = 5;
        int total = miniW*4 + miniGap*3;
        int bx = cx - total/2;
        smallRegion(g,font,"pause_advancements","Avanços",bx,bottomY,miniW,28,mouseX,mouseY); bx += miniW+miniGap;
        smallRegion(g,font,"pause_statistics","Estatísticas",bx,bottomY,miniW,28,mouseX,mouseY); bx += miniW+miniGap;
        smallRegion(g,font,"pause_player_reporting","Denúncias",bx,bottomY,miniW,28,mouseX,mouseY); bx += miniW+miniGap;
        smallRegion(g,font,"pause_minecraftfolder","Pasta Minecraft",bx,bottomY,miniW,28,mouseX,mouseY);

        String fan = "Kush Fan Edition • base FastClient HUD";
        g.drawString(font, fan, w - font.width(fan) - 8, h - 14, 0xFF8E8587, false);
    }

    public static void renderSkinToggle(GuiGraphics g, Font font, int w, int h, int mouseX, int mouseY) {
        int[] r = toggleBounds(w);
        toggleBounds = r;
        small(g,font,"MC",r[0],r[1],r[2],r[3],mouseX,mouseY,true);
    }

    public static void renderVanillaOverlay(GuiGraphics g, Font font, int w, int h, int mouseX, int mouseY) {
        int[] r = toggleBounds(w);
        toggleBounds = r;
        small(g,font,"K",r[0],r[1],r[2],r[3],mouseX,mouseY,true);
        String fan = "Kush Fan Edition • base FastClient HUD";
        g.drawString(font, fan, w - font.width(fan) - 8, h - 14, 0xB08E8587, false);
    }

    private static int[] toggleBounds(int w) { return new int[]{w - 68, 18, 50, 36}; }

    private static void button(GuiGraphics g, Font font, String id, String label, int x, int y, int w, int h, int mx, int my) {
        boolean hover = hit(x,y,w,h,mx,my);
        g.fill(x,y,x+w,y+h, hover ? PANEL_HOVER : PANEL);
        outline(g,x,y,w,h, hover ? RED : BORDER);
        g.drawCenteredString(font,label,x+w/2,y+(h-8)/2,TEXT);
        REGIONS.put(id,new int[]{x,y,w,h});
    }

    private static void accentButton(GuiGraphics g, Font font, String id, String label, int x, int y, int w, int h, int mx, int my) {
        boolean hover = hit(x,y,w,h,mx,my);
        g.fill(x,y,x+w,y+h, hover ? RED_HOVER : RED);
        outline(g,x,y,w,h,0xFFFF7780);
        g.drawCenteredString(font,label,x+w/2,y+(h-8)/2,0xFFFFFFFF);
        REGIONS.put(id,new int[]{x,y,w,h});
    }

    private static void smallRegion(GuiGraphics g, Font font, String id, String label, int x, int y, int w, int h, int mx, int my) {
        small(g,font,label,x,y,w,h,mx,my,false);
        REGIONS.put(id,new int[]{x,y,w,h});
    }

    private static void small(GuiGraphics g, Font font, String label, int x, int y, int w, int h, int mx, int my, boolean accent) {
        boolean hover = hit(x,y,w,h,mx,my);
        int fill = accent ? (hover ? RED_HOVER : RED) : (hover ? PANEL_HOVER : PANEL);
        g.fill(x,y,x+w,y+h,fill);
        outline(g,x,y,w,h,accent ? 0xFFFF7780 : (hover ? RED : BORDER));
        g.drawCenteredString(font,label,x+w/2,y+(h-8)/2,0xFFFFFFFF);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,y,x+w,y+1,color); g.fill(x,y+h-1,x+w,y+h,color); g.fill(x,y,x+1,y+h,color); g.fill(x+w-1,y,x+w,y+h,color);
    }
    private static boolean hit(int x,int y,int w,int h,int mx,int my) { return mx>=x && mx<x+w && my>=y && my<y+h; }
}
