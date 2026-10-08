package net.fastclient.hud.mixin.client;

import net.fastclient.client.render.KushWaveyBridge;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="dev.tr7zw.waveycapes.renderlayers.CustomCapeRenderLayer",remap=false)
public abstract class KushWaveyLayerMixin {
    @Inject(method="submit",at=@At("HEAD"),cancellable=true,remap=false)
    private void kush$delegate(class_4587 stack,class_11659 queue,int light,class_10055 state,float yaw,float pitch,CallbackInfo ci){
        if(KushWaveyBridge.active(state))KushWaveyBridge.submittedFrames++;
        else {KushWaveyBridge.fallback(stack,queue,light,state,yaw,pitch);ci.cancel();}
    }
}
