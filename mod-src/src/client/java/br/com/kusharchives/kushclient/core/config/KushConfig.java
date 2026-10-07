package br.com.kusharchives.kushclient.core.config;

import com.google.gson.JsonElement;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KushConfig {
    public int schemaVersion = 2;
    public boolean useKushUi = true;
    public Map<String, ModuleConfig> modules = new LinkedHashMap<>();

    public static final class ModuleConfig {
        public boolean enabled;
        public Map<String, JsonElement> settings = new LinkedHashMap<>();
    }
}
