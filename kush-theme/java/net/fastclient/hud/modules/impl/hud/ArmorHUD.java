/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1304
 *  net.minecraft.class_1799
 *  net.minecraft.class_332
 */
package net.fastclient.hud.modules.impl.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.modules.Category;
import net.fastclient.hud.modules.Module;
import net.fastclient.hud.modules.settings.BooleanSetting;
import net.fastclient.hud.modules.settings.ModeSetting;
import net.fastclient.hud.modules.settings.NumberSetting;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_332;

@Environment(value=EnvType.CLIENT)
public class ArmorHUD
extends Module {
    private final BooleanSetting showDurability = this.register(new BooleanSetting("show_durability", "Show durability values", true));
    private final ModeSetting displayMode = this.register(new ModeSetting("display_mode", "Durability display format", "value", new String[]{"value", "percentage", "both"}));
    private final BooleanSetting horizontal = this.register(new BooleanSetting("horizontal", "Horizontal layout", false));
    private final BooleanSetting background = this.register(new BooleanSetting("background", "Show background", true));
    private final NumberSetting bgOpacity = this.register(new NumberSetting("opacity", "Background opacity", 80.0, 0.0, 255.0, 5.0));

    private final BooleanSetting showHeld=register(new BooleanSetting("show_held","Show held items",true));
    private final BooleanSetting showBars=register(new BooleanSetting("show_bars","Show durability bars",true));
    private class_1799[] items(){
        if(mc.field_1724==null)return new class_1799[0];
        var p=mc.field_1724;
        return showHeld.isEnabled()?new class_1799[]{p.method_6118(class_1304.field_6169),p.method_6118(class_1304.field_6174),p.method_6118(class_1304.field_6172),p.method_6118(class_1304.field_6166),p.method_6047(),p.method_6079()}:new class_1799[]{p.method_6118(class_1304.field_6169),p.method_6118(class_1304.field_6174),p.method_6118(class_1304.field_6172),p.method_6118(class_1304.field_6166)};
    }
    public ArmorHUD() {
        super("ArmorHUD", "Shows equipped armor with durability", Category.HUD);
        this.bgOpacity.visibleWhen(this.background::isEnabled);
        this.displayMode.visibleWhen(this.showDurability::isEnabled);
        this.showBars.visibleWhen(this.showDurability::isEnabled);
    }

    @Override
    public void onRender(class_332 graphics, float tickDelta) {
        if (!this.isInGame()) {
            return;
        }
        int x = this.getHudX();
        int y = this.getHudY();
        float scale = this.getHudScale();
        graphics.method_51448().pushMatrix();
        graphics.method_51448().translate((float)x, (float)y);
        graphics.method_51448().scale(scale, scale);
        x=0;y=0;
        class_1799[] armor=items();
        int textWidth = 0;
        if (((Boolean)this.showDurability.getValue()).booleanValue()) {
            for (class_1799 stack : armor) {
                String text;
                int w;
                if (stack.method_7960() || !stack.method_7963() || (w = ArmorHUD.mc.field_1772.method_1727(text = this.getDurabilityText(stack))) <= textWidth) continue;
                textWidth = w;
            }
        }
        if (((Boolean)this.background.getValue()).booleanValue()) {
            if (((Boolean)this.horizontal.getValue()).booleanValue()) {
                FastClientUI.hudPanel(graphics, x - 5, y - 4, armor.length * horizontalStep() + 10, 20 + ((Boolean)this.showDurability.getValue() != false ? 11 : 0) + 6, this.bgOpacity.getIntValue());
            } else {
                FastClientUI.hudPanel(graphics, x - 5, y - 4, 20 + (this.showDurability.isEnabled() ? Math.max(20,textWidth) : 0) + 10, armor.length * 20 + 6, this.bgOpacity.getIntValue());
            }
        }
        int offsetX = 0;
        int offsetY = 0;
        for (class_1799 stack : armor) {
            if (!stack.method_7960()) {
                graphics.method_51427(stack, x + offsetX, y + offsetY);
                if (((Boolean)this.showDurability.getValue()).booleanValue() && stack.method_7963()) {
                    int currentDamage = stack.method_7919();
                    int maxDurability = stack.method_7936();
                    int durability = maxDurability - currentDamage;
                    float percent = (float)durability / (float)maxDurability;
                    int color = percent > 0.5f ? -11141291 : (percent > 0.25f ? -171 : -43691);
                    if(showBars.isEnabled() && !horizontal.isEnabled()) {
                        int length=Math.max(20,textWidth),bx=x+20,by=y+offsetY+14;
                        graphics.method_25294(bx,by,bx+length,by+2,0xAA171317);
                        graphics.method_25294(bx,by,bx+Math.max(0,Math.round(length*percent)),by+2,color);
                    }
                    String durText = this.getDurabilityText(stack);
                    if (((Boolean)this.horizontal.getValue()).booleanValue()) {
                        graphics.method_51433(ArmorHUD.mc.field_1772, durText, x + offsetX + 8 - ArmorHUD.mc.field_1772.method_1727(durText) / 2, y + offsetY + 17, color, true);
                    } else {
                        graphics.method_51433(ArmorHUD.mc.field_1772, durText, x + 20, y + offsetY + 4, color, true);
                    }
                }
            }
            if (((Boolean)this.horizontal.getValue()).booleanValue()) {
                offsetX += horizontalStep();
                continue;
            }
            offsetY += 20;
        }
        graphics.method_51448().popMatrix();
    }

    private int horizontalStep(){int w=20;if(showDurability.isEnabled())for(var item:items())if(!item.method_7960() && item.method_7963())w=Math.max(w,mc.field_1772.method_1727(getDurabilityText(item))+6);return w;}
    private String getDurabilityText(class_1799 stack) {
        int currentDamage = stack.method_7919();
        int maxDurability = stack.method_7936();
        int durability = maxDurability - currentDamage;
        float percent = (float)durability / (float)maxDurability * 100.0f;
        String mode = (String)this.displayMode.getValue();
        if (mode.equals("percentage")) {
            return String.format("%.0f%%", Float.valueOf(percent));
        }
        if (mode.equals("both")) {
            return String.format("%d (%.0f%%)", durability, Float.valueOf(percent));
        }
        return String.valueOf(durability);
    }

    @Override
    public int getHudWidth() {
        int textWidth = 0;
        if (ArmorHUD.mc.field_1724 != null && ((Boolean)this.showDurability.getValue()).booleanValue()) {
            class_1799[] armor;
            for (class_1799 stack : armor = items()) {
                if (stack.method_7960() || !stack.method_7963()) continue;
                textWidth = Math.max(textWidth, ArmorHUD.mc.field_1772.method_1727(this.getDurabilityText(stack)));
            }
        }
        if (((Boolean)this.horizontal.getValue()).booleanValue()) {
            return items().length * horizontalStep()+10;
        }
        return 20 + (this.showDurability.isEnabled() ? Math.max(20,textWidth) : 0) + 10;
    }

    @Override
    public int getHudHeight() {
        if (((Boolean)this.horizontal.getValue()).booleanValue()) {
            return 20 + ((Boolean)this.showDurability.getValue() != false ? 11 : 0) + 6;
        }
        return items().length * 20 + 6;
    }
}

