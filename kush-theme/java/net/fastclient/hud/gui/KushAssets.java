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
    public static final class_2960 RED_K = texture("k_overlay_red.png");
    public static final class_2960 WHITE_K = texture("k_pixel_gray.png");
    public static final class_2960 WORDMARK = texture("wordmark.png");
    private static final class_2960 GRAY_WORDMARK = texture("wordmark_gray.png");
    public static final class_2960 PIXEL_RED_K = texture("k_pixel_red.png");
    public static final class_2960 PIXEL_GRAY_K = texture("k_pixel_gray.png");
    private static final class_2960 LABEL_K = texture("k_white_label_runtime");
    private static final class_2960 PIXEL_RED_LABEL = texture("k_pixel_red_label_runtime");
    private static final class_2960 PIXEL_GRAY_LABEL = texture("k_pixel_gray_label_runtime");
    private static final class_2960 BACKGROUND = texture("menu-landscape.png");
    private static final Map<class_2960, BufferedImage> SOURCES = new HashMap<>();
    private static final Map<String, class_2960> SCALED = new LinkedHashMap<>(32, .75f, true);
    private static final int CACHE_LIMIT = 64;
    private static final long CACHE_PIXEL_LIMIT = 16_000_000;
    private static final Map<String, Long> PIXELS = new HashMap<>();
    private static long cachedPixels;
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
    /** Align the visible K to the Minecraft cap height, not its transparent canvas. */
    public static void labelK(class_332 g, int x, int top, int capHeight, int color) {
        try {
            BufferedImage glyph = source(LABEL_K);
            image(g, LABEL_K, x, top, capHeight, capHeight, glyph.getWidth(), glyph.getHeight(), color);
        } catch (Exception unavailable) {
            image(g, WHITE_K, x, top, capHeight, capHeight, 1246, 1263, color);
        }
    }
    /** Use the visible pixel glyph bounds for the small menu labels. */
    public static void pixelLabelK(class_332 g, int x, int top, int capHeight, boolean hovered) {
        class_2960 id = hovered ? PIXEL_RED_LABEL : PIXEL_GRAY_LABEL;
        try {
            BufferedImage glyph = source(id);
            image(g, id, x, top, capHeight, capHeight, glyph.getWidth(), glyph.getHeight(), -1);
        } catch (Exception unavailable) {
            image(g, hovered ? PIXEL_RED_K : PIXEL_GRAY_K, x, top, capHeight, capHeight, 1254, 1254, -1);
        }
    }
    public static void symbol(class_332 g, String name, int x, int y, int size, int color) {
        int pw = physicalWidth(size), ph = physicalHeight(size);
        String key = "symbol/" + name + "@" + pw + "x" + ph;
        class_2960 id = SCALED.get(key);
        if (id == null) {
            BufferedImage raw = KushImageResampler.icon(name, pw, ph);
            BufferedImage trimmed = KushImageResampler.trimAlpha(raw);
            BufferedImage icon = KushImageResampler.resize(trimmed, pw, ph);
            if (trimmed != raw) trimmed.flush();
            raw.flush();
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
        class_2960 resource = id.equals(LABEL_K) || id.equals(PIXEL_GRAY_LABEL) ? PIXEL_GRAY_K
            : id.equals(PIXEL_RED_LABEL) ? PIXEL_RED_K : id;
        try (InputStream stream = KushAssets.class.getResourceAsStream("/assets/" + resource.method_12836() + "/" + resource.method_12832())) {
            if (stream == null) throw new IllegalArgumentException("Missing original PNG " + resource);
            source = ImageIO.read(stream);
        }
        if (source == null) throw new IllegalArgumentException("Invalid original PNG " + resource);
        if (id.equals(LABEL_K) || id.equals(PIXEL_RED_LABEL) || id.equals(PIXEL_GRAY_LABEL))
            source = KushImageResampler.trimAlpha(source);
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
        long pixels = (long)source.getWidth()*source.getHeight();
        PIXELS.put(key,pixels);
        cachedPixels += pixels;
        while (SCALED.size() > CACHE_LIMIT || (cachedPixels > CACHE_PIXEL_LIMIT && SCALED.size()>1)) {
            String oldest = SCALED.keySet().iterator().next();
            class_2960 previous = SCALED.remove(oldest);
            cachedPixels -= PIXELS.getOrDefault(oldest,0L);
            PIXELS.remove(oldest);
            if (previous.method_12832().contains("filtered_")) class_310.method_1551().method_1531().method_4615(previous);
        }
        return id;
    }
    public static void backdrop(class_332 g, int w, int h) {
        if (class_310.method_1551().field_1687 != null) return;
        menuLandscape(g, w, h);
        g.method_25294(0, 0, w, h, 0x9910090D);
    }
    public static void menuLandscape(class_332 g, int w, int h) {
        double scale = Math.max(w / 1920.0, h / 1017.0);
        int bw = (int)Math.ceil(1920 * scale), bh = (int)Math.ceil(1017 * scale);
        image(g, BACKGROUND, (w-bw)/2, (h-bh)/2, bw, bh, 1920, 1017, -1);
    }
    public static void glass(class_332 g, int x, int y, int w, int h, int alpha) {
        // One fill: overlapping translucent rectangles darken their middle.
        g.method_25294(x, y, x+w, y+h, FastClientUI.fade(0xA0141115, alpha));
        FastClientUI.outline(g, x, y, w, h, FastClientUI.fade(0x806C303C, alpha));
    }
}

