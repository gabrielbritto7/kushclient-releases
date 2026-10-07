/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_11909
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 */
package net.fastclient.hud.gui.components;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.hud.gui.FastClientUI;
import net.fastclient.hud.gui.components.UIComponent;
import net.fastclient.hud.render.AnimationUtils;
import net.fastclient.hud.render.Theme;
import net.minecraft.class_11909;
import net.minecraft.class_310;
import net.minecraft.class_332;

@Environment(value=EnvType.CLIENT)
public class ToggleButton
extends UIComponent {
    private boolean enabled;
    private final Consumer<Boolean> onChange;
    private final String label;
    private float toggleProgress;
    private float hoverProgress;
    private long lastUpdate = System.currentTimeMillis();

    public ToggleButton(int x, int y, int width, int height, String label, boolean initialState, Consumer<Boolean> onChange) {
        super(x, y, width, height);
        this.label = label;
        this.enabled = initialState;
        this.onChange = onChange;
        this.toggleProgress = initialState ? 1.0f : 0.0f;
    }

    @Override
    public void render(class_332 graphics, int mouseX, int mouseY, float delta) {
        this.hovered = this.isHovered(mouseX, mouseY);
        long now = System.currentTimeMillis();
        float dt = (float)(now - this.lastUpdate) / 1000.0f;
        this.lastUpdate = now;
        this.toggleProgress = AnimationUtils.smoothDelta(this.toggleProgress, this.enabled ? 1.0f : 0.0f, 0.3f, dt * 60.0f);
        this.hoverProgress = AnimationUtils.smoothDelta(this.hoverProgress, this.hovered ? 1.0f : 0.0f, 0.4f, dt * 60.0f);
        class_310 mc = class_310.method_1551();
        if (this.hoverProgress > 0.01f) {
            int bgAlpha = (int)(20.0f * this.hoverProgress);
            FastClientUI.roundedRect(graphics, this.x, this.y, this.width, this.height, 4, FastClientUI.withAlpha(-266722777, bgAlpha));
        }
        String displayLabel = Theme.formatSettingName(this.label);
        int labelColor = FastClientUI.blend(-7303024, -723724, this.hoverProgress);
        this.drawUiText(graphics, mc, displayLabel, this.x + 12, this.centeredTextY(mc, this.y, this.height), labelColor);
        int toggleWidth = 34;
        int toggleHeight = 14;
        int toggleX = this.x + this.width - toggleWidth - 12;
        int toggleY = this.y + (this.height - toggleHeight) / 2;
        net.fastclient.hud.gui.KushAssets.softRect(graphics, toggleX, toggleY, toggleWidth, toggleHeight, 7, 0xFF443C45);
        int trackColor = FastClientUI.blend(0xFF342E37, 0xFFE53542, this.toggleProgress);
        net.fastclient.hud.gui.KushAssets.softRect(graphics, toggleX+1, toggleY+1, toggleWidth-2, toggleHeight-2, 6, trackColor);
        int handleX = toggleX + 3 + Math.round((toggleWidth-14)*this.toggleProgress);
        net.fastclient.hud.gui.KushAssets.softRect(graphics, handleX, toggleY+3, 8, 8, 4, 0xFFF9F4F6);

    }

    @Override
    public boolean mouseClicked(class_11909 event, boolean bl) {
        if (super.mouseClicked(event, bl) && event.method_74245() == 0) {
            boolean bl2 = this.enabled = !this.enabled;
            if (this.onChange != null) {
                this.onChange.accept(this.enabled);
            }
            return true;
        }
        return false;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

