    private void drawHeader(class_332 graphics, int x, int y, int w, int alpha) {
        this.backBtnX = x + 16;
        this.backBtnY = y + 18;
        FastClientUI.roundedRect(graphics, this.backBtnX, this.backBtnY, this.backBtnSize, this.backBtnSize, 0, FastClientUI.fade(0xA51D171C, alpha));
        String backLabel = "\u2190";
        this.drawUiText(graphics, backLabel, this.backBtnX + (this.backBtnSize - this.uiTextWidth(backLabel)) / 2, this.backBtnY + (this.backBtnSize - this.uiLineHeight()) / 2 + 1, FastClientUI.fade(-7303024, alpha));
        int iconSize = 32, iconX = x + 56, iconY = y + 17;
        FastClientUI.roundedRect(graphics, iconX, iconY, iconSize, iconSize, 0, FastClientUI.fade(0xA51D171C, alpha));
        graphics.method_25291(class_10799.field_56883, FastClientUI.icon(this.module), iconX + 5, iconY + 5, 0.0f, 0.0f, 22, 22, 22, 22, FastClientUI.fade(-1, alpha));
        String status = this.module.isEnabled() ? "ATIVO" : "INATIVO";
        int statusColor = this.module.isEnabled() ? 0xFFE53542 : -7303024;
        graphics.method_25294(x, y, x + w, y + 2, FastClientUI.fade(this.module.isEnabled() ? 0xFFE53542 : 0x507B3A44, alpha));
        int textX = x + 100, centerY = y + 33, gap = 14;
        class_2561 name = FastClientFonts.title(this.module.getDisplayName());
        int rawWidth = this.field_22793.method_27525(name);
        int room = Math.max(1, x + w - 24 - textX - this.uiTextWidth(status) - gap);
        float scale = Math.min(FastClientFonts.titleScale(), room / (float)Math.max(1, rawWidth));
        graphics.method_51448().pushMatrix();
        graphics.method_51448().translate(textX, centerY - 4.5f * scale);
        graphics.method_51448().scale(scale, scale);
        graphics.method_51439(this.field_22793, name, 0, 0, FastClientUI.fade(-723724, alpha), false);
        graphics.method_51448().popMatrix();
        this.drawUiText(graphics, status, textX + Math.round(rawWidth * scale) + gap,
            centerY - this.uiLineHeight() / 2, FastClientUI.fade(statusColor, alpha));
        String desc = this.fitBodyText(this.module.getDescription() == null ? "" : this.module.getDescription(), w - 116);
        this.drawUiText(graphics, desc, textX, y + 64, FastClientUI.fade(-7303024, alpha));
        graphics.method_25294(x + 14, y + 92, x + w - 14, y + 93, FastClientUI.fade(0xFF281A20, alpha));
    }

