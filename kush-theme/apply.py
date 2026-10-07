from pathlib import Path
import re, shutil, json

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent / 'base'
JAVA = PROJECT / 'src/main/java/net/fastclient/hud'
ASSETS = PROJECT / 'src/main/resources/assets/fastclient-hud'
dest = ASSETS / 'textures/gui/kush'
dest.mkdir(parents=True, exist_ok=True)
for p in (ROOT/'assets').glob('*'):shutil.copyfile(p,dest/p.name)
# Source contains constants inlined by the original compiler, so apply the
# palette to all UI classes, including widgets and setting controls.
palette = {
    1717331565: "0x709C455B", -11020659: "0xFF943044", -9193473: "0xFFB84159",
    -11930: "0xFFD86377", -3704321: "0xFF8C263C", -2564638: "0xFFBB5268",
    -234156528: '0xA6141115', -435219433: '0x781B1418',
    -653586413: '0x981A161B', -434955745: '0xB32A1A20',
    -435153640: '0xA51D171C', -266722777: '0xCA352129',
    -39373: '0xFFAA293D', -34227: '0xFFCF4056',
    -15511009: '0xCC6D2535', -15243738: '0xE58F2E43',
    -8658034: '0xFFFFB5BF', 1143616571: '0x507B3A44',
    -1978658025: '0x8A191216', -1475275496: '0xAC24171D',
    1725422816: '0x665F3741', 1154997472: '0x4469404B',
    -15921133: '0xAA191317', -14406863: '0x806F3946',
    -13616829: '0xB39B4055', -12958640: '0xCD582130',
    -15591911: '0xBE27161C', -13954548: '0xFF581F2E',
    -11263981: '0xFF722237', -30106: '0xFFDB657A',
    -45747: '0xFFAA293D', -3073494: '0xFF581F2E',
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
s=s.replace('    public static int blend(', '''    public static int fade(int color, int alpha) {
        return withAlpha(color, Math.round(((color >>> 24) & 255) * Math.max(0, Math.min(255, alpha)) / 255.0f));
    }

    public static int blend(''')
p.write_text(s)

p=JAVA/'gui/FastClientFonts.java';s=p.read_text()
s=s.replace('ACTIVE_TYPEFACE = Typeface.MINECRAFT_DEFAULT', 'ACTIVE_TYPEFACE = Typeface.INTER')
s=s.replace('return ACTIVE_TYPEFACE == Typeface.MINECRAFT_DEFAULT ? 2.0f : 1.0f;', 'return 1.75f;')
p.write_text(s)

p=JAVA/'gui/widgets/ModuleCard.java';s=p.read_text()
s=s.replace('float titleScale = FastClientFonts.titleScale();', 'float titleScale = 1.35f;')
p.write_text(s)

p=JAVA/'gui/screens/ClickGUIScreen.java';s=p.read_text()
s=s.replace('"FastClientHUD"','"Kush Mods"')
s=s.replace('"Fastclient 1.21.11 (release/ca786cd3)"','"KushMod 0.3.0  ·  Minecraft 1.21.11"')
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
s=s.replace('FastClientUI.roundedRect(graphics, x, y, w, h, 7, FastClientUI.fade(0xA6141115, alpha));','net.fastclient.hud.gui.KushAssets.glass(graphics, x, y, w, h, alpha);')
s=s.replace('FastClientUI.fade(-16777216, 170)', 'FastClientUI.fade(-16777216, 55)')
p.write_text(s)

p=JAVA/'gui/screens/HudOverlayScreen.java';s=p.read_text()
s=s.replace('"textures/gui/logo.png"','"textures/gui/kush/k_red.png"')
s=s.replace('"textures/gui/fasticon_white.png"','"textures/gui/kush/k_white.png"')
s=s.replace('new String[][]{{"overlay_store", "\\uea12", "Store"}, {"overlay_cosmetics", "\\uf19e", "Cosmetics"}, {"overlay_social", "\\ue8af", "Social"}}','new String[][]{{"overlay_cosmetics", "\\uf19e", "Cosméticos"}}')
s=s.replace('graphics.method_25291(class_10799.field_56883, DisplaySpace.texture(LOGO_TEXTURE), logoX, logoY, 0.0f, 0.0f, logoSize, logoSize, logoSize, logoSize, logoColor);','net.fastclient.hud.gui.KushAssets.image(graphics, LOGO_TEXTURE, logoX, logoY, logoSize, logoSize, 1254, 1254, logoColor);')
s=s.replace('graphics.method_25293(class_10799.field_56883, DisplaySpace.texture(FAST_SETTINGS_ICON), contentX, iconY, 0.0f, 0.0f, iconWidth, iconWidth, 96, 96, 96, 96, color);','net.fastclient.hud.gui.KushAssets.image(graphics, FAST_SETTINGS_ICON, contentX, iconY, iconWidth, iconWidth, 1246, 1263, color);')
s=s.replace('        DisplaySpace.push(graphics);\n        if (this.dragging == null)', '        if (this.dragging == null) graphics.method_71278();\n        DisplaySpace.push(graphics);\n        net.fastclient.hud.gui.KushAssets.backdrop(graphics, this.screenWidth(), this.screenHeight());\n        if (this.dragging == null)',1)
s=re.sub(r'"Drag modules to reposition[^"\n]*"', '"Arraste para mover · Clique direito para configurar · Right Shift para fechar"',s)
p.write_text(s)

props=PROJECT/'gradle.properties';s=props.read_text().replace('mod_version=1.0.72-unlocked','mod_version=0.3.0').replace('archives_base_name=fastclient-hud','archives_base_name=KushMod-1.21.11')
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
p=JAVA/'gui/screens/HudOverlayScreen.java';s=p.read_text().replace('                class_310.method_1551().method_1507((class_437)new StoreScreen((class_437)this));','                try {\n                    class_310.method_1551().method_1507((class_437) Class.forName("net.fastclient.client.gui.CosmeticsScreen").getConstructor().newInstance());\n                } catch (ReflectiveOperationException unavailable) {\n                    class_310.method_1551().method_1507(new StoreScreen(this));\n                }',1);p.write_text(s)

for p in (JAVA/'gui').rglob('*.java'):
    s=p.read_text().replace('"Enabled"', '"Ativo"').replace('"Disabled"', '"Inativo"')
    p.write_text(s)
