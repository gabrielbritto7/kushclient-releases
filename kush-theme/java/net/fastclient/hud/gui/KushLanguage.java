package net.fastclient.hud.gui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_332;
import net.minecraft.class_327;
import net.minecraft.class_2561;

/** Display translations only: identifiers, saved options and Minecraft language stay intact. */
public final class KushLanguage {
    private static final Map<String,String> PT = load();
    private static final Map<String,String> EN = reverse();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("kushmod/language.txt");
    private static boolean portuguese = preference();
    private KushLanguage() {}
    private static Map<String,String> load() {
        try(var stream=KushLanguage.class.getResourceAsStream("/assets/fastclient-hud/lang/kush_pt_br.json")) {
            if(stream==null)throw new IllegalStateException("Missing Kush language dictionary");
            return Map.copyOf(new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),
                new TypeToken<Map<String,String>>(){}.getType()));
        } catch(Exception e){throw new IllegalStateException("Unable to load Kush translations",e);}
    }
    private static Map<String,String> reverse() {
        Map<String,String> result=new HashMap<>();
        // Avoid relying on Map iteration order for canonical, duplicate labels.
        PT.forEach((en,pt)->{if(!en.equals(pt))result.putIfAbsent(pt,en);});
        result.put("ATIVO","ACTIVE");result.put("INATIVO","INACTIVE");
        result.put("Um jogador","Singleplayer");
        result.put("ESC para voltar · Alterações salvas automaticamente","ESC to go back - changes save automatically");
        return Map.copyOf(result);
    }
    private static boolean preference() {
        try {return !Files.readString(FILE).trim().equals("en_us");}
        catch(Exception e){return true;}
    }
    public static String code(){return portuguese?"PT":"EN";}
    public static boolean isPortuguese(){return portuguese;}
    public static boolean toggle() {
        boolean next=!portuguese;
        try {
            Files.createDirectories(FILE.getParent());
            Path temp=Files.createTempFile(FILE.getParent(),"language-",".tmp");
            try {
                Files.writeString(temp,next?"pt_br":"en_us",StandardCharsets.UTF_8);
                try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException e){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
            } finally {Files.deleteIfExists(temp);}
            portuguese=next;return true;
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger("Kush").warn("Could not save language",e);return false;}
    }
    public static String translate(String text) {
        if(text==null || text.isEmpty())return text;
        String exact=portuguese?PT.get(text):EN.get(text);
        if(exact!=null)return exact;
        // Counts retain their numerical value and spacing.
        if(portuguese && text.matches("[0-9]+ layouts"))return text.replace(" layouts"," opções");
        if(!portuguese && text.matches("[0-9]+ opções"))return text.replace(" opções"," layouts");
        if(text.endsWith(" Settings"))return portuguese?translate(text.substring(0,text.length()-9))+" · Opções":text;
        return text;
    }
    public static void drawButton(class_332 g,class_327 font,int x,int y,int w,int h,int mx,int my) {
        boolean hover=hit(x,y,w,h,mx,my);
        FastClientUI.borderedRoundedRect(g,x,y,w,h,0,hover?0xFF352228:0xFF1D1A1E,0xFF110F12);
        if(hover)g.method_25294(x+1,y+h-2,x+w-1,y+h-1,0xFFE53542);
        class_2561 label=FastClientFonts.strong(code());float scale=1.5f;
        g.method_51448().pushMatrix();
        g.method_51448().translate(x+(w-font.method_27525(label)*scale)/2f,y+(h-9*scale)/2f);
        g.method_51448().scale(scale,scale);
        g.method_51439(font,label,0,0,0xFFF5EFF1,false);g.method_51448().popMatrix();
    }
    public static boolean hit(int x,int y,int w,int h,double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
}
