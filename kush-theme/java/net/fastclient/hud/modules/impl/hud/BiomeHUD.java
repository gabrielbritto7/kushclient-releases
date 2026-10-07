/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1959
 *  net.minecraft.class_2338
 *  net.minecraft.class_332
 *  net.minecraft.class_6880
 */
package net.fastclient.hud.modules.impl.hud;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.modules.Category;
import net.fastclient.hud.modules.Module;
import net.fastclient.hud.modules.settings.BooleanSetting;
import net.fastclient.hud.modules.settings.ColorSetting;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_332;
import net.minecraft.class_6880;

@Environment(value=EnvType.CLIENT)
public class BiomeHUD
extends Module {
    private final BooleanSetting showLabel = this.register(new BooleanSetting("show_label", "Show 'Biome:' label", true));
    private final BooleanSetting formatName = this.register(new BooleanSetting("format_name", "Format biome name", true));
    private final BooleanSetting background = this.register(new BooleanSetting("background", "Show background", true));
    private final ColorSetting color = this.register(new ColorSetting("color", "Text color", 100, 200, 100));

    public BiomeHUD() {
        super("Biome", "Shows the current biome", Category.HUD);
    }

    @Override
    public void onRender(class_332 graphics, float tickDelta) {
        Object text;
        if (!this.isInGame() || BiomeHUD.mc.field_1724 == null || BiomeHUD.mc.field_1687 == null) {
            return;
        }
        int x = this.getHudX();
        int y = this.getHudY();
        float scale = this.getHudScale();
        graphics.method_51448().pushMatrix();
        graphics.method_51448().translate((float)x, (float)y);
        graphics.method_51448().scale(scale, scale);
        graphics.method_51448().translate((float)(-x), (float)(-y));
        class_2338 pos = BiomeHUD.mc.field_1724.method_24515();
        class_6880 biomeHolder = BiomeHUD.mc.field_1687.method_23753(pos);
        String biomeName = this.getBiomeName((class_6880<class_1959>)biomeHolder);
        if (this.formatName.isEnabled()) {
            biomeName = this.formatBiomeName(biomeName);
        }
        Object object = text = this.showLabel.isEnabled() ? net.fastclient.hud.gui.KushLanguage.translate("Biome: ") + biomeName : biomeName;
        if (this.background.isEnabled()) {
            FastClientUI.hudText(graphics, BiomeHUD.mc.field_1772, (String)text, x, y, this.color.getRGB() | 0xFF000000, true);
        } else {
            graphics.method_51433(BiomeHUD.mc.field_1772, (String)text, x, y, this.color.getRGB() | 0xFF000000, true);
        }
        graphics.method_51448().popMatrix();
    }

    private String getBiomeName(class_6880<class_1959> biomeHolder) {
        return biomeHolder.method_40230().map(key -> key.method_29177().method_12832()).orElse("unknown");
    }

    private String formatBiomeName(String name) {
        String[] parts = name.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            formatted.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT)).append(" ");
        }
        return formatted.toString().trim();
    }

    @Override
    public int getHudWidth() {
        if (BiomeHUD.mc.field_1724 == null || BiomeHUD.mc.field_1687 == null) {
            return 80;
        }
        class_2338 pos = BiomeHUD.mc.field_1724.method_24515();
        class_6880 biomeHolder = BiomeHUD.mc.field_1687.method_23753(pos);
        String biomeName = this.getBiomeName((class_6880<class_1959>)biomeHolder);
        if (this.formatName.isEnabled()) {
            biomeName = this.formatBiomeName(biomeName);
        }
        Object text = this.showLabel.isEnabled() ? net.fastclient.hud.gui.KushLanguage.translate("Biome: ") + biomeName : biomeName;
        return BiomeHUD.mc.field_1772.method_1727((String)text) + 10;
    }

    @Override
    public int getHudHeight() {
        return 17;
    }
}

