package net.fastclient.hud.gui;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** One complete catalog viewing angle from a WebP canvas, including ANMF offsets.
 * Layout follows https://developers.google.com/speed/webp/docs/riff_container.
 */
public final class KushThumbnailFrames {
    private KushThumbnailFrames() {}
    private static int u24(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8)|((b[p+2]&255)<<16);}
    private static long u32(byte[] b,int p){return u24(b,p)|((long)(b[p+3]&255)<<24);}
    private static boolean tag(byte[] b,int p,String s){for(int i=0;i<4;i++)if(b[p+i]!=(byte)s.charAt(i))return false;return true;}
    public static BufferedImage firstView(byte[] raw,BufferedImage decoded)throws IOException {
        int cw=decoded.getWidth(),ch=decoded.getHeight(),fx=0,fy=0;
        if(raw.length>=12 && tag(raw,0,"RIFF") && tag(raw,8,"WEBP")) {
            int pos=12;
            while(pos+8<=raw.length) {
                long size=u32(raw,pos+4);int data=pos+8;
                if(size>raw.length-data)throw new IOException("Truncated WebP chunk");
                if(tag(raw,pos,"VP8X") && size>=10){cw=1+u24(raw,data+4);ch=1+u24(raw,data+7);}
                if(tag(raw,pos,"ANMF") && size>=16){fx=2*u24(raw,data);fy=2*u24(raw,data+3);break;}
                pos=(int)(data+size+(size&1));
            }
        }
        if(cw<1 || ch<1 || cw>2048 || ch>8192 || (long)cw*ch>8_388_608L)throw new IOException("Thumbnail canvas bounds");
        // Some ImageIO decoders return the full canvas; TwelveMonkeys returns the frame rectangle.
        if(decoded.getWidth()==cw && decoded.getHeight()==ch){fx=0;fy=0;}
        if(fx<0 || fy<0 || (long)fx+decoded.getWidth()>cw || (long)fy+decoded.getHeight()>ch)throw new IOException("Thumbnail frame bounds");
        int height=ch>cw && ch%cw==0 && ch/cw<=16?cw:ch;
        BufferedImage view=new BufferedImage(cw,height,BufferedImage.TYPE_INT_ARGB);
        var g=view.createGraphics();try{g.setComposite(AlphaComposite.Src);g.drawImage(decoded,fx,fy,null);}finally{g.dispose();}
        return view;
    }
}
