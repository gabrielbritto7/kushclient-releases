package br.com.kusharchives.kushclient.core.module;

import br.com.kusharchives.kushclient.KushClientClient;
import br.com.kusharchives.kushclient.core.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();

        KushClientClient client = KushClientClient.getInstance();
        if (client.getConfigManager() != null) client.getConfigManager().requestSave();
    }

    public final void toggle() { setEnabled(!enabled); }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onClientTick(Minecraft client) {}
    public void onHudRender(Minecraft client, GuiGraphics graphics) {}

    public final String getId() { return id; }
    public final String getName() { return name; }
    public final String getDescription() { return description; }
    public final Category getCategory() { return category; }
    public final boolean isEnabled() { return enabled; }
    public final List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
    public boolean isAvailable() { return true; }
}
