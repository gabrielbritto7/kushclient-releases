/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_10017
 *  net.minecraft.class_10042
 *  net.minecraft.class_11905
 *  net.minecraft.class_11908
 *  net.minecraft.class_11909
 *  net.minecraft.class_1297
 *  net.minecraft.class_1304
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_4184
 *  net.minecraft.class_437
 *  net.minecraft.class_898
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package net.fastclient.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.client.FastClientCoreClient;
import net.fastclient.client.gui.CosmeticPreviewPlayer;
import net.fastclient.core.data.CatalogEntry;
import net.fastclient.core.data.PlayerCosmetics;
import net.minecraft.class_10017;
import net.minecraft.class_10042;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4184;
import net.minecraft.class_437;
import net.minecraft.class_898;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

@Environment(value=EnvType.CLIENT)
public class CosmeticsScreen
extends class_437 {
    private static final int DIM = -1207959552;
    private static final int PANEL = -234025715;
    private static final int LINE = -13421773;
    private static final int ROW = -15329770;
    private static final int ROW_HOVER = -14277082;
    private static final int TEXT = -1;
    private static final int MUTED = -6052957;
    private static final int ACCENT = -1890762;
    private static final int GREEN = -11751570;
    private static final int HEADER_H = 32;
    private static final int MODE_H = 32;
    private static final int ROW_H = 20;
    private static final int PAD = 10;
    private static final int TAB_H = 24;
    private static final int TAB_PAD = 8;
    private static final int TAB_GAP = 2;
    private static final int TAB_SCROLL_H = 3;
    private static final List<Cat> CATS = List.of(new Cat("cape", "Capes"), new Cat("hats", "Hats"), new Cat("face", "Face"), new Cat("arm", "Arm"), new Cat("boots", "Boots"), new Cat("back", "Back"), new Cat("shields", "Shields"), new Cat("wings", "Wings"), new Cat("pets", "Pets"), new Cat("auras", "Auras"), new Cat("emotes", "Emotes"));
    private final List<int[]> catRects = new ArrayList<int[]>();
    private final List<Object[]> rowRects = new ArrayList<Object[]>();
    private int[] languageRect = new int[4];
    private int[] searchRect = new int[4];
    private int[] previewRect = new int[4];
    private int[] closeRect = new int[4];
    private int[] tabsRect = new int[4];
    private int[] wardrobeRect = new int[4];
    private int[] dressRect = new int[4];
    private int[] clearRect = new int[4];
    private int selected = 0;
    private String searchText = "";
    private boolean searchFocused = false;
    private Mode mode = Mode.DRESS;
    private int scroll = 0;
    private int maxScroll = 0;
    private int tabScroll = 0;
    private int tabMaxScroll = 0;
    private boolean previewDragging = false;
    private float previewYaw = 0.0f;
    private float previewPitch = 0.0f;
    private float previewZoom = 1.0f;
    private CosmeticPreviewPlayer previewPlayer;

    public CosmeticsScreen() {
        super((class_2561)class_2561.method_43470((String)"Cosmetics"));
    }

    private static boolean in(int[] r, double mx, double my) {
        return mx >= (double)r[0] && mx < (double)(r[0] + r[2]) && my >= (double)r[1] && my < (double)(r[1] + r[3]);
    }

    private int width(String s){return this.field_22793.method_1727(net.fastclient.hud.gui.KushLanguage.translate(s));}

    private void drawText(class_332 g, String s, int x, int y, int color) {
        g.method_51433(this.field_22793, net.fastclient.hud.gui.KushLanguage.translate(s), x, y, color, false);
    }

    private void drawOutline(class_332 g, int x, int y, int w, int h, int color) {
        g.method_25294(x, y, x + w, y + 1, color);
        g.method_25294(x, y + h - 1, x + w, y + h, color);
        g.method_25294(x, y + 1, x + 1, y + h - 1, color);
        g.method_25294(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public void method_25394(class_332 g, int mouseX, int mouseY, float partial) {
        g.method_25294(0,0,this.field_22789,this.field_22790,0xB009070A);
        int w=Math.min(this.field_22789-16,760),h=Math.min(this.field_22790-16,460);
        int x=(this.field_22789-w)/2,y=(this.field_22790-h)/2;
        g.method_25294(x,y,x+w,y+h,0xFF141115);
        this.drawOutline(g,x,y,w,h,0xFF742332);
        this.drawText(g,"KUSH · Cosméticos",x+10,y+12,0xFFF5EFF1);
        this.closeRect=new int[]{x+w-28,y+6,22,22};
        g.method_25294(closeRect[0],closeRect[1],closeRect[0]+22,closeRect[1]+22,in(closeRect,mouseX,mouseY)?0xFF742332:0xFF25191F);
        this.drawText(g,"×",x+w-20,y+13,-1);
        int languageX=x+w-72;
        net.fastclient.hud.gui.KushLanguage.drawButton(g,this.field_22793,languageX,y+6,36,22,mouseX,mouseY);
        this.languageRect=new int[]{languageX,y+6,36,22};
        int bodyX=x+10,bodyW=w-20;
        this.renderModes(g,mouseX,mouseY,bodyX,y+32,bodyW);
        this.renderTabs(g,mouseX,mouseY,bodyX,y+64,bodyW);
        int leftW=Math.max(80,Math.round(bodyW*.48f)),listY=y+121,listH=Math.max(30,h-150);
        this.renderControls(g,mouseX,mouseY,bodyX,y+96,leftW);
        this.renderList(g,mouseX,mouseY,bodyX,listY,leftW,listH);
        this.renderPreview(g,mouseX,mouseY,bodyX+leftW+12,y+91,bodyW-leftW-12,h-99);
        this.drawText(g,"Local preview · saved on this device",bodyX,y+h-16,0xFFB4A4AC);
    }

    private void renderModes(class_332 g, int mouseX, int mouseY, int x, int y, int w) {
        int buttonY = y + 5;
        int wardrobeW = this.width("Wardrobe") + 20;
        int dressW = this.width("Dress Room") + 20;
        this.wardrobeRect = new int[]{x, buttonY, wardrobeW, 22};
        this.dressRect = new int[]{x + wardrobeW + 5, buttonY, dressW, 22};
        this.renderModeButton(g, mouseX, mouseY, this.wardrobeRect, "Wardrobe", this.mode == Mode.WARDROBE);
        this.renderModeButton(g, mouseX, mouseY, this.dressRect, "Dress Room", this.mode == Mode.DRESS);
        String help = this.mode == Mode.DRESS ? "Try anything. Your preview is private and temporary." : "Equip catalog items. Saved locally for your Minecraft account.";
        int helpX = x + w - this.width(help);
        if (helpX > this.dressRect[0] + this.dressRect[2] + 10) {
            this.drawText(g, help, helpX, y + 12, -6052957);
        }
    }

    private void renderModeButton(class_332 g, int mouseX, int mouseY, int[] rect, String label, boolean active) {
        boolean hover = CosmeticsScreen.in(rect, mouseX, mouseY);
        g.method_25294(rect[0], rect[1], rect[0] + rect[2], rect[1] + rect[3], active ? -1890762 : (hover ? -14277082 : -15329770));
        this.drawText(g, label, rect[0] + (rect[2] - this.width(label)) / 2, rect[1] + 7, active || hover ? -1 : -6052957);
    }

    private void renderTabs(class_332 g, int mouseX, int mouseY, int x, int y, int w) {
        this.catRects.clear();
        this.tabsRect = new int[]{x, y, w, 27};
        int[] tabW = new int[CATS.size()];
        int total = 0;
        for (int i = 0; i < CATS.size(); ++i) {
            tabW[i] = this.width(CATS.get(i).label()) + 16;
            total += tabW[i] + (i == 0 ? 0 : 2);
        }
        this.tabMaxScroll = Math.max(0, total - w);
        this.tabScroll = Math.min(this.tabScroll, this.tabMaxScroll);
        g.method_44379(x, y, x + w, y + 24);
        int tx = x - this.tabScroll;
        for (int i = 0; i < CATS.size(); ++i) {
            if (tx + tabW[i] > x && tx < x + w) {
                boolean hover;
                int hitX = Math.max(tx, x);
                int hitW = Math.min(tx + tabW[i], x + w) - hitX;
                boolean sel = i == this.selected;
                boolean bl = hover = mouseX >= hitX && mouseX < hitX + hitW && mouseY >= y && mouseY < y + 24;
                if (sel) {
                    g.method_25294(tx, y, tx + tabW[i], y + 24, -14277082);
                    g.method_25294(tx, y + 24 - 2, tx + tabW[i], y + 24, -1890762);
                } else if (hover) {
                    g.method_25294(tx, y, tx + tabW[i], y + 24, -15329770);
                }
                this.drawText(g, CATS.get(i).label(), tx + 8, y + 8, sel || hover ? -1 : -6052957);
                this.catRects.add(new int[]{hitX, y, hitW, 24, i});
            }
            tx += tabW[i] + 2;
        }
        g.method_44380();
        if (this.tabMaxScroll > 0) {
            int barW = Math.max(12, Math.round((float)w * ((float)w / (float)total)));
            int barX = x + Math.round((float)(w - barW) * ((float)this.tabScroll / (float)this.tabMaxScroll));
            int barY = y + 24 + 1;
            g.method_25294(x, barY, x + w, barY + 2, -15329770);
            g.method_25294(barX, barY, barX + barW, barY + 2, -13421773);
        }
    }

    private void renderControls(class_332 g, int mouseX, int mouseY, int x, int y, int w) {
        int h = 18;
        int searchW = w;
        g.method_25294(x, y, x + searchW, y + h, -15329770);
        if (this.searchFocused) {
            this.drawOutline(g, x, y, searchW, h, -1890762);
        }
        boolean placeholder = this.searchText.isEmpty() && !this.searchFocused;
        this.drawText(g, placeholder ? "Search..." : this.searchText, x + 6, y + (h - 8) / 2, placeholder ? -6052957 : -1);
        if (this.searchFocused) {
            int caretX = x + 6 + this.width(this.searchText);
            g.method_25294(caretX + 1, y + 4, caretX + 2, y + h - 4, -1);
        }
        this.searchRect = new int[]{x, y, searchW, h};
    }

    private void renderList(class_332 g, int mouseX, int mouseY, int x, int y, int w, int h) {
        this.rowRects.clear();
        Cat cat = CATS.get(this.selected);
        List<Entry> entries = this.visibleEntries(cat);
        if (entries.isEmpty()) {
            this.drawText(g, this.catalogStatus(), x, y + 4, -6052957);
            this.maxScroll = 0;
            return;
        }
        int step = 22;
        this.maxScroll = Math.max(0, entries.size() * step - h);
        this.scroll = Math.min(this.scroll, this.maxScroll);
        boolean overflow = this.maxScroll > 0;
        int rowW = overflow ? w - 4 : w;
        g.method_44379(x, y, x + w, y + h);
        for (int i = 0; i < entries.size(); ++i) {
            String status;
            Entry e = entries.get(i);
            int ry = y + i * step - this.scroll;
            if (ry + 20 < y || ry > y + h) continue;
            boolean actionable = this.mode == Mode.DRESS || e.owned() && FastClientCoreClient.canEquipOwn();
            boolean hover = actionable && mouseX >= x && mouseX < x + rowW && mouseY >= ry && mouseY < ry + 20 && mouseY >= y && mouseY < y + h;
            boolean active = this.mode == Mode.DRESS ? FastClientCoreClient.localEquip().isPreviewActive(cat.key(), e.entry().id()) : FastClientCoreClient.localEquip().isActive(cat.key(), e.entry().id());
            g.method_25294(x, ry, x + rowW, ry + 20, hover ? -14277082 : -15329770);
            if (active) {
                g.method_25294(x, ry, x + 2, ry + 20, this.mode == Mode.DRESS ? -1890762 : -11751570);
            }
            this.drawText(g, this.field_22793.method_27523(e.entry().label(), Math.max(20,rowW-90)), x + 10, ry + 6, !actionable ? -6052957 : (active ? (this.mode == Mode.DRESS ? -1890762 : -11751570) : -1));
            status = this.mode == Mode.DRESS ? (active ? "trying on" : (e.owned() ? "owned" : "preview")) : (active ? "equipped" : (FastClientCoreClient.canEquipOwn() ? "" : "sign in"));
            if (!status.isEmpty()) {
                this.drawText(g, status, x + rowW - 6 - this.width(status), ry + 6, active ? (this.mode == Mode.DRESS ? -1890762 : -11751570) : -6052957);
            }
            this.rowRects.add(new Object[]{x, ry, rowW, 20, e});
        }
        g.method_44380();
        if (overflow) {
            int barH = Math.max(12, Math.round((float)h * ((float)h / (float)(entries.size() * step))));
            int barY = y + Math.round((float)(h - barH) * ((float)this.scroll / (float)this.maxScroll));
            g.method_25294(x + w - 2, y, x + w, y + h, -15329770);
            g.method_25294(x + w - 2, barY, x + w, barY + barH, -13421773);
        }
    }

    private List<Entry> visibleEntries(Cat cat) {
        Set<String> owned = FastClientCoreClient.cache().owned(this.field_22787.method_1548().method_44717());
        String q = this.searchText.trim().toLowerCase(Locale.ROOT);
        ArrayList<Entry> out = new ArrayList<Entry>();
        for (CatalogEntry e : this.mode == Mode.WARDROBE ? FastClientCoreClient.cache().wardrobe(this.field_22787.method_1548().method_44717()) : FastClientCoreClient.cache().catalog()) {
            boolean isOwned;
            if (!cat.key().equals(e.category())) continue;
            boolean bl = isOwned = e.defaultOwned() || owned.contains(e.id());
            if (this.mode == Mode.WARDROBE && !isOwned || !q.isEmpty() && !e.label().toLowerCase(Locale.ROOT).contains(q) && !e.id().toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(new Entry(e, isOwned));
        }
        return out;
    }

    private String catalogStatus() {
        if (FastClientCoreClient.cache().isCatalogUnavailable()) {
            return "Catalog unavailable. Retrying...";
        }
        if (FastClientCoreClient.cache().isCatalogLoaded()) {
            return this.mode == Mode.DRESS ? "No catalog items in this category." : "No owned items in this category.";
        }
        return "Loading...";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderPreview(class_332 g, int mouseX, int mouseY, int x, int bodyY, int w, int bodyH) {
        class_10017 state;
        PlayerCosmetics look;
        int top = bodyY + 10;
        int bottom = bodyY + bodyH - 10;
        this.previewRect = new int[]{x, top, w, bottom - top};
        String title = this.mode == Mode.DRESS ? "Dress Room" : "Your equipped look";
        String subtitle = this.mode == Mode.DRESS ? "Preview owned and unowned items together" : "Local preview · saved on this device";
        this.drawText(g, title, x + (w - this.width(title)) / 2, top + 3, -1);
        if (this.width(subtitle) + 20 < w) {
            this.drawText(g, subtitle, x + (w - this.width(subtitle)) / 2, top + 17, -6052957);
        }
        String clearLabel = this.mode == Mode.DRESS ? "Clear preview" : "Unequip all";
        int clearW = this.width(clearLabel) + 18;
        this.clearRect = new int[]{x + (w - clearW) / 2, bottom - 23, clearW, 20};
        boolean clearEnabled = this.mode == Mode.DRESS ? FastClientCoreClient.localEquip().hasPreview() : FastClientCoreClient.canEquipOwn();
        boolean clearHover = clearEnabled && CosmeticsScreen.in(this.clearRect, mouseX, mouseY);
        g.method_25294(this.clearRect[0], this.clearRect[1], this.clearRect[0] + this.clearRect[2], this.clearRect[1] + this.clearRect[3], clearHover ? -14277082 : -15329770);
        this.drawText(g, clearLabel, this.clearRect[0] + 9, this.clearRect[1] + 6, clearEnabled ? -1 : -6052957);
        int modelTop = top + 30;
        int modelBottom = bottom - 28;
        int size = Math.max(10, Math.round(Math.min((float)(modelBottom - modelTop) * 0.43f, (float)w * 0.46f) * this.previewZoom));
        Quaternionf pose = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf tilt = new Quaternionf().rotateX(this.previewPitch * ((float)Math.PI / 180));
        pose.mul((Quaternionfc)tilt);
        if (this.previewPlayer == null) {
            this.previewPlayer = CosmeticPreviewPlayer.create(this.field_22787);
        }
        if (this.previewPlayer == null) {
            this.drawText(g, CosmeticPreviewPlayer.preparationFailed() ? "Preview unavailable" : "Preparing preview...", x + 10, modelTop + 10, -6052957);
            return;
        }
        CosmeticPreviewPlayer player = this.previewPlayer;
        player.advancePreviewTime();
        if (FastClientCoreClient.localEquip().isAssetUnavailable()) {
            this.drawText(g, "Asset unavailable. Select again to retry.", x + 10, modelTop, -6052957);
        }
        for (class_1304 slot : class_1304.values()) {
            player.method_5673(slot, this.field_22787.field_1724 == null ? class_1799.field_8037 : this.field_22787.field_1724.method_6118(slot).method_7972());
        }
        PlayerCosmetics playerCosmetics = look = this.mode == Mode.DRESS ? FastClientCoreClient.localEquip().forPreview() : FastClientCoreClient.localEquip().forRender();
        if (!(look.shields().isEmpty() || player.method_6047().method_31574(class_1802.field_8255) || player.method_6079().method_31574(class_1802.field_8255))) {
            player.method_5673(class_1304.field_6171, new class_1799((class_1935)class_1802.field_8255));
        }
        if (this.mode == Mode.DRESS) {
            FastClientCoreClient.beginCosmeticPreviewRender();
        }
        class_898 dispatcher = this.field_22787.method_1561();
        class_4184 previousCamera = dispatcher.field_4686;
        if (previousCamera == null) {
            dispatcher.field_4686 = new class_4184();
        }
        try {
            state = this.field_22787.method_1561().method_72977((class_1297)player, 1.0f);
        }
        finally {
            dispatcher.field_4686 = previousCamera;
            if (this.mode == Mode.DRESS) {
                FastClientCoreClient.endCosmeticPreviewRender();
            }
        }
        state.field_61820 = 0xF000F0;
        state.field_61823.clear();
        state.field_61821 = 0;
        if (state instanceof class_10042) {
            class_10042 living = (class_10042)state;
            living.field_53446 = 180.0f + this.previewYaw;
            living.field_53447 = 0.0f;
            living.field_53448 = 0.0f;
            living.field_53329 /= living.field_53453;
            living.field_53330 /= living.field_53453;
            living.field_53453 = 1.0f;
        }
        Vector3f translation = new Vector3f(0.0f, state.field_53330 / 2.0f + 0.0625f, 0.0f);
        g.method_70856(state, (float)size, translation, pose, tilt, x, modelTop, x + w, modelBottom);
    }

    public boolean method_25402(class_11909 event, boolean doubled) {
        if (event.method_74245() == 0) {
            if(in(languageRect,event.comp_4798(),event.comp_4799())) { net.fastclient.hud.gui.KushLanguage.toggle();return true; }
            double my;
            double mx = event.comp_4798();
            if (CosmeticsScreen.in(this.closeRect, mx, my = event.comp_4799())) {
                this.method_25419();
                return true;
            }
            if (CosmeticsScreen.in(this.wardrobeRect, mx, my) || CosmeticsScreen.in(this.dressRect, mx, my)) {
                this.mode = CosmeticsScreen.in(this.dressRect, mx, my) ? Mode.DRESS : Mode.WARDROBE;
                this.scroll = 0;
                this.searchFocused = false;
                return true;
            }
            for (int[] nArray : this.catRects) {
                if (!CosmeticsScreen.in(nArray, mx, my)) continue;
                if (this.selected != nArray[4]) {
                    this.selected = nArray[4];
                    this.scroll = 0;
                }
                this.searchFocused = false;
                return true;
            }
            this.searchFocused = CosmeticsScreen.in(this.searchRect, mx, my);
            if (this.searchFocused) {
                return true;
            }
            for (Object[] objectArray : this.rowRects) {
                if (!CosmeticsScreen.in(new int[]{(Integer)objectArray[0], (Integer)objectArray[1], (Integer)objectArray[2], (Integer)objectArray[3]}, mx, my)) continue;
                Entry entry = (Entry)objectArray[4];
                if (this.mode == Mode.DRESS) {
                    FastClientCoreClient.localEquip().togglePreview(entry.entry());
                } else if (entry.owned() && FastClientCoreClient.canEquipOwn()) {
                    FastClientCoreClient.localEquip().toggle(entry.entry());
                }
                return true;
            }
            if (CosmeticsScreen.in(this.clearRect, mx, my)) {
                if (this.mode == Mode.DRESS) {
                    FastClientCoreClient.localEquip().clearPreview();
                } else if (FastClientCoreClient.canEquipOwn()) {
                    FastClientCoreClient.localEquip().clear();
                }
                return true;
            }
            if (CosmeticsScreen.in(this.previewRect, mx, my)) {
                this.previewDragging = true;
                return true;
            }
        }
        return super.method_25402(event, doubled);
    }

    public boolean method_25403(class_11909 event, double dragX, double dragY) {
        if (this.previewDragging) {
            this.previewYaw = (this.previewYaw - (float)dragX * 1.2f) % 360.0f;
            this.previewPitch = Math.max(-35.0f, Math.min(35.0f, this.previewPitch + (float)dragY * 0.8f));
            return true;
        }
        return super.method_25403(event, dragX, dragY);
    }

    public boolean method_25406(class_11909 event) {
        if (this.previewDragging && event.method_74245() == 0) {
            this.previewDragging = false;
            return true;
        }
        return super.method_25406(event);
    }

    public boolean method_25401(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (CosmeticsScreen.in(this.tabsRect, mouseX, mouseY)) {
            if (this.tabMaxScroll > 0) {
                double d = scrollX != 0.0 ? -scrollX : scrollY;
                this.tabScroll = Math.max(0, Math.min(this.tabMaxScroll, this.tabScroll - (int)Math.round(d * 24.0)));
            }
            return true;
        }
        if (CosmeticsScreen.in(this.previewRect, mouseX, mouseY)) {
            this.previewZoom = Math.max(0.6f, Math.min(2.5f, this.previewZoom + (float)scrollY * 0.12f));
            return true;
        }
        if (this.maxScroll > 0) {
            this.scroll = Math.max(0, Math.min(this.maxScroll, this.scroll - (int)Math.round(scrollY * 20.0)));
            return true;
        }
        return super.method_25401(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean method_25400(class_11905 event) {
        int cp;
        if (this.searchFocused && (cp = event.comp_4793()) >= 32 && cp != 127 && this.searchText.length() < 32) {
            this.searchText = this.searchText + event.method_74226();
            this.scroll = 0;
            return true;
        }
        return super.method_25400(event);
    }

    public boolean method_25404(class_11908 event) {
        if (this.searchFocused) {
            int key = event.comp_4795();
            if (key == 259) {
                if (!this.searchText.isEmpty()) {
                    this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
                }
                this.scroll = 0;
                return true;
            }
            if (key == 256 || key == 257 || key == 335) {
                this.searchFocused = false;
                return true;
            }
        }
        return super.method_25404(event);
    }

    public boolean method_25421() {
        return false;
    }

    public void method_25432() {
        this.previewPlayer = null;
        FastClientCoreClient.localEquip().clearPreview();
        super.method_25432();
    }

    @Environment(value=EnvType.CLIENT)
    private static enum Mode {
        WARDROBE,
        DRESS;

    }

    @Environment(value=EnvType.CLIENT)
    private record Cat(String key, String label) {
    }

    @Environment(value=EnvType.CLIENT)
    private record Entry(CatalogEntry entry, boolean owned) {
    }
}

