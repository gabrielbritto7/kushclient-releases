package dev.kush.capture;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public final class CaptureDriver implements ClientModInitializer {
    private enum State {
        WAIT_TITLE, CAPTURE_MAIN, SET_MAIN_HOVER, CAPTURE_MAIN_HOVER, SET_TOOL_HOVER, CAPTURE_TOOL_HOVER, OPEN_MODS, CAPTURE_ALL,
        SET_HUD, CAPTURE_HUD, SET_RENDER, CAPTURE_RENDER,
        SET_MOVEMENT, CAPTURE_MOVEMENT, SET_PLAYER, CAPTURE_PLAYER,
        SET_UTILITY, CAPTURE_UTILITY, OPEN_OVERLAY, CAPTURE_OVERLAY,
        OPEN_VANILLA, CAPTURE_VANILLA, OPEN_INSTALLED, CAPTURE_INSTALLED, OPEN_CONFIG, CAPTURE_CONFIG, ENABLE_CONFIG, CAPTURE_ACTIVE_CONFIG, OPEN_DROPDOWN, CAPTURE_DROPDOWN, OPEN_LONG_CONFIG, CAPTURE_LONG_CONFIG,
        START_DEMO, WAIT_WORLD, PRESS_INPUT, CAPTURE_WORLD, OPEN_WORLD_OVERLAY, CAPTURE_WORLD_OVERLAY,
        OPEN_WORLD_CONFIG, CAPTURE_WORLD_CONFIG, SCROLL_KEYSTROKES, CAPTURE_KEYSTROKES_MORE, OPEN_ARMOR_CONFIG, CAPTURE_ARMOR_CONFIG, OPEN_PAUSE, CAPTURE_PAUSE, OPEN_RESET, CAPTURE_RESET, OPEN_PRESETS, CAPTURE_PRESETS, SET_EN, CAPTURE_PRESETS_EN, OPEN_CONFIG_EN, CAPTURE_CONFIG_EN, OPEN_COSMETICS, WAIT_CATALOG, CAPTURE_COSMETICS, TRY_CAPE, WAIT_CAPE, CAPTURE_CAPE, EQUIP_CAPE, WAIT_EQUIP, CAPTURE_EQUIP, TRY_COSMETICA, WAIT_COSMETICA, CAPTURE_COSMETICA, TRY_ACCESSORIES, WAIT_ACCESSORIES, CAPTURE_ACCESSORIES, SET_ACCESSORIES_FRONT, CAPTURE_ACCESSORIES_FRONT, REQUEST_PAGE2, WAIT_PAGE2, CAPTURE_PAGE2, VERIFY_PERSISTENCE, VERIFY_CACHED_RESTART, OPEN_CPS_RGB, CAPTURE_CPS_RGB, OPEN_KEYS_RGB, CAPTURE_KEYS_RGB, OPEN_PHYSICS, CAPTURE_PHYSICS, SET_PHYSICS_EN, CAPTURE_PHYSICS_EN, PREPARE_CLOTH, WAIT_CLOTH, CLOTH_FRAMES, PHYSICS_OFF, CAPTURE_PHYSICS_OFF, DONE
    }
    private static final boolean ACCESSORIES_ONLY="1".equals(System.getenv("KUSH_CAPTURE_ACCESSORIES_ONLY"));
    private static State state = State.WAIT_TITLE;
    private static int waitTicks;
    private static int stableTicks;
    private static boolean capturing;
    private static int clothFrame;
    private static float[] clothFirst;
    private static volatile boolean cacheVerified;
    private static volatile Throwable cacheFailure;

    public void onInitializeClient() {
        String loaded = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("fastclient-hud")
            .orElseThrow().getMetadata().getVersion().getFriendlyString();
        if (!loaded.equals("1.0.72+kush.0.3.9")) throw new IllegalStateException("Wrong test JAR: " + loaded);
        System.out.println("[KushCapture] loaded KushMod=" + loaded);
        ClientTickEvents.END_CLIENT_TICK.register(CaptureDriver::tick);
        System.out.println("[KushCapture] initialized");
        for(String name:new String[]{"net.fastclient.core.equip.CosmeticsAvailability","net.fastclient.hud.web.StoreAvailability"}) {
            try {Class<?> c=Class.forName(name);Field flag=c.getDeclaredField("AVAILABLE");flag.setAccessible(true);
                System.out.println("[KushCapture] "+name+" AVAILABLE="+flag.get(null));}
            catch(Exception missing){System.out.println("[KushCapture] availability check: "+missing);}
        }
    }

    private static void tick(Minecraft client) {
        try {
            if (capturing) return;
            if (waitTicks > 0) { waitTicks--; return; }
            switch (state) {
                case WAIT_TITLE -> {
                    if (client.screen instanceof TitleScreen) {
                        if (++stableTicks >= 80) state = State.CAPTURE_MAIN;
                    } else stableTicks = 0;
                }
                case CAPTURE_MAIN -> capture(client, "01-main-menu", ACCESSORIES_ONLY?State.START_DEMO:State.SET_TOOL_HOVER);
                case SET_MAIN_HOVER -> { cursor(client,640,327);waitTicks=35;state=State.CAPTURE_MAIN_HOVER; }
                case CAPTURE_MAIN_HOVER -> capture(client,"16-main-menu-hover",State.SET_TOOL_HOVER);
                case SET_TOOL_HOVER -> { cursor(client,1046,42);waitTicks=35;state=State.CAPTURE_TOOL_HOVER; }
                case CAPTURE_TOOL_HOVER -> capture(client,"17-toolbar-hover",State.OPEN_MODS);
                case OPEN_MODS -> { cursor(client,20,680);open(client, "net.fastclient.hud.gui.screens.ClickGUIScreen"); waitTicks=60; state=State.CAPTURE_ALL; }
                case CAPTURE_ALL -> { category(client, null); capture(client, "02-kush-mods-all", State.SET_HUD); }
                case SET_HUD -> setCat(client,"HUD",State.CAPTURE_HUD);
                case CAPTURE_HUD -> capture(client,"03-kush-mods-hud",State.SET_RENDER);
                case SET_RENDER -> setCat(client,"RENDER",State.CAPTURE_RENDER);
                case CAPTURE_RENDER -> capture(client,"04-kush-mods-render",State.SET_MOVEMENT);
                case SET_MOVEMENT -> setCat(client,"MOVEMENT",State.CAPTURE_MOVEMENT);
                case CAPTURE_MOVEMENT -> capture(client,"05-kush-mods-movement",State.SET_PLAYER);
                case SET_PLAYER -> setCat(client,"PLAYER",State.CAPTURE_PLAYER);
                case CAPTURE_PLAYER -> capture(client,"06-kush-mods-player",State.SET_UTILITY);
                case SET_UTILITY -> setCat(client,"UTILITY",State.CAPTURE_UTILITY);
                case CAPTURE_UTILITY -> capture(client,"07-kush-mods-utility",State.OPEN_OVERLAY);
                case OPEN_OVERLAY -> { open(client,"net.fastclient.hud.gui.screens.HudOverlayScreen"); waitTicks=60; state=State.CAPTURE_OVERLAY; }
                case CAPTURE_OVERLAY -> capture(client,"08-right-shift",State.OPEN_VANILLA);
                case OPEN_VANILLA -> {
                    Class<?> pref=Class.forName("net.fastclient.hud.launcher.LauncherSkinPreference");
                    Method is=pref.getDeclaredMethod("isFastClientSkinEnabled");
                    if ((boolean)is.invoke(null)) pref.getDeclaredMethod("toggle").invoke(null);
                    client.setScreen(new TitleScreen()); waitTicks=80; state=State.CAPTURE_VANILLA;
                }
                case CAPTURE_VANILLA -> capture(client,"09-minecraft-ui",State.OPEN_INSTALLED);

                case OPEN_INSTALLED -> {
                    Class<?> c=Class.forName("net.fastclient.hud.gui.screens.KushInstalledModsScreen");
                    client.setScreen((Screen)c.getConstructor(Screen.class).newInstance(new TitleScreen()));
                    waitTicks=45;state=State.CAPTURE_INSTALLED;
                }
                case CAPTURE_INSTALLED -> capture(client,"10-installed-mods",State.OPEN_CONFIG);
                case OPEN_CONFIG -> { setFpsEnabled(false);openConfig(client);waitTicks=45;state=State.CAPTURE_CONFIG; }
                case CAPTURE_CONFIG -> capture(client,"11-module-settings",State.ENABLE_CONFIG);
                case ENABLE_CONFIG -> { setFpsEnabled(true);openConfig(client);waitTicks=45;state=State.CAPTURE_ACTIVE_CONFIG; }
                case CAPTURE_ACTIVE_CONFIG -> capture(client,"20-active-module-header",State.OPEN_DROPDOWN);
                case OPEN_DROPDOWN -> { expandDropdown(client);waitTicks=40;state=State.CAPTURE_DROPDOWN; }
                case CAPTURE_DROPDOWN -> capture(client,"18-dropdown-expanded",State.OPEN_LONG_CONFIG);
                case OPEN_LONG_CONFIG -> {
                    Object chosen = null;
                    for(Object mod:(java.util.List<?>)manager().getClass().getMethod("getModules").invoke(manager())) {
                        String name = mod.getClass().getMethod("getDisplayName").invoke(mod).toString();
                        if(chosen == null || name.length() > chosen.getClass().getMethod("getDisplayName").invoke(chosen).toString().length()) chosen = mod;
                    }
                    Class<?> c=Class.forName("net.fastclient.hud.gui.screens.ModuleConfigScreen");
                    client.setScreen((Screen)c.getConstructor(Class.forName("net.fastclient.hud.modules.Module"),Screen.class).newInstance(chosen,client.screen));
                    waitTicks=45;state=State.CAPTURE_LONG_CONFIG;
                }
                case CAPTURE_LONG_CONFIG -> capture(client,"19-long-module-header",State.START_DEMO);
                case START_DEMO -> {
                    Class<?> pref=Class.forName("net.fastclient.hud.launcher.LauncherSkinPreference");
                    if((boolean)pref.getDeclaredMethod("isFastClientSkinEnabled").invoke(null))pref.getDeclaredMethod("toggle").invoke(null);
                    client.setScreen(new TitleScreen());
                    waitTicks=60;state=State.WAIT_WORLD;stableTicks=0;
                }
                case WAIT_WORLD -> {
                    if(client.level!=null && client.player!=null){
                        Class<?> pref=Class.forName("net.fastclient.hud.launcher.LauncherSkinPreference");
                        if(!(boolean)pref.getDeclaredMethod("isFastClientSkinEnabled").invoke(null))pref.getDeclaredMethod("toggle").invoke(null);
                        enableHud();client.setScreen(null);equipTestArmor(client);waitTicks=120;state=State.PRESS_INPUT;
                    } else {
                        if(stableTicks++==0)pressDemo(client);
                        if(stableTicks>1800)throw new IllegalStateException("Demo test world did not load: "+client.screen);
                    }
                }
                case PRESS_INPUT -> {
                    if(client.screen!=null){client.setScreen(null);waitTicks=40;return;}
                    Class<?> tracker=Class.forName("net.fastclient.hud.gui.KushModernKeys");tracker.getMethod("clear").invoke(null);
                    Method mouse=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onButton",long.class,net.minecraft.client.input.MouseButtonInfo.class,int.class);mouse.setAccessible(true);
                    for(int button=0;button<2;button++)for(int i=0;i<(button==0?7:3);i++){
                        mouse.invoke(client.mouseHandler,client.getWindow().handle(),new net.minecraft.client.input.MouseButtonInfo(button,0),1);
                        mouse.invoke(client.mouseHandler,client.getWindow().handle(),new net.minecraft.client.input.MouseButtonInfo(button,0),0);
                    }
                    if((int)tracker.getMethod("cps",int.class).invoke(null,0)!=7 || (int)tracker.getMethod("cps",int.class).invoke(null,1)!=3)throw new IllegalStateException("Rapid click events were missed");
                    client.options.keyUp.setDown(true);waitTicks=2;state=State.CAPTURE_WORLD;
                    System.out.println("[KushCapture] native mouse callback counted 7 left / 3 right rapid clicks; armor and WASD enabled");
                }
                case CAPTURE_WORLD -> {
                    if(client.screen!=null && client.screen.getClass().getName().endsWith("DemoIntroScreen")) {
                        client.setScreen(null);waitTicks=40;
                    } else {capture(client,"12-hud-in-game",ACCESSORIES_ONLY?State.OPEN_COSMETICS:State.OPEN_WORLD_OVERLAY);client.options.keyUp.setDown(false);}
                }
                case OPEN_WORLD_OVERLAY -> {
                    Class<?> tracker=Class.forName("net.fastclient.hud.gui.KushModernKeys");
                    if((int)tracker.getMethod("cps",int.class).invoke(null,0)!=0 || (int)tracker.getMethod("cps",int.class).invoke(null,1)!=0)throw new IllegalStateException("CPS did not expire after one second");
                    System.out.println("[KushCapture] left and right CPS expired to zero after one-second window");
                    open(client,"net.fastclient.hud.gui.screens.HudOverlayScreen");waitTicks=50;state=State.CAPTURE_WORLD_OVERLAY;}
                case CAPTURE_WORLD_OVERLAY -> capture(client,"13-right-shift-in-game",State.OPEN_WORLD_CONFIG);
                case OPEN_WORLD_CONFIG -> {openKeystrokesConfig(client);waitTicks=50;state=State.CAPTURE_WORLD_CONFIG;}
                case CAPTURE_WORLD_CONFIG -> capture(client,"14-glass-in-game",State.SCROLL_KEYSTROKES);
                case SCROLL_KEYSTROKES -> {scrollConfigToBottom(client);waitTicks=50;state=State.CAPTURE_KEYSTROKES_MORE;}
                case CAPTURE_KEYSTROKES_MORE -> capture(client,"33-keystrokes-cps-settings",State.OPEN_ARMOR_CONFIG);
                case OPEN_ARMOR_CONFIG -> {
                    Object armor=((java.util.List<?>)manager().getClass().getMethod("getModules").invoke(manager())).stream().filter(e->e.getClass().getSimpleName().equals("ArmorHUD")).findFirst().orElseThrow();
                    Class<?> screen=Class.forName("net.fastclient.hud.gui.screens.ModuleConfigScreen");
                    client.setScreen((Screen)screen.getConstructor(Class.forName("net.fastclient.hud.modules.Module"),Screen.class).newInstance(armor,client.screen));
                    scrollConfigToBottom(client);waitTicks=50;state=State.CAPTURE_ARMOR_CONFIG;
                }
                case CAPTURE_ARMOR_CONFIG -> capture(client,"32-armorstatus-settings",State.OPEN_PAUSE);
                case OPEN_PAUSE -> {client.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));waitTicks=50;state=State.CAPTURE_PAUSE;}
                case CAPTURE_PAUSE -> capture(client,"15-pause-menu",State.OPEN_RESET);
                case OPEN_RESET -> {open(client,"net.fastclient.hud.gui.screens.ClickGUIScreen");dialog(client,"RESET");waitTicks=50;state=State.CAPTURE_RESET;}
                case CAPTURE_RESET -> capture(client,"21-reset-pt",State.OPEN_PRESETS);
                case OPEN_PRESETS -> {dialog(client,"PRESETS");waitTicks=30;state=State.CAPTURE_PRESETS;}
                case CAPTURE_PRESETS -> capture(client,"22-presets-pt",State.SET_EN);
                case SET_EN -> {language(false);waitTicks=30;state=State.CAPTURE_PRESETS_EN;}
                case CAPTURE_PRESETS_EN -> capture(client,"23-presets-en",State.OPEN_CONFIG_EN);
                case OPEN_CONFIG_EN -> {openConfig(client);waitTicks=40;state=State.CAPTURE_CONFIG_EN;}
                case CAPTURE_CONFIG_EN -> capture(client,"24-module-en",State.OPEN_COSMETICS);
                case OPEN_COSMETICS -> {
                    for(var slot:net.minecraft.world.entity.EquipmentSlot.values())client.player.setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);
                    if(ACCESSORIES_ONLY)language(false);language(true);open(client,"net.fastclient.client.gui.CosmeticsScreen");stableTicks=0;state=State.WAIT_CATALOG;}
                case WAIT_CATALOG -> {
                    if(!catalog().isEmpty()){waitTicks=80;state=State.CAPTURE_COSMETICS;}
                    else if(stableTicks++>600)throw new IllegalStateException("Kush site catalog never loaded");
                }
                case CAPTURE_COSMETICS -> {
                    Field thumbs=Class.forName("net.fastclient.client.gui.KushCatalogThumbnails").getDeclaredField("READY");thumbs.setAccessible(true);
                    java.util.Map<?,?> ready=(java.util.Map<?,?>)thumbs.get(null);
                    if(ready.size()<12){if(stableTicks++>600)throw new IllegalStateException("Real catalog thumbnails never decoded");return;}
                    Field rows=client.screen.getClass().getDeclaredField("rowRects");rows.setAccessible(true);
                    java.util.List<?> cards=(java.util.List<?>)rows.get(client.screen);if(cards.size()<4)throw new IllegalStateException("Square cards were not laid out");
                    Object own=catalog().stream().filter(e->{try{return !e.getClass().getMethod("id").invoke(e).toString().startsWith("cosmetica-");}catch(Exception x){throw new RuntimeException(x);}}).findFirst().orElseThrow();
                    Object cardPreview=ready.get(own.getClass().getMethod("id").invoke(own));if(cardPreview==null || (int)cardPreview.getClass().getMethod("frames").invoke(cardPreview)!=12)throw new IllegalStateException("Own cape has no animated 3D card");
                    System.out.println("[KushCapture] 12-frame real UV cape card animation ready");
                    System.out.println("[KushCapture] real catalog previews decoded="+ready.size()+"; visible square cards="+cards.size());
                    capture(client,"25-kush-catalog",State.TRY_CAPE);
                }
                case TRY_CAPE -> {Object entry=catalog().get(0);
                    Field rows=client.screen.getClass().getDeclaredField("rowRects");rows.setAccessible(true);Object[] card=(Object[])((java.util.List<?>)rows.get(client.screen)).get(0);
                    client.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent((Integer)card[0]+(Integer)card[2]/2.0,(Integer)card[1]+(Integer)card[3]/2.0,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
                    Field chosen=client.screen.getClass().getDeclaredField("selectedEntry");chosen.setAccessible(true);
                    if(chosen.get(client.screen)==null || !chosen.get(client.screen).equals(entry))throw new IllegalStateException("Native grid card click did not select first cape");
                    if((boolean)local().getClass().getMethod("isActive",String.class,String.class).invoke(local(),"cape",entry.getClass().getMethod("id").invoke(entry)))throw new IllegalStateException("Card selection equipped automatically");
                    Field yaw=client.screen.getClass().getDeclaredField("previewYaw");yaw.setAccessible(true);yaw.setFloat(client.screen,140f);
                    stableTicks=0;state=State.WAIT_CAPE;}
                case WAIT_CAPE -> {
                    if(cape("forPreview")!=null){System.out.println("[KushCapture] real catalog cape decoded for native preview");waitTicks=60;state=State.CAPTURE_CAPE;}
                    else if(stableTicks++>600)throw new IllegalStateException("Catalog cape preview did not resolve");
                }
                case CAPTURE_CAPE -> capture(client,"26-cape-preview",State.EQUIP_CAPE);
                case EQUIP_CAPE -> {clickRect(client,"equipRect");stableTicks=0;state=State.WAIT_EQUIP;}
                case WAIT_EQUIP -> {
                    if(cape("forRender")!=null){System.out.println("[KushCapture] real cape equipped locally");waitTicks=80;state=State.CAPTURE_EQUIP;}
                    else if(stableTicks++>600)throw new IllegalStateException("Equipped cape did not resolve");
                }
                case CAPTURE_EQUIP -> {
                    Object entry=catalog().get(0);String id=entry.getClass().getMethod("id").invoke(entry).toString();
                    clickRect(client,"equipRect");
                    if((boolean)local().getClass().getMethod("isActive",String.class,String.class).invoke(local(),"cape",id))throw new IllegalStateException("Equip button failed to unequip");
                    clickRect(client,"equipRect");
                    if(!(boolean)local().getClass().getMethod("isActive",String.class,String.class).invoke(local(),"cape",id))throw new IllegalStateException("Equip button failed to equip again");
                    System.out.println("[KushCapture] card preview is separate from equipment; native Equip/Unequip button toggles correctly");
                    capture(client,"27-cape-equipped",State.TRY_COSMETICA);
                }
                case TRY_COSMETICA -> {
                    Object found=null;for(Object entry:catalog())if(entry.getClass().getMethod("id").invoke(entry).toString().equals("cosmetica-cAPe9")){found=entry;break;}
                    if(found==null)throw new IllegalStateException("Real Cosmetica animated cape missing");
                    selectEntry(client,found);stableTicks=0;state=State.WAIT_COSMETICA;
                }
                case WAIT_COSMETICA -> {
                    Object preview=cape("forPreview");
                    if(preview!=null && preview.getClass().getMethod("id").invoke(preview).equals("cosmetica-cAPe9")){
                        if(((Number)preview.getClass().getMethod("mspf").invoke(preview)).intValue()!=250)throw new IllegalStateException("Wrong Cosmetica animation speed");
                        System.out.println("[KushCapture] real Cosmetica animated cape decoded, six frames / 250ms");waitTicks=80;state=State.CAPTURE_COSMETICA;
                    }else if(stableTicks++>600)throw new IllegalStateException("Cosmetica cape did not resolve");
                }
                case CAPTURE_COSMETICA -> capture(client,"28-cosmetica-animated-cape",State.TRY_ACCESSORIES);
                case TRY_ACCESSORIES -> {
                    local().getClass().getMethod("clearPreview").invoke(local());
                    Object provider=Class.forName("net.fastclient.core.equip.KushCatalogProvider").getMethod("current").invoke(null);
                    java.util.List<?> list=(java.util.List<?>)provider.getClass().getMethod("loaded").invoke(provider);
                    for(String id:new String[]{"hQtyJ","7Qgsq","QNifj","NHpZe"}) {
                        Object item=list.stream().filter(e->{try{return e.getClass().getMethod("id").invoke(e).equals("cosmetica-"+id);}catch(Exception ex){throw new RuntimeException(ex);}}).findFirst().orElseThrow();
                        local().getClass().getMethod("toggle",item.getClass()).invoke(local(),item);
                    }
                    Object wing=list.stream().filter(e->{try{return e.getClass().getMethod("id").invoke(e).equals("cosmetica-NHpZe");}catch(Exception ex){throw new RuntimeException(ex);}}).findFirst().orElseThrow();selectEntry(client,wing);
                    Field category=client.screen.getClass().getDeclaredField("selected");category.setAccessible(true);category.setInt(client.screen,7);
                    stableTicks=0;state=State.WAIT_ACCESSORIES;
                }
                case WAIT_ACCESSORIES -> {
                    Object look=local().getClass().getMethod("forRender").invoke(local());
                    boolean ready=true;for(String slot:new String[]{"hats","face","arm","wings"}) {
                        java.util.List<?> records=(java.util.List<?>)look.getClass().getMethod(slot).invoke(look);if(records.size()!=1){ready=false;continue;}
                        Object item=records.get(0);String json=(String)item.getClass().getMethod("modelJson").invoke(item);
                        if(json==null || !json.contains("kushCosmetica"))throw new IllegalStateException("Missing accessory transform "+slot);
                        Object model=Class.forName("net.fastclient.client.render.CosmeticModel").getMethod("get",String.class,String.class).invoke(null,item.getClass().getMethod("id").invoke(item),json);
                        if(model==null){ready=false;continue;}
                    }
                    if(ready){System.out.println("[KushCapture] real halo + hat + 4-frame hand flames + 6-frame wings equipped together");waitTicks=120;state=State.CAPTURE_ACCESSORIES;}
                    else if(stableTicks++>600)throw new IllegalStateException("Accessory equip did not resolve");
                }
                case CAPTURE_ACCESSORIES -> {
                    Field field=Class.forName("net.fastclient.client.render.CosmeticTextures").getDeclaredField("ANIMATED");field.setAccessible(true);
                    java.util.Map<?,?> map=(java.util.Map<?,?>)field.get(null);
                    for(String[] spec:new String[][]{{"cosmetica-qnifj","4","3"},{"cosmetica-nhpze","6","4"}}){
                        Object animation=map.entrySet().stream().filter(e->e.getKey().toString().contains(spec[0])).map(java.util.Map.Entry::getValue).findFirst().orElseThrow();
                        Field frames=animation.getClass().getDeclaredField("frames"),delay=animation.getClass().getDeclaredField("framesPerStep");frames.setAccessible(true);delay.setAccessible(true);
                        if(frames.getInt(animation)!=Integer.parseInt(spec[1]) || delay.getInt(animation)!=Integer.parseInt(spec[2]))throw new IllegalStateException("Wrong accessory texture animation "+spec[0]);
                    }
                    Field thumbnails=Class.forName("net.fastclient.client.gui.KushCatalogThumbnails").getDeclaredField("READY");thumbnails.setAccessible(true);
                    Object wingThumb=((java.util.Map<?,?>)thumbnails.get(null)).get("cosmetica-NHpZe");
                    if(wingThumb==null)throw new IllegalStateException("Animated wing thumbnail missing");
                    int tw=(int)wingThumb.getClass().getMethod("width").invoke(wingThumb),th=(int)wingThumb.getClass().getMethod("height").invoke(wingThumb);
                    if(tw>128 || th>128 || tw<th/2)throw new IllegalStateException("Wing thumbnail still shows a film strip: "+tw+"x"+th);
                    System.out.println("[KushCapture] animated wing thumbnail is a complete native card preview: "+tw+"x"+th);
                    System.out.println("[KushCapture] GPU animations verified: hand 4 frames / 150ms and wings 6 frames / 200ms");
                    capture(client,"29-accessories-back",State.SET_ACCESSORIES_FRONT);
                }
                case SET_ACCESSORIES_FRONT -> {
                    Field yaw=client.screen.getClass().getDeclaredField("previewYaw");yaw.setAccessible(true);yaw.setFloat(client.screen,-25f);
                    waitTicks=45;state=State.CAPTURE_ACCESSORIES_FRONT;
                }
                case CAPTURE_ACCESSORIES_FRONT -> capture(client,"30-accessories-front",State.REQUEST_PAGE2);
                case REQUEST_PAGE2 -> {
                    Method method=client.screen.getClass().getDeclaredMethod("requestPage",int.class);method.setAccessible(true);method.invoke(client.screen,2);stableTicks=0;state=State.WAIT_PAGE2;
                }
                case WAIT_PAGE2 -> {
                    Field loading=client.screen.getClass().getDeclaredField("loadingRemote"),page=client.screen.getClass().getDeclaredField("remotePage");loading.setAccessible(true);page.setAccessible(true);
                    if(!loading.getBoolean(client.screen)){
                        if(page.getInt(client.screen)!=2)throw new IllegalStateException("Catalog page two did not load");
                        Object provider=Class.forName("net.fastclient.core.equip.KushCatalogProvider").getMethod("current").invoke(null);
                        java.util.Set<?> ids=(java.util.Set<?>)provider.getClass().getMethod("pageIds",String.class,String.class,int.class).invoke(provider,"wings","",2);
                        if(ids==null || ids.isEmpty() || ids.size()>20)throw new IllegalStateException("Invalid actual remote page");
                        System.out.println("[KushCapture] native page two searched and loaded "+ids.size()+" actual Cosmetica records");waitTicks=50;state=State.CAPTURE_PAGE2;
                    }else if(stableTicks++>600)throw new IllegalStateException("Remote pagination stuck");
                }
                case CAPTURE_PAGE2 -> capture(client,"31-catalog-page-two",State.VERIFY_PERSISTENCE);
                case VERIFY_PERSISTENCE -> {
                    Path dir=client.gameDirectory.toPath().resolve("config/kushmod/cosmetics");
                    try(var files=Files.list(dir)) {
                        Path loadout=files.filter(p->p.getFileName().toString().startsWith("loadout-")).findFirst().orElseThrow();
                        if(!Files.readString(loadout).contains("cosmetica-NHpZe") || !Files.readString(loadout).contains("cosmetica-QNifj"))throw new IllegalStateException("Cape selection was not persisted");
                    }
                    client.setScreen(null);
                    if((boolean)local().getClass().getMethod("hasPreview").invoke(local()))throw new IllegalStateException("Preview leaked after screen close");
                    if(cape("forRender")==null)throw new IllegalStateException("Equipped cape lost on screen close");
                    String saved=Files.readString(client.gameDirectory.toPath().resolve("config/kushmod/language.txt"));
                    if(!saved.equals("pt_br"))throw new IllegalStateException("Language preference not persisted");
                    System.out.println("[KushCapture] verified loadout persistence, preview cleanup and PT/EN preference");
                    Thread worker=new Thread(()->{try {
                        Class<?> type=Class.forName("net.fastclient.core.equip.KushCatalogProvider");
                        Object restored=type.getConstructor(Path.class,java.util.UUID.class).newInstance(dir,client.getUser().getProfileId());
                        long start=System.nanoTime();java.util.List<?> entries=(java.util.List<?>)type.getMethod("catalog").invoke(restored);
                        if(entries.isEmpty() || System.nanoTime()-start>2_000_000_000L)throw new IllegalStateException("Cached catalog delayed by network");
                        Object look=type.getMethod("fetch",java.util.UUID.class,String.class).invoke(restored,client.getUser().getProfileId(),client.getUser().getName());
                        for(String slot:new String[]{"hats","face","arm","wings"})if(((java.util.List<?>)look.getClass().getMethod(slot).invoke(look)).size()!=1)throw new IllegalStateException("Cached restart lost "+slot);
                        if(look.getClass().getMethod("cape").invoke(look)==null)throw new IllegalStateException("Cached restart lost cape");
                        System.out.println("[KushCapture] fresh provider restored cape, halo, hat, arm and wings from disk cache");cacheVerified=true;
                    }catch(Throwable failure){cacheFailure=failure;}},"Kush-Cached-Restart-Test");worker.setDaemon(true);worker.start();stableTicks=0;state=State.VERIFY_CACHED_RESTART;
                }
                case VERIFY_CACHED_RESTART -> {if(cacheFailure!=null)throw new IllegalStateException("Cached restart failed",cacheFailure);if(cacheVerified)state=State.OPEN_CPS_RGB;else if(stableTicks++>600)throw new IllegalStateException("Cached restore timed out");}
                case OPEN_CPS_RGB -> {
                    Object m=module("CPSCounter");Object rgb=m.getClass().getMethod("getSetting",String.class).invoke(m,"rgb_border");
                    if((boolean)rgb.getClass().getMethod("isEnabled").invoke(rgb))throw new IllegalStateException("RGB must default to off");
                    setSetting(m,"rgb_border",true);openModule(client,m);waitTicks=35;state=State.CAPTURE_CPS_RGB;
                }
                case CAPTURE_CPS_RGB -> capture(client,"34-cps-rgb-settings",State.OPEN_KEYS_RGB);
                case OPEN_KEYS_RGB -> {Object m=module("Keystrokes");setSetting(m,"rgb_border",true);openModule(client,m);waitTicks=35;state=State.CAPTURE_KEYS_RGB;}
                case CAPTURE_KEYS_RGB -> capture(client,"35-wasd-rgb-settings",State.OPEN_PHYSICS);
                case OPEN_PHYSICS -> {
                    Object m=module("CapePhysics");if(!(boolean)m.getClass().getMethod("isEnabled").invoke(m))throw new IllegalStateException("Cape physics missing default");
                    if(((java.util.List<?>)manager().getClass().getMethod("getModules").invoke(manager())).size()!=44)throw new IllegalStateException("Original 43 modules were not retained plus new cape physics");
                    openModule(client,m);waitTicks=35;state=State.CAPTURE_PHYSICS;
                }
                case CAPTURE_PHYSICS -> capture(client,"36-cape-physics-settings-pt",State.SET_PHYSICS_EN);
                case SET_PHYSICS_EN -> {language(false);waitTicks=25;state=State.CAPTURE_PHYSICS_EN;}
                case CAPTURE_PHYSICS_EN -> capture(client,"37-cape-physics-settings-en",State.PREPARE_CLOTH);
                case PREPARE_CLOTH -> {
                    language(true);local().getClass().getMethod("clearPreview").invoke(local());local().getClass().getMethod("clear").invoke(local());
                    Object entry=catalog().stream().filter(e->{try{return e.getClass().getMethod("category").invoke(e).equals("cape")&&!e.getClass().getMethod("id").invoke(e).toString().startsWith("cosmetica-");}catch(Exception x){throw new RuntimeException(x);}}).findFirst().orElseThrow();
                    local().getClass().getMethod("toggle",entry.getClass()).invoke(local(),entry);
                    open(client,"net.fastclient.client.gui.CosmeticsScreen");selectEntry(client,entry);
                    Field yaw=client.screen.getClass().getDeclaredField("previewYaw");yaw.setAccessible(true);yaw.setFloat(client.screen,140f);
                    setSetting(module("CapePhysics"),"wind_strength",80.0);stableTicks=0;state=State.WAIT_CLOTH;
                }
                case WAIT_CLOTH -> {
                    if(cape("forRender")==null){if(stableTicks++>500)throw new IllegalStateException("Own cape missing");return;}
                    waitTicks=35;clothFrame=0;state=State.CLOTH_FRAMES;
                }
                case CLOTH_FRAMES -> {
                    Field field=Class.forName("net.fastclient.client.render.KushCapeRenderer").getDeclaredField("MOTION");field.setAccessible(true);
                    java.util.Map<?,?> motions=(java.util.Map<?,?>)field.get(null);float[] pose=null;
                    for(Object motion:motions.values()){Field preview=motion.getClass().getDeclaredField("preview"),points=motion.getClass().getDeclaredField("points");preview.setAccessible(true);points.setAccessible(true);if(preview.getBoolean(motion)&&points.get(motion)!=null)pose=(float[])points.get(motion);}
                    if(pose==null)throw new IllegalStateException("Native cape physics renderer did not submit a cloth mesh");
                    if(clothFrame==0)clothFirst=pose.clone();
                    if(clothFrame==9){boolean changed=false;for(int i=0;i<pose.length;i++)if(Math.abs(pose[i]-clothFirst[i])>.001)changed=true;if(!changed)throw new IllegalStateException("Cape mesh remained static");System.out.println("[KushCapture] real custom cape mesh changed across native frames; 44 modules preserved");}
                    String file=String.format("38-cape-motion-%02d",clothFrame++);capture(client,file,clothFrame>=10?State.PHYSICS_OFF:State.CLOTH_FRAMES);
                }
                case PHYSICS_OFF -> {module("CapePhysics").getClass().getMethod("setEnabled",boolean.class).invoke(module("CapePhysics"),false);waitTicks=30;state=State.CAPTURE_PHYSICS_OFF;}
                case CAPTURE_PHYSICS_OFF -> {
                    capture(client,"39-cape-physics-disabled",State.DONE);
                    module("CapePhysics").getClass().getMethod("setEnabled",boolean.class).invoke(module("CapePhysics"),true);
                    setSetting(module("CapePhysics"),"wind_strength",35.0);manager().getClass().getMethod("saveConfig").invoke(manager());
                    Path cfg=client.gameDirectory.toPath().resolve("config/fast-client-hud/modules.json");
                    String saved=Files.readString(cfg);if(!saved.contains("CapePhysics")||!saved.contains("rgb_border"))throw new IllegalStateException("New cape/RGB settings not saved");
                    System.out.println("[KushCapture] native enabled/disabled cape render path and persisted RGB/physics settings verified");
                }
                case DONE -> { System.out.println("[KushCapture] complete"); Thread.sleep(500); System.exit(0); }
            }
        } catch (Throwable t) { t.printStackTrace(); System.err.println("[KushCapture] failed state="+state); System.exit(2); }
    }


    private static void language(boolean portuguese)throws Exception {
        Class<?> c=Class.forName("net.fastclient.hud.gui.KushLanguage");
        if((boolean)c.getMethod("isPortuguese").invoke(null)!=portuguese)
            if(!(boolean)c.getMethod("toggle").invoke(null))throw new IllegalStateException("Language save failed");
        String expected=portuguese?"Ativo":"Enabled";
        if(!c.getMethod("translate",String.class).invoke(null,"Enabled").equals(expected))throw new IllegalStateException("Language translation failed");
    }
    private static void dialog(Minecraft client,String name)throws Exception {
        Field f=client.screen.getClass().getDeclaredField("actionDialog");f.setAccessible(true);
        f.set(client.screen,Enum.valueOf((Class)f.getType(),name));
    }
    private static Object local()throws Exception {return Class.forName("net.fastclient.client.FastClientCoreClient").getMethod("localEquip").invoke(null);}
    private static Object cape(String method)throws Exception {
        Object look=local().getClass().getMethod(method).invoke(local());return look.getClass().getMethod("cape").invoke(look);
    }
    private static java.util.List<?> catalog()throws Exception {
        Object cache=Class.forName("net.fastclient.client.FastClientCoreClient").getMethod("cache").invoke(null);
        return (java.util.List<?>)cache.getClass().getMethod("catalog").invoke(cache);
    }

    private static Object module(String name)throws Exception {Object m=manager().getClass().getMethod("getModule",String.class).invoke(manager(),name);if(m==null)throw new IllegalStateException("Missing module "+name);return m;}
    private static void setSetting(Object module,String name,Object value)throws Exception {Object setting=module.getClass().getMethod("getSetting",String.class).invoke(module,name);setting.getClass().getMethod("setValue",Object.class).invoke(setting,value);}
    private static void openModule(Minecraft client,Object module)throws Exception {Class<?> screen=Class.forName("net.fastclient.hud.gui.screens.ModuleConfigScreen");client.setScreen((Screen)screen.getConstructor(Class.forName("net.fastclient.hud.modules.Module"),Screen.class).newInstance(module,client.screen));}
    private static Object manager() throws Exception {
        Class<?> main=Class.forName("net.fastclient.hud.FastClientHUDClient");
        Object instance=main.getDeclaredMethod("getInstance").invoke(null);
        return main.getDeclaredMethod("getModuleManager").invoke(instance);
    }
    private static void cursor(Minecraft client,double x,double y) throws Exception {
        Object window=client.getWindow();
        // The GUI tick need not have the OpenGL context bound. Obtain the
        // native handle from the window itself, not glfwGetCurrentContext().
        for(Method m:window.getClass().getMethods()) {
            if(m.getParameterCount()==0 && m.getReturnType()==long.class && m.getDeclaringClass()==window.getClass()) {
                long handle=(long)m.invoke(window);
                if(handle!=0){org.lwjgl.glfw.GLFW.glfwSetCursorPos(handle,x,y);return;}
            }
        }
        throw new IllegalStateException("Native cursor window handle unavailable");
    }
    private static void expandDropdown(Minecraft client) throws Exception {
        Field components=client.screen.getClass().getDeclaredField("components");components.setAccessible(true);
        for(Object component:(java.util.List<?>)components.get(client.screen))if(component.getClass().getSimpleName().equals("Dropdown")) {
            Field expanded=component.getClass().getDeclaredField("expanded");expanded.setAccessible(true);expanded.setBoolean(component,true);
            System.out.println("[KushCapture] expanded settings dropdown");return;
        }
        throw new IllegalStateException("Settings dropdown missing");
    }
    private static void verifyTypeface(Minecraft client) throws Exception {
        String name=client.screen==null?"":client.screen.getClass().getName();
        boolean settings=name.endsWith("ClickGUIScreen")||name.endsWith("ModuleConfigScreen");
        Object actual=Class.forName("net.fastclient.hud.gui.FastClientFonts").getDeclaredMethod("activeTypeface").invoke(null);
        String expected=settings?"INTER":"MINECRAFT_DEFAULT";
        if(!actual.toString().equals(expected))throw new IllegalStateException("Incorrect typeface for "+name+": "+actual);
        if(name.endsWith("HudOverlayScreen"))for(Method m:client.screen.getClass().getDeclaredMethods())
            if(m.getName().equals("isDisableActionHovered"))throw new IllegalStateException("HUD delete action is still present");
        System.out.println("[KushCapture] verified typeface="+actual+" screen="+name);
    }
    private static Object fpsModule() throws Exception {
        Object m=manager();
        for(Object mod:(java.util.List<?>)m.getClass().getMethod("getModules").invoke(m))
            if(mod.getClass().getMethod("getName").invoke(mod).toString().equalsIgnoreCase("FPS"))return mod;
        throw new IllegalStateException("FPS module missing");
    }
    private static void openKeystrokesConfig(Minecraft client) throws Exception {
        Object mod=null;
        for(Object candidate:(java.util.List<?>)manager().getClass().getMethod("getModules").invoke(manager()))
            if(candidate.getClass().getMethod("getName").invoke(candidate).toString().equalsIgnoreCase("Keystrokes")) mod=candidate;
        if(mod==null) throw new IllegalStateException("Keystrokes module missing");
        Class<?> moduleClass=Class.forName("net.fastclient.hud.modules.Module");
        Class<?> screen=Class.forName("net.fastclient.hud.gui.screens.ModuleConfigScreen");
        client.setScreen((Screen)screen.getConstructor(moduleClass,Class.forName("net.minecraft.client.gui.screens.Screen")).newInstance(mod,client.screen));
    }
    private static void openConfig(Minecraft client) throws Exception {
        Object mod=fpsModule();Class<?> c=Class.forName("net.fastclient.hud.gui.screens.ModuleConfigScreen");
        client.setScreen((Screen)c.getConstructor(Class.forName("net.fastclient.hud.modules.Module"),Screen.class)
            .newInstance(mod,client.screen));
    }
    private static void setFpsEnabled(boolean enabled) throws Exception {
        Object mod = fpsModule();
        if ((boolean)mod.getClass().getMethod("isEnabled").invoke(mod) != enabled)
            manager().getClass().getMethod("toggleModule",Class.forName("net.fastclient.hud.modules.Module")).invoke(manager(),mod);
    }
    private static void enableHud() throws Exception {
        Object m=manager();int count=0;
        for(Object mod:(java.util.List<?>)m.getClass().getMethod("getModules").invoke(m)) {
            count++;String name=mod.getClass().getMethod("getName").invoke(mod).toString();
            if(java.util.Set.of("FPS","Coordinates","Keystrokes","CPSCounter","ArmorHUD").contains(name)) {
                if(!(boolean)mod.getClass().getMethod("isEnabled").invoke(mod))m.getClass().getMethod("toggleModule",Class.forName("net.fastclient.hud.modules.Module")).invoke(m,mod);
                int x=switch(name){case "FPS"->32;case "Keystrokes"->330;case "CPSCounter"->560;case "ArmorHUD"->990;default->770;};
                mod.getClass().getMethod("setHudPosition",int.class,int.class).invoke(mod,x,name.equals("Coordinates")?400:120);
            }
        }
        m.getClass().getMethod("saveConfig").invoke(m);
        System.out.println("[KushCapture] Verified modules="+count+" and enabled FPS/Coordinates/Keystrokes");
    }
    private static void scrollConfigToBottom(Minecraft client)throws Exception {
        Class<?> type=client.screen.getClass();Field max=type.getDeclaredField("maxScroll"),target=type.getDeclaredField("targetScrollOffset");max.setAccessible(true);target.setAccessible(true);target.setDouble(client.screen,max.getDouble(client.screen));
        Class<?> language=Class.forName("net.fastclient.hud.gui.KushLanguage");
        for(String label:new String[]{"Show CPS","Show Held","Show Bars"})if(language.getMethod("translate",String.class).invoke(null,label).equals(label))throw new IllegalStateException("New HUD title has no PT translation: "+label);
    }
    private static void equipTestArmor(Minecraft client) {
        var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET,net.minecraft.world.entity.EquipmentSlot.MAINHAND};
        var items=new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.DIAMOND_HELMET,net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,net.minecraft.world.item.Items.DIAMOND_LEGGINGS,net.minecraft.world.item.Items.DIAMOND_BOOTS,net.minecraft.world.item.Items.DIAMOND_SWORD};
        for(int i=0;i<items.length;i++){var stack=new net.minecraft.world.item.ItemStack(items[i]);stack.setDamageValue(stack.getMaxDamage()/3);client.player.setItemSlot(slots[i],stack);}
    }
    private static void selectEntry(Minecraft client,Object entry)throws Exception {
        Method select=client.screen.getClass().getDeclaredMethod("selectEntry",entry.getClass());select.setAccessible(true);select.invoke(client.screen,entry);
    }
    private static void clickRect(Minecraft client,String field)throws Exception {
        Field f=client.screen.getClass().getDeclaredField(field);f.setAccessible(true);int[] r=(int[])f.get(client.screen);
        client.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(r[0]+r[2]/2.0,r[1]+r[3]/2.0,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
    }
    private static void pressDemo(Minecraft client) throws Exception {
        System.out.println("[KushCapture] Demo screen="+client.screen.getClass().getName());
        for(Object child:client.screen.children()) {
            if(!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget))continue;
            String label=widget.getMessage().getString();
            System.out.println("[KushCapture] Demo control="+label+" class="+widget.getClass().getName());
            if(!label.equals("Play Demo World")&&!label.equals("Jogar mundo de demonstração"))continue;
            // Minecraft 1.21.11 builds a concrete subclass of abstract Button;
            // its OnPress callback is declared on the superclass.
            for(Class<?> owner=widget.getClass();owner!=null;owner=owner.getSuperclass())
            for(java.lang.reflect.Field field:owner.getDeclaredFields()) {
                if(!field.getType().isInterface())continue;
                for(Method method:field.getType().getMethods()) {
                    if(method.getReturnType()==void.class && method.getParameterCount()==1 && method.getParameterTypes()[0].isAssignableFrom(widget.getClass())) {
                        field.setAccessible(true);method.invoke(field.get(widget),widget);
                        System.out.println("[KushCapture] Started official demo world");return;
                    }
                }
            }
        }
        throw new IllegalStateException("Play Demo World control was not found");
    }

    private static void setCat(Minecraft client,String name,State next) throws Exception {
        category(client,name); waitTicks=35; state=next;
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static void category(Minecraft client,String name) throws Exception {
        if (client.screen==null || !client.screen.getClass().getName().equals("net.fastclient.hud.gui.screens.ClickGUIScreen"))
            open(client,"net.fastclient.hud.gui.screens.ClickGUIScreen");
        Object screen=client.screen;
        Class<?> sc=screen.getClass();
        Field f=sc.getDeclaredField("selectedCategory"); f.setAccessible(true);
        Object value=null;
        if(name!=null){
            Class<?> cc=Class.forName("net.fastclient.hud.modules.Category");
            value=Enum.valueOf((Class<? extends Enum>)cc.asSubclass(Enum.class),name);
        }
        f.set(screen,value);
        Method m=sc.getDeclaredMethod("updateModuleList"); m.setAccessible(true); m.invoke(screen);
        System.out.println("[KushCapture] category="+(name==null?"ALL":name));
    }

    private static void open(Minecraft client,String className) throws Exception {
        client.setScreen((Screen)Class.forName(className).getConstructor().newInstance());
        System.out.println("[KushCapture] opened="+className);
    }

    private static void capture(Minecraft client,String name,State next) throws Exception {
        verifyTypeface(client);
        capturing=true;
        File root=new File(client.gameDirectory,"kush-capture-temp/"+name);
        Path rootPath=root.toPath();
        Path outDir=new File(client.gameDirectory,"kush-captures").toPath();
        Files.createDirectories(rootPath); Files.createDirectories(outDir);
        System.out.println("[KushCapture] capture="+name);
        Screenshot.grab(root,client.getMainRenderTarget(),msg -> client.execute(() -> {
            try {
                Path shots=rootPath.resolve("screenshots");
                Path latest;
                try(Stream<Path> s=Files.list(shots)){
                    latest=s.filter(p->p.getFileName().toString().endsWith(".png"))
                            .max(Comparator.comparingLong(p->p.toFile().lastModified())).orElseThrow();
                }
                Path out=outDir.resolve(name+".png");
                Files.copy(latest,out,StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[KushCapture] saved="+out.toAbsolutePath());
                state=next; waitTicks=45; capturing=false;
            } catch(Throwable t){ t.printStackTrace(); System.exit(3); }
        }));
    }
}

