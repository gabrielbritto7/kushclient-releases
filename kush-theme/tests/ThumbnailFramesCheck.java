import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.fastclient.hud.gui.KushThumbnailFrames;

/** Regression for the real animated thumbnail's 392x3954 frame inside a 512x4096 canvas. */
public class ThumbnailFramesCheck {
    private static void n24(byte[] b,int p,int n){b[p]=(byte)n;b[p+1]=(byte)(n>>8);b[p+2]=(byte)(n>>16);}
    private static byte[] fixture(int canvasWidth,int canvasHeight,int x,int y) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write("RIFF".getBytes());out.write(new byte[4]);out.write("WEBPVP8X".getBytes());out.write(new byte[]{10,0,0,0});
        byte[] canvas=new byte[10];n24(canvas,4,canvasWidth-1);n24(canvas,7,canvasHeight-1);out.write(canvas);
        out.write("ANMF".getBytes());out.write(new byte[]{16,0,0,0});byte[] frame=new byte[16];n24(frame,0,x/2);n24(frame,3,y/2);out.write(frame);return out.toByteArray();
    }
    public static void main(String[] args)throws Exception {
        var decoded=new BufferedImage(392,3954,BufferedImage.TYPE_INT_ARGB);decoded.setRGB(0,0,0xFFFF1234);
        var view=KushThumbnailFrames.firstView(fixture(512,4096,62,88),decoded);
        if(view.getWidth()!=512 || view.getHeight()!=512 || view.getRGB(62,88)!=0xFFFF1234 || view.getRGB(0,0)!=0)throw new AssertionError("Frame offsets or viewing-angle crop");
        var full=new BufferedImage(512,4096,BufferedImage.TYPE_INT_ARGB);full.setRGB(0,0,0xFF12FF34);
        if(KushThumbnailFrames.firstView(fixture(512,4096,62,88),full).getRGB(0,0)!=0xFF12FF34)throw new AssertionError("Full-canvas decoder shifted twice");
        try{KushThumbnailFrames.firstView(fixture(16384,16384,0,0),decoded);throw new AssertionError("Oversized canvas accepted");}catch(IOException expected){}
        System.out.println("[KushCheck] PASS static/animated WebP frames, offsets, one viewing angle and canvas bounds");
    }
}
