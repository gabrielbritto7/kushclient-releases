package br.com.kusharchives.kushclient.core.module;

public enum Category {
    HUD("HUD"),
    RENDER("Render"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    UTILITY("Utility");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
