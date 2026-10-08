package net.fastclient.client.render;

import net.fabricmc.loader.api.FabricLoader;
import net.fastclient.hud.modules.impl.render.CapePhysics;
import net.minecraft.*;
import java.lang.reflect.Method;

/** Optional adapter to the installed, official Wavey Capes. No Wavey code or JAR is bundled. */
public final class KushWaveyBridge {
    private static final boolean PRESENT=FabricLoader.getInstance().isModLoaded("waveycapes");
    private static Method settingsFactory,layerGetter;
    private static Object wavey;
    private static boolean warned;
    public static int submittedFrames,texturedFrames;
    private KushWaveyBridge() {}
    public static boolean installed(){return PRESENT;}
    public static boolean active(class_10055 state){
        CapePhysics config=CapePhysics.current();
        return PRESENT && config!=null && config.isEnabled() && config.useWavey()
            && (config.preview() || !KushCapeRenderer.isPreview(state));
    }
    public static boolean openSettings(class_437 parent){
        if(!PRESENT)return false;
        try {
            if(settingsFactory==null)settingsFactory=Class.forName("dev.tr7zw.waveycapes.WaveyCapesConfigScreen").getMethod("createConfigScreen",class_437.class);
            class_310.method_1551().method_1507((class_437)settingsFactory.invoke(null,parent));return true;
        }catch(ReflectiveOperationException error){warn(error);return false;}
    }
    /** Wavey replaces the vanilla layer, so explicitly restore it when Kush physics is off. */
    public static void fallback(class_4587 stack,class_11659 queue,int light,class_10055 state,float yaw,float pitch){
        if(KushCapeRenderer.render(stack,queue,light,state))return;
        try {
            if(wavey==null){wavey=Class.forName("dev.tr7zw.waveycapes.versionless.ModBase").getField("INSTANCE").get(null);layerGetter=wavey.getClass().getMethod("getCapeLayer");}
            class_972 layer=(class_972)layerGetter.invoke(wavey);
            if(layer!=null)layer.method_4177(stack,queue,light,state,yaw,pitch);
        }catch(ReflectiveOperationException error){warn(error);}
    }
    private static void warn(Exception error){if(!warned){warned=true;System.err.println("[KushMod] Wavey settings adapter: "+error);}}
}
