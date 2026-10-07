package net.fastclient.hud.gui;

import java.io.InputStream;
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
    private static boolean grayInitialized;
    private static class_2960 grayTexture;
    private KushAssets() {}
    private static class_2960 texture(String file) {
        return class_2960.method_60655("fastclient-hud", "textures/gui/kush/" + file);
    }
    public static void image(class_332 g, class_2960 id, int x, int y, int w, int h, int tw, int th, int color) {
        if (w > 0 && h > 0 && (color >>> 24) != 0)
            g.method_25293(class_10799.field_56883, id, x, y, 0, 0, w, h, tw, th, tw, th, color);
    }
    /** Desaturated UI state generated once in native texture memory. */
    public static class_2960 grayWordmark() {
        if (!grayInitialized) {
            grayInitialized = true;
            try (InputStream stream = KushAssets.class.getResourceAsStream("/assets/fastclient-hud/textures/gui/kush/wordmark.png");
                 class_1011 source = class_1011.method_4309(stream)) {
                class_1011 gray = new class_1011(source.method_4307(), source.method_4323(), false);
                for (int y = 0; y < source.method_4323(); y++) for (int x = 0; x < source.method_4307(); x++) {
                    int argb = source.method_61940(x, y);
                    int luma = Math.min(255, Math.round(1.3f * (0.2126f * ((argb >>> 16) & 255) + 0.7152f * ((argb >>> 8) & 255) + 0.0722f * (argb & 255))));
                    gray.method_61941(x, y, (argb & 0xFF000000) | luma << 16 | luma << 8 | luma);
                }
                class_1043 texture = new class_1043(() -> "Kush UI desaturated state", gray);
                class_310.method_1551().method_1531().method_4616(GRAY_WORDMARK, texture);
                grayTexture = GRAY_WORDMARK;
            } catch (Exception error) {
                System.err.println("[KushMod] Unable to prepare UI icon: " + error.getMessage());
                grayTexture = WHITE_K;
            }
        }
        return grayTexture;
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
