package net.fastclient.hud.modules.impl.render;

import net.fastclient.hud.modules.Module;
import net.fastclient.hud.modules.Category;
import net.fastclient.hud.modules.settings.*;

public final class CapePhysics extends Module {
    private static CapePhysics instance;
    private final NumberSetting strength=register(new NumberSetting("motion_strength","Cape motion intensity",100,0,200,5));
    private final NumberSetting wind=register(new NumberSetting("wind_strength","Wind intensity",35,0,100,5));
    private final NumberSetting damping=register(new NumberSetting("damping","Cape damping",65,0,100,5));
    private final ModeSetting quality=register(new ModeSetting("quality","Cape geometry quality","balanced",new String[]{"low","balanced","high"}));
    private final BooleanSetting preview=register(new BooleanSetting("preview_physics","Animate cape in the catalog preview",true));
    public CapePhysics(){super("CapePhysics","Cape Physics","Smooth cape movement with wind and adjustable physics",Category.RENDER);instance=this;setEnabled(true);}
    public static CapePhysics current(){return instance;}
    public float strength(){return strength.getFloatValue()/100;}
    public float wind(){return wind.getFloatValue()/100;}
    public float damping(){return damping.getFloatValue()/100;}
    public boolean preview(){return preview.isEnabled();}
    public int segments(){return quality.is("low")?8:quality.is("high")?24:16;}
}
