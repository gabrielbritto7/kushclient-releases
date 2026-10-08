package net.fastclient.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fastclient.hud.mixin.client.KushCapeRenderLayerInvoker;
import net.minecraft.*;

/** Unlit texture with ordinary alpha blending, depth testing and both cloth faces. */
public final class KushCapeRenderTypes {
    private static final Map<class_2960,class_1921> CACHE=new LinkedHashMap<>();
    private static final RenderPipeline PIPELINE=createPipeline();
    private static RenderPipeline createPipeline(){
        RenderPipeline eyes=class_10799.field_56912;
        var builder=RenderPipeline.builder().withLocation(class_2960.method_60655("fastclientcore","pipeline/cape_brightness"))
            .withVertexShader(eyes.getVertexShader()).withFragmentShader(eyes.getFragmentShader())
            .withVertexFormat(eyes.getVertexFormat(),eyes.getVertexFormatMode())
            .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
            .withShaderDefine("ALPHA_CUTOUT",0.001f)
            .withBlend(BlendFunction.TRANSLUCENT).withCull(false).withDepthWrite(true);
        for(var uniform:eyes.getUniforms())builder.withUniform(uniform.name(),uniform.type());
        for(String sampler:eyes.getSamplers())builder.withSampler(sampler);
        return builder.build();
    }
    public static class_1921 bright(class_2960 texture){
        class_1921 layer=CACHE.get(texture);
        if(layer==null){
            layer=KushCapeRenderLayerInvoker.kush$create("kush_cape_brightness",class_12247.method_75927(PIPELINE).method_75934("Sampler0",texture).method_75937().method_75938());
            if(CACHE.size()>=64)CACHE.remove(CACHE.keySet().iterator().next());
            CACHE.put(texture,layer);
        }
        return layer;
    }
}
