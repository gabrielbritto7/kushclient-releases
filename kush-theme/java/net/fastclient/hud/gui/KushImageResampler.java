package net.fastclient.hud.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** UI-only image reduction: retain alpha and sample at the actual screen size. */
public final class KushImageResampler {
    private KushImageResampler() {}

    public static int[] contain(int sourceWidth, int sourceHeight, int boxWidth, int boxHeight) {
        double scale = Math.min((double)boxWidth / sourceWidth, (double)boxHeight / sourceHeight);
        return new int[]{Math.max(1, (int)Math.round(sourceWidth * scale)),
                         Math.max(1, (int)Math.round(sourceHeight * scale))};
    }

    public static BufferedImage resize(BufferedImage source, int width, int height) {
        // Premultiplied alpha prevents dark fringes around transparent logos.
        BufferedImage current = source;
        do {
            int w = Math.max(width, current.getWidth() / 2);
            int h = Math.max(height, current.getHeight() / 2);
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = next.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, w, h, null);
            g.dispose();
            if (current != source) current.flush();
            current = next;
        } while (current.getWidth() != width || current.getHeight() != height);
        return current;
    }

    /** Runtime layout correction only; the supplied logo file remains intact. */
    public static BufferedImage uprightGlyph(BufferedImage source, double degrees) {
        BufferedImage ink = trimAlpha(source);
        double angle = Math.toRadians(degrees);
        int w = (int)Math.ceil(ink.getWidth()*Math.cos(angle)+ink.getHeight()*Math.sin(angle));
        int h = (int)Math.ceil(ink.getHeight()*Math.cos(angle)+ink.getWidth()*Math.sin(angle));
        BufferedImage rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = rotated.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(w/2.0, h/2.0);g.rotate(angle);
        g.drawImage(ink, -ink.getWidth()/2, -ink.getHeight()/2, null);g.dispose();
        return trimAlpha(rotated);
    }
    public static BufferedImage trimAlpha(BufferedImage source) {
        int left=source.getWidth(),right=-1,top=source.getHeight(),bottom=-1;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)
            if((source.getRGB(x,y)>>>24)>16){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        if(right<left)return source;
        return source.getSubimage(left,top,right-left+1,bottom-top+1);
    }

    public static BufferedImage icon(String name, int width, int height) {
        // Draw simple chrome icons at 4x, then reduce once with alpha coverage.
        BufferedImage image = new BufferedImage(width * 4, height * 4, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.scale(width * 4 / 32.0, height * 4 / 32.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double path = new Path2D.Double();
        if (name.equals("gear")) {
            for (int i = 0; i < 64; i++) {
                double angle = i * Math.PI / 32;
                double radius = (i % 8 >= 2 && i % 8 <= 5) ? 13 : 10.4;
                double x = 16 + Math.cos(angle) * radius, y = 16 + Math.sin(angle) * radius;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.closePath();
            Area shape = new Area(path);
            shape.subtract(new Area(new Ellipse2D.Double(11, 11, 10, 10)));
            g.fill(shape);
        } else if (name.equals("folder")) {
            path.moveTo(4,25);path.lineTo(4,8);path.quadTo(4,6,6,6);path.lineTo(12,6);
            path.lineTo(15,10);path.lineTo(26,10);path.quadTo(28,10,28,12);path.lineTo(28,25);
            path.closePath();g.draw(path);path.reset();path.moveTo(4,14);path.lineTo(28,14);g.draw(path);
        } else if (name.equals("hanger")) {
            path.moveTo(13, 10); path.curveTo(13, 4, 21, 4, 21, 10);
            path.curveTo(21, 13, 16, 13, 16, 16);
            path.moveTo(16, 16); path.lineTo(3, 25); path.lineTo(29, 25);
            path.lineTo(16, 16); g.draw(path);
        } else if (name.startsWith("chevron")) {
            int a=name.equals("chevron-up")?20:12, b=32-a;
            path.moveTo(8,a);path.lineTo(16,b);path.lineTo(24,a);g.draw(path);
        } else if (name.equals("back")) {
            path.moveTo(14,7);path.lineTo(5,16);path.lineTo(14,25);
            path.moveTo(5,16);path.lineTo(28,16);g.draw(path);
        } else if (name.equals("mods")) {
            g.fillRect(5,5,9,9);g.fillRect(18,5,9,9);
            g.fillRect(5,18,9,9);g.fillRect(18,18,9,9);
        } else if (name.equals("lan")) {
            g.draw(new Ellipse2D.Double(6,5,7,7));g.draw(new Ellipse2D.Double(20,7,6,6));
            path.moveTo(3,26);path.lineTo(3,22);path.curveTo(3,13,17,13,17,22);path.lineTo(17,26);
            path.moveTo(21,17);path.curveTo(27,17,29,20,29,25);g.draw(path);
        } else if (name.equals("exit")) {
            path.moveTo(13,5);path.lineTo(5,5);path.lineTo(5,27);path.lineTo(13,27);
            path.moveTo(11,16);path.lineTo(28,16);
            path.moveTo(22,10);path.lineTo(28,16);path.lineTo(22,22);g.draw(path);
        } else if (name.equals("chart")) {
            path.moveTo(4,5);path.lineTo(4,27);path.lineTo(28,27);g.draw(path);
            g.fillRect(8,18,4,6);g.fillRect(15,12,4,12);g.fillRect(22,6,4,18);
        } else if (name.equals("award")) {
            path.moveTo(9,5);path.lineTo(23,5);path.lineTo(23,13);
            path.curveTo(23,24,9,24,9,13);path.closePath();g.draw(path);
            path.reset();path.moveTo(9,8);path.lineTo(4,8);path.lineTo(4,11);path.quadTo(4,17,10,17);
            path.moveTo(23,8);path.lineTo(28,8);path.lineTo(28,11);path.quadTo(28,17,22,17);
            path.moveTo(16,22);path.lineTo(16,27);path.moveTo(10,27);path.lineTo(22,27);g.draw(path);
        } else throw new IllegalArgumentException("Unknown UI icon " + name);
        g.dispose();
        BufferedImage result = resize(image, width, height);
        image.flush();
        return result;
    }

    public static BufferedImage roundedMask(int width,int height,int radius) {
        BufferedImage image=new BufferedImage(width*4,height*4,BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.fillRoundRect(0,0,width*4,height*4,radius*8,radius*8);g.dispose();
        BufferedImage result=resize(image,width,height);image.flush();return result;
    }
}
