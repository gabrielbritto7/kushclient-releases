package net.fastclient.hud.gui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** Bakes the real vanilla UV texture onto a rotating, curved 3D cape, off-thread. */
public final class KushCapeCards {
    public static final int SIZE=128,FRAMES=12,DELAY=150;
    private KushCapeCards() {}
    public static BufferedImage bake(BufferedImage texture) {
        int unit=texture.getWidth()/64;
        if(unit<1 || texture.getWidth()%64!=0 || texture.getHeight()<32*unit)throw new IllegalArgumentException("Cape UV layout");
        BufferedImage face=texture.getSubimage(unit,unit,10*unit,16*unit);
        BufferedImage sheet=new BufferedImage(SIZE,SIZE*FRAMES,BufferedImage.TYPE_INT_ARGB);
        for(int f=0;f<FRAMES;f++){
            Graphics2D g=sheet.createGraphics();
            try {
                g.translate(0,f*SIZE);g.clipRect(0,0,SIZE,SIZE);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                double phase=f*Math.PI*2/FRAMES,yaw=.55+.3*Math.sin(phase),tilt=-.12;
                double[][] left=new double[17][],right=new double[17][],back=new double[17][];
                for(int i=0;i<=16;i++){
                    double t=i/16.,z=.11*t+.065*Math.sin(phase-t*3.3)*t*t;
                    left[i]=project(-.3125,t,z,yaw,tilt);right[i]=project(.3125,t,z,yaw,tilt);back[i]=project(.3125,t,z-.0625,yaw,tilt);
                }
                // Visible edge is part of the cuboid, rather than a flat image skew.
                g.setColor(new Color(27,21,29,240));
                for(int i=0;i<16;i++)g.fill(path(right[i],back[i],back[i+1],right[i+1]));
                for(int i=0;i<16;i++){
                    double sy0=face.getHeight()*i/16.,sy1=face.getHeight()*(i+1)/16.;
                    triangle(g,face,new double[]{0,sy0},new double[]{face.getWidth(),sy0},new double[]{face.getWidth(),sy1},left[i],right[i],right[i+1]);
                    triangle(g,face,new double[]{0,sy0},new double[]{face.getWidth(),sy1},new double[]{0,sy1},left[i],right[i+1],left[i+1]);
                }
                g.setColor(new Color(255,255,255,65));g.setStroke(new BasicStroke(.7f));g.draw(new Line2D.Double(left[0][0],left[0][1],right[0][0],right[0][1]));
            } finally {g.dispose();}
        }
        return sheet;
    }
    private static double[] project(double x,double y,double z,double yaw,double tilt){
        double xx=x*Math.cos(yaw)+z*Math.sin(yaw),zz=-x*Math.sin(yaw)+z*Math.cos(yaw);
        double yy=(y-.5)*Math.cos(tilt)-zz*Math.sin(tilt);zz=(y-.5)*Math.sin(tilt)+zz*Math.cos(tilt);
        double scale=103/(1+zz*.28);return new double[]{64+xx*scale,64+yy*scale};
    }
    private static Path2D path(double[]...points){Path2D p=new Path2D.Double();p.moveTo(points[0][0],points[0][1]);for(int i=1;i<points.length;i++)p.lineTo(points[i][0],points[i][1]);p.closePath();return p;}
    private static void triangle(Graphics2D g,BufferedImage image,double[] s0,double[] s1,double[] s2,double[] d0,double[] d1,double[] d2){
        AffineTransform src=new AffineTransform(s1[0]-s0[0],s1[1]-s0[1],s2[0]-s0[0],s2[1]-s0[1],s0[0],s0[1]);
        AffineTransform dst=new AffineTransform(d1[0]-d0[0],d1[1]-d0[1],d2[0]-d0[0],d2[1]-d0[1],d0[0],d0[1]);
        try{dst.concatenate(src.createInverse());}catch(NoninvertibleTransformException invalid){throw new IllegalArgumentException(invalid);}
        Shape saved=g.getClip();g.clip(path(d0,d1,d2));g.drawImage(image,dst,null);g.setClip(saved);
    }
}
