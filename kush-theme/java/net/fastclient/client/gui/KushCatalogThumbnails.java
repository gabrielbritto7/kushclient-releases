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
    public record Image(byte[] png,int width,int height,int frames,int delay) {public Image(byte[] png,int width,int height){this(png,width,height,1,0);}}
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
            int frames=1,delay=0;BufferedImage small;int[] fit;
            if(cape){small=net.fastclient.hud.gui.KushCapeCards.bake(source);source.flush();fit=new int[]{128,128};frames=net.fastclient.hud.gui.KushCapeCards.FRAMES;delay=net.fastclient.hud.gui.KushCapeCards.DELAY;}
            else{BufferedImage view=net.fastclient.hud.gui.KushThumbnailFrames.firstView(raw,source);source.flush();source=KushImageResampler.trimAlpha(view);
                fit=KushImageResampler.contain(source.getWidth(),source.getHeight(),128,128);small=KushImageResampler.resize(source,fit[0],fit[1]);source.flush();}
            ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(small,"PNG",out);small.flush();
            Image image=new Image(out.toByteArray(),fit[0],fit[1],frames,delay);
            synchronized(READY){while(READY.size()>=64)READY.remove(READY.keySet().iterator().next());READY.put(entry.id(),image);}
            FAILED.remove(entry.id());
        }catch(Exception failure){FAILED.put(entry.id(),System.currentTimeMillis()+30_000L);org.slf4j.LoggerFactory.getLogger("Kush").debug("Catalog thumbnail unavailable for {}: {}",entry.id(),failure.toString());}
        finally{PENDING.remove(entry.id());}
    }
}
