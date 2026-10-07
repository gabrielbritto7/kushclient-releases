"""Transactional per-user installation; game data lives outside this folder."""
from __future__ import annotations
import hashlib
import json
import os
import shutil
import uuid
import configparser
import time
from pathlib import Path

MARKER = '.kushclient-install.json'


def install_update(source,target,data_root,result_path):
    """Atualiza uma instalação identificada e deixa os atalhos existentes intactos."""
    source,target,data_root=Path(source).resolve(),Path(target).resolve(),Path(data_root).resolve()
    manifest=json.loads((source/'install-manifest.json').read_text(encoding='utf-8'))
    failure=data_root/'updates/failed.json'
    try:
        marker=json.loads((target/MARKER).read_text(encoding='utf-8'))
        if marker.get('product')!='KushClient':
            raise ValueError('Esta pasta não é uma instalação do KushClient.')
        installed=install(source,target,data_root)
        result=configparser.ConfigParser()
        result['Install']={'directory':str(installed),'desktop':'0','startmenu':'0','launch':'1'}
        with Path(result_path).open('w',encoding='utf-16') as output:
            result.write(output)
        failure.unlink(missing_ok=True)
        return installed
    except Exception:
        try:
            failure.parent.mkdir(parents=True,exist_ok=True)
            failure.write_text(json.dumps({'version':manifest.get('version'),'when':time.time()}),encoding='utf-8')
        except OSError:
            pass
        raise

def destination_path(value, source, data_root):
    target = Path(value).expanduser().resolve()
    source, data_root = Path(source).resolve(), Path(data_root).resolve()
    if len(str(target)) > 180:
        raise ValueError('Escolha uma pasta com um caminho mais curto.')
    if target == Path(target.anchor) or len(target.parts) < 3:
        raise ValueError('Escolha uma pasta exclusiva para o KushClient.')
    if target == source or target in source.parents or source in target.parents:
        raise ValueError('Escolha uma pasta fora dos arquivos temporários do instalador.')
    if target == data_root or target in data_root.parents or data_root in target.parents:
        raise ValueError('A pasta de instalação deve ser separada dos seus mundos e contas.')
    if target.exists() and not target.is_dir():
        raise ValueError('Esse caminho não é uma pasta.')
    if target.exists() and any(target.iterdir()) and not (target / MARKER).is_file():
        raise ValueError('Essa pasta já tem arquivos. Escolha uma pasta vazia ou uma instalação do KushClient.')
    return target

def install(source, target, data_root, progress=None, cancel=None):
    source = Path(source).resolve()
    target = destination_path(target, source, data_root)
    manifest = json.loads((source / 'install-manifest.json').read_text(encoding='utf-8'))
    entries = manifest['files']
    total = sum(item['size'] for item in entries)
    target.parent.mkdir(parents=True, exist_ok=True)
    free = shutil.disk_usage(target.parent).free
    previous_size = sum(p.stat().st_size for p in target.rglob('*') if p.is_file()) if target.exists() else 0
    if free < total + previous_size + 64*1024*1024:
        raise ValueError('Não há espaço suficiente nessa unidade para instalar com segurança.')
    suffix = uuid.uuid4().hex[:12]
    staging = target.parent / ('.kush-stage-' + suffix)
    backup = target.parent / ('.kush-backup-' + suffix)
    moved = committed = False
    transferred = 0
    def check():
        if cancel and cancel():
            raise InterruptedError('Instalação cancelada. Seus dados foram preservados.')
    try:
        staging.mkdir()
        # Preserve files the owner may have added to a previous installation.
        if target.exists():
            shutil.copytree(target, staging, dirs_exist_ok=True)
        for item in entries:
            check()
            name = item['path']
            relative = Path(name)
            if relative.is_absolute() or '..' in relative.parts or ':' in name or '\\' in name:
                raise ValueError('O pacote contém um caminho inválido.')
            src, dst = source / relative, staging / relative
            dst.parent.mkdir(parents=True, exist_ok=True)
            digest = hashlib.sha256()
            with src.open('rb') as a, dst.open('wb') as b:
                while block := a.read(1024*1024):
                    check(); b.write(block); digest.update(block); transferred += len(block)
                    if progress: progress(transferred, total)
            if dst.stat().st_size != item['size'] or digest.hexdigest() != item['sha256']:
                raise ValueError('A verificação de integridade falhou. Baixe o instalador novamente.')
        (staging / MARKER).write_text(json.dumps({'product':'KushClient','version':manifest['version'],
           'owned_files':[item['path'] for item in entries]},ensure_ascii=False),encoding='utf-8')
        check()
        if target.exists():
            os.replace(target, backup); moved = True
        os.replace(staging, target); committed = True
    except BaseException:
        if moved and not committed:
            os.replace(backup, target)
        raise
    finally:
        shutil.rmtree(staging,ignore_errors=True)
        if committed:
            shutil.rmtree(backup,ignore_errors=True)
    return target
