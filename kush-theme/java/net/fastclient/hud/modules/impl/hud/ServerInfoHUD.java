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
import net.fastclient.hud.modules.settings.NumberSetting;
import net.minecraft.class_332;

@Environment(value=EnvType.CLIENT)
public class ServerInfoHUD
extends Module {
    private final BooleanSetting background = this.register(new BooleanSetting("background", "Show background", true));
    private final NumberSetting bgOpacity = this.register(new NumberSetting("opacity", "Background opacity", 80.0, 0.0, 255.0, 5.0));

    public ServerInfoHUD() {
        super("ServerInfo", "Shows server information", Category.HUD);
        this.bgOpacity.visibleWhen(this.background::isEnabled);
    }

    @Override
    public void onRender(class_332 graphics, float tickDelta) {
        String serverAddress;
        String serverName;
        if (!this.isInGame()) {
            return;
        }
        int x = this.getHudX();
        int y = this.getHudY();
        float scale = this.getHudScale();
        graphics.method_51448().pushMatrix();
        graphics.method_51448().translate((float)x, (float)y);
        graphics.method_51448().scale(scale, scale);
        graphics.method_51448().translate((float)(-x), (float)(-y));
        int offsetY = 0;
        if (mc.method_1496()) {
            serverName = net.fastclient.hud.gui.KushLanguage.translate("Singleplayer");
            serverAddress = "Local";
        } else if (mc.method_1558() != null) {
            serverName = ServerInfoHUD.mc.method_1558().field_3752;
            serverAddress = ServerInfoHUD.mc.method_1558().field_3761;
        } else {
            return;
        }
        if (this.background.isEnabled()) {
            FastClientUI.hudTwoLine(graphics, ServerInfoHUD.mc.field_1772, serverName, serverAddress, x, y, -1, -3025448, this.bgOpacity.getIntValue());
        } else {
            graphics.method_51433(ServerInfoHUD.mc.field_1772, serverName, x, y + offsetY, -1, true);
            graphics.method_51433(ServerInfoHUD.mc.field_1772, serverAddress, x, y + (offsetY += 10), -3025448, true);
        }
        graphics.method_51448().popMatrix();
    }

    @Override
    public int getHudWidth() {
        String serverAddress;
        String serverName;
        if (mc.method_1496()) {
            serverName = net.fastclient.hud.gui.KushLanguage.translate("Singleplayer");
            serverAddress = "Local";
        } else if (mc.method_1558() != null) {
            serverName = ServerInfoHUD.mc.method_1558().field_3752;
            serverAddress = ServerInfoHUD.mc.method_1558().field_3761;
        } else {
            return 80;
        }
        return Math.max(ServerInfoHUD.mc.field_1772.method_1727(serverName), ServerInfoHUD.mc.field_1772.method_1727(serverAddress)) + 10;
    }

    @Override
    public int getHudHeight() {
        return 28;
    }
}

