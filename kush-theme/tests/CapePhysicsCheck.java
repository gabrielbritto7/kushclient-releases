import net.fastclient.hud.gui.*;
import java.awt.image.BufferedImage;

public final class CapePhysicsCheck {
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        KushCapeSimulation calm=new KushCapeSimulation(16),moving=new KushCapeSimulation(16);
        for(int t=0;t<400;t++){calm.step(.05,t*.05,0,0,1,0,.65,false);moving.step(.05,t*.05,3,.3,1,.6,.65,false);}
        float[] a=calm.points(),b=moving.points();require(b[b.length-1]>a[a.length-1]+.15,"Movement must bend the cape");require(Math.abs(b[b.length-3])>.01,"Turning must sway sideways");
        for(int t=0;t<5000;t++){moving.step(.1,t*.1,t%2==0?100:-100,t%2==0?100:-100,5,4,-3,t%3==0);float[] p=moving.points();for(float f:p)require(Float.isFinite(f),"Solver diverged");require(p[p.length-2]>0 && p[p.length-2]<=1.001,"Cloth length or body constraint");}
        BufferedImage cape=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);
        for(int y=1;y<17;y++)for(int x=1;x<11;x++)cape.setRGB(x,y,0xFFFF3344);
        BufferedImage sheet=KushCapeCards.bake(cape);require(sheet.getWidth()==128 && sheet.getHeight()==128*12,"Card animation layout");
        int count=0,different=0;for(int y=0;y<128;y++)for(int x=0;x<128;x++){if((sheet.getRGB(x,y)>>>24)>0)count++;if(sheet.getRGB(x,y)!=sheet.getRGB(x,y+128*3))different++;}
        require(count>2000 && count<14000,"Real cape face must occupy a contained 3D model");require(different>500,"3D preview frames must move");
        require(KushRgbBorder.color(1,6,0)!=KushRgbBorder.color(2,6,0),"RGB must animate");require(KushRgbBorder.color(1,6,0)==KushRgbBorder.color(7,6,0),"RGB cycle must repeat");
        System.out.println("Cape physics stable; motion/turning, 12 real UV preview frames and RGB clock verified");
    }
}
