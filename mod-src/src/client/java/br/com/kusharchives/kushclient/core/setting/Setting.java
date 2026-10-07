package br.com.kusharchives.kushclient.core.setting;

import br.com.kusharchives.kushclient.KushClientClient;
import java.util.Objects;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final T defaultValue;
    private T value;
    protected Setting(String id, String name, T defaultValue) { this.id=id; this.name=name; this.defaultValue=Objects.requireNonNull(defaultValue); this.value=defaultValue; }
    public void setValue(T value) {
        this.value = validate(Objects.requireNonNull(value));
        try { KushClientClient.getInstance().getConfigManager().requestSave(); } catch (IllegalStateException ignored) {}
    }
    protected T validate(T value) { return value; }
    public void reset() { setValue(defaultValue); }
    public String getId() { return id; }
    public String getName() { return name; }
    public T getDefaultValue() { return defaultValue; }
    public T getValue() { return value; }
}
