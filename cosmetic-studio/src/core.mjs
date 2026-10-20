import * as F from 'fflate';

  'use strict';
  
  const enc = new TextEncoder(), dec = new TextDecoder('utf-8', { fatal: true });
  const CAT = 'assets/kush/cosmetics/catalog.json';
  const categories = ['hats', 'face', 'back', 'arm', 'boots', 'wings'];
  const attaches = ['HEAD', 'BODY', 'LEFT_ARM', 'RIGHT_ARM', 'LEFT_LEG', 'RIGHT_LEG', 'BOTH_LEGS'];
  const fail = message => { throw new Error(message); };
  const jsonBytes = value => enc.encode(JSON.stringify(value, null, 2) + '\n');
  function readJSON(bytes, label = 'JSON') {
    try { return JSON.parse(dec.decode(bytes)); } catch { fail(label + ': JSON inválido.'); }
  }
  function inspectZip(bytes) {
    if (!(bytes instanceof Uint8Array) || bytes.length > 64 * 1024 * 1024) fail('O JAR deve ter no máximo 64 MB.');
    const d = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    let end = -1;
    for (let i = bytes.length - 22; i >= Math.max(0, bytes.length - 65557); i--) {
      if (d.getUint32(i, true) === 0x06054b50 && i + 22 + d.getUint16(i + 20, true) === bytes.length) { end = i; break; }
    }
    if (end < 0) fail('Arquivo JAR/ZIP inválido.');
    const count = d.getUint16(end + 10, true), offset = d.getUint32(end + 16, true), size = d.getUint32(end + 12, true);
    if (d.getUint16(end + 4, true) || d.getUint16(end + 6, true) || d.getUint16(end + 8, true) !== count || count === 65535 || count > 20000 || offset + size !== end) fail('Formato ZIP não suportado (ZIP64, multidisco ou diretório inválido).');
    let p = offset, total = 0; const paths = new Set();
    for (let i = 0; i < count; i++) {
      if (p + 46 > end || d.getUint32(p, true) !== 0x02014b50) fail('Diretório ZIP inválido.');
      const flags = d.getUint16(p + 8, true), method = d.getUint16(p + 10, true);
      const compressed = d.getUint32(p + 20, true), length = d.getUint32(p + 24, true);
      const nl = d.getUint16(p + 28, true), el = d.getUint16(p + 30, true), cl = d.getUint16(p + 32, true), local = d.getUint32(p + 42, true);
      if (p + 46 + nl + el + cl > end || flags & 1 || ![0, 8].includes(method) || local + 30 > offset) fail('Entrada ZIP não suportada.');
      const name = dec.decode(bytes.subarray(p + 46, p + 46 + nl));
      if (!name || name.includes('\\') || name.startsWith('/') || name.includes(':') || name.split('/').some(x => x === '..' || x === '.') || paths.has(name)) fail('Caminho inseguro ou repetido no JAR.');
      paths.add(name);
      if (d.getUint32(local, true) !== 0x04034b50 || d.getUint16(local + 6, true) !== flags || d.getUint16(local + 8, true) !== method) fail('Cabeçalho ZIP inconsistente.');
      const lnl = d.getUint16(local + 26, true), lel = d.getUint16(local + 28, true);
      if (local + 30 + lnl + lel + compressed > offset || dec.decode(bytes.subarray(local + 30, local + 30 + lnl)) !== name) fail('Entrada ZIP truncada ou inconsistente.');
      total += length;
      if (total > 256 * 1024 * 1024 || length > 64 * 1024 * 1024) fail('O conteúdo descompactado do JAR excede o limite de memória.');
      p += 46 + nl + el + cl;
    }
    if (p !== end) fail('Diretório ZIP inconsistente.');
    return paths;
  }
  function readJar(bytes) {
    const names = inspectZip(bytes);
    if ([...names].some(n => /^META-INF\/[^/]+\.(SF|RSA|DSA|EC)$/i.test(n))) fail('JAR assinado: use uma versão do KUSHMOD sem assinatura de JAR.');
    const files = F.unzipSync(bytes);
    if (!files['fabric.mod.json'] || !files[CAT] || !files['net/fastclient/core/equip/KushCatalogProvider.class']) fail('Este JAR não contém o catálogo integrado compatível do KUSHMOD. Use um KUSHMOD com catálogo integrado schema 1.');
    const manifest = readJSON(files['fabric.mod.json'], 'fabric.mod.json');
    if (manifest.id !== 'fastclient-hud' || !/kush/i.test(manifest.name || '')) fail('Selecione o JAR do KUSHMOD.');
    const catalog = readJSON(files[CAT], 'Catálogo');
    if (catalog.schema !== 1 || !Array.isArray(catalog.items) || catalog.items.some(x => !x || typeof x.id !== 'string')) fail('Estrutura de catálogo incompatível.');
    const provider = new TextDecoder().decode(files['net/fastclient/core/equip/KushCatalogProvider.class']);
    if (!provider.includes('builtin/') || !provider.includes('/assets/kush/cosmetics/catalog.json')) fail('O carregador deste mod não oferece suporte ao catálogo integrado esperado.');
    return { files, manifest, catalog };
  }
  function checkPNG(bytes) {
    if (!(bytes instanceof Uint8Array) || bytes.length < 33 || bytes.length > 8 * 1024 * 1024 || ![137,80,78,71,13,10,26,10].every((x,i) => bytes[i] === x)) fail('Envie uma textura PNG válida de até 8 MB.');
    const d = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const w = d.getUint32(16), h = d.getUint32(20);
    if (d.getUint32(8) !== 13 || dec.decode(bytes.subarray(12,16)) !== 'IHDR' || !w || !h || w > 4096 || h > 8192 || w * h > 16777216) fail('Dimensões de PNG não suportadas.');
    let p = 8, hasData = false, ended = false;
    while (p + 12 <= bytes.length) {
      const n = d.getUint32(p), type = dec.decode(bytes.subarray(p+4,p+8));
      if (p + n + 12 > bytes.length) fail('PNG truncado.');
      if (type === 'acTL') fail('APNG não é suportado. Exporte um PNG estático.');
      if (type === 'IDAT') hasData = true;
      p += n + 12;
      if (type === 'IEND') { ended = true; break; }
    }
    if (!hasData || !ended || p !== bytes.length) fail('PNG incompleto.');
    return { width: w, height: h };
  }
  function checkModel(bytes) {
    if (bytes.length > 2 * 1024 * 1024) fail('O modelo deve ter no máximo 2 MB.');
    const m = readJSON(bytes, 'Modelo');
    if (m.meta?.model_format || m.outliner || m.animations?.length) fail('Exporte um modelo Java Block/Item em JSON. Projetos .bbmodel e animações de ossos não são aceitos nesta versão.');
    if (m.parent) fail('O modelo precisa conter seus próprios cubos, sem parent externo.');
    if (!Array.isArray(m.elements) || !m.elements.length || m.elements.length > 2048) fail('O modelo deve ter entre 1 e 2048 cubos em elements.');
    const vector = v => Array.isArray(v) && v.length === 3 && v.every(x => typeof x === 'number' && Number.isFinite(x) && Math.abs(x) <= 4096);
    for (const e of m.elements) {
      if (!e || e.type && e.type !== 'cube' || !vector(e.from) || !vector(e.to) || !e.faces || typeof e.faces !== 'object') fail('Formato de cubo inválido. Meshes não são aceitos.');
      if (!Object.keys(e.faces).length) fail('Há um cubo sem faces.');
      for (const [side, face] of Object.entries(e.faces)) {
        if (!['north','south','east','west','up','down'].includes(side) || !face || !Array.isArray(face.uv) || face.uv.length !== 4 || face.uv.some(x => !Number.isFinite(x))) fail('Uma face possui UV inválido.');
        if (face.rotation != null && ![0,90,180,270].includes(face.rotation)) fail('Rotação UV inválida.');
      }
      if (e.rotation && (!vector(e.rotation.origin) || (e.rotation.angles ? !vector(e.rotation.angles) : !['x','y','z'].includes(e.rotation.axis) || !Number.isFinite(e.rotation.angle)))) fail('Rotação de cubo incompatível.');
    }
    const refs = new Set(m.elements.flatMap(e => Object.values(e.faces).map(f => f.texture).filter(x => x != null)));
    if (refs.size > 1) fail('O modelo usa mais de uma referência de textura. Unifique o UV e exporte com uma única textura.');
    if (m.kushCosmetica) {
      if (!attaches.includes(m.kushCosmetica.attachment) || !Array.isArray(m.kushCosmetica.offset) || m.kushCosmetica.offset.length !== 6 || m.kushCosmetica.offset.some(x => !Number.isFinite(x))) fail('Metadados kushCosmetica inválidos.');
    }
    let groupCount = 0;
    const checkGroup = (g, depth = 0) => {
      if (++groupCount > 10000 || depth > 64) fail('Hierarquia de grupos grande demais.');
      if (typeof g === 'number') {
        if (!Number.isInteger(g) || g < 0 || g >= m.elements.length) fail('Grupo referencia um cubo inexistente.');
      } else {
        if (!g || typeof g !== 'object' || Array.isArray(g) || g.origin != null && !vector(g.origin) || g.rotation != null && !vector(g.rotation) || g.children != null && !Array.isArray(g.children)) fail('Grupo inválido no modelo.');
        for (const child of g.children || []) checkGroup(child, depth + 1);
      }
    };
    if (m.groups != null) {
      if (!Array.isArray(m.groups)) fail('groups deve ser uma lista.');
      for (const g of m.groups) checkGroup(g);
    }
    return m;
  }
  function options(input, model) {
    const o = { ...input };
    if (!/^[a-z0-9][a-z0-9_-]{2,63}$/.test(o.id || '')) fail('ID: use de 3 a 64 letras minúsculas, números, hífen ou sublinhado.');
    o.displayName = String(o.displayName || '').trim();
    if (!o.displayName || o.displayName.length > 100) fail('Informe um nome com até 100 caracteres.');
    if (!categories.includes(o.category) || !attaches.includes(o.attach)) fail('Categoria ou ponto de fixação inválido.');
    if (!Number.isInteger(o.flags) || o.flags < 0 || o.flags > 65535) fail('Flags precisam ser um número inteiro entre 0 e 65535.');
    if (model.kushCosmetica && model.kushCosmetica.attachment !== o.attach) fail('A fixação selecionada difere de kushCosmetica.attachment no modelo. Ajuste o modelo ou selecione a fixação original.');
    return o;
  }
  function makeAssets(input, modelBytes, texture) {
    const model = checkModel(modelBytes), image = checkPNG(texture), o = options(input, model);
    const item = { id: o.id, displayName: o.displayName, category: o.category, defaultOwned: true, attach: o.attach, flags: o.flags, model: 'model.json', texture: 'texture.png', anim: 'none', frames: 1, mspf: 250 };
    return { o, model, image, item, files: { 'catalog-item.json': jsonBytes(item), 'model.json': modelBytes, 'texture.png': texture } };
  }
  function build(input, jarBytes, modelBytes, texture) {
    const base = readJar(jarBytes), assets = makeAssets(input, modelBytes, texture);
    const { o } = assets;
    const old = base.catalog.items.filter(x => x.id === o.id);
    const folder = 'custom_' + o.id, prefix = 'assets/kush/cosmetics/' + folder + '/';
    if ((old.length || Object.keys(base.files).some(n => n.startsWith(prefix))) && !o.replace) fail('Este ID ou pasta já existe. Escolha outro ID ou marque a substituição explicitamente.');
    if (o.version && !/^\d+\.\d+\.\d+$/.test(o.version)) fail('A versão deve seguir o formato 0.3.23.');
    if (o.version && o.version !== base.manifest.version) base.manifest.version = o.version;
    const integrated = { ...assets.item, model: 'builtin/' + folder + '/model.json', texture: 'builtin/' + folder + '/texture.png' };
    base.catalog.items = base.catalog.items.filter(x => x.id !== o.id);
    base.catalog.items.push(integrated);
    const files = Object.assign(Object.create(null), base.files);
    files[CAT] = jsonBytes(base.catalog);
    files[prefix + 'model.json'] = modelBytes;
    files[prefix + 'texture.png'] = texture;
    // Keep manifest bytes unchanged when no version change was requested.
    if (o.version && o.version !== readJSON(base.files['fabric.mod.json']).version) files['fabric.mod.json'] = jsonBytes(base.manifest);
    const jar = F.zipSync(files, { level: 6 });
    return { jar, package: F.zipSync(assets.files, { level: 6 }), item: assets.item, integrated, version: base.manifest.version, cubes: assets.model.elements.length, image: assets.image, count: base.catalog.items.length };
  }
  const api = { readJar, checkModel, checkPNG, makeAssets, build, jsonBytes, categories, attaches, packageAssets: assets => F.zipSync(assets.files, { level: 6 }) };
export {readJar, checkModel, checkPNG, makeAssets, build, jsonBytes, categories, attaches};
export default api;
