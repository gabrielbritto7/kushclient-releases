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

    public static BufferedImage icon(String name, int width, int height) {
        // Draw simple chrome icons at 4x, then reduce once with alpha coverage.
        BufferedImage image = new BufferedImage(width * 4, height * 4, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.scale(width * 4 / 32.0, height * 4 / 32.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
            path.moveTo(3, 24); path.lineTo(3, 7); path.lineTo(12, 7);
            path.lineTo(15, 10); path.lineTo(28, 10);
            g.draw(path);
            path.reset();
            path.moveTo(4, 24); path.lineTo(8, 14); path.lineTo(29, 14);
            path.lineTo(25, 25); path.lineTo(4, 25); path.closePath();
            g.fill(path);
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
            g.fillRect(4,4,9,9);g.fillRect(19,4,9,9);
            g.fillRect(4,19,9,9);g.fillRect(19,19,9,9);
        } else if (name.equals("lan")) {
            g.fill(new Ellipse2D.Double(4,5,8,8));g.fill(new Ellipse2D.Double(20,5,8,8));
            path.moveTo(2,26);path.curveTo(2,13,14,13,14,26);
            path.moveTo(18,26);path.curveTo(18,13,30,13,30,26);g.draw(path);
        } else if (name.equals("exit")) {
            path.moveTo(14,4);path.lineTo(4,4);path.lineTo(4,28);path.lineTo(14,28);
            path.moveTo(11,16);path.lineTo(28,16);
            path.moveTo(22,10);path.lineTo(28,16);path.lineTo(22,22);g.draw(path);
        } else if (name.equals("chart")) {
            g.fillRect(4,19,5,9);g.fillRect(13,11,5,17);g.fillRect(22,4,5,24);
        } else if (name.equals("award")) {
            path.moveTo(8,4);path.lineTo(24,4);path.lineTo(24,14);
            path.curveTo(24,24,8,24,8,14);path.closePath();g.draw(path);
            path.reset();path.moveTo(8,7);path.lineTo(3,7);path.lineTo(3,14);path.lineTo(8,17);
            path.moveTo(24,7);path.lineTo(29,7);path.lineTo(29,14);path.lineTo(24,17);
            path.moveTo(16,22);path.lineTo(16,28);path.moveTo(10,28);path.lineTo(22,28);g.draw(path);
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
