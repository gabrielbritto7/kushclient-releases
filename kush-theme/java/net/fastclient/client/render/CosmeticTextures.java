/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1011
 *  net.minecraft.class_1043
 *  net.minecraft.class_1044
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.fastclient.client.render;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.client.gui.CosmeticImageDecode;
import net.fastclient.core.equip.CosmeticAssetWork;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public final class CosmeticTextures {
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"fastclientcore");
    private static final int MS_PER_TICK = 50;
    private static final Map<String,Long> PIXELS = new HashMap<>();
    private static long cachedPixels;
    private static final Map<String, class_2960> READY = new java.util.LinkedHashMap<String, class_2960>(64,.75f,true);
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Map<String, Animation> ANIMATED = new ConcurrentHashMap<String, Animation>();
    private static final Map<String, String> SANITIZED = new HashMap<String, String>();
    private static final Map<byte[], String> HASHES = new WeakHashMap<byte[], String>();

    private CosmeticTextures() {
    }

    public static class_2960 get(String cosmeticId, byte[] png) {
        return CosmeticTextures.get(cosmeticId, png, 1, 0);
    }

    public static class_2960 get(String cosmeticId, byte[] png, int aspectRatio, int frameDelayMs) {
        if (png == null) {
            return null;
        }
        var provider=net.fastclient.core.equip.KushCatalogProvider.current();
        int[] metadata=provider==null?null:provider.animation(cosmeticId);
        if(metadata!=null){aspectRatio=-metadata[0];frameDelayMs=metadata[1];}
        final int ratio=aspectRatio,delay=frameDelayMs;
        String key = CosmeticTextures.sanitize(cosmeticId) + "-" + CosmeticTextures.hash(png) + "-a" + aspectRatio + "-d" + frameDelayMs;
        class_2960 ready = READY.get(key);
        if (ready != null) {
            return ready;
        }
        if (!CosmeticAssetWork.isCoolingDown(key) && PENDING.add(key) && !CosmeticImageDecode.submit(png, image -> CosmeticTextures.create(key, cosmeticId, image, ratio, delay), failure -> CosmeticTextures.failed(key, failure))) {
            PENDING.remove(key);
        }
        return null;
    }

    public static void tick() {
        if (ANIMATED.isEmpty()) {
            return;
        }
        for (Animation animation : ANIMATED.values()) {
            animation.tick();
        }
    }

    private static void create(String key, String cosmeticId, class_1011 master, int aspectRatio, int frameDelayMs) {
        try {
            int width = master.method_4307();
            int height = master.method_4323();
            int frames = aspectRatio<0?-aspectRatio:width>0?Math.max(1,aspectRatio*height/width):1;
            if(frames<1 || height%frames!=0)throw new IllegalArgumentException("Animation frame layout");
            long pixels=(long)width*height+(frames>1 && frameDelayMs>0?(long)width*(height/frames):0);
            if(pixels>16_777_216L)throw new IllegalArgumentException("Cosmetic texture memory budget");
            while(!READY.isEmpty() && (READY.size()>=64 || cachedPixels+pixels>16_777_216L)) {
                String old=READY.keySet().iterator().next();
                class_310.method_1551().method_1531().method_4615(READY.remove(old));
                Animation animation=ANIMATED.remove(old);if(animation!=null)animation.master.close();
                cachedPixels-=PIXELS.getOrDefault(old,0L);PIXELS.remove(old);
            }
            class_2960 id = class_2960.method_60655((String)"fastclientcore", (String)("dynamic/" + key));
            if (frames > 1) {
                int frameHeight = height / frames;
                class_1011 frame = new class_1011(width, frameHeight, false);
                CosmeticTextures.copySlice(master, frame, 0, frameHeight, width);
                class_1043 texture = new class_1043(() -> "fastclientcore/" + key, frame);
                class_310.method_1551().method_1531().method_4616(id, (class_1044)texture);
                if (frameDelayMs > 0) {
                    int framesPerStep = Math.max(1, Math.round((float)frameDelayMs / 50.0f));
                    ANIMATED.put(key, new Animation(master, texture, width, frameHeight, frames, framesPerStep));
                    LOGGER.info("Registered animated cosmetic texture {} \u2014 {} frames @ {}ms ({} ticks/frame)", new Object[]{id, frames, frameDelayMs, framesPerStep});
                } else {
                    master.close();
                    LOGGER.info("Registered cosmetic texture {} \u2014 {}x{} film-strip of {} frames with no frame delay, pinned to frame 0 (set the flag bits to animate it)", new Object[]{id, width, height, frames});
                }
            } else {
                class_1043 texture = new class_1043(() -> "fastclientcore/" + key, master);
                class_310.method_1551().method_1531().method_4616(id, (class_1044)texture);
                LOGGER.debug("Uploaded cosmetic texture {} ({}x{})", new Object[]{id, width, height});
            }
            READY.put(key, id);PIXELS.put(key,pixels);cachedPixels+=pixels;
            PENDING.remove(key);
        }
        catch (Exception e) {
            master.close();
            CosmeticTextures.failed(key, e);
        }
    }

    private static void failed(String key, Exception failure) {
        CosmeticAssetWork.failed(key);
        PENDING.remove(key);
        LOGGER.warn("Cosmetic texture preparation failed ({})", (Object)failure.getClass().getSimpleName());
    }

    private static void copySlice(class_1011 master, class_1011 dest, int frame, int frameHeight, int width) {
        int srcY0 = frame * frameHeight;
        for (int y = 0; y < frameHeight; ++y) {
            for (int x = 0; x < width; ++x) {
                dest.method_61941(x, y, master.method_61940(x, srcY0 + y));
            }
        }
    }

    private static String sanitize(String id) {
        String cached = SANITIZED.get(id);
        if (cached != null) {
            return cached;
        }
        StringBuilder sb = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c = (char)(c + 32);
            }
            boolean safe = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '/' || c == '.' || c == '_' || c == '-';
            sb.append(safe ? c : (char)'_');
        }
        String result = sb.toString();
        SANITIZED.put(id, result);
        return result;
    }

    private static String hash(byte[] data) {
        String result;
        String cached = HASHES.get(data);
        if (cached != null) {
            return cached;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; ++i) {
                sb.append(String.format(Locale.ROOT, "%02x", digest[i]));
            }
            result = sb.toString();
        }
        catch (Exception e) {
            result = Integer.toHexString(Arrays.hashCode(data));
        }
        HASHES.put(data, result);
        return result;
    }

    @Environment(value=EnvType.CLIENT)
    private static final class Animation {
        private final class_1011 master;
        private final class_1043 texture;
        private final int width;
        private final int frameHeight;
        private final int frames;
        private final int framesPerStep;
        private int tickCounter;
        private int frame;

        Animation(class_1011 master, class_1043 texture, int width, int frameHeight, int frames, int framesPerStep) {
            this.master = master;
            this.texture = texture;
            this.width = width;
            this.frameHeight = frameHeight;
            this.frames = frames;
            this.framesPerStep = framesPerStep;
        }

        void tick() {
            if (++this.tickCounter < this.framesPerStep) {
                return;
            }
            this.tickCounter = 0;
            this.frame = (this.frame + 1) % this.frames;
            class_1011 backing = this.texture.method_4525();
            if (backing == null) {
                return;
            }
            CosmeticTextures.copySlice(this.master, backing, this.frame, this.frameHeight, this.width);
            this.texture.method_4524();
        }
    }
}

