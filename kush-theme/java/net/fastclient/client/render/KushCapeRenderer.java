package net.fastclient.client.render;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.*;
import net.fastclient.hud.gui.KushCapeSimulation;
import net.fastclient.hud.modules.impl.render.CapePhysics;
import net.fastclient.client.gui.CosmeticPreviewPlayer;

/** Textured, closed cloth mesh using vanilla cape UVs and the native render queue. */
public final class KushCapeRenderer {
    private static final Map<Integer,Motion> MOTION=new LinkedHashMap<>(32,.75f,true);
    private static final class Motion {KushCapeSimulation solver;int segments;boolean preview;double age=-1,x,y,z,yaw;float[] points;}
    private KushCapeRenderer() {}
    public static boolean isPreview(class_10055 state){Motion m=MOTION.get(state.field_53528);return m!=null&&m.preview;}
    public static void note(class_11890 entity,class_10055 state){
        Motion m=MOTION.computeIfAbsent(state.field_53528,id->new Motion());m.preview=entity instanceof CosmeticPreviewPlayer;
        while(MOTION.size()>128)MOTION.remove(MOTION.keySet().iterator().next());
    }
    public static boolean render(class_4587 stack,class_11659 queue,int light,class_10055 state){
        CapePhysics config=CapePhysics.current();
        if(KushWaveyBridge.active(state))return false;
        if(config==null || !config.isEnabled() || state.field_53333 || !state.field_53532 || state.field_53520==null || state.field_53520.comp_1627()==null || state.field_53411 || state.field_53412 || state.field_53418.method_31574(class_1802.field_8833))return false;
        Motion m=MOTION.computeIfAbsent(state.field_53528,id->new Motion());
        if(m.preview && !config.preview())return false;
        int segments=state.field_53332>6400?8:config.segments();
        double time=state.field_53328/20.,dt=m.age<0?.05:time-m.age;
        double dx=state.field_53325-m.x,dy=state.field_53326-m.y,dz=state.field_53327-m.z;
        double distance=Math.sqrt(dx*dx+dz*dz);
        if(m.solver==null || m.segments!=segments || dt<0 || dt>1 || distance>4){m.solver=new KushCapeSimulation(segments);m.segments=segments;dt=.05;distance=0;}
        double turn=Math.IEEEremainder(state.field_53446-m.yaw,360)/90;
        if(dt>0){m.solver.step(dt,time,m.preview?.35:distance/Math.max(.001,dt),turn,config.strength(),config.wind(),config.damping(),state.field_53458);m.points=m.solver.points();}
        m.age=time;m.x=state.field_53325;m.y=state.field_53326;m.z=state.field_53327;m.yaw=state.field_53446;
        final float[] points=m.points!=null?m.points:m.solver.points();
        final class_2960 texture=state.field_53520.comp_1627().comp_3627();
        stack.method_22903();
        stack.method_46416(0,state.field_53410?.15f:0,!state.field_53418.method_7960()?.22f:.14f);
        if(state.field_53410)stack.method_22907(new org.joml.Quaternionf().rotationX(.35f));
        final boolean bright=config.preserveBrightness();
        queue.method_73529(0).method_73483(stack,bright?class_12249.method_75984(texture,false):class_12249.method_76000(texture),(pose,vc)->mesh(vc,pose,light,points));
        stack.method_22909();return true;
    }
    private static void vertex(class_4588 vc,class_4587.class_4665 pose,int light,float x,float y,float z,float u,float v,float nx,float ny,float nz){vc.method_56824(pose,x,y,z).method_1336(255,255,255,255).method_22913(u,v).method_22922(class_4608.field_21444).method_60803(light).method_60831(pose,nx,ny,nz);}
    private static void mesh(class_4588 vc,class_4587.class_4665 pose,int light,float[] p){
        int n=p.length/3-1;float half=5/16f,thickness=1/16f;
        for(int i=0;i<n;i++){
            int a=i*3,b=a+3;float x0=p[a],y0=p[a+1],z0=p[a+2],x1=p[b],y1=p[b+1],z1=p[b+2];
            float v0=(1+16*i/(float)n)/32,v1=(1+16*(i+1)/(float)n)/32;
            float normY=-(z1-z0),normZ=y1-y0,len=(float)Math.sqrt(normY*normY+normZ*normZ);normY/=len;normZ/=len;
            // Outside/back: primary 10 x 16 cape face. Inside: second vanilla face.
            vertex(vc,pose,light,x0-half,y0,z0+thickness,1/64f,v0,0,normY,normZ);
            vertex(vc,pose,light,x1-half,y1,z1+thickness,1/64f,v1,0,normY,normZ);
            vertex(vc,pose,light,x1+half,y1,z1+thickness,11/64f,v1,0,normY,normZ);
            vertex(vc,pose,light,x0+half,y0,z0+thickness,11/64f,v0,0,normY,normZ);
            vertex(vc,pose,light,x0+half,y0,z0,12/64f,v0,0,-normY,-normZ);
            vertex(vc,pose,light,x1+half,y1,z1,12/64f,v1,0,-normY,-normZ);
            vertex(vc,pose,light,x1-half,y1,z1,22/64f,v1,0,-normY,-normZ);
            vertex(vc,pose,light,x0-half,y0,z0,22/64f,v0,0,-normY,-normZ);
            for(int side=-1;side<=1;side+=2){
                float x=side*half,u=side<0?0:11/64f;
                vertex(vc,pose,light,x0+x,y0,z0,u,v0,side,0,0);vertex(vc,pose,light,x1+x,y1,z1,u,v1,side,0,0);
                vertex(vc,pose,light,x1+x,y1,z1+thickness,u+1/64f,v1,side,0,0);vertex(vc,pose,light,x0+x,y0,z0+thickness,u+1/64f,v0,side,0,0);
            }
        }
        for(int end:new int[]{0,n}){
            int i=end*3;float sign=end==0?-1:1;
            vertex(vc,pose,light,p[i]-half,p[i+1],p[i+2],1/64f,0,0,sign,0);vertex(vc,pose,light,p[i]+half,p[i+1],p[i+2],11/64f,0,0,sign,0);
            vertex(vc,pose,light,p[i]+half,p[i+1],p[i+2]+thickness,11/64f,1/32f,0,sign,0);vertex(vc,pose,light,p[i]-half,p[i+1],p[i+2]+thickness,1/64f,1/32f,0,sign,0);
        }
    }
}
