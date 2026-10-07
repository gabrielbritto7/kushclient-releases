import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.fastclient.hud.gui.KushImageResampler;

public final class ResamplerCheck {
    public static void main(String[] args) {
        int[] square = KushImageResampler.contain(1254, 1254, 30, 36);
        check(square[0] == 30 && square[1] == 30, "Square logos must not stretch");
        int[] skate = KushImageResampler.contain(1920, 1080, 80, 80);
        check(skate[0] == 80 && skate[1] == 45, "Skate aspect ratio");
        BufferedImage checker = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<256;y++) for (int x=0;x<256;x++) checker.setRGB(x,y,((x+y)%2==0)?0xFFFFFFFF:0xFF000000);
        BufferedImage averaged = KushImageResampler.resize(checker, 13, 13);
        for (int y=2;y<11;y++) for (int x=2;x<11;x++) {
            int red=(averaged.getRGB(x,y)>>>16)&255;
            check(red>=105 && red<=150, "Reduction must average detail instead of nearest sampling");
        }
        BufferedImage circle=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=circle.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.fillOval(40,40,320,320);g.dispose();
        BufferedImage small=KushImageResampler.resize(circle,28,28);
        int coverage=0;
        for(int y=0;y<28;y++)for(int x=0;x<28;x++) {
            int argb=small.getRGB(x,y),a=argb>>>24;
            if(a>40 && a<240){coverage++;check((argb&0xFFFFFF)>=0xFDFDFD,"Transparent edge must not turn black");}
        }
        check(coverage>10,"Reduced logos need smooth alpha coverage");
        for(String name:new String[]{"gear","folder","hanger","mods","lan","exit","chart","award"}) {
            BufferedImage icon=KushImageResampler.icon(name,24,24);
            int soft=0;for(int y=0;y<24;y++)for(int x=0;x<24;x++) {int a=icon.getRGB(x,y)>>>24;if(a>0 && a<255)soft++;}
            check(soft>10,"Chrome icon requires antialiased edges: "+name);
        }
        System.out.println("[KushCheck] PASS proportions, detail reduction, transparent edges and chrome icons");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
