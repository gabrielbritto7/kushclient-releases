package net.fastclient.hud.mixin.client;

import java.lang.reflect.Constructor;
import net.fastclient.hud.modules.impl.render.CapePhysics;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve the supplied cape pixels, independent of bend normals and directional shading. */
@Pseudo
@Mixin(targets="dev.tr7zw.waveycapes.render.VanillaCapeRenderer",remap=false)
public abstract class KushWaveyBrightnessMixin {
    @Unique private static Constructor<?> kush$info;
    @Inject(method="getCapeInfo",at=@At("RETURN"),cancellable=true,remap=false)
    private void kush$brightness(@Coerce Object wrapper,CallbackInfoReturnable<Object> ci){
        CapePhysics config=CapePhysics.current();Object info=ci.getReturnValue();
        if(info==null || config==null || !config.isEnabled() || !config.preserveBrightness())return;
        try {
            class_2960 texture=(class_2960)wrapper.getClass().getMethod("getCapeTexture").invoke(wrapper);
            if(texture==null)return;
            if(kush$info==null)kush$info=info.getClass().getConstructors()[0];
            net.fastclient.client.render.KushWaveyBridge.texturedFrames++;
            ci.setReturnValue(kush$info.newInstance(this,net.fastclient.client.render.KushCapeRenderTypes.bright(texture),false));
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Kush cape brightness adapter",error);}
    }
}
