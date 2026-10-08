package net.fastclient.hud.mixin.client;

import net.minecraft.class_312;
import net.minecraft.class_11910;
import net.minecraft.class_310;
import net.fastclient.hud.gui.KushModernKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Modern Keystrokes MouseHandler adaptation for the 1.21.11 intermediary API. */
@Mixin(class_312.class)
public class KushMouseInputMixin {
    @Inject(method="method_1601",at=@At("HEAD"))
    private void kushMouse(long window,class_11910 input,int action,CallbackInfo ci) {
        var client=class_310.method_1551();
        if(action==1 && client.field_1687!=null && client.field_1755==null && window==client.method_22683().method_4490())KushModernKeys.press(input.comp_4801());
    }
}
