from pathlib import Path
import re, shutil, json, base64

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent / 'base'
JAVA = PROJECT / 'src/main/java/net/fastclient/hud'
ASSETS = PROJECT / 'src/main/resources/assets/fastclient-hud'
dest = ASSETS / 'textures/gui/kush'
dest.mkdir(parents=True, exist_ok=True)
for p in (ROOT/'assets').glob('*'):
    if p.suffix=='.base64':(dest/p.stem).write_bytes(base64.b64decode(p.read_text()))
    else:shutil.copyfile(p,dest/p.name)
# Source contains constants inlined by the original compiler, so apply the
# palette to all UI classes, including widgets and setting controls.
palette = {
    1717331565: "0x709C455B", -11020659: "0xFF943044", -9193473: "0xFFB84159",
    -11930: "0xFFD86377", -3704321: "0xFF8C263C", -2564638: "0xFFBB5268",
    -234156528: '0xA6141115', -435219433: '0x781B1418',
    -653586413: '0x981A161B', -434955745: '0xB32A1A20',
    -435153640: '0xA51D171C', -266722777: '0xCA352129',
    -39373: '0xFFE53542', -34227: '0xFFF64B57',
    -15511009: '0xDDBE2732', -15243738: '0xFFE53542',
    -8658034: '0xFFFFCBD0', 1143616571: '0x507B3A44',
    -1978658025: '0x8A191216', -1475275496: '0xAC24171D',
    1725422816: '0x665F3741', 1154997472: '0x4469404B',
    -15921133: '0xAA191317', -14406863: '0x806F3946',
    -13616829: '0xB39B4055', -12958640: '0xCD582130',
    -15591911: '0xBE27161C', -13954548: '0xFF581F2E',
    -11263981: '0xFF722237', -30106: '0xFFDB657A',
    -45747: '0xFFE53542', -3073494: '0xFF581F2E',
}
for folder in [JAVA/'gui',JAVA/'launcher']:
    for p in folder.rglob('*.java'):
        s=p.read_text()
        s=re.sub(r'(?<![\w.])-?\d+(?![\w.])', lambda m:palette.get(int(m[0]),m[0]),s)
        s=s.replace('"Fast Settings"','"Kush Settings"')
        if 'screens' in p.parts or 'widgets' in p.parts or 'components' in p.parts:
            s=s.replace('FastClientUI.withAlpha(', 'FastClientUI.fade(')
        p.write_text(s)

p=JAVA/'gui/FastClientUI.java';s=p.read_text()
s=re.sub(r'    public static void outline\(.*?(?=    public static void roundedOutline)', '''    public static void outline(class_332 graphics, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        graphics.method_25294(x, y, x + w, y + 1, color);
        if (h > 1) graphics.method_25294(x, y + h - 1, x + w, y + h, color);
        if (h > 2) {
            graphics.method_25294(x, y + 1, x + 1, y + h - 1, color);
            if (w > 1) graphics.method_25294(x + w - 1, y + 1, x + w, y + h - 1, color);
        }
    }

''',s,flags=re.S)
# Legacy roundedRect drew two overlapping fills, making translucent middles
# darker than their corners. All Kush chrome is square and has exactly one fill.
s=re.sub(r'    public static void roundedRect\(.*?(?=    public static void outline)', '''    public static void roundedRect(class_332 graphics, int x, int y, int w, int h, int radius, int color) {
        if (w > 0 && h > 0) graphics.method_25294(x, y, x + w, y + h, color);
    }

''',s,flags=re.S)
s=re.sub(r'    public static void roundedOutline\(.*?(?=    public static void borderedRoundedRect)', '''    public static void roundedOutline(class_332 graphics, int x, int y, int w, int h, int radius, int color) {
        outline(graphics, x, y, w, h, color);
    }

''',s,flags=re.S)
s=re.sub(r'    public static void borderedRoundedRect\(.*?(?=    public static void hudPanel)', '''    public static void borderedRoundedRect(class_332 graphics, int x, int y, int w, int h, int radius, int fillColor, int borderColor) {
        if (w > 2 && h > 2) graphics.method_25294(x + 1, y + 1, x + w - 1, y + h - 1, fillColor);
        outline(graphics, x, y, w, h, borderColor);
    }

''',s,flags=re.S)
s=s.replace('    public static int blend(', '''    public static int fade(int color, int alpha) {
        return withAlpha(color, Math.round(((color >>> 24) & 255) * Math.max(0, Math.min(255, alpha)) / 255.0f));
    }

    public static int blend(''')
p.write_text(s)

p=JAVA/'gui/FastClientFonts.java';s=p.read_text()
s=s.replace('return ACTIVE_TYPEFACE;', 'return usesSettingsFont() ? Typeface.INTER : Typeface.MINECRAFT_DEFAULT;')
s=s.replace('return ACTIVE_TYPEFACE == Typeface.MINECRAFT_DEFAULT ? 2.0f : 1.0f;', 'return usesSettingsFont() ? 1.5f : 2.0f;')
s=s.replace('if (ACTIVE_TYPEFACE ==', 'if (activeTypeface() ==')
s=s.replace('    private static float configuredUiScale()', '''    private static boolean usesSettingsFont() {
        net.minecraft.class_310 client = net.minecraft.class_310.method_1551();
        if (client == null) return false;
        return client.field_1755 instanceof net.fastclient.hud.gui.screens.ClickGUIScreen
            || client.field_1755 instanceof net.fastclient.hud.gui.screens.ModuleConfigScreen;
    }

    private static float configuredUiScale()''')
p.write_text(s)

p=JAVA/'gui/widgets/ModuleCard.java';s=p.read_text()
s=s.replace('float titleScale = FastClientFonts.titleScale();', 'float titleScale = 1.35f;')
p.write_text(s)

p=JAVA/'gui/screens/ClickGUIScreen.java';s=p.read_text()
s=s.replace('"FastClientHUD"','"Kush Mods"')
s=s.replace('"Fastclient 1.21.11 (release/ca786cd3)"','"KushMod 0.3.10  ·  Minecraft 1.21.11"')
s=s.replace('"MOD MENU"','"KUSH MODS"')
s=s.replace('"All" : categories', '"Todos" : categories')
s=s.replace('"Search"','"Buscar"')
s=s.replace('"Reset"','"Resetar"')
# Deliberately call the native blur once, before entering physical coordinates.
s=s.replace('        this.ensurePhysicalLayoutCurrent();\n        int pxMouseX', '        this.ensurePhysicalLayoutCurrent();\n        graphics.method_71278();\n        int pxMouseX',1)
s=s.replace('        this.method_25420(graphics, pxMouseX, pxMouseY, delta);', '        net.fastclient.hud.gui.KushAssets.backdrop(graphics, DisplaySpace.width(), DisplaySpace.height());\n        this.method_25420(graphics, pxMouseX, pxMouseY, delta);',1)
s=s.replace('graphics.method_25294(x, y, x + w, y + h, FastClientUI.fade(0xA6141115, alpha));', 'net.fastclient.hud.gui.KushAssets.glass(graphics, x, y, w, h, alpha);')
# Header navigation in upstream was entirely decorative. Keep useful controls
# and wire every hit area to a screen rather than leaving inert icons.
s=s.replace('HEADER_NAV_SYMBOLS = new String[]{"\\ue8b8", "\\ue30c", "\\uf19e", "\\ue9e4", "\\ue8af", "\\uf02e"};', 'HEADER_NAV_SYMBOLS = new String[]{"\\ue8b8", "\\ue30c", "\\uf19e"};')
needle='        TopControlsLayout controls = this.getTopControlsLayout(this.panelX, animatedPanelY, this.panelWidth);'
replacement='''        int headerY = this.getHeaderNavY();
        int iconStartX = this.panelX + 144 + this.getNavGap();
        if (button == 0 && mouseY >= headerY && mouseY < headerY + 52) {
            for (int i = 0; i < HEADER_NAV_SYMBOLS.length; i++) {
                int bx = iconStartX + i * (this.getNavButtonWidth() + this.getNavGap());
                if (mouseX >= bx && mouseX < bx + this.getNavButtonWidth()) {
                    if (i == 0) this.field_22787.method_1507(new ModuleConfigScreen(
                        FastClientHUDClient.getInstance().getModuleManager().getModules().stream()
                        .filter(m -> m.getName().equalsIgnoreCase("GUISettings")).findFirst().orElse(
                            FastClientHUDClient.getInstance().getModuleManager().getModules().get(0)), this));
                    else if (i == 1) this.field_22787.method_1507(new HudOverlayScreen());
                    else this.field_22787.method_1507(new StoreScreen(this));
                    return true;
                }
            }
        }
'''+needle
s=s.replace(needle,replacement,1)
p.write_text(s)

p=JAVA/'gui/screens/ModuleConfigScreen.java';s=p.read_text()
s=s.replace('        int pxMouseX = DisplaySpace.mouseX(mouseX);','        graphics.method_71278();\n        int pxMouseX = DisplaySpace.mouseX(mouseX);',1)
s=s.replace('        this.method_25420(graphics, pxMouseX, pxMouseY, delta);','        net.fastclient.hud.gui.KushAssets.backdrop(graphics, DisplaySpace.width(), DisplaySpace.height());',1)
s=s.replace('FastClientUI.roundedRect(graphics, x, y, w, h, 7, FastClientUI.fade(0xA6141115, alpha));','FastClientUI.roundedRect(graphics, x, y, w, h, 3, FastClientUI.fade(0xF2141115, alpha));')
s=s.replace('FastClientUI.fade(0x507B3A44, alpha)', 'FastClientUI.fade(0xFF281A20, alpha)')
s=s.replace('FastClientUI.fade(0x981A161B, 150)', 'FastClientUI.fade(0xE81A181C, 210)')
s=s.replace('FastClientUI.fade(0x507B3A44, 130)', 'FastClientUI.fade(0xFF110E12, 210)')
s=s.replace('"\\u2713 Enabled"', '"\\u2713 Ativo"').replace('"\\u25cb Disabled"', '"\\u25cb Inativo"')
s=s.replace('"ESC to go back - changes save automatically"', '"ESC para voltar · Alterações salvas automaticamente"')
s=re.sub(r'    private void drawFooter\(.*?(?=    private void drawSettingRowBackground)', '''    private void drawFooter(class_332 graphics, int x, int y, int w, int alpha) {
        net.minecraft.class_2561 hint = FastClientFonts.body("ESC para voltar · Alterações salvas automaticamente");
        float scale = 1.0f;
        int hintWidth = this.field_22793.method_27525(hint);
        graphics.method_51448().pushMatrix();
        graphics.method_51448().translate(x + (w - hintWidth * scale) / 2.0f, y + 12);
        graphics.method_51448().scale(scale, scale);
        graphics.method_51439(this.field_22793, hint, 0, 0, FastClientUI.fade(0xFF969098, Math.round(alpha * .45f)), false);
        graphics.method_51448().popMatrix();
    }

''', s, flags=re.S)
s=s.replace('FastClientUI.fade(-16777216, 170)', 'FastClientUI.fade(-16777216, 55)')
p.write_text(s)

p=JAVA/'gui/screens/HudOverlayScreen.java';s=p.read_text()
s=s.replace('"textures/gui/logo.png"','"textures/gui/kush/k_overlay_red.png"')
s=s.replace('"textures/gui/fasticon_white.png"','"textures/gui/kush/k_pixel_gray.png"')
s=s.replace('new String[][]{{"overlay_store", "\\uea12", "Store"}, {"overlay_cosmetics", "\\uf19e", "Cosmetics"}, {"overlay_social", "\\ue8af", "Social"}}','new String[][]{{"overlay_cosmetics", "\\uf19e", "Cosméticos"}}')
s=s.replace('graphics.method_25291(class_10799.field_56883, DisplaySpace.texture(LOGO_TEXTURE), logoX, logoY, 0.0f, 0.0f, logoSize, logoSize, logoSize, logoSize, logoColor);','net.fastclient.hud.gui.KushAssets.image(graphics, LOGO_TEXTURE, logoX, logoY, logoSize, logoSize, 1254, 1254, logoColor);')
s=s.replace('graphics.method_25293(class_10799.field_56883, DisplaySpace.texture(FAST_SETTINGS_ICON), contentX, iconY, 0.0f, 0.0f, iconWidth, iconWidth, 96, 96, 96, 96, color);','net.fastclient.hud.gui.KushAssets.image(graphics, FAST_SETTINGS_ICON, contentX, iconY, iconWidth, iconWidth, 1246, 1263, color);')
s=s.replace('        DisplaySpace.push(graphics);\n        if (this.dragging == null)', '        DisplaySpace.push(graphics);\n        net.fastclient.hud.gui.KushAssets.backdrop(graphics, this.screenWidth(), this.screenHeight());\n        if (this.dragging == null)',1)
# Real modules have already rendered through RenderManager's HUD callback.
# Leave that framebuffer sharp in the editor; config screens own their blur.
s=s.replace('            this.renderDraggableModule(graphics, dm, pxMouseX, pxMouseY);',
    '            dm.updateDimensions();\n            this.renderDraggableModule(graphics, dm, pxMouseX, pxMouseY);',1)
s=s.replace('FastClientUI.roundedRect(graphics, bx, by, bw, bh, 3, FastClientUI.fade(0x981A161B, 58));',
    '// Draw only bounds: retain the actual HUD appearance without another fill.')
# Keep editor controls beside the name, never over FPS/coordinate text.
s=s.replace('        int leftX = bx + 4;\n        int chipY = by + bh - chipSize - 4;',
    '        int[] settingsBounds = this.getModuleSettingsBounds(dm);\n        int leftX = settingsBounds[0];\n        int chipY = settingsBounds[1];')
s=re.sub(r'    private boolean isSettingsActionHovered\(.*?(?=    private int getModuleActionSize)', '''    private boolean isSettingsActionHovered(DraggableModule dm, int mouseX, int mouseY) {
        if (!this.hasSettings(dm)) return false;
        int[] bounds = this.getModuleSettingsBounds(dm);
        return mouseX >= bounds[0] && mouseX < bounds[0]+bounds[2]
            && mouseY >= bounds[1] && mouseY < bounds[1]+bounds[2];
    }

    private int[] getModuleSettingsBounds(DraggableModule dm) {
        int size = this.getModuleActionSize(dm);
        int padding = dm.module.getName().equals("Block Overlay") ? 0 : (int)(5.0f * dm.scale);
        class_2561 label = FastClientFonts.moduleName(dm.module.getDisplayName());
        int textWidth = this.field_22793.method_27525(label);
        int nameX = dm.x-padding+dm.width/2-textWidth/2;
        nameX = Math.max(2, Math.min(this.screenWidth()-textWidth-2, nameX));
        int nameY = dm.y-padding-12;
        if (nameY < 2) nameY = dm.y+dm.height+3;
        int x = nameX+textWidth+9;
        if (x+size > this.screenWidth()-2) x = nameX-size-9;
        int y = nameY-3+(14-size)/2;
        return new int[]{Math.max(2,x), Math.max(2,Math.min(this.screenHeight()-size-2,y)), size};
    }

''',s,flags=re.S)
s=re.sub(r'"Drag modules to reposition[^"\n]*"', '"Arraste para mover · Clique direito para configurar · Right Shift para fechar"',s)
# The editor repositions enabled modules. Enabling/disabling stays in Settings.
s=re.sub(r'        int removeSize = this.getModuleRemoveSize\(dm\);\n', '', s)
s=re.sub(r'        int rightX = bx \+ bw - handleSize - removeSize - 4 - 4;\n', '', s)
s=re.sub(r'        int removeY = by \+ bh - removeSize - 4;\n', '', s)
s=s.replace('        boolean disableHovered = this.isDisableActionHovered(dm, mouseX, mouseY);\n', '')
s=re.sub(r'        FastClientUI.roundedRect\(graphics, rightX, removeY[^\n]*\n', '', s)
s=re.sub(r'        this.drawScaledText\(graphics, FastClientFonts.filledMaterialSymbol\("\\ue872"\)[^\n]*\n', '', s)
s=s.replace(' || this.isDisableActionHovered(dm, mouseX, mouseY)', '')
s=re.sub(r'    private boolean isDisableActionHovered\(.*?(?=    private int getModuleActionSize)', '', s, flags=re.S)
s=re.sub(r'    private int getModuleRemoveSize\(.*?(?=    private int getResizeHandleSize)', '', s, flags=re.S)
s=re.sub(r'                if \(!this.isDisableActionHovered\(dm, \(int\)mouseX, \(int\)mouseY\)\) continue;\n.*?                return true;\n', '', s, flags=re.S)
assert 'isDisableActionHovered' not in s and '\\ue872' not in s
# Square chrome, with a darker border and a subtle red hover accent.
s=re.sub(r'    private void drawChromeIconButtonSurface\(.*?(?=    private void drawFastSettingsButton)', '''    private void drawChromeIconButtonSurface(class_332 graphics, int x, int y, int size, boolean hovered, int alpha) {
        int fill = hovered ? 0xF0352228 : 0xD81D1A1E;
        FastClientUI.borderedRoundedRect(graphics, x, y, size, size, 0,
            FastClientUI.fade(fill, alpha), FastClientUI.fade(0xFF110E12, alpha));
        if (hovered) graphics.method_25294(x+1, y+size-2, x+size-1, y+size-1, FastClientUI.fade(0xFFE53542, alpha));
    }

''', s, flags=re.S)
s=re.sub(r'    private void drawFastSettingsButton\(.*?(?=    private void drawQuickButtonTooltip)', '''    private void drawFastSettingsButton(class_332 graphics, int x, int y, int width, int height, float hoverProgress, int alpha) {
        int fill = FastClientUI.blend(0xD81D1A1E, 0xF0352228, hoverProgress);
        FastClientUI.borderedRoundedRect(graphics, x, y, width, height, 0,
            FastClientUI.fade(fill, alpha), FastClientUI.fade(0xFF110E12, alpha));
        if (hoverProgress > .01f) graphics.method_25294(x+1, y+height-2, x+width-1, y+height-1,
            FastClientUI.fade(0xFFE53542, Math.round(alpha*hoverProgress)));
        class_2561 label = FastClientFonts.body("Kush Settings");
        float scale = FastClientFonts.bodyScale();
        int iconWidth = HudOverlayScreen.centerFastSettingsIconSize();
        int labelWidth = Math.round(this.field_22793.method_27525((class_5348)label)*scale);
        int gap = Math.max(9, Math.round(height*.08f));
        int contentX = x + (width-iconWidth-gap-labelWidth)/2;
        int centerY = y+height/2;
        int color = FastClientUI.fade(-723724, alpha);
        net.fastclient.hud.gui.KushAssets.labelK(graphics, contentX-3, centerY-Math.round(4.5f*scale),
            Math.round(8*scale), color);
        this.drawScaledText(graphics, label, contentX+iconWidth+gap+labelWidth/2, centerY, scale, color, false);
    }

''', s, flags=re.S)
s=s.replace('this.drawScaledText(graphics, FastClientFonts.filledMaterialSymbol(symbol), x + size / 2, y + size / 2 + Math.round(3.3f), 1.65f, FastClientUI.fade(hovered ? -723724 : -7303024, alpha), false);',
    'net.fastclient.hud.gui.KushAssets.symbol(graphics, "hanger", x+(size-22)/2, y+(size-22)/2, 22, FastClientUI.fade(hovered ? -723724 : -7303024, alpha));')
s=s.replace('this.drawScaledText(graphics, FastClientFonts.filledMaterialSymbol("\\ue8b8"), leftX + chipSize / 2, chipY + chipSize / 2 + 4, 1.35f, FastClientUI.fade(settingsHovered ? -723724 : -7303024, 235), false);',
    'net.fastclient.hud.gui.KushAssets.symbol(graphics, "gear", leftX+3, chipY+3, chipSize-6, FastClientUI.fade(settingsHovered ? -723724 : -7303024, 235));')
p.write_text(s)

props=PROJECT/'gradle.properties';s=props.read_text().replace('mod_version=1.0.72-unlocked','mod_version=0.3.10').replace('archives_base_name=fastclient-hud','archives_base_name=KushMod-1.21.11')
props.write_text(s)
print('Theme applied')

# Overlay the small Kush additions; upstream implementation stays intact.
for p in (ROOT/'java').rglob('*.java'):
    target=PROJECT/'src/main/java'/p.relative_to(ROOT/'java')
    target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(p,target)

p=JAVA/'launcher/OptionalMenuIntegrations.java';s=p.read_text().replace("        if (!OptionalMenuIntegrations.isModMenuAvailable()) {\n            return false;\n        }\n        try {", "        if (!OptionalMenuIntegrations.isModMenuAvailable()) {\n            class_310.method_1551().method_1507(new net.fastclient.hud.gui.screens.KushInstalledModsScreen(parent));\n            return true;\n        }\n        try {",1);p.write_text(s)
p=JAVA/'mixin/client/TitleScreenMixin.java';s=p.read_text().replace('            case "store": {', '            case "skins": {\n                mc.method_1507(new net.fastclient.hud.gui.screens.StoreScreen(self));\n                break;\n            }\n            case "store": {');p.write_text(s)

p=JAVA/'mixin/client/TitleScreenMixin.java';s=p.read_text().replace('                mc.method_1507(new net.fastclient.hud.gui.screens.StoreScreen(self));','                try {\n                    mc.method_1507((net.minecraft.class_437) Class.forName("net.fastclient.client.gui.CosmeticsScreen").getConstructor().newInstance());\n                } catch (ReflectiveOperationException unavailable) {\n                    mc.method_1507(new net.fastclient.hud.gui.screens.StoreScreen(self));\n                }');p.write_text(s)
p=JAVA/'mixin/client/PauseScreenMixin.java';s=p.read_text().replace('        class_310 mc = class_310.method_1551();\n        DisplaySpace.push(graphics);','        class_310 mc = class_310.method_1551();\n        graphics.method_71278();\n        DisplaySpace.push(graphics);',1);p.write_text(s)
p=JAVA/'mixin/client/TitleScreenMixin.java';s=p.read_text().replace(
    'graphics.method_25291(class_10799.field_56883, DisplaySpace.texture(BACKGROUND), backgroundX, backgroundY, 0.0f, 0.0f, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight, -1);',
    'net.fastclient.hud.gui.KushAssets.menuLandscape(graphics, width, height);')
p.write_text(s)
p=JAVA/'gui/screens/HudOverlayScreen.java';s=p.read_text().replace('                class_310.method_1551().method_1507((class_437)new StoreScreen((class_437)this));','                try {\n                    class_310.method_1551().method_1507((class_437) Class.forName("net.fastclient.client.gui.CosmeticsScreen").getConstructor().newInstance());\n                } catch (ReflectiveOperationException unavailable) {\n                    class_310.method_1551().method_1507(new StoreScreen(this));\n                }',1);p.write_text(s)

for p in (JAVA/'gui').rglob('*.java'):
    s=p.read_text().replace('"Enabled"', '"Ativo"').replace('"Disabled"', '"Inativo"')
    p.write_text(s)


# Apply the shared header last, after every upstream and local screen overlay.
p=JAVA/'gui/screens/ModuleConfigScreen.java';s=p.read_text()
header=(ROOT/'snippets/module-header.java').read_text()
s,count=re.subn(r'    private void drawHeader\(.*?(?=    private void drawFooter)',lambda _:header+'\n',s,flags=re.S)
assert count == 1, 'Module header patch must replace exactly one shared renderer'
assert 'String status = this.module.isEnabled() ? "ATIVO" : "INATIVO";' in s
p.write_text(s)
print('Single-line module header applied to all modules')


# Remove the three requested navigation shortcuts, including their hit areas.
p=JAVA/'gui/screens/ClickGUIScreen.java';s=p.read_text()
s=re.sub(r'HEADER_NAV_SYMBOLS = new String\[\]\{[^;]+;', 'HEADER_NAV_SYMBOLS = new String[0];', s)
p.write_text(s)

# Redraw only the selected enabled HUD after the native background blur.
# No other world module is redrawn; settings never change enabled state.
p=JAVA/'gui/screens/ModuleConfigScreen.java';s=p.read_text()
needle='        net.fastclient.hud.gui.KushAssets.backdrop(graphics, DisplaySpace.width(), DisplaySpace.height());'
assert s.count(needle)==1
s=s.replace(needle,needle+'''
        if (this.module.isEnabled() && this.module.isHudVisible() && this.field_22787.field_1687 != null) {
            int hudX=this.module.getHudX(), hudY=this.module.getHudY();
            int hudW=Math.max(1,Math.round(this.module.getHudWidth()*this.module.getHudScale()));
            int hudH=Math.max(1,Math.round(this.module.getHudHeight()*this.module.getHudScale()));
            boolean covered=hudX<this.panelX+this.panelWidth && hudX+hudW>this.panelX
                && hudY<this.panelY+this.panelHeight && hudY+hudH>this.panelY;
            graphics.method_51448().pushMatrix();
            try {
                if (covered) {
                    float previewScale=Math.min(1f,Math.min(Math.max(1,this.panelX-32)/(float)hudW,
                        Math.max(1,DisplaySpace.height()-32)/(float)hudH));
                    int previewX=Math.max(8,Math.round((this.panelX-hudW*previewScale)/2));
                    int previewY=Math.max(16,Math.min(hudY,Math.round(DisplaySpace.height()-16-hudH*previewScale)));
                    graphics.method_51448().translate(previewX,previewY);
                    graphics.method_51448().scale(previewScale,previewScale);
                    graphics.method_51448().translate(-hudX,-hudY);
                }
                this.module.onRender(graphics, delta);
            } finally {
                graphics.method_51448().popMatrix();
            }
        }''',1)
p.write_text(s)

# Kush 0.3.10: language, readable modal dialogs and restored cosmetics.
lang=ASSETS/'lang/kush_pt_br.json';lang.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ROOT/'language/pt_br.json',lang)
gradle=PROJECT/'build.gradle';s=gradle.read_text();s=s.replace('dependencies {','dependencies {\n    compileOnly files("../kush-theme/base.jar")',1);gradle.write_text(s)
# All display text shares the same translator; underlying IDs are unchanged.
p=JAVA/'gui/FastClientFonts.java';s=p.read_text().replace('        class_2583 style;','        text=KushLanguage.translate(text);\n        class_2583 style;',1);p.write_text(s)
p=JAVA/'gui/screens/ClickGUIScreen.java';s=p.read_text()
a=s.index('    private void drawActionDialog(');b=s.index('    private int presetAccent(',a)
modal=s[a:b].replace('FastClientUI.fade(0xA6141115, alpha)','0xFF141115').replace('FastClientUI.fade(-16777216, 100)','0xB0000000')
s=s[:a]+modal+s[b:]
# Translation must happen before truncation, not after an English string has lost its lookup key.
s=s.replace('    private String fitText(String text, int maxWidth) {','    private String fitText(String text, int maxWidth) {\n        text=net.fastclient.hud.gui.KushLanguage.translate(text);')
s=s.replace('        int closeSize = 48;\n        int closeX = this.panelX + this.panelWidth - closeSize;', '''        int lx=this.panelX+this.panelWidth-112;
        net.fastclient.hud.gui.KushLanguage.drawButton(graphics,this.field_22793,lx,y+2,48,48,mouseX,mouseY);
        int cosmeticsX=lx-60;
        this.drawNavIcon(graphics,cosmeticsX,y+2,48,48,"\\uf19e",alpha,mouseX,mouseY);
        int closeSize = 48;
        int closeX = this.panelX + this.panelWidth - closeSize;''',1)
# This handler is before search/category hit areas, and modal overlays own all input.
s=s.replace('        int closeSize = 48;\n        int closeX = this.panelX + this.panelWidth - closeSize;', '''        if(button==0 && net.fastclient.hud.gui.KushLanguage.hit(this.panelX+this.panelWidth-112,this.getHeaderNavY()+2,48,48,mouseX,mouseY)) {
            net.fastclient.hud.gui.KushLanguage.toggle();this.method_25426();return true;
        }
        if(button==0 && net.fastclient.hud.gui.KushLanguage.hit(this.panelX+this.panelWidth-172,this.getHeaderNavY()+2,48,48,mouseX,mouseY)) {
            this.field_22787.method_1507(new net.fastclient.client.gui.CosmeticsScreen());return true;
        }
        int closeSize = 48;
        int closeX = this.panelX + this.panelWidth - closeSize;''',1 if False else 0) if False else s
# Target only click method (drawHeader has the same closeSize declaration).
a=s.index('    public boolean method_25402(')
head,click=s[:a],s[a:]
click=click.replace('        int closeSize = 48;', '''        if(button==0 && net.fastclient.hud.gui.KushLanguage.hit(this.panelX+this.panelWidth-112,this.getHeaderNavY()+2,48,48,mouseX,mouseY)) {
            net.fastclient.hud.gui.KushLanguage.toggle();this.method_25426();return true;
        }
        if(button==0 && net.fastclient.hud.gui.KushLanguage.hit(this.panelX+this.panelWidth-172,this.getHeaderNavY()+2,48,48,mouseX,mouseY)) {
            this.field_22787.method_1507(new net.fastclient.client.gui.CosmeticsScreen());return true;
        }
        int closeSize = 48;''',1)
s=head+click;p.write_text(s)
p=JAVA/'gui/widgets/ModuleCard.java';s=p.read_text();s=s.replace('String lowerDisplayName = this.module.getDisplayName().toLowerCase(Locale.ROOT);','String lowerDisplayName = net.fastclient.hud.gui.KushLanguage.translate(this.module.getDisplayName()).toLowerCase(Locale.ROOT);');p.write_text(s)
for name in ['TitleScreenMixin','PauseScreenMixin']:
    p=JAVA/'mixin/client'/f'{name}.java';s=p.read_text().replace('        switch (clicked) {','''        switch (clicked) {
            case "kush_language": {
                net.fastclient.hud.gui.KushLanguage.toggle();LANGUAGE_RETURN
            }''',1).replace("LANGUAGE_RETURN", "cir.setReturnValue(true);return;" if name=="TitleScreenMixin" else "return true;");p.write_text(s)
p=JAVA/'gui/screens/HudOverlayScreen.java';s=p.read_text()
s=s.replace('        if (this.dragging == null) {\n', '        if (this.dragging == null) {\n',1)
# Toolbar at top left is separate from draggable modules and the central editor controls.
needle='        float animProgress = this.openAnimation.getValue();'
s=s.replace(needle,'        net.fastclient.hud.gui.KushLanguage.drawButton(graphics,this.field_22793,18,18,48,36,pxMouseX,pxMouseY);\n'+needle,1)
a=s.index('    public boolean method_25402(');head,click=s[:a],s[a:]
click=click.replace('        int button = event.method_74245();','''        int button = event.method_74245();
        if(button==0 && net.fastclient.hud.gui.KushLanguage.hit(18,18,48,36,mouseX,mouseY)) {
            net.fastclient.hud.gui.KushLanguage.toggle();this.method_25426();return true;
        }''',1)
s=head+click;p.write_text(s)
print('Kush language, modal panels and cosmetics controls applied')

# Use local text coordinates for native GUI stratum bounds under scaled fonts.
p=JAVA/'gui/screens/ClickGUIScreen.java';s=p.read_text()
s=s.replace('        graphics.method_51448().translate((float)(-x), (float)(-y));\n        graphics.method_51439(this.field_22793, FastClientFonts.body(text), x, y, color, false);', '        graphics.method_51439(this.field_22793, FastClientFonts.body(text), 0, 0, color, false);')
s=s.replace('        graphics.method_51448().translate(-textX, -textY);\n        graphics.method_51439(this.field_22793, text, Math.round(textX), Math.round(textY), color, false);', '        graphics.method_51439(this.field_22793, text, 0, 0, color, false);')
p.write_text(s)

# Pure-Java decoder for the official WebP catalog thumbnails. Include every runtime dependency.
gradle=PROJECT/'build.gradle';s=gradle.read_text();s+='\n'
for group,artifact in [('imageio','imageio-webp'),('imageio','imageio-core'),('imageio','imageio-metadata'),('common','common-lang'),('common','common-io'),('common','common-image')]:
    s+=f'\ndependencies {{ implementation "com.twelvemonkeys.{group}:{artifact}:3.12.0"; include "com.twelvemonkeys.{group}:{artifact}:3.12.0" }}\n'
gradle.write_text(s)

config=PROJECT/'src/main/resources/fastclient-hud.client.mixins.json'
data=json.loads(config.read_text());data['client'].extend(['KushMouseInputMixin','KushCapeFeatureMixin','KushCapeModuleMixin','KushWaveyLayerMixin','KushWaveyBrightnessMixin']);config.write_text(json.dumps(data,indent=2))

# Independent Kush cape physics icon, matched to the outline module icons.
p=JAVA/'gui/FastClientUI.java';s=p.read_text().replace('    public static class_2960 icon(Module module) {','    public static class_2960 icon(Module module) {\n        if (module.getName().equals("CapePhysics")) return class_2960.method_60655("fastclient-hud","textures/gui/kush/cape-physics.png");');p.write_text(s)

# Optional official Wavey settings are part of the cape module's native form.
p=JAVA/'gui/screens/ModuleConfigScreen.java';s=p.read_text()
needle='        for (Setting<?> setting : this.module.getSettings()) {'
assert s.count(needle)==1
s=s.replace(needle,'''        if (this.module.getName().equals("CapePhysics") && net.fastclient.client.render.KushWaveyBridge.installed()) {
            this.addRow(new net.fastclient.hud.gui.components.KushWaveyButton(centerX,y,settingWidth,this),
                "The installed Wavey Capes handles movement. Open its original settings here.",32);
            y += this.rowHeight(32) + rowGap;
        }
'''+needle)
p.write_text(s)
