from pathlib import Path
import json,zipfile,hashlib
from brand import brand_class
base=Path('kush-theme/base.jar')
patch=next(p for p in Path('base/build/libs').glob('*.jar') if 'sources' not in p.name)
out=Path('dist/KushMod-1.21.11-v0.3.6.jar');out.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(patch) as pz:
    changes={n:pz.read(n) for n in pz.namelist() if n.startswith(('net/fastclient/hud/gui/','net/fastclient/hud/launcher/','net/fastclient/client/gui/CosmeticsScreen','net/fastclient/client/FastClientCoreClient','net/fastclient/client/render/CosmeticTextures','net/fastclient/mixin/AvatarRendererMixin','net/fastclient/core/equip/Kush','net/fastclient/core/equip/CosmeticsAvailability','net/fastclient/hud/modules/impl/player/CosmeticsModule','net/fastclient/hud/modules/Module','net/fastclient/hud/modules/impl/hud/DayCounter','net/fastclient/hud/modules/impl/hud/BiomeHUD','net/fastclient/hud/modules/impl/hud/ServerInfoHUD')) and n.endswith('.class')}
    for n in ['net/fastclient/hud/mixin/client/TitleScreenMixin.class','net/fastclient/hud/mixin/client/PauseScreenMixin.class']:
        changes[n]=pz.read(n)
    changes.update({n:pz.read(n) for n in pz.namelist() if n.startswith(('assets/fastclient-hud/textures/gui/kush/','assets/fastclient-hud/lang/kush_'))})
with zipfile.ZipFile(base) as zin,zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as zout:
    for info in zin.infolist():
        if info.filename in changes:continue
        data=zin.read(info.filename)
        if info.filename=='fabric.mod.json':
            meta=json.loads(data);meta['name']='KushMod';meta['version']='1.0.72+kush.0.3.6'
            meta['description']='Kush para Minecraft 1.21.11. Interface e módulos configuráveis.'
            meta['icon']='assets/fastclient-hud/textures/gui/kush/k_pixel_red.png'
            meta['mixins'].append({'config':'fastclientcore.client.mixins.json','environment':'client'})
            data=json.dumps(meta,indent=2,ensure_ascii=False).encode()
        if info.filename.endswith('.class'): data=brand_class(data)
        zout.writestr(info,data)
    for n,data in changes.items():zout.writestr(n,brand_class(data) if n.endswith(".class") else data)
    zout.writestr('KUSH-BUILD.txt','KushMod 0.3.6 / Minecraft 1.21.11 / FastClient HUD base retained\n')
Path('dist/SHA256.txt').write_text(hashlib.sha256(out.read_bytes()).hexdigest()+'  '+out.name+'\n')
print(out,out.stat().st_size,'bytes')

