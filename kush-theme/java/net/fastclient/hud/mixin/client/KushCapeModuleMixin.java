package net.fastclient.hud.mixin.client;

import net.fastclient.hud.core.ModuleManager;
import net.fastclient.hud.modules.impl.render.CapePhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Register before default snapshots and config loading, preserving all existing modules. */
@Mixin(value=ModuleManager.class,remap=false)
public abstract class KushCapeModuleMixin {
    @Inject(method="registerModules",at=@At(value="INVOKE",target="Lnet/fastclient/hud/core/ModuleManager;captureDefaults()V"),remap=false)
    private void kush$register(CallbackInfo ci){((ModuleManager)(Object)this).register(new CapePhysics());}
}
