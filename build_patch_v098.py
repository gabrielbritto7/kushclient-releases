from pathlib import Path
import argparse, base64, hashlib, json, shutil

VERSION='0.9.8'

def replace(path, old, new):
    p=Path(path); s=p.read_text(encoding='utf-8')
    if old not in s:
        raise RuntimeError(f'pattern not found in {p}: {old[:80]!r}')
    p.write_text(s.replace(old,new),encoding='utf-8')

def patch(app, patchroot):
    app=Path(app); patchroot=Path(patchroot)
    replace(app/'core.py','VERSION = "0.9.7"','VERSION = "0.9.8"')
    replace(app/'installer.py',"VERSION = '0.9.7'","VERSION = '0.9.8'")
    replace(app/'installer.py',"self.setWindowTitle('Instalar KushClient');self.setWindowIcon(QIcon(str(ROOT/'assets/kushclient.png')))","self.setWindowTitle('Instalar KushClient');self.setWindowIcon(QIcon(str(ROOT/'assets'/('kushclient.ico' if os.name=='nt' else 'kushclient.png'))))")
    replace(app/'installer.py','Use o arquivo kushclient.exe para iniciar a instalação.','Use o arquivo kushclient-v.0.9.8.exe para iniciar a instalação.')

    replace(app/'launcher.py','self.setWindowIcon(QIcon(str(ASSETS / "kushclient.png")))','self.setWindowIcon(QIcon(str(ASSETS / ("kushclient.ico" if os.name == "nt" else "kushclient.png"))))')
    replace(app/'launcher.py','app.setWindowIcon(QIcon(str(ASSETS / "kushclient.png")))','app.setWindowIcon(QIcon(str(ASSETS / ("kushclient.ico" if os.name == "nt" else "kushclient.png"))))')

    replace(app/'modern_ui.py','''        self.filter_installed=False\n''','''        self.filter_installed=False\n        self.setAttribute(Qt.WidgetAttribute.WA_OpaquePaintEvent, True)\n        self.setAttribute(Qt.WidgetAttribute.WA_NoSystemBackground, True)\n''')
    replace(app/'modern_ui.py','''            self.offset += difference * (1 - math.exp(-elapsed / 760))\n''','''            # Responde rápido ao mouse sem arrastar o cenário por quase um segundo.\n            self.offset += difference * (1 - math.exp(-elapsed / 145))\n''')
    replace(app/'modern_ui.py','''        # Filtragem bilinear preserva o deslocamento fracionário: o cenário\n        # não salta de pixel em pixel quando a animação é muito lenta.\n        p.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform)\n        p.drawPixmap(QRectF(-20+self.offset.x(), -20+self.offset.y(),\n                           self.width()+40, self.height()+40), self.canvas,\n                     QRectF(0, 0, self.canvas.width(), self.canvas.height()))\n''','''        # O canvas já nasce no tamanho/DPI correto. Só deslocar evita\n        # reamostrar a janela inteira em cada frame no Windows.\n        p.drawPixmap(QPointF(-20 + self.offset.x(), -20 + self.offset.y()), self.canvas)\n''')

    replace(app/'widgets.py','''def profile_cover(profile, size=64):\n    """Ícone em cubo para instâncias sem uma imagem do modpack."""\n''','''def profile_cover(profile, size=64):\n    """Usa o bloco clássico enviado pelo usuário em perfis Vanilla."""\n    if str(profile.get("loader", "")).casefold() == "vanilla":\n        vanilla = QPixmap(str(Path(__file__).parent / "assets" / "vanilla-block.png"))\n        if not vanilla.isNull():\n            return vanilla.scaled(size, size, Qt.AspectRatioMode.KeepAspectRatio,\n                                  Qt.TransformationMode.SmoothTransformation)\n''')

    replace(app/'windows_shell.py','''        self.properties = []\n        self.icons = []\n''','''        self.properties = []\n        self.icons = []\n        self.class_icons = []\n''')
    replace(app/'windows_shell.py','''                send(self.hwnd,0x0080,slot,icon)  # WM_SETICON\n''','''                send(self.hwnd,0x0080,slot,icon)  # WM_SETICON\n                # Qt pode manter o ícone de classe genérico do pythonw.exe.\n                # Definir também GCLP_HICON/GCLP_HICONSM força o mesmo K no taskbar.\n                try:\n                    setter = user.SetClassLongPtrW\n                    setter.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_ssize_t]\n                    setter.restype = ctypes.c_ssize_t\n                    index = -34 if slot == 0 else -14\n                    old = setter(self.hwnd, index, icon)\n                    self.class_icons.append((index, old))\n                except AttributeError:\n                    pass\n''')
    replace(app/'windows_shell.py','''        if self.icons:\n            user = ctypes.windll.user32\n            destroy = user.DestroyIcon\n''','''        if self.icons:\n            user = ctypes.windll.user32\n            if self.class_icons:\n                try:\n                    setter = user.SetClassLongPtrW\n                    setter.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_ssize_t]\n                    setter.restype = ctypes.c_ssize_t\n                    for index, old in reversed(self.class_icons):\n                        setter(self.hwnd, index, old)\n                except AttributeError:\n                    pass\n                self.class_icons = []\n            destroy = user.DestroyIcon\n''')

    chunks=[]
    for part in sorted(patchroot.glob('vanilla-block.b64.*')):
        chunks.append(part.read_text(encoding='ascii').strip())
    if not chunks:
        raise RuntimeError('vanilla block chunks missing')
    (app/'assets/vanilla-block.png').write_bytes(base64.b64decode(''.join(chunks)))

    (app/'native').mkdir(exist_ok=True); (app/'scripts').mkdir(exist_ok=True)
    for src in (patchroot/'native').iterdir():
        shutil.copy2(src, app/'native'/src.name)
    shutil.copy2(patchroot/'prepare_setup.py', app/'scripts/prepare_setup.py')


def finalize(app):
    app=Path(app)
    runtime=app/'RUNTIME.json'
    data=json.loads(runtime.read_text(encoding='utf-8'))
    data['version']=VERSION
    data['exe_sha256']=hashlib.sha256((app/'KushClient.exe').read_bytes()).hexdigest()
    runtime.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    cfg=app/'release-config.json'
    if cfg.exists():
        c=json.loads(cfg.read_text(encoding='utf-8')); c['version']=VERSION
        cfg.write_text(json.dumps(c,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':
    ap=argparse.ArgumentParser(); ap.add_argument('mode',choices=['patch','finalize']); ap.add_argument('app'); ap.add_argument('--patchroot')
    a=ap.parse_args()
    if a.mode=='patch': patch(a.app,a.patchroot)
    else: finalize(a.app)
