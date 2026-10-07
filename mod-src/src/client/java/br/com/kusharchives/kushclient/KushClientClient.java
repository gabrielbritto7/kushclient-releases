package br.com.kusharchives.kushclient;

import br.com.kusharchives.kushclient.core.config.ConfigManager;
import br.com.kusharchives.kushclient.core.event.EventBus;
import br.com.kusharchives.kushclient.core.event.events.ClientTickEvent;
import br.com.kusharchives.kushclient.core.module.ModuleManager;
import br.com.kusharchives.kushclient.input.KushKeybinds;
import br.com.kusharchives.kushclient.gui.KushUiController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KushClientClient implements ClientModInitializer {
    public static final String MOD_ID = "kushclient";
    public static final String NAME = "KushClient";
    public static final String VERSION = "0.2.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static KushClientClient instance;
    private final EventBus eventBus = new EventBus();
    private final ModuleManager moduleManager = new ModuleManager();
    private ConfigManager configManager;

    @Override
    public void onInitializeClient() {
        instance = this;
        configManager = new ConfigManager(moduleManager);
        moduleManager.setConfigManager(configManager);
        moduleManager.registerCoreModules();
        configManager.load();
        KushKeybinds.register();
        KushUiController.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            eventBus.post(new ClientTickEvent(client));
            moduleManager.onClientTick(client);
            KushKeybinds.onClientTick(client);
            KushUiController.onClientTick(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) ->
                moduleManager.onHudRender(Minecraft.getInstance(), graphics));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { configManager.save(); }
            catch (Exception exception) { LOGGER.warn("Não foi possível salvar a configuração durante o encerramento.", exception); }
        }, "KushClient-Config-Shutdown"));

        LOGGER.info("{} {} iniciado para Minecraft 1.21.1. Módulos: {}", NAME, VERSION, moduleManager.getModules().size());
    }

    public static KushClientClient getInstance() {
        if (instance == null) throw new IllegalStateException("KushClient ainda não foi inicializado.");
        return instance;
    }

    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
}
