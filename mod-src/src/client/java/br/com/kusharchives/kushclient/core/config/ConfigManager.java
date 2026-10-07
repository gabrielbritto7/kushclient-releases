package br.com.kusharchives.kushclient.core.config;

import br.com.kusharchives.kushclient.KushClientClient;
import br.com.kusharchives.kushclient.core.module.Module;
import br.com.kusharchives.kushclient.core.module.ModuleManager;
import br.com.kusharchives.kushclient.core.setting.BooleanSetting;
import br.com.kusharchives.kushclient.core.setting.EnumSetting;
import br.com.kusharchives.kushclient.core.setting.NumberSetting;
import br.com.kusharchives.kushclient.core.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SAVE_DEBOUNCE_MS = 350L;

    private final ModuleManager moduleManager;
    private final Path configDirectory;
    private final Path configFile;
    private final AtomicBoolean savePending = new AtomicBoolean(false);
    private volatile long lastSaveRequest;
    private boolean loading;
    private boolean useKushUi = true;

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.configDirectory = FabricLoader.getInstance().getConfigDir().resolve("kushclient");
        this.configFile = configDirectory.resolve("config.json");
    }

    public void load() {
        loading = true;
        try {
            Files.createDirectories(configDirectory);
            if (!Files.exists(configFile)) {
                loading = false;
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                KushConfig config = GSON.fromJson(reader, KushConfig.class);
                if (config == null || config.modules == null) {
                    return;
                }
                useKushUi = config.useKushUi;

                for (Module module : moduleManager.getModules()) {
                    KushConfig.ModuleConfig moduleConfig = config.modules.get(module.getId());
                    if (moduleConfig == null) {
                        continue;
                    }

                    for (Setting<?> setting : module.getSettings()) {
                        JsonElement value = moduleConfig.settings.get(setting.getId());
                        if (value != null) {
                            restoreSetting(setting, value);
                        }
                    }
                    module.setEnabled(moduleConfig.enabled);
                }
            }
        } catch (Exception exception) {
            KushClientClient.LOGGER.error("Falha ao carregar {}", configFile, exception);
            backupBrokenConfig();
        } finally {
            loading = false;
            savePending.set(false);
        }
    }

    public synchronized void save() {
        if (loading) {
            return;
        }

        try {
            Files.createDirectories(configDirectory);
            KushConfig config = snapshot();
            Path temporary = configDirectory.resolve("config.json.tmp");

            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }

            try {
                Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
            savePending.set(false);
        } catch (Exception exception) {
            KushClientClient.LOGGER.error("Falha ao salvar {}", configFile, exception);
        }
    }

    public void requestSave() {
        if (loading) {
            return;
        }
        lastSaveRequest = System.currentTimeMillis();
        savePending.set(true);
    }

    public void flushPendingSave() {
        if (savePending.get() && System.currentTimeMillis() - lastSaveRequest >= SAVE_DEBOUNCE_MS) {
            save();
        }
    }

    public Path getConfigFile() {
        return configFile;
    }

    public boolean isUseKushUi() {
        return useKushUi;
    }

    public void setUseKushUi(boolean value) {
        if (useKushUi == value) return;
        useKushUi = value;
        requestSave();
    }

    private KushConfig snapshot() {
        KushConfig config = new KushConfig();
        config.useKushUi = useKushUi;
        for (Module module : moduleManager.getModules()) {
            KushConfig.ModuleConfig moduleConfig = new KushConfig.ModuleConfig();
            moduleConfig.enabled = module.isEnabled();
            for (Setting<?> setting : module.getSettings()) {
                moduleConfig.settings.put(setting.getId(), GSON.toJsonTree(setting.getValue()));
            }
            config.modules.put(module.getId(), moduleConfig);
        }
        return config;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restoreSetting(Setting<?> setting, JsonElement value) {
        try {
            if (setting instanceof BooleanSetting booleanSetting) {
                booleanSetting.setValue(value.getAsBoolean());
            } else if (setting instanceof NumberSetting numberSetting) {
                numberSetting.setValue(value.getAsDouble());
            } else if (setting instanceof EnumSetting enumSetting) {
                Enum restored = Enum.valueOf(enumSetting.getEnumClass(), value.getAsString());
                enumSetting.setValue(restored);
            }
        } catch (Exception exception) {
            KushClientClient.LOGGER.warn("Configuração inválida ignorada: {}", setting.getId());
        }
    }

    private void backupBrokenConfig() {
        try {
            if (!Files.exists(configFile)) {
                return;
            }
            Path backup = configDirectory.resolve("config.broken-" + System.currentTimeMillis() + ".json");
            Files.copy(configFile, backup, StandardCopyOption.REPLACE_EXISTING);
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonParser.parseReader(reader);
            }
            KushClientClient.LOGGER.warn("Configuração problemática preservada em {}", backup);
        } catch (Exception ignored) {
        }
    }
}
