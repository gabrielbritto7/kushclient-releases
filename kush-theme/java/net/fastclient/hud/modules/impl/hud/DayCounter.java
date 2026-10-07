/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_332
 */
package net.fastclient.hud.modules.impl.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.modules.Category;
import net.fastclient.hud.modules.Module;
import net.fastclient.hud.modules.settings.BooleanSetting;
import net.fastclient.hud.modules.settings.ColorSetting;
import net.minecraft.class_332;

@Environment(value=EnvType.CLIENT)
public class DayCounter
extends Module {
    private final BooleanSetting showLabel = this.register(new BooleanSetting("show_label", "Show 'Day' label", true));
    private final BooleanSetting showTime = this.register(new BooleanSetting("show_time", "Show in-game time", false));
    private final BooleanSetting background = this.register(new BooleanSetting("background", "Show background", true));
    private final ColorSetting color = this.register(new ColorSetting("color", "Text color", 255, 220, 100));

    public DayCounter() {
        super("DayCounter", "Shows in-game day count", Category.HUD);
    }

    @Override
    public void onRender(class_332 graphics, float tickDelta) {
        if (!this.isInGame() || DayCounter.mc.field_1687 == null) {
            return;
        }
        int x = this.getHudX();
        int y = this.getHudY();
        float scale = this.getHudScale();
        graphics.method_51448().pushMatrix();
        graphics.method_51448().translate((float)x, (float)y);
        graphics.method_51448().scale(scale, scale);
        graphics.method_51448().translate((float)(-x), (float)(-y));
        long worldTime = DayCounter.mc.field_1687.method_8532();
        long day = worldTime / 24000L + 1L;
        StringBuilder text = new StringBuilder();
        if (this.showLabel.isEnabled()) {
            text.append(net.fastclient.hud.gui.KushLanguage.translate("Day "));
        }
        text.append(day);
        if (this.showTime.isEnabled()) {
            int hours = (int)(worldTime % 24000L / 1000L + 6L) % 24;
            int minutes = (int)(worldTime % 1000L * 60L / 1000L);
            text.append(String.format(" (%02d:%02d)", hours, minutes));
        }
        String displayText = text.toString();
        if (this.background.isEnabled()) {
            FastClientUI.hudText(graphics, DayCounter.mc.field_1772, displayText, x, y, this.color.getRGB() | 0xFF000000, true);
        } else {
            graphics.method_51433(DayCounter.mc.field_1772, displayText, x, y, this.color.getRGB() | 0xFF000000, true);
        }
        graphics.method_51448().popMatrix();
    }

    @Override
    public int getHudWidth() {
        if (DayCounter.mc.field_1687 == null) {
            return 60;
        }
        long worldTime = DayCounter.mc.field_1687.method_8532();
        long day = worldTime / 24000L + 1L;
        StringBuilder text = new StringBuilder();
        if (this.showLabel.isEnabled()) {
            text.append(net.fastclient.hud.gui.KushLanguage.translate("Day "));
        }
        text.append(day);
        if (this.showTime.isEnabled()) {
            int hours = (int)(worldTime % 24000L / 1000L + 6L) % 24;
            int minutes = (int)(worldTime % 1000L * 60L / 1000L);
            text.append(String.format(" (%02d:%02d)", hours, minutes));
        }
        return DayCounter.mc.field_1772.method_1727(text.toString()) + 10;
    }

    @Override
    public int getHudHeight() {
        return 17;
    }
}

