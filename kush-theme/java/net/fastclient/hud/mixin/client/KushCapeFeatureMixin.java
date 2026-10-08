package net.fastclient.hud.mixin.client;

import net.minecraft.*;
import net.fastclient.client.render.KushCapeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_972.class)
public abstract class KushCapeFeatureMixin {
    @Inject(method="method_4177",at=@At("HEAD"),cancellable=true)
    private void kush$cloth(class_4587 matrices,class_11659 queue,int light,class_10055 state,float yaw,float pitch,CallbackInfo ci){
        if(KushCapeRenderer.render(matrices,queue,light,state))ci.cancel();
    }
}
