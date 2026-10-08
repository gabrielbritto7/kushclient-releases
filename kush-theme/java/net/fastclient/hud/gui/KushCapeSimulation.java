package net.fastclient.hud.gui;

/** Kush's independent damped cloth-hinge solver. No WaveyCapes code is used. */
public final class KushCapeSimulation {
    private final double[] angle,velocity,side,sideVelocity;
    public KushCapeSimulation(int segments){if(segments<4 || segments>32)throw new IllegalArgumentException("segments");angle=new double[segments];velocity=new double[segments];side=new double[segments];sideVelocity=new double[segments];}
    public void step(double dt,double time,double speed,double turn,double strength,double wind,double damping,boolean water) {
        dt=Math.max(0,Math.min(.1,dt));speed=Math.max(0,Math.min(6,speed));turn=Math.max(-1,Math.min(1,turn));
        strength=Math.max(0,Math.min(2,strength));wind=Math.max(0,Math.min(1,wind));damping=Math.max(0,Math.min(1,damping));
        int steps=Math.max(1,(int)Math.ceil(dt/.008));double h=dt/steps,drag=(8+12*damping)*(water?1.8:1);
        for(int s=0;s<steps;s++)for(int i=0;i<angle.length;i++){
            double u=(i+.5)/angle.length;
            double gust=wind*(.10+.08*Math.sin(time*2.1-u*3.5));
            double target=.08+strength*(Math.min(.72,speed*.15)*(.6+.4*u)+gust*u);
            double neighbor=i==0?target:angle[i-1];
            velocity[i]+=(75*(target-angle[i])+20*(neighbor-angle[i])-drag*velocity[i])*h;
            angle[i]=Math.max(.015,Math.min(1.15,angle[i]+velocity[i]*h));
            double lateral=strength*(turn*.16*u+wind*.045*Math.sin(time*1.7-u*4)*u);
            sideVelocity[i]+=(65*(lateral-side[i])-drag*sideVelocity[i])*h;
            side[i]=Math.max(-.22,Math.min(.22,side[i]+sideVelocity[i]*h));
        }
    }
    /** Centerline in model units: anchored at the neck, y downward, z behind. */
    public float[] points() {
        float[] p=new float[(angle.length+1)*3];double x=0,y=0,z=0,len=1./angle.length;
        for(int i=0;i<angle.length;i++){x+=Math.sin(side[i])*len;y+=Math.cos(angle[i])*Math.cos(side[i])*len;z+=Math.sin(angle[i])*len;p[(i+1)*3]=(float)x;p[(i+1)*3+1]=(float)y;p[(i+1)*3+2]=(float)z;}
        return p;
    }
}
