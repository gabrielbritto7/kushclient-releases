import hashlib
import importlib.util
import json
import tempfile
from pathlib import Path
import sys

extracted = Path(sys.argv[1])
manifest_path = next(extracted.rglob('install-manifest.json'))
payload = manifest_path.parent
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
assert manifest['version'] == '0.9.17'
for item in manifest['files']:
    p = payload / item['path']
    assert p.stat().st_size == item['size'], item['path']
    assert hashlib.sha256(p.read_bytes()).hexdigest() == item['sha256'], item['path']
assert not (payload / 'accounts.json').exists()
assert not (payload / '.env').exists()
assert not (payload / 'Chave-De-Atualizacoes.pem').exists()
assert not (payload / 'saves').exists()
assert (payload / 'assets/dolar$-ação.txt').read_text().startswith('Arquivo')
spec = importlib.util.spec_from_file_location('model', payload / 'installer_model.py')
model = importlib.util.module_from_spec(spec)
spec.loader.exec_module(model)
with tempfile.TemporaryDirectory(prefix='kush-validation-') as tmp:
    root = Path(tmp)
    data = root / 'dados-do-usuario'
    data.mkdir()
    (data / 'accounts.json').write_text('CONTAS PRESERVADAS')
    (data / 'saves').mkdir()
    (data / 'saves/mundo.txt').write_text('MUNDO PRESERVADO')
    installed = model.install(payload, root / 'app', data)
    assert json.loads((installed / model.MARKER).read_text())['version'] == '0.9.17'
    (installed / 'arquivo-pessoal.txt').write_text('ARQUIVO PRESERVADO')
    result = root / 'result.ini'
    model.install_update(payload, installed, data, result)
    assert (installed / 'arquivo-pessoal.txt').read_text() == 'ARQUIVO PRESERVADO'
    assert (data / 'accounts.json').read_text() == 'CONTAS PRESERVADAS'
    assert (data / 'saves/mundo.txt').read_text() == 'MUNDO PRESERVADO'
    before = (installed / 'launcher.py').read_bytes()
    (payload / 'launcher.py').write_bytes(b'ARQUIVO CORROMPIDO')
    try:
        model.install_update(payload, installed, data, result)
    except ValueError:
        pass
    else:
        raise AssertionError('O instalador aceitou um payload corrompido')
    assert (installed / 'launcher.py').read_bytes() == before
    assert (data / 'accounts.json').read_text() == 'CONTAS PRESERVADAS'
report = {'success': True, 'checks': ['Payload extraído do EXE: tamanho e SHA-256 de todos os arquivos',
    'Nomes com acentos e cifrão preservados', 'Instalação com manifesto e versão corretos',
    'Atualização preserva contas, mundos e arquivos pessoais', 'Falha de integridade mantém a instalação anterior']}
Path(sys.argv[2]).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(report, ensure_ascii=False))
