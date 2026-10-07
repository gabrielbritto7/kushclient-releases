package net.fastclient.hud.gui;

import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_10799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;

/** Native texture/glass rendering. Original PNG alpha is preserved. */
public final class KushAssets {
    public static final class_2960 SKATE = texture("skate.png");
    public static final class_2960 RED_K = texture("k_red.png");
    public static final class_2960 WHITE_K = texture("k_white.png");
    public static final class_2960 WORDMARK = texture("wordmark.png");
    private static final class_2960 GRAY_WORDMARK = texture("wordmark_gray_runtime");
    private static final class_2960 BACKGROUND = class_2960.method_60655("fastclient-hud", "textures/gui/new-background.png");
    private static final Map<class_2960, BufferedImage> SOURCES = new HashMap<>();
    private static final Map<String, class_2960> SCALED = new LinkedHashMap<>(32, .75f, true);
    private static final int CACHE_LIMIT = 64;
    private static int textureSerial;
    private KushAssets() {}
    private static class_2960 texture(String file) {
        return class_2960.method_60655("fastclient-hud", "textures/gui/kush/" + file);
    }
    public static void image(class_332 g, class_2960 id, int x, int y, int w, int h, int tw, int th, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        if (id.method_12832().startsWith("textures/gui/kush/")) {
            int[] fit = KushImageResampler.contain(tw, th, w, h);
            x += (w - fit[0]) / 2; y += (h - fit[1]) / 2;
            w = fit[0]; h = fit[1];
            int pw = physicalWidth(w), ph = physicalHeight(h);
            String key = id + "@" + pw + "x" + ph;
            class_2960 prepared = SCALED.get(key);
            if (prepared == null) {
                try {
                    BufferedImage source = source(id);
                    BufferedImage small = KushImageResampler.resize(source, pw, ph);
                    prepared = register(key, small);
                    small.flush();
                } catch (Exception error) {
                    // Cache the fallback too: never retry disk IO every frame.
                    System.err.println("[KushMod] UI image preparation failed: " + key + ": " + error.getMessage());
                    prepared = id.equals(GRAY_WORDMARK) ? WORDMARK : id;
                    SCALED.put(key, prepared);
                }
            }
            if (prepared.method_12832().contains("filtered_")) { tw = pw; th = ph; }
            id = prepared;
        }
        g.method_25293(class_10799.field_56883, id, x, y, 0, 0, w, h, tw, th, tw, th, color);
    }
    /** The enabled/disabled brand state uses the same proportions and filtering. */
    public static class_2960 grayWordmark() {
        return GRAY_WORDMARK;
    }
    public static void symbol(class_332 g, String name, int x, int y, int size, int color) {
        int pw = physicalWidth(size), ph = physicalHeight(size);
        String key = "symbol/" + name + "@" + pw + "x" + ph;
        class_2960 id = SCALED.get(key);
        if (id == null) {
            BufferedImage icon = KushImageResampler.icon(name, pw, ph);
            id = register(key, icon);
            icon.flush();
        }
        g.method_25293(class_10799.field_56883, id, x, y, 0, 0, size, size, pw, ph, pw, ph, color);
    }
    public static void softRect(class_332 g,int x,int y,int w,int h,int radius,int color) {
        int pw=physicalWidth(w),ph=physicalHeight(h),pr=physicalHeight(radius);
        String key="rounded/"+pw+"x"+ph+"r"+pr;
        class_2960 id=SCALED.get(key);
        if(id==null){BufferedImage mask=KushImageResampler.roundedMask(pw,ph,pr);id=register(key,mask);mask.flush();}
        g.method_25293(class_10799.field_56883,id,x,y,0,0,w,h,pw,ph,pw,ph,color);
    }
    private static int physicalWidth(int w) {
        return Math.max(1, Math.round(w * (float)class_310.method_1551().method_22683().method_4489() / DisplaySpace.width()));
    }
    private static int physicalHeight(int h) {
        return Math.max(1, Math.round(h * (float)class_310.method_1551().method_22683().method_4506() / DisplaySpace.height()));
    }
    private static BufferedImage source(class_2960 id) throws Exception {
        BufferedImage source = SOURCES.get(id);
        if (source != null) return source;
        class_2960 resource = id.equals(GRAY_WORDMARK) ? WORDMARK : id;
        try (InputStream stream = KushAssets.class.getResourceAsStream("/assets/" + resource.method_12836() + "/" + resource.method_12832())) {
            if (stream == null) throw new IllegalArgumentException("Missing original PNG " + resource);
            source = ImageIO.read(stream);
        }
        if (source == null) throw new IllegalArgumentException("Invalid original PNG " + resource);
        if (id.equals(GRAY_WORDMARK)) {
            for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int luma = Math.min(255, Math.round(1.3f * (.2126f * ((argb >>> 16) & 255) + .7152f * ((argb >>> 8) & 255) + .0722f * (argb & 255))));
                source.setRGB(x, y, (argb & 0xFF000000) | luma << 16 | luma << 8 | luma);
            }
        }
        SOURCES.put(id, source);
        return source;
    }
    private static class_2960 register(String key, BufferedImage source) {
        class_1011 image = new class_1011(source.getWidth(), source.getHeight(), false);
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++)
            image.method_61941(x, y, source.getRGB(x, y));
        class_2960 id = texture("filtered_" + textureSerial++);
        class_310.method_1551().method_1531().method_4616(id, new class_1043(() -> "Kush UI " + key, image));
        SCALED.put(key, id);
        if (SCALED.size() > CACHE_LIMIT) {
            String oldest = SCALED.keySet().iterator().next();
            class_2960 previous = SCALED.remove(oldest);
            if (previous.method_12832().contains("filtered_")) class_310.method_1551().method_1531().method_4615(previous);
        }
        return id;
    }
    public static void backdrop(class_332 g, int w, int h) {
        if (class_310.method_1551().field_1687 != null) return;
        int bh = Math.max(h, Math.round(w * 9f / 16f));
        int bw = Math.max(w, Math.round(bh * 16f / 9f));
        image(g, DisplaySpace.texture(BACKGROUND), (w-bw)/2, (h-bh)/2, bw, bh, 3840, 2160, -1);
        g.method_25294(0, 0, w, h, 0x9910090D);
    }
    public static void glass(class_332 g, int x, int y, int w, int h, int alpha) {
        FastClientUI.roundedRect(g, x-2, y+4, w+4, h+3, 12, FastClientUI.withAlpha(0xFF000000, Math.round(alpha*0.10f)));
        // A translucent border filled across the panel adds another dark layer.
        // Keep it on the perimeter so the scene remains visible through glass.
        FastClientUI.roundedRect(g, x, y, w, h, 10, FastClientUI.fade(0x740E0C10, alpha));
        FastClientUI.roundedOutline(g, x, y, w, h, 10, FastClientUI.fade(0x607B3A44, alpha));
        g.method_25294(x+12, y+1, x+w-12, y+2, FastClientUI.withAlpha(0xFFF5CAD3, Math.round(alpha*0.16f)));
        g.method_25294(x+12, y+2, x+w-12, y+32, FastClientUI.withAlpha(0xFF7B3A44, Math.round(alpha*0.025f)));
    }
}
