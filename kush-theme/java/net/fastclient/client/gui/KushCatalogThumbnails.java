package net.fastclient.client.gui;

import java.io.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import net.fastclient.core.data.CatalogEntry;
import net.fastclient.core.equip.KushCatalogProvider;
import net.fastclient.client.render.CosmeticTextures;
import net.fastclient.hud.gui.KushImageResampler;

/** Official catalog thumbnails, fetched and reduced off the render thread. */
public final class KushCatalogThumbnails {
    public record Image(byte[] png,int width,int height) {}
    private static final Map<String,Image> READY=Collections.synchronizedMap(new LinkedHashMap<>(64,.75f,true));
    private static final Map<String,Long> FAILED=new ConcurrentHashMap<>();
    private static final Set<String> PENDING=ConcurrentHashMap.newKeySet();
    private static final ThreadPoolExecutor WORK=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(40),r->{Thread t=new Thread(r,"Kush-Thumbnail");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    static {javax.imageio.spi.IIORegistry.getDefaultInstance().registerServiceProvider(new com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi());}
    private KushCatalogThumbnails() {}
    public static boolean failed(String id){return FAILED.getOrDefault(id,0L)>System.currentTimeMillis();}
    public static Image get(CatalogEntry entry) {
        Image image=READY.get(entry.id());if(image!=null)return image;
        if(failed(entry.id()) || !PENDING.add(entry.id()))return null;
        KushCatalogProvider provider=KushCatalogProvider.current();
        if(provider==null){PENDING.remove(entry.id());return null;}
        try{WORK.execute(()->prepare(entry,provider));}catch(RejectedExecutionException ex){PENDING.remove(entry.id());}
        return null;
    }
    private static void prepare(CatalogEntry entry,KushCatalogProvider provider) {
        try {
            // Explicit registration avoids ImageIO's context-classloader discovery in Fabric.
            String ref=provider.thumbnail(entry.id());boolean cape=ref==null && entry.category().equals("cape");
            if(cape)ref=entry.texture();
            if(ref==null)throw new IOException("No item thumbnail");
            byte[] raw=provider.assetBytes(ref);if(raw==null)throw new IOException("No thumbnail bytes");
            BufferedImage source;
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
                var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Unknown thumbnail format");
                var reader=readers.next();try{
                    reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(w<1 || h<1 || w>2048 || h>8192 || (long)w*h>8_388_608L)throw new IOException("Thumbnail bounds");
                    source=reader.read(0);
                }finally{reader.dispose();}
            }
            if(cape){int unit=Math.max(1,source.getWidth()/64);BufferedImage face=new BufferedImage(10*unit,16*unit,BufferedImage.TYPE_INT_ARGB);
                var g=face.createGraphics();try{g.drawImage(source,0,0,10*unit,16*unit,unit,unit,11*unit,17*unit,null);}finally{g.dispose();}source.flush();source=face;}
            // Official WebP thumbnails contain eight vertical square viewing angles.
            // Keep one complete view, rather than squeezing the whole strip into a card.
            if(!cape && source.getHeight()>source.getWidth() && source.getHeight()%source.getWidth()==0 && source.getHeight()/source.getWidth()<=16) {
                int size=source.getWidth();BufferedImage frame=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
                var g=frame.createGraphics();try{g.drawImage(source,0,0,size,size,0,0,size,size,null);}finally{g.dispose();}source.flush();source=frame;
            }
            source=KushImageResampler.trimAlpha(source);
            int[] fit=KushImageResampler.contain(source.getWidth(),source.getHeight(),128,128);
            BufferedImage small=KushImageResampler.resize(source,fit[0],fit[1]);source.flush();
            ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(small,"PNG",out);small.flush();
            Image image=new Image(out.toByteArray(),fit[0],fit[1]);
            synchronized(READY){while(READY.size()>=64)READY.remove(READY.keySet().iterator().next());READY.put(entry.id(),image);}
            FAILED.remove(entry.id());
        }catch(Exception failure){FAILED.put(entry.id(),System.currentTimeMillis()+30_000L);org.slf4j.LoggerFactory.getLogger("Kush").debug("Catalog thumbnail unavailable for {}: {}",entry.id(),failure.toString());}
        finally{PENDING.remove(entry.id());}
    }
}
