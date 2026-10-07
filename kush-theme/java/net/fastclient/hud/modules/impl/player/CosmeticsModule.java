/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.fastclient.hud.modules.impl.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fastclient.hud.modules.Category;
import net.fastclient.hud.modules.Module;

@Environment(value=EnvType.CLIENT)
public final class CosmeticsModule
extends Module {
    public CosmeticsModule() {
        super("Cosmetics", "Manage equipped cosmetics and try complete looks in the Dress Room", Category.PLAYER);
        this.setKeyBinding(75);
        this.setEnabled(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if(enabled) net.minecraft.class_310.method_1551().method_1507(new net.fastclient.client.gui.CosmeticsScreen());
        super.setEnabled(false);
    }
}

