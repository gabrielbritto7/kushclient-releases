package net.fastclient.hud.mixin.client;

import net.minecraft.*;
import net.fastclient.client.render.CosmeticsStateHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Wavey's wrapper reads the account skin; supply Kush's equipped render-state cape. */
@Pseudo
@Mixin(targets="dev.tr7zw.transition.mc.entitywrapper.PlayerWrapper",remap=false)
public abstract class KushWaveyTextureMixin {
    @Shadow(remap=false) public abstract class_10055 getRenderState();
    @Inject(method="getCapeTexture",at=@At("HEAD"),cancellable=true,remap=false)
    private void kush$texture(CallbackInfoReturnable<class_2960> ci){
        class_10055 state=getRenderState();
        if(state instanceof CosmeticsStateHolder holder && holder.fastclientcore$getCosmetics()!=null
            && holder.fastclientcore$getCosmetics().cape()!=null && state.field_53520!=null && state.field_53520.comp_1627()!=null)
            ci.setReturnValue(state.field_53520.comp_1627().comp_3627());
    }
}
