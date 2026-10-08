package net.fastclient.core.equip;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.fastclient.core.data.*;
import net.fastclient.core.provider.CosmeticProvider;

/** Kush public capes plus user-authored local descriptors; no third-party backend session. */
public final class KushCatalogProvider implements CosmeticProvider {
    private static volatile KushCatalogProvider current;
    public static KushCatalogProvider current(){return current;}
    public List<CatalogEntry> loaded(){return entries;}
    private final Map<String,JsonObject> models=new ConcurrentHashMap<>();
    private final Set<String> accessoryTextures=ConcurrentHashMap.newKeySet();
    private final Map<String,int[]> animations=new ConcurrentHashMap<>();
    public int[] animation(String id){return animations.get(id);}
    private final Map<String,Set<String>> pageIds=new ConcurrentHashMap<>();
    public Set<String> pageIds(String category,String query,int page){return pageIds.get(category+":"+query.trim()+":"+page);}
    private final Map<String,JsonObject> pages=new LinkedHashMap<>();
    private static final String SITE="https://kush-archives.com.br";
    private static final Set<String> TYPES=Set.of("cape","hats","face","arm","boots","back","shields","wings","pets","auras","emotes");
    private static final int LIMIT=8*1024*1024;
    private final Path root;
    private final UUID owner;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final Map<String,byte[]> assets=Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String,String> hashes=new ConcurrentHashMap<>();
    private final Map<String,Integer> cosmeticaFrames=new ConcurrentHashMap<>();
    private volatile List<CatalogEntry> entries=List.of();
    private volatile Set<String> selected=Set.of();
    private volatile PlayerCosmetics snapshot=PlayerCosmetics.emptyInstance();
    private volatile boolean resolved;
    private volatile boolean catalogReady;
    public KushCatalogProvider(Path root,UUID owner) {
        this.root=root.toAbsolutePath().normalize();this.owner=owner;current=this;
        try {
            Files.createDirectories(this.root.resolve("assets"));
            Path file=this.root.resolve("catalog.json");
            if(!Files.exists(file))atomic(file,"{\"schema\":1,\"items\":[]}");
            Path loadout=loadout();
            if(Files.exists(loadout)) {
                JsonArray array=JsonParser.parseString(Files.readString(loadout)).getAsJsonArray();
                Set<String> ids=new HashSet<>();for(JsonElement e:array)if(ids.size()<11)ids.add(e.getAsString());
                selected=Set.copyOf(ids);
            }
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger("Kush").warn("Could not open cosmetic preferences",e);}
    }
    private Path loadout(){return root.resolve("loadout-"+owner+".json");}
    public static String site(){return SITE;}
    private static void atomic(Path file,String text)throws IOException {
        Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),"kush-",".tmp");
        try{Files.writeString(temp,text,StandardCharsets.UTF_8);
            try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
    private byte[] get(String url,int limit)throws Exception {
        URI uri=URI.create(url);
        if(!"https".equals(uri.getScheme()) || !Set.of("kush-archives.com.br","api.cloaks.gg","cdn.cosmetica.cc").contains(uri.getHost()) || uri.getPort()!=-1 || uri.getUserInfo()!=null)throw new IOException("Unexpected asset host");
        HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).header("User-Agent","Mozilla/5.0 KushMod/0.3.7").GET().build();
        HttpResponse<InputStream> response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(InputStream stream=response.body()) {
            if(response.statusCode()!=200)throw new IOException("Catalog HTTP "+response.statusCode());
            byte[] data=stream.readNBytes(limit+1);if(data.length>limit)throw new IOException("Catalog size limit");return data;
        }
    }
    private static CatalogEntry cape(String id,String name,String ref) {
        return new CatalogEntry(id,name,"cape",true,Attach.BODY,0,null,ref,null,null,null,null,null,null,null,null,null,null,null,0,null);
    }
    private void addCosmetica(List<CatalogEntry> target,JsonObject response) {
        Set<String> seen=new HashSet<>();target.forEach(e->{if(e.id().startsWith("cosmetica-"))seen.add(e.id().substring(10));});
        for(JsonElement element:response.getAsJsonArray("results")) {
            if(target.size()>=240)break;
            try {
                JsonObject row=element.getAsJsonObject();
                if(!"animated_texture_cosmetic".equals(row.get("type").getAsString()))continue;
                JsonObject item=row.getAsJsonObject("animatedTextureCosmetic");
                if(!"cape".equals(item.get("type").getAsString()) || item.get("state").getAsInt()!=2)continue;
                String id=item.get("id").getAsString(),ref=item.get("texture").getAsString();
                if(!id.matches("[a-zA-Z0-9_-]{1,64}") || !seen.add(id))continue;
                URI uri=URI.create(ref);
                if(!"https".equals(uri.getScheme()) || !"cdn.cosmetica.cc".equals(uri.getHost()) || uri.getPort()!=-1 || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || !uri.getPath().matches("/assets/[a-f0-9]{3}/[a-f0-9-]{36}\\.png"))continue;
                int frames=item.get("frames").getAsInt(),ticks=item.get("ticksPerFrame").getAsInt();
                if(frames<1 || frames>256 || ticks<1 || ticks>200)continue;
                cosmeticaFrames.put(ref,frames);
                target.add(new CatalogEntry("cosmetica-"+id,"[Cosmetica] "+item.get("name").getAsString(),"cape",true,Attach.BODY,0,null,ref,null,null,null,null,null,null,null,null,null,null,null,frames>1?ticks*50:0,null));
            }catch(RuntimeException ignored){}
        }
    }
    private void loadCosmetica(List<CatalogEntry> target) {
        Path file=root.resolve("cosmetica-catalog.json");
        try {
            String payload="{\"query\":\"\",\"pageSize\":20,\"page\":1,\"attachments\":[\"cape\"],\"sortBy\":\"most-popular\"}";
            HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.cloaks.gg/search/cosmetics"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("User-Agent","Mozilla/5.0 KushMod/0.3.7")
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<InputStream> response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(InputStream input=response.body()) {
                if(response.statusCode()!=200)throw new IOException("Cosmetica HTTP "+response.statusCode());
                byte[] bytes=input.readNBytes(1024*1024+1);if(bytes.length>1024*1024)throw new IOException("Cosmetica catalog size");
                String text=new String(bytes,StandardCharsets.UTF_8);JsonObject obj=JsonParser.parseString(text).getAsJsonObject();
                addCosmetica(target,obj);atomic(file,text);
            }
        }catch(Exception e) {
            org.slf4j.LoggerFactory.getLogger("Kush").debug("Cosmetica unavailable; using cached capes");
            try{if(Files.exists(file) && Files.size(file)<=1024*1024)addCosmetica(target,JsonParser.parseString(Files.readString(file)).getAsJsonObject());}
            catch(Exception ignored){}
        }
    }

    private static boolean cdn(String ref,String extension) {
        try{URI u=URI.create(ref);return "https".equals(u.getScheme()) && "cdn.cosmetica.cc".equals(u.getHost()) && u.getPort()==-1 && u.getUserInfo()==null && u.getQuery()==null && u.getFragment()==null && u.getPath().matches("/assets/[a-f0-9]{3}/[a-f0-9-]{36}\\."+extension);}catch(Exception e){return false;}
    }
    private void addAccessories(List<CatalogEntry> target,JsonObject response) {
        Set<String> seen=new HashSet<>();target.forEach(e->seen.add(e.id()));
        for(JsonElement row:response.getAsJsonArray("results"))try {
            JsonObject object=row.getAsJsonObject();
            if(!"accessory".equals(object.get("type").getAsString()))continue;
            JsonObject a=object.getAsJsonObject("accessory");
            String id=a.get("id").getAsString(),model=a.get("model").getAsString(),texture=a.get("texture").getAsString();
            if(a.get("state").getAsInt()!=2 || !id.matches("[a-zA-Z0-9_-]{1,64}") || !cdn(model,"json") || !cdn(texture,"png"))continue;
            int frames=Math.abs(a.get("frames").getAsInt()),ticks=a.get("ticksPerFrame").getAsInt();
            if(frames<1 || frames>256 || ticks<1 || ticks>200)continue;
            Attach attach=Attach.of(a.get("attachment").getAsString(),null);if(attach==null)continue;
            String name=a.get("name").getAsString(),lower=name.toLowerCase(Locale.ROOT);
            String category=switch(attach){case HEAD->lower.contains("halo")?"face":"hats";case BODY->lower.contains("wing")?"wings":"back";case LEFT_ARM,RIGHT_ARM->"arm";case LEFT_LEG,RIGHT_LEG,BOTH_LEGS->"boots";};
            JsonArray offset=a.getAsJsonArray("offset");if(offset==null || offset.size()!=6)continue;
            for(JsonElement value:offset)if(!Double.isFinite(value.getAsDouble()) || Math.abs(value.getAsDouble())>128)throw new IllegalArgumentException();
            JsonObject metadata=new JsonObject();metadata.add("offset",offset.deepCopy());metadata.addProperty("attachment",attach.name());
            models.put(model,metadata);accessoryTextures.add(texture);
            animations.put("cosmetica-"+id,new int[]{frames,a.get("frames").getAsInt()>0?ticks*50:0});
            if(seen.add("cosmetica-"+id))target.add(new CatalogEntry("cosmetica-"+id,"[Cosmetica] "+name,category,true,attach,1|4|8,model,texture,null,null,"none",null,null,null,null,null,null,null,null,null,null));
        }catch(RuntimeException ignored){}
    }
    private JsonObject accessoryPage(String query,String attachment,int page)throws Exception {
        JsonObject payload=new JsonObject();payload.addProperty("query",query);payload.addProperty("pageSize",20);payload.addProperty("page",page);payload.addProperty("sortBy","most-popular");JsonArray filters=new JsonArray();filters.add(attachment);payload.add("attachments",filters);
        HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.cloaks.gg/search/cosmetics")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("User-Agent","Mozilla/5.0 KushMod/0.3.7").POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
        HttpResponse<InputStream> response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(InputStream input=response.body()){if(response.statusCode()!=200)throw new IOException("Catalog HTTP "+response.statusCode());byte[] b=input.readNBytes(1024*1024+1);if(b.length>1024*1024)throw new IOException("Catalog limit");return JsonParser.parseString(new String(b,StandardCharsets.UTF_8)).getAsJsonObject();}
    }
    private void savePages()throws IOException {JsonArray a=new JsonArray();pages.values().forEach(a::add);atomic(root.resolve("cosmetica-accessories.json"),a.toString());}
    private void loadAccessories(List<CatalogEntry> target) {
        Path file=root.resolve("cosmetica-accessories.json");
        try{if(Files.exists(file) && Files.size(file)<16*1024*1024){int n=0;for(JsonElement e:JsonParser.parseString(Files.readString(file)).getAsJsonArray()){JsonObject o=e.getAsJsonObject();pages.put("cached-"+n++,o);addAccessories(target,o);addCosmetica(target,o);}}}catch(Exception ignored){}
        for(String[] spec:new String[][]{{"","head"},{"","body"},{"","arm"},{"","leg"},{"wings","body"},{"halo","head"}})try {
            JsonObject result=accessoryPage(spec[0],spec[1],1);pages.put(spec[0]+":"+spec[1]+":1",result);addAccessories(target,result);
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger("Kush").debug("Accessory catalog unavailable; using cache");}
        try{savePages();}catch(IOException ignored){}
    }
    public synchronized int search(String category,String query,int page) throws Exception {
        String attachment=switch(category){case "hats","face"->"head";case "arm"->"arm";case "boots"->"leg";case "back","wings"->"body";default->"cape";};
        String term=query.isBlank()?(category.equals("wings")?"wings":category.equals("face")?"halo":""):query;
        JsonObject result=accessoryPage(term,attachment,page);
        List<CatalogEntry> next=new ArrayList<>(entries);
        if(attachment.equals("cape")){Set<String> existing=new HashSet<>();next.forEach(e->existing.add(e.id()));List<CatalogEntry> more=new ArrayList<>();addCosmetica(more,result);for(CatalogEntry e:more)if(existing.add(e.id()))next.add(e);}
        else addAccessories(next,result);
        Set<String> ids=new HashSet<>();for(JsonElement row:result.getAsJsonArray("results")){JsonObject o=row.getAsJsonObject();JsonObject item=o.has("accessory")?o.getAsJsonObject("accessory"):o.has("animatedTextureCosmetic")?o.getAsJsonObject("animatedTextureCosmetic"):null;if(item!=null)ids.add("cosmetica-"+item.get("id").getAsString());}
        pageIds.put(category+":"+query.trim()+":"+page,Set.copyOf(ids));
        entries=List.copyOf(next);resolved=false;
        pages.put(term+":"+attachment+":"+page,result);while(pages.size()>100)pages.remove(pages.keySet().iterator().next());savePages();
        return result.has("estimatedPages")?Math.max(page,result.get("estimatedPages").getAsInt()):page;
    }

    @Override public synchronized List<CatalogEntry> catalog() {
        List<CatalogEntry> next=new ArrayList<>();boolean remoteOk=false;
        try {
            int total=1;
            for(int page=1;page<=Math.min(25,(total+7)/8);page++) {
                byte[] bytes=get(SITE+"/api/kush?kind=cape&size=8&page="+page,1024*1024);
                JsonObject obj=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
                if(obj.get("schema").getAsInt()!=1 || !"cape".equals(obj.get("kind").getAsString()))throw new IOException("Unexpected catalog schema");
                total=Math.min(200,obj.get("total").getAsInt());
                for(JsonElement element:obj.getAsJsonArray("items")) {
                    JsonObject item=element.getAsJsonObject();String id=UUID.fromString(item.get("id").getAsString()).toString();
                    String digest=item.get("sha256").getAsString();if(!digest.matches("[a-f0-9]{64}"))continue;
                    String ref=SITE+"/api/kush?resource=texture&id="+id;
                    String previous=hashes.put(ref,digest);if(previous!=null && !previous.equals(digest))assets.remove(ref);next.add(cape("kush-site-"+id,item.get("name").getAsString(),ref));
                }
                atomic(root.resolve("site-catalog-"+page+".json"),new String(bytes,StandardCharsets.UTF_8));
            }
            remoteOk=true;
        }catch(Exception e){
            org.slf4j.LoggerFactory.getLogger("Kush").debug("Kush catalog unavailable; retaining cached entries");
            next.addAll(entries.stream().filter(e2->e2.id().startsWith("kush-site-")).toList());
            // Cached metadata is useful across a game restart while offline.
            if(next.isEmpty())for(int page=1;page<=25;page++) {
                Path file=root.resolve("site-catalog-"+page+".json");if(!Files.exists(file))break;
                try{JsonObject obj=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    for(JsonElement el:obj.getAsJsonArray("items")) {
                        JsonObject item=el.getAsJsonObject();String id=UUID.fromString(item.get("id").getAsString()).toString();
                        String digest=item.get("sha256").getAsString();if(!digest.matches("[a-f0-9]{64}"))continue;
                        String ref=SITE+"/api/kush?resource=texture&id="+id;hashes.put(ref,digest);
                        next.add(cape("kush-site-"+id,item.get("name").getAsString(),ref));
                    }
                }catch(Exception ignored){}
            }
        }
        loadCosmetica(next);
        loadAccessories(next);
        try {
            Path file=root.resolve("catalog.json");if(Files.size(file)>1024*1024)throw new IOException("Local catalog size limit");
            JsonObject obj=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(obj.get("schema").getAsInt()!=1)throw new IOException("Unsupported local catalog schema");
            Set<String> seen=new HashSet<>();
            for(JsonElement el:obj.getAsJsonArray("items")) {
                if(next.size()>=600)break;
                CatalogEntry entry=new Gson().fromJson(el,CatalogEntry.class);
                if(entry.id()==null || !entry.id().matches("[a-zA-Z0-9_-]{1,80}") || (entry.id().startsWith("kush-site-") || entry.id().startsWith("cosmetica-")) || !TYPES.contains(entry.category()) || !seen.add(entry.id()))continue;
                // Local entries belong to this private catalog; no fabricated purchase/coin flow.
                JsonObject owned=el.getAsJsonObject().deepCopy();owned.addProperty("defaultOwned",true);
                next.add(new Gson().fromJson(owned,CatalogEntry.class));
            }
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger("Kush").warn("Invalid local cosmetics catalog",e);}
        entries=List.copyOf(next);resolved=false;catalogReady=true;
        return !remoteOk && entries.isEmpty()?null:entries;
    }
    @Override public boolean canEquipOwn(){return true;}
    @Override public Set<String> owned(UUID uuid){return owner.equals(uuid)?Set.copyOf(entries.stream().map(CatalogEntry::id).toList()):Set.of();}
    @Override public synchronized boolean replaceLoadout(Set<String> ids) {
        Set<String> known=new HashSet<>();Set<String> categories=new HashSet<>();
        for(CatalogEntry e:entries)if(ids.contains(e.id()) && categories.add(e.category()))known.add(e.id());
        try{atomic(loadout(),new Gson().toJson(known));selected=Set.copyOf(known);resolved=false;return true;}
        catch(IOException e){org.slf4j.LoggerFactory.getLogger("Kush").warn("Could not save cosmetic loadout",e);return false;}
    }
    @Override public synchronized PlayerCosmetics fetch(UUID uuid,String name) {
        if(!owner.equals(uuid))return PlayerCosmetics.emptyInstance();
        if(!catalogReady)return null;
        if(resolved)return snapshot;
        List<ModelCosmetic> hats=new ArrayList<>(),face=new ArrayList<>(),arm=new ArrayList<>(),boots=new ArrayList<>(),back=new ArrayList<>();
        List<ShieldSkinCosmetic> shields=new ArrayList<>();List<WingCosmetic>wings=new ArrayList<>();
        List<PetCosmetic>pets=new ArrayList<>();List<AuraCosmetic>auras=new ArrayList<>();List<EmoteCosmetic>emotes=new ArrayList<>();CapeCosmetic cape=null;
        for(CatalogEntry e:entries)if(selected.contains(e.id())) {
            try {
                // Reuse the native loader's format and animation support.
                var method=LocalCosmetics.class.getDeclaredMethod("resolve",CatalogEntry.class,CosmeticProvider.class);method.setAccessible(true);
                Object o=method.invoke(null,e,this);if(o==null)continue;
                switch(e.category()) {
                    case "cape"->cape=(CapeCosmetic)o;
                    case "hats"->hats.add((ModelCosmetic)o);case "face"->face.add((ModelCosmetic)o);
                    case "arm"->arm.add((ModelCosmetic)o);case "boots"->boots.add((ModelCosmetic)o);case "back"->back.add((ModelCosmetic)o);
                    case "shields"->shields.add((ShieldSkinCosmetic)o);case "wings"->wings.add((WingCosmetic)o);
                    case "pets"->pets.add((PetCosmetic)o);case "auras"->auras.add((AuraCosmetic)o);case "emotes"->emotes.add((EmoteCosmetic)o);
                }
            }catch(Exception ex){org.slf4j.LoggerFactory.getLogger("Kush").warn("Could not restore cosmetic {}",e.id());}
        }
        snapshot=new PlayerCosmetics(hats,face,arm,boots,back,shields,wings,cape,pets,auras,emotes);resolved=true;return snapshot;
    }
    @Override public byte[] assetBytes(String ref) {
        if(ref==null)return null;
        byte[] cached=assets.get(ref);if(cached!=null)return cached;
        try {
            byte[] bytes;
            if(ref.startsWith("https://")) {
                String expected=hashes.get(ref);boolean cosmetica=cosmeticaFrames.containsKey(ref) || accessoryTextures.contains(ref) || models.containsKey(ref);
                if(expected==null && !cosmetica)throw new IOException("Unlisted remote asset");
                String cacheKey=cosmetica?"cosmetica-"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ref.getBytes(StandardCharsets.UTF_8))):expected;
                Path disk=root.resolve("cache/"+cacheKey+(models.containsKey(ref)?".json":".png"));
                bytes=Files.exists(disk) && Files.size(disk)<=2*1024*1024?Files.readAllBytes(disk):null;
                if(bytes==null || (!cosmetica && !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(expected))) {
                    bytes=get(ref,2*1024*1024);
                    if(!cosmetica && !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(expected))throw new IOException("Texture hash mismatch");
                    Files.createDirectories(disk.getParent());Files.write(disk,bytes);
                }
            }else {
                Path base=root.resolve("assets").toRealPath();Path p=base.resolve(ref).normalize();
                if(!p.startsWith(base) || !p.toRealPath().startsWith(base) || Files.size(p)>LIMIT)throw new IOException("Invalid local asset");bytes=Files.readAllBytes(p);
            }
            if(bytes.length>LIMIT)return null;
            if(models.containsKey(ref)) { JsonObject model=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();if(!model.has("elements") || model.getAsJsonArray("elements").size()>2048)throw new IOException("Model element limit"); }
            else if(ref.endsWith(".png") || ref.startsWith("https://")) {
                try(var input=javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                    var readers=javax.imageio.ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Invalid PNG");
                    var reader=readers.next();try {
                        reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);
                        if(!reader.getFormatName().equalsIgnoreCase("PNG") || w<1 || h<1 || w>4096 || h>8192 || (long)w*h>16_777_216)throw new IOException("Texture dimensions");
                        Integer frames=cosmeticaFrames.get(ref);if(frames!=null && (w%2!=0 || h!=(w/2)*frames))throw new IOException("Cape animation layout");
                    }finally{reader.dispose();}
                }
            }
            synchronized(assets){
                int used=assets.values().stream().mapToInt(a->a.length).sum();
                while(!assets.isEmpty()&&(assets.size()>=32 || used+bytes.length>32*1024*1024)) {
                    String first=assets.keySet().iterator().next();used-=assets.remove(first).length;
                }assets.put(ref,bytes);
            }return bytes;
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger("Kush").warn("Cosmetic asset unavailable: {}",e.getClass().getSimpleName());return null;}
    }
    @Override public String assetText(String ref){byte[] b=assetBytes(ref);if(b==null)return null;String text=new String(b,StandardCharsets.UTF_8);JsonObject metadata=models.get(ref);if(metadata==null)return text;JsonObject model=JsonParser.parseString(text).getAsJsonObject();model.remove("display");model.add("kushCosmetica",metadata.deepCopy());return model.toString();}
}
