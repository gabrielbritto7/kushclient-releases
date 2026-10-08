/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_10055
 *  net.minecraft.class_11659
 *  net.minecraft.class_12249
 *  net.minecraft.class_2960
 *  net.minecraft.class_3883
 *  net.minecraft.class_3887
 *  net.minecraft.class_4050
 *  net.minecraft.class_4587
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_4588
 *  net.minecraft.class_4608
 *  net.minecraft.class_591
 *  net.minecraft.class_630
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.fastclient.client.render;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.client.render.CosmeticModel;
import net.fastclient.client.render.CosmeticTextures;
import net.fastclient.client.render.CosmeticsStateHolder;
import net.fastclient.core.data.Attach;
import net.fastclient.core.data.AuraCosmetic;
import net.fastclient.core.data.ModelCosmetic;
import net.fastclient.core.data.PetAnimationState;
import net.fastclient.core.data.PetCosmetic;
import net.fastclient.core.data.PlayerCosmetics;
import net.fastclient.core.data.WingCosmetic;
import net.fastclient.core.model.BedrockAnimation;
import net.fastclient.core.model.BonePose;
import net.minecraft.class_10055;
import net.minecraft.class_11659;
import net.minecraft.class_12249;
import net.minecraft.class_2960;
import net.minecraft.class_3883;
import net.minecraft.class_3887;
import net.minecraft.class_4050;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4608;
import net.minecraft.class_591;
import net.minecraft.class_630;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public class CosmeticLayer
extends class_3887<class_10055, class_591> {
    private boolean slim;
    private static final Map<String,com.google.gson.JsonObject> SOURCE_CACHE=new java.util.LinkedHashMap<>();
    private static com.google.gson.JsonObject source(String id,String json){
        String key=id+"#"+json.hashCode();
        com.google.gson.JsonObject cached=SOURCE_CACHE.get(key);if(cached!=null)return cached;
        if(SOURCE_CACHE.size()>=64)SOURCE_CACHE.remove(SOURCE_CACHE.keySet().iterator().next());
        com.google.gson.JsonObject root=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        cached=root.has("kushCosmetica")?root.getAsJsonObject("kushCosmetica"):new com.google.gson.JsonObject();SOURCE_CACHE.put(key,cached);return cached;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"fastclientcore");
    private static final AtomicBoolean LOGGED_FIRST_RENDER = new AtomicBoolean(false);
    private static final AtomicBoolean WARNED_PARTICLE_AURA = new AtomicBoolean(false);
    private static final Quaternionf Y_FLIP = new Quaternionf().rotationY((float)Math.PI);
    private static final float DEG_TO_RAD = (float)Math.PI / 180;
    private static final float WORN_ITEM_SCALE = 0.625f;
    private static final float FAIRY_SWEEP = 22.5f;
    private static final float FAIRY_RATE = 0.22847946f;
    private static final float ITEM_PIVOT_Y = 0.5f;

    public CosmeticLayer(class_3883<class_10055, class_591> parent) {
        super(parent);
    }

    private static void applyHeadDisplay(class_4587 poseStack, CosmeticModel.HeadDisplay head, float grow) {
        poseStack.method_46416(0.0f, -0.25f, 0.0f);
        poseStack.method_22907((Quaternionfc)Y_FLIP);
        poseStack.method_22905(0.625f * grow, -0.625f * grow, -0.625f * grow);
        poseStack.method_46416(head.translation().x(), head.translation().y(), head.translation().z());
        poseStack.method_22907((Quaternionfc)new Quaternionf().rotationXYZ(head.rotation().x() * ((float)Math.PI / 180), head.rotation().y() * ((float)Math.PI / 180), head.rotation().z() * ((float)Math.PI / 180)));
        poseStack.method_22905(head.scale().x(), head.scale().y(), head.scale().z());
        poseStack.method_46416(0.0f, -0.5f, 0.0f);
    }

    private static Vector3f restOrigin(Attach attach) {
        return switch (attach) {
            default -> throw new MatchException(null, null);
            case Attach.HEAD, Attach.BODY -> new Vector3f(8.0f, 24.0f, 8.0f);
            case Attach.LEFT_ARM -> new Vector3f(3.0f, 22.0f, 8.0f);
            case Attach.RIGHT_ARM -> new Vector3f(13.0f, 22.0f, 8.0f);
            case Attach.LEFT_LEG, Attach.BOTH_LEGS -> new Vector3f(6.1f, 12.0f, 8.0f);
            case Attach.RIGHT_LEG -> new Vector3f(9.9f, 12.0f, 8.0f);
        };
    }

    private static Vector3f offset(int flags, Attach attach, float ox, float oy, float oz) {
        if (!ModelCosmetic.Flags.playerSpace(flags)) {
            return new Vector3f(ox, oy, oz);
        }
        Vector3f rest = CosmeticLayer.restOrigin(attach);
        return new Vector3f((8.0f - rest.x()) / 16.0f, -rest.y() / 16.0f, (8.0f - rest.z()) / 16.0f);
    }

    @Nullable
    private static Map<String, BonePose> fairyBeat(@Nullable CosmeticModel model, float age) {
        if (model == null || model.namedGroups().isEmpty()) {
            return null;
        }
        float beat = ((float)Math.sin(age * 0.22847946f) - 1.0f) * 11.25f;
        HashMap<String, BonePose> pose = new HashMap<String, BonePose>();
        for (Map.Entry<String, float[]> group : model.namedGroups().entrySet()) {
            float side = group.getValue()[0] < 0.0f ? -1.0f : 1.0f;
            pose.put(group.getKey(), BonePose.rotation(0.0f, side * beat, 0.0f));
        }
        return pose;
    }

    private static @Nullable CosmeticModel.HeadDisplay headDisplayOf(String id, String modelJson) {
        CosmeticModel model = CosmeticModel.get(id, modelJson);
        return model == null ? null : model.headDisplay();
    }

    @Nullable
    private static Quaternionf headDisplayRotation(@Nullable CosmeticModel.HeadDisplay head) {
        if (head == null) {
            return null;
        }
        Vector3f rotation = head.rotation();
        if (rotation.x() == 0.0f && rotation.y() == 0.0f && rotation.z() == 0.0f) {
            return null;
        }
        return new Quaternionf().rotationXYZ(rotation.x() * ((float)Math.PI / 180), rotation.y() * ((float)Math.PI / 180), rotation.z() * ((float)Math.PI / 180));
    }

    private static void ringVertex(class_4588 vc, class_4587.class_4665 pose, int light, float x, float z, float u, float v, float normalY) {
        vc.method_56824(pose, x, 0.0f, z).method_1336(255, 255, 255, 255).method_22913(u, v).method_22922(class_4608.field_21444).method_60803(light).method_60831(pose, 0.0f, normalY, 0.0f);
    }

    public void method_4199(class_4587 poseStack, class_11659 collector, int packedLight, class_10055 state, float yRot, float xRot) {
        Vector3f o;
        if (state.field_53333) {
            return;
        }
        PlayerCosmetics cosmetics = ((CosmeticsStateHolder)state).fastclientcore$getCosmetics();
        if (cosmetics == null) {
            return;
        }
        slim=state.field_53520!=null && state.field_53520.comp_1629()==net.minecraft.class_7920.field_41122;
        class_591 model = (class_591)this.method_17165();
        float age = state.field_53328;
        int hatIndex = 0;
        for (ModelCosmetic hat : cosmetics.hats()) {
            boolean locked = (hat.flags() & 2) != 0;
            boolean headBlock = !locked && !ModelCosmetic.Flags.playerSpace(hat.flags());
            o = CosmeticLayer.offset(hat.flags(), locked ? Attach.BODY : Attach.HEAD, 0.0f, 0.5f, 0.0f);
            this.submitCosmetic(poseStack, collector, packedLight, hat.id(), hat.modelJson(), hat.texture(), hat.flags(), locked ? model.field_3391 : model.field_3398, o.x(), o.y(), o.z(), false, 1.0f + 0.001f * (float)hatIndex++, null, 1.0f, headBlock);
        }
        for (ModelCosmetic face : cosmetics.face()) {
            Vector3f o2 = CosmeticLayer.offset(face.flags(), Attach.HEAD, 0.0f, 0.0f, -0.26f);
            this.submitCosmetic(poseStack, collector, packedLight, face.id(), face.modelJson(), face.texture(), face.flags(), model.field_3398, o2.x(), o2.y(), o2.z(), false, 1.001f, null, 1.0f);
        }
        for (ModelCosmetic arm : cosmetics.arm()) {
            boolean left = arm.attach() == Attach.LEFT_ARM;
            Vector3f o3 = CosmeticLayer.offset(arm.flags(), left ? Attach.LEFT_ARM : Attach.RIGHT_ARM, 0.0f, 0.0f, 0.0f);
            this.submitCosmetic(poseStack, collector, packedLight, arm.id(), arm.modelJson(), arm.texture(), arm.flags(), left ? model.field_27433 : model.field_3401, o3.x(), o3.y(), o3.z(), !left && (arm.flags() & 4) == 0, 1.001f, null, 1.0f);
        }
        for (ModelCosmetic boots : cosmetics.boots()) {
            boolean mirror;
            boolean both = boots.attach() == Attach.BOTH_LEGS;
            boolean bl = mirror = (boots.flags() & 4) == 0;
            if (both || boots.attach() == Attach.LEFT_LEG) {
                o = CosmeticLayer.offset(boots.flags(), Attach.LEFT_LEG, 0.0f, -0.75f, 0.0f);
                this.submitCosmetic(poseStack, collector, packedLight, boots.id(), boots.modelJson(), boots.texture(), boots.flags(), model.field_3397, o.x(), o.y(), o.z(), false, 1.001f, null, 1.0f);
            }
            if (!both && boots.attach() != Attach.RIGHT_LEG) continue;
            o = CosmeticLayer.offset(boots.flags(), Attach.RIGHT_LEG, 0.0f, -0.75f, 0.0f);
            this.submitCosmetic(poseStack, collector, packedLight, boots.id(), boots.modelJson(), boots.texture(), boots.flags(), model.field_3392, o.x(), o.y(), o.z(), mirror, 1.001f, null, 1.0f);
        }
        for (ModelCosmetic back : cosmetics.back()) {
            this.submitCosmetic(poseStack, collector, packedLight, back.id(), back.modelJson(), back.texture(), back.flags(), model.field_3391, 0.0f, -1.43f, 0.05f, false, 1.001f, null, 1.0f);
        }
        for (WingCosmetic wing : cosmetics.wings()) {
            Quaternionf stand;
            boolean playerSpace = ModelCosmetic.Flags.playerSpace(wing.flags());
            CosmeticModel.HeadDisplay head = playerSpace ? null : CosmeticLayer.headDisplayOf(wing.id(), wing.modelJson());
            Quaternionf spin = stand = CosmeticLayer.headDisplayRotation(head);
            Map<String, BonePose> groupPose = CosmeticLayer.clipPose(wing.id(), wing.animationJson(), age);
            if (groupPose == null && "fairy".equalsIgnoreCase(wing.anim())) {
                groupPose = CosmeticLayer.fairyBeat(CosmeticModel.get(wing.id(), wing.modelJson()), age);
            } else if (groupPose == null && !"none".equalsIgnoreCase(wing.anim())) {
                float beat = (float)Math.sin(age * 0.2f) * 10.0f - 5.0f;
                Quaternionf flap = new Quaternionf().rotationX(beat * ((float)Math.PI / 180));
                spin = stand == null ? flap : flap.mul((Quaternionfc)stand, new Quaternionf());
            }
            boolean item = head != null;
            Vector3f o4 = CosmeticLayer.offset(wing.flags(), Attach.BODY, 0.0f, item ? -0.48f : -0.15f, 0.16f);
            this.submitCosmetic(poseStack, collector, packedLight, wing.id(), wing.modelJson(), wing.texture(), wing.flags(), model.field_3391, o4.x(), o4.y(), o4.z(), false, 1.0f, spin, item ? 0.625f : 1.0f, false, item ? 0.5f : 0.0f, groupPose);
        }
        PetAnimationState petState = CosmeticLayer.petAnimationState(state);
        for (PetCosmetic pet : cosmetics.pets()) {
            Map<String, BonePose> groupPose = CosmeticLayer.clipPose(pet.id(), pet.animationJson(), petState.clipName(), age);
            float bob = groupPose == null ? (float)Math.sin(age * 0.1f) * 0.05f : 0.0f;
            Vector3f o5 = CosmeticLayer.offset(pet.flags(), Attach.BODY, 0.0f, -0.45f + bob, 0.55f);
            class_2960 textureOverride = pet.usesPlayerSkin() ? state.field_53520.comp_1626().comp_3627() : null;
            this.submitCosmetic(poseStack, collector, packedLight, pet.id(), pet.modelJson(), pet.texture(), pet.flags(), model.field_3391, o5.x() + pet.offsetX(), o5.y() + (ModelCosmetic.Flags.playerSpace(pet.flags()) ? bob : 0.0f) + pet.offsetY(), o5.z() + pet.offsetZ(), false, 1.0f, null, Math.max(0.05f, pet.scale()), false, 0.0f, groupPose, textureOverride);
        }
        for (AuraCosmetic aura : cosmetics.auras()) {
            if (aura.hasModel()) {
                this.submitAuraModel(poseStack, collector, packedLight, aura, age);
                continue;
            }
            if ("ring".equalsIgnoreCase(aura.style())) {
                this.submitAuraRing(poseStack, collector, packedLight, aura, age);
                continue;
            }
            if (!WARNED_PARTICLE_AURA.compareAndSet(false, true)) continue;
            LOGGER.warn("Aura style '{}' is not implemented yet (only 'ring', or give it a model)", (Object)aura.style());
        }
    }

    @Nullable
    private static Map<String, BonePose> clipPose(String id, @Nullable String animationJson, float age) {
        BedrockAnimation clip = BedrockAnimation.getAsync(id, animationJson);
        return clip == null ? null : clip.pose(age);
    }

    @Nullable
    private static Map<String, BonePose> clipPose(String id, @Nullable String animationJson, String clipName, float age) {
        BedrockAnimation clip = BedrockAnimation.getAsync(id, animationJson, clipName);
        return clip == null ? null : clip.pose(age);
    }

    private static PetAnimationState petAnimationState(class_10055 state) {
        if (state.method_62613(class_4050.field_18079)) {
            return PetAnimationState.SWIM;
        }
        if (state.field_53534 > 0.0f || state.method_62613(class_4050.field_18077)) {
            return PetAnimationState.FLY;
        }
        if (state.field_53451 > 0.08f) {
            return PetAnimationState.RUN;
        }
        return PetAnimationState.IDLE;
    }

    private void submitCosmetic(class_4587 poseStack, class_11659 collector, int packedLight, String id, String modelJson, byte[] textureBytes, int flags, class_630 bone, float ox, float oy, float oz, boolean mirror, float grow, @Nullable Quaternionf extraRotation, float scale) {
        this.submitCosmetic(poseStack, collector, packedLight, id, modelJson, textureBytes, flags, bone, ox, oy, oz, mirror, grow, extraRotation, scale, false);
    }

    private void submitCosmetic(class_4587 poseStack, class_11659 collector, int packedLight, String id, String modelJson, byte[] textureBytes, int flags, class_630 bone, float ox, float oy, float oz, boolean mirror, float grow, @Nullable Quaternionf extraRotation, float scale, boolean honorHeadDisplay) {
        this.submitCosmetic(poseStack, collector, packedLight, id, modelJson, textureBytes, flags, bone, ox, oy, oz, mirror, grow, extraRotation, scale, honorHeadDisplay, 0.0f);
    }

    private void submitCosmetic(class_4587 poseStack, class_11659 collector, int packedLight, String id, String modelJson, byte[] textureBytes, int flags, class_630 bone, float ox, float oy, float oz, boolean mirror, float grow, @Nullable Quaternionf extraRotation, float scale, boolean honorHeadDisplay, float rotationPivotY) {
        this.submitCosmetic(poseStack, collector, packedLight, id, modelJson, textureBytes, flags, bone, ox, oy, oz, mirror, grow, extraRotation, scale, honorHeadDisplay, rotationPivotY, null);
    }

    private void submitCosmetic(class_4587 poseStack, class_11659 collector, int packedLight, String id, String modelJson, byte[] textureBytes, int flags, class_630 bone, float ox, float oy, float oz, boolean mirror, float grow, @Nullable Quaternionf extraRotation, float scale, boolean honorHeadDisplay, float rotationPivotY, @Nullable Map<String, BonePose> groupPose) {
        this.submitCosmetic(poseStack, collector, packedLight, id, modelJson, textureBytes, flags, bone, ox, oy, oz, mirror, grow, extraRotation, scale, honorHeadDisplay, rotationPivotY, groupPose, null);
    }

    private void submitCosmetic(class_4587 poseStack, class_11659 collector, int packedLight, String id, String modelJson, byte[] textureBytes, int flags, class_630 bone, float ox, float oy, float oz, boolean mirror, float grow, @Nullable Quaternionf extraRotation, float scale, boolean honorHeadDisplay, float rotationPivotY, @Nullable Map<String, BonePose> groupPose, @Nullable class_2960 textureOverride) {
        class_2960 texture;
        CosmeticModel model = CosmeticModel.get(id, modelJson);
        class_2960 class_29602 = texture = textureOverride != null ? textureOverride : CosmeticTextures.get(id, textureBytes, 1, ModelCosmetic.Flags.frameDelayMs(flags));
        if (model == null || texture == null || !bone.field_3665) {
            return;
        }
        com.google.gson.JsonObject meta=source(id,modelJson);
        if(meta.has("offset")){
            var offset=meta.getAsJsonArray("offset");String attachment=meta.get("attachment").getAsString();
            float dx=attachment.equals("LEFT_ARM")?-1:attachment.equals("RIGHT_ARM")?1:0;
            float dy=attachment.equals("HEAD")?4:attachment.endsWith("ARM")?-6:-8;
            int index=slim?3:0;
            if(slim)dx+=attachment.equals("LEFT_ARM")?0.5f:attachment.equals("RIGHT_ARM")?-0.5f:0;
            ox=(offset.get(index).getAsFloat()+dx)/16;oy=(offset.get(index+1).getAsFloat()+dy)/16-0.25f;oz=offset.get(index+2).getAsFloat()/16;
            mirror=false;extraRotation=null;scale=1;grow=1;rotationPivotY=0;honorHeadDisplay=false;
        }
        CosmeticModel.HeadDisplay head = honorHeadDisplay ? model.headDisplay() : null;
        poseStack.method_22903();
        bone.method_22703(poseStack);
        if (head != null) {
            CosmeticLayer.applyHeadDisplay(poseStack, head, grow);
        } else {
            poseStack.method_22905(grow, -grow, -grow);
            poseStack.method_22907((Quaternionfc)Y_FLIP);
            poseStack.method_46416(ox, oy + rotationPivotY * scale, oz);
        }
        if (extraRotation != null) {
            poseStack.method_22907((Quaternionfc)extraRotation);
        }
        if (scale != 1.0f) {
            poseStack.method_22905(scale, scale, scale);
        }
        if (rotationPivotY != 0.0f) {
            poseStack.method_46416(0.0f, -rotationPivotY, 0.0f);
        }
        if (mirror) {
            poseStack.method_22905(-1.0f, 1.0f, 1.0f);
        }
        collector.method_73529(0).method_73483(poseStack, class_12249.method_76000((class_2960)texture), (pose, vertexConsumer) -> model.render(pose, vertexConsumer, packedLight, groupPose));
        poseStack.method_22909();
        if (LOGGED_FIRST_RENDER.compareAndSet(false, true)) {
            LOGGER.info("Cosmetics are rendering \u2014 first cosmetic '{}' submitted", (Object)id);
        }
    }

    private void submitAuraModel(class_4587 poseStack, class_11659 collector, int packedLight, AuraCosmetic aura, float age) {
        String id = "aura/" + aura.id();
        Map<String, BonePose> groupPose = CosmeticLayer.clipPose(id, aura.animationJson(), age);
        this.submitCosmetic(poseStack, collector, packedLight, id, aura.modelJson(), aura.texture(), aura.flags(), ((class_591)this.method_17165()).field_3391, aura.offsetX(), -CosmeticLayer.restOrigin(Attach.BODY).y() / 16.0f + aura.offsetY(), aura.offsetZ(), false, 1.0f, null, 1.0f, false, 0.0f, groupPose);
    }

    private void submitAuraRing(class_4587 poseStack, class_11659 collector, int packedLight, AuraCosmetic aura, float age) {
        class_2960 texture = CosmeticTextures.get("aura/" + aura.id(), aura.texture());
        if (texture == null) {
            return;
        }
        float angle = age / 20.0f * aura.spinSpeed() * ((float)Math.PI / 180);
        poseStack.method_22903();
        poseStack.method_46416(0.0f, 1.499f - aura.height(), 0.0f);
        poseStack.method_22907((Quaternionfc)new Quaternionf().rotationY(angle));
        float r = 0.7f;
        collector.method_73529(0).method_73483(poseStack, class_12249.method_76000((class_2960)texture), (pose, vc) -> {
            CosmeticLayer.ringVertex(vc, pose, packedLight, -0.7f, -0.7f, 0.0f, 0.0f, -1.0f);
            CosmeticLayer.ringVertex(vc, pose, packedLight, -0.7f, 0.7f, 0.0f, 1.0f, -1.0f);
            CosmeticLayer.ringVertex(vc, pose, packedLight, 0.7f, 0.7f, 1.0f, 1.0f, -1.0f);
            CosmeticLayer.ringVertex(vc, pose, packedLight, 0.7f, -0.7f, 1.0f, 0.0f, -1.0f);
            CosmeticLayer.ringVertex(vc, pose, packedLight, 0.7f, -0.7f, 1.0f, 0.0f, 1.0f);
            CosmeticLayer.ringVertex(vc, pose, packedLight, 0.7f, 0.7f, 1.0f, 1.0f, 1.0f);
            CosmeticLayer.ringVertex(vc, pose, packedLight, -0.7f, 0.7f, 0.0f, 1.0f, 1.0f);
            CosmeticLayer.ringVertex(vc, pose, packedLight, -0.7f, -0.7f, 0.0f, 0.0f, 1.0f);
        });
        poseStack.method_22909();
    }
}

