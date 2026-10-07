package br.com.kusharchives.kushclient.core.setting;

public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final Class<E> enumClass;
    public EnumSetting(String id, String name, Class<E> enumClass, E defaultValue) { super(id, name, defaultValue); this.enumClass = enumClass; }
    public Class<E> getEnumClass() { return enumClass; }
}
