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
        OPEN_VANILLA, CAPTURE_VANILLA, OPEN_INSTALLED, CAPTURE_INSTALLED, OPEN_CONFIG, CAPTURE_CONFIG, OPEN_DROPDOWN, CAPTURE_DROPDOWN, OPEN_LONG_CONFIG, CAPTURE_LONG_CONFIG,
        START_DEMO, WAIT_WORLD, CAPTURE_WORLD, OPEN_WORLD_OVERLAY, CAPTURE_WORLD_OVERLAY,
        OPEN_WORLD_CONFIG, CAPTURE_WORLD_CONFIG, OPEN_PAUSE, CAPTURE_PAUSE, DONE
    }
    private static State state = State.WAIT_TITLE;
    private static int waitTicks;
    private static int stableTicks;
    private static boolean capturing;

    public void onInitializeClient() {
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
                case CAPTURE_MAIN -> capture(client, "01-main-menu", State.OPEN_MODS);
                case SET_MAIN_HOVER -> { cursor(client,640,327);waitTicks=35;state=State.CAPTURE_MAIN_HOVER; }
                case CAPTURE_MAIN_HOVER -> capture(client,"16-main-menu-hover",State.SET_TOOL_HOVER);
                case SET_TOOL_HOVER -> { cursor(client,1110,42);waitTicks=35;state=State.CAPTURE_TOOL_HOVER; }
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
                case OPEN_CONFIG -> { openConfig(client);waitTicks=45;state=State.CAPTURE_CONFIG; }
                case CAPTURE_CONFIG -> capture(client,"11-module-settings",State.OPEN_DROPDOWN);
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
                    client.setScreen(new TitleScreen());
                    waitTicks=60;state=State.WAIT_WORLD;stableTicks=0;
                }
                case WAIT_WORLD -> {
                    if(client.level!=null && client.player!=null){
                        Class<?> pref=Class.forName("net.fastclient.hud.launcher.LauncherSkinPreference");
                        if(!(boolean)pref.getDeclaredMethod("isFastClientSkinEnabled").invoke(null))pref.getDeclaredMethod("toggle").invoke(null);
                        enableHud();client.setScreen(null);waitTicks=120;state=State.CAPTURE_WORLD;
                    } else {
                        if(stableTicks++==0)pressDemo(client);
                        if(stableTicks>1800)throw new IllegalStateException("Demo test world did not load: "+client.screen);
                    }
                }
                case CAPTURE_WORLD -> {
                    if(client.screen!=null && client.screen.getClass().getName().endsWith("DemoIntroScreen")) {
                        client.setScreen(null);waitTicks=40;
                    } else capture(client,"12-hud-in-game",State.OPEN_WORLD_OVERLAY);
                }
                case OPEN_WORLD_OVERLAY -> {open(client,"net.fastclient.hud.gui.screens.HudOverlayScreen");waitTicks=50;state=State.CAPTURE_WORLD_OVERLAY;}
                case CAPTURE_WORLD_OVERLAY -> capture(client,"13-right-shift-in-game",State.OPEN_WORLD_CONFIG);
                case OPEN_WORLD_CONFIG -> {openConfig(client);waitTicks=50;state=State.CAPTURE_WORLD_CONFIG;}
                case CAPTURE_WORLD_CONFIG -> capture(client,"14-glass-in-game",State.OPEN_PAUSE);
                case OPEN_PAUSE -> {client.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));waitTicks=50;state=State.CAPTURE_PAUSE;}
                case CAPTURE_PAUSE -> capture(client,"15-pause-menu",State.DONE);
                case DONE -> { System.out.println("[KushCapture] complete"); Thread.sleep(500); System.exit(0); }
            }
        } catch (Throwable t) { t.printStackTrace(); System.err.println("[KushCapture] failed state="+state); System.exit(2); }
    }


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
    private static void openConfig(Minecraft client) throws Exception {
        Object mod=fpsModule();Class<?> c=Class.forName("net.fastclient.hud.gui.screens.ModuleConfigScreen");
        client.setScreen((Screen)c.getConstructor(Class.forName("net.fastclient.hud.modules.Module"),Screen.class)
            .newInstance(mod,client.screen));
    }
    private static void enableHud() throws Exception {
        Object m=manager();int count=0;
        for(Object mod:(java.util.List<?>)m.getClass().getMethod("getModules").invoke(m)) {
            count++;String name=mod.getClass().getMethod("getName").invoke(mod).toString();
            if(name.equalsIgnoreCase("FPS")||name.equalsIgnoreCase("Coordinates")||name.equalsIgnoreCase("Keystrokes")) {
                if(!(boolean)mod.getClass().getMethod("isEnabled").invoke(mod))m.getClass().getMethod("toggleModule",Class.forName("net.fastclient.hud.modules.Module")).invoke(m,mod);
                int x=name.equalsIgnoreCase("FPS")?32:name.equalsIgnoreCase("Keystrokes")?420:770;
                mod.getClass().getMethod("setHudPosition",int.class,int.class).invoke(mod,x,120);
            }
        }
        m.getClass().getMethod("saveConfig").invoke(m);
        System.out.println("[KushCapture] Verified modules="+count+" and enabled FPS/Coordinates/Keystrokes");
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
