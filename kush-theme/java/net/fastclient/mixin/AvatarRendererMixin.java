/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_10055
 *  net.minecraft.class_1007
 *  net.minecraft.class_11890
 *  net.minecraft.class_12079$class_10726
 *  net.minecraft.class_12079$class_12081
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_3883
 *  net.minecraft.class_5617$class_5618
 *  net.minecraft.class_583
 *  net.minecraft.class_591
 *  net.minecraft.class_8685
 *  net.minecraft.class_922
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.fastclient.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.client.FastClientCoreClient;
import net.fastclient.client.emote.EmoteManager;
import net.fastclient.client.gui.CosmeticPreviewPlayer;
import net.fastclient.client.render.CosmeticLayer;
import net.fastclient.client.render.CosmeticTextures;
import net.fastclient.client.render.CosmeticsStateHolder;
import net.fastclient.core.data.PlayerCosmetics;
import net.minecraft.class_10055;
import net.minecraft.class_1007;
import net.minecraft.class_11890;
import net.minecraft.class_12079;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3883;
import net.minecraft.class_5617;
import net.minecraft.class_583;
import net.minecraft.class_591;
import net.minecraft.class_8685;
import net.minecraft.class_922;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_1007.class})
public abstract class AvatarRendererMixin
extends class_922<class_11890, class_10055, class_591> {
    protected AvatarRendererMixin(class_5617.class_5618 context, class_591 model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void fastclientcore$addCosmeticLayers(class_5617.class_5618 context, boolean slim, CallbackInfo ci) {
        this.method_4046(new CosmeticLayer((class_3883<class_10055, class_591>)this));
    }

    @Inject(method={"method_62604"}, at={@At(value="TAIL")})
    private void fastclientcore$extractCosmetics(class_11890 entity, class_10055 state, float partialTick, CallbackInfo ci) {
        net.fastclient.client.render.KushCapeRenderer.note(entity,state);
        class_2960 capeTexture;
        CosmeticsStateHolder holder = (CosmeticsStateHolder)state;
        PlayerCosmetics cosmetics = null;
        class_310 mc = class_310.method_1551();
        if (entity instanceof CosmeticPreviewPlayer || mc.field_1724 != null && entity.method_5667().equals(mc.field_1724.method_5667())) {
            cosmetics = FastClientCoreClient.localCosmeticsForRender();
        }
        holder.fastclientcore$setCosmetics(cosmetics);
        EmoteManager.Active emote = EmoteManager.get(entity.method_5667());
        holder.fastclientcore$setEmotePose(emote == null ? null : emote.pose(partialTick));
        if (cosmetics != null && cosmetics.cape() != null && !state.field_53333 && (capeTexture = CosmeticTextures.get("cape/" + cosmetics.cape().id(), cosmetics.cape().texture(), 2, cosmetics.cape().mspf())) != null) {
            class_8685 skin = state.field_53520;
            state.field_53520 = new class_8685(skin.comp_1626(), (class_12079.class_12081)new class_12079.class_10726(capeTexture, capeTexture), skin.comp_1628(), skin.comp_1629(), skin.comp_1630());
            state.field_53532 = true;
        }
    }
}

