package net.fastclient.client.render;

import net.fabricmc.loader.api.FabricLoader;
import net.fastclient.hud.modules.impl.render.CapePhysics;
import net.minecraft.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.WeakHashMap;

/** Optional adapter to the installed, official Wavey Capes. No Wavey code or JAR is bundled. */
public final class KushWaveyBridge {
    private static final boolean PRESENT=FabricLoader.getInstance().isModLoaded("waveycapes");
    private static Method settingsFactory,layerGetter;
    private static Object wavey;
    private static boolean warned;
    private static Method previewUpdate,previewSimulate,previewGravity;
    private static Constructor<?> previewDelegate;
    private static final WeakHashMap<class_11890,Integer> PREVIEW_TICKS=new WeakHashMap<>();
    public static int submittedFrames,texturedFrames;
    private KushWaveyBridge() {}
    public static boolean installed(){return PRESENT;}
    /** Catalog avatars do not receive world ticks; advance Wavey's own solver at 20 Hz. */
    public static void previewTick(class_11890 player){
        CapePhysics config=CapePhysics.current();
        if(!PRESENT || config==null || !config.isEnabled() || !config.useWavey() || !config.preview())return;
        Integer previous=PREVIEW_TICKS.get(player);
        if(previous!=null && previous==player.field_6012)return;
        try {
            if(previewUpdate==null){
                Class<?> holder=Class.forName("dev.tr7zw.waveycapes.versionless.CapeHolder");
                previewUpdate=holder.getMethod("updateSimulation",int.class);
                previewSimulate=holder.getMethod("simulate",Class.forName("dev.tr7zw.waveycapes.versionless.nms.MinecraftPlayer"));
                previewGravity=holder.getMethod("setGravityVectorRequest",boolean.class);
                previewDelegate=Class.forName("dev.tr7zw.waveycapes.delegate.PlayerDelegate").getConstructor(class_11890.class);
            }
            previewUpdate.invoke(player,16);previewGravity.invoke(player,true);
            Object delegate=previewDelegate.newInstance(player);
            for(int i=0;i<(previous==null?6:1);i++)previewSimulate.invoke(player,delegate);
            PREVIEW_TICKS.put(player,player.field_6012);
        }catch(ReflectiveOperationException error){warn(error);}
    }
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
