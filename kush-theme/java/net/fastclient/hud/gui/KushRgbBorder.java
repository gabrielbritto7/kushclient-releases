package net.fastclient.hud.gui;

/** One monotonic clock shared by both HUDs, without a worker or ticking task. */
public final class KushRgbBorder {
    private KushRgbBorder() {}
    public static int color(double seconds,double period,double position) {
        double safe=Math.max(1,Math.min(20,period));
        float hue=(float)((seconds/safe+position)%1);if(hue<0)hue+=1;
        return 0xFF000000|java.awt.Color.HSBtoRGB(hue,.8f,1f);
    }
    public static double now(){return System.nanoTime()/1_000_000_000.0;}
}
