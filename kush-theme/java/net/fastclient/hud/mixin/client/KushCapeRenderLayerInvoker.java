package net.fastclient.hud.mixin.client;

import net.minecraft.class_1921;
import net.minecraft.class_12247;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_1921.class)
public interface KushCapeRenderLayerInvoker {
    @Invoker("method_75940")
    static class_1921 kush$create(String name,class_12247 setup){throw new AssertionError();}
}
