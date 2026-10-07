/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_310
 *  net.minecraft.class_642
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.fastclient.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fastclient.core.account.SocialNotifications;
import net.fastclient.core.cache.PlayerCosmeticCache;
import net.fastclient.core.data.PlayerCosmetics;
import net.fastclient.core.equip.LocalCosmetics;
import net.fastclient.core.presence.PresenceTracker;
import net.fastclient.core.provider.CosmeticProvider;
import net.fastclient.core.equip.KushCatalogProvider;
import net.fastclient.core.session.HudSession;
import net.minecraft.class_310;
import net.minecraft.class_642;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public final class FastClientCoreClient
implements ClientModInitializer {
    public static final String MOD_ID = "fastclientcore";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"fastclientcore");
    private static final String DEFAULT_BACKEND_URL = "https://kush-archives.com.br";
    private static final int EMOTE_KEY = 71;
    private static CosmeticProvider provider;
    private static PlayerCosmeticCache cache;
    private static LocalCosmetics localEquip;
    private static PresenceTracker presence;
    private static HudSession hudSession;
    private static boolean emoteKeyWasDown;
    private static final ThreadLocal<Boolean> COSMETIC_PREVIEW_RENDER;

    public static String storefrontApiBaseUrl() {
        return KushCatalogProvider.site();
    }

    private static String resolveBackendUrl(Path modConfigDir) {
        String property = System.getProperty("fastclient.backend.url");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        Path file = modConfigDir.resolve("backend_url.txt");
        if (Files.isRegularFile(file, new LinkOption[0])) {
            try {
                String url = Files.readString(file).trim();
                if (!url.isBlank()) {
                    return url;
                }
            }
            catch (IOException e) {
                LOGGER.warn("Failed to read backend URL from {}", (Object)file, (Object)e);
            }
        }
        return DEFAULT_BACKEND_URL;
    }

    public static PlayerCosmeticCache cache() {
        return cache;
    }

    public static LocalCosmetics localEquip() {
        return localEquip;
    }

    public static PlayerCosmetics localCosmeticsForRender() {
        return Boolean.TRUE.equals(COSMETIC_PREVIEW_RENDER.get()) ? localEquip.forPreview() : localEquip.forRender();
    }

    public static void beginCosmeticPreviewRender() {
        COSMETIC_PREVIEW_RENDER.set(true);
    }

    public static void endCosmeticPreviewRender() {
        COSMETIC_PREVIEW_RENDER.remove();
    }

    public static PresenceTracker presence() {
        return presence;
    }

    public static boolean canEquipOwn() {
        return provider != null && provider.canEquipOwn();
    }

    public static void replaceAccountSession(HudSession replacement) {
        class_310 client = class_310.method_1551();
        if (!client.method_18854() || client.field_1687 != null || !replacement.matchesAccount(client.method_1548().method_44717())) {
            throw new IllegalStateException("HUD account replacement requires the selected disconnected identity");
        }
        if(hudSession!=null)hudSession.clear();
        cache.close();
        localEquip.close();
        presence.close();
        hudSession = replacement;
        Path assets = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("asset-cache");
        provider = new KushCatalogProvider(FabricLoader.getInstance().getConfigDir().resolve("kushmod/cosmetics"),client.method_1548().method_44717());
        cache = new PlayerCosmeticCache(provider);
        localEquip = new LocalCosmetics(provider, true);
        presence = new PresenceTracker(provider);
        COSMETIC_PREVIEW_RENDER.remove();
        emoteKeyWasDown = false;
        cache.get(client.method_1548().method_44717(), client.method_1548().method_1676());
    }

    public static boolean usesAccountSession(HudSession expected) {
        return hudSession == expected;
    }

    public static boolean canRenderOwnAppearance() {
        return true;
    }

    public static boolean isActiveName(String name) {
        return cache != null && cache.isActiveName(name);
    }

    public static boolean isActive(UUID uuid, String name) {
        if (cache == null || uuid == null) {
            return false;
        }
        cache.get(uuid, name);
        return cache.isActive(uuid);
    }

    public static PlayerCosmeticCache.AppearanceUrls appearance(String name) {
        return cache == null ? PlayerCosmeticCache.AppearanceUrls.empty() : cache.appearance(name);
    }

    public static PlayerCosmeticCache.AppearanceUrls appearance(UUID uuid, String name) {
        if (cache == null || uuid == null) {
            return PlayerCosmeticCache.AppearanceUrls.empty();
        }
        cache.get(uuid, name);
        return cache.appearance(uuid);
    }

    public void onInitializeClient() {

        Path modConfigDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        String backendUrl = KushCatalogProvider.site();
        provider = new KushCatalogProvider(FabricLoader.getInstance().getConfigDir().resolve("kushmod/cosmetics"),class_310.method_1551().method_1548().method_44717());
        cache = new PlayerCosmeticCache(provider);
        localEquip = new LocalCosmetics(provider, true);
        presence = new PresenceTracker(provider);
        Thread restore=new Thread(()->{
            provider.catalog();
            class_310 client=class_310.method_1551();
            PlayerCosmetics look=provider.fetch(client.method_1548().method_44717(),client.method_1548().method_1676());
            client.method_18859(()->localEquip.syncFromBackend(look));
        },"Kush-Catalog-Restore");
        restore.setDaemon(true);restore.start();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.field_1687!=null || client.field_1755 instanceof net.fastclient.client.gui.CosmeticsScreen)
                net.fastclient.client.render.CosmeticTextures.tick();
            cache.setWatching(client.field_1687 != null && client.method_1569());
            if (client.field_1724 == null) {
                presence.tick(null, null, false, null);
            } else {
                class_642 server = client.method_1558();
                presence.tick(client.field_1724.method_5667(), client.method_1548().method_1676(), client.method_1548().method_44717().version() == 4, server != null ? server.field_3761 : null);
            }
        });
        LOGGER.info("Kush cosmetics enabled for 1.21.11; public catalog {}", (Object)backendUrl);
    }

    static {
        COSMETIC_PREVIEW_RENDER = ThreadLocal.withInitial(() -> false);
    }
}

