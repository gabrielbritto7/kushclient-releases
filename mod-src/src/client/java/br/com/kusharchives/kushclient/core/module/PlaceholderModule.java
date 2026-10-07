package br.com.kusharchives.kushclient.core.module;

public final class PlaceholderModule extends Module {
    public PlaceholderModule(String id, String name, String description, Category category) {
        super(id, name, description, category);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
