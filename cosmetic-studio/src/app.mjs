import * as T from 'three';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';
import {TransformControls} from 'three/addons/controls/TransformControls.js';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';
import {DRACOLoader} from 'three/addons/loaders/DRACOLoader.js';
import {KTX2Loader} from 'three/addons/loaders/KTX2Loader.js';
import {MeshoptDecoder} from 'three/addons/libs/meshopt_decoder.module.js';
import * as F from 'fflate';
import K from './core.mjs';
import {ANCHORS,FACE_NAMES,createModel,fitModel,makeFit,simpleModel,disposeObject} from './model.mjs';
import {inspectGLB,prepareMesh,voxelize} from './voxel.mjs';
const $=id=>document.getElementById(id),viewport=$('viewport');
const state={model:null,texture:null,textureBytes:null,palette:false,fit:{position:[0,33,0],rotation:[0,0,0],scale:1,attach:'HEAD'},center:[0,0,0],naturalCenter:[0,0,0],dimensions:[0,0,0],jar:null,jarName:'KUSHMOD.jar',original:null,triangles:null,converted:false,selected:-1,revision:0,busy:false,history:[],controller:null};
let cosmetic,convertedMesh,selectionBox,skinTexture,player,loadRevision=0,textureRevision=0,skinRevision=0;
const renderer=new T.WebGLRenderer({antialias:true,alpha:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(Math.min(devicePixelRatio,2));renderer.setClearColor(0x16171e,0);renderer.outputColorSpace=T.SRGBColorSpace;renderer.toneMapping=T.NoToneMapping;viewport.prepend(renderer.domElement);
const scene=new T.Scene(),camera=new T.PerspectiveCamera(38,1,.1,2000);camera.position.set(46,30,-65);
const orbit=new OrbitControls(camera,renderer.domElement);orbit.target.set(0,18,0);orbit.enableDamping=true;orbit.minDistance=12;orbit.maxDistance=200;
scene.add(new T.HemisphereLight(0xfaf5ff,0x69647a,2.2));
const key=new T.DirectionalLight(0xffeee2,2);key.position.set(-25,50,-35);scene.add(key);const rim=new T.DirectionalLight(0xaebaff,1.4);rim.position.set(25,35,25);scene.add(rim);
const grid=new T.GridHelper(96,24,0x55515e,0x34323d);grid.material.transparent=true;grid.material.opacity=.45;scene.add(grid);
const disk=new T.Mesh(new T.CircleGeometry(15,64),new T.MeshBasicMaterial({color:0x27232f,transparent:true,opacity:.35,depthWrite:false}));disk.rotation.x=-Math.PI/2;disk.position.y=-.03;scene.add(disk);
cosmetic=new T.Group();cosmetic.rotation.order='ZYX';scene.add(cosmetic);
const gizmo=new TransformControls(camera,renderer.domElement);gizmo.setSize(.8);scene.add(gizmo.getHelper());
gizmo.addEventListener('dragging-changed',e=>{orbit.enabled=!e.value;});
gizmo.addEventListener('mouseDown',()=>remember());
gizmo.addEventListener('objectChange',()=>{
 if(state.busy)return;
 state.fit.position=cosmetic.position.toArray();state.fit.rotation=[cosmetic.rotation.x,cosmetic.rotation.y,cosmetic.rotation.z].map(T.MathUtils.radToDeg);state.revision++;syncFitFields();dirty();
});
const observer=new ResizeObserver(()=>{
 const w=viewport.clientWidth,h=viewport.clientHeight;renderer.setSize(w,h);camera.aspect=w/h;camera.updateProjectionMatrix();
});observer.observe(viewport);
renderer.setAnimationLoop(()=>{orbit.update();if(selectionBox&&convertedMesh?.visible)selectionBox.update();renderer.render(scene,camera);});
function say(message,kind='info'){$('status').textContent=message;$('status').dataset.kind=kind;}
function dirty(){$('saveState').textContent='Alterações no projeto';updateButtons();}
function change(){state.revision++;dirty();}
function ready(){return !!(state.model&&state.textureBytes&&!state.busy&&(!state.original||state.converted));}
function updateButtons(){
 for(const id of ['model','texture','project'])$(id).disabled=state.busy;
 document.querySelectorAll('[data-preset]').forEach(button=>button.disabled=state.busy);
 $('exportAssets').disabled=!ready();$('exportJar').disabled=!ready()||!state.jar;
 $('convert').disabled=!state.triangles||state.busy;$('undo').disabled=!state.history.length||state.busy;
 $('addCube').disabled=state.busy||!!state.original&&!state.converted;
 for(const id of ['duplicateCube','deleteCube'])$(id).disabled=state.busy||state.selected<0;
}
function remember(){
 if(!state.model)return;
 state.history.push({model:structuredClone(state.model),fit:structuredClone(state.fit),center:state.center.slice(),palette:state.palette});
 if(state.history.length>30)state.history.shift();updateButtons();
}
function applyFit(){
 cosmetic.position.fromArray(state.fit.position);cosmetic.rotation.set(...state.fit.rotation.map(T.MathUtils.degToRad),'ZYX');cosmetic.scale.setScalar(state.fit.scale);
 syncFitFields();
}
function syncFitFields(){
 ['px','py','pz'].forEach((id,i)=>$(id).value=Number(state.fit.position[i].toFixed(3)));
 ['rx','ry','rz'].forEach((id,i)=>$(id).value=Number(state.fit.rotation[i].toFixed(2)));
 $('scale').value=Math.round(state.fit.scale*100);$('scaleValue').value=Math.round(state.fit.scale*100)+'%';
}
function clearSelection(){
 if(selectionBox){scene.remove(selectionBox);selectionBox.geometry.dispose();selectionBox.material.dispose();selectionBox=null;}
}
function selectedCube(index){
 state.selected=index;$('cubeList').value=String(index);$('cubeEditor').hidden=index<0;
 clearSelection();
 if(index>=0&&state.model){
  const e=state.model.elements[index];
  ['cx','cy','cz'].forEach((id,i)=>$(id).value=Number(((e.from[i]+e.to[i])/2).toFixed(4)));
  ['sx','sy','sz'].forEach((id,i)=>$(id).value=Number((e.to[i]-e.from[i]).toFixed(4)));
  $('paintLabel').hidden=!state.palette;
  if(state.palette)$('cubeColor').value=String(Math.floor(Object.values(e.faces)[0]?.uv[0]||0));
  let target;convertedMesh?.traverse(o=>{if(o.isMesh&&o.userData.element===index)target=o;});
  if(target){target.updateWorldMatrix(true,true);selectionBox=new T.BoxHelper(target,0xfa9caa);scene.add(selectionBox);}
 }
 updateButtons();
}
function refreshList(){
 const select=$('cubeList');select.replaceChildren();
 (state.model?.elements||[]).forEach((e,i)=>{const option=document.createElement('option');option.value=String(i);option.textContent=(i+1)+'. '+(e.name||'Cubo '+(i+1));select.append(option);});
 $('cubeCount').textContent=state.model?state.model.elements.length.toLocaleString('pt-BR')+' cubos':'GLB original';
 selectedCube(state.selected>=0&&state.selected<(state.model?.elements.length||0)?state.selected:-1);
}
function rebuild({recenter=false}={}){
 clearSelection();
 if(convertedMesh){cosmetic.remove(convertedMesh);disposeObject(convertedMesh);convertedMesh=null;}
 if(state.model){
  convertedMesh=createModel(state.model,state.texture);
  const bounds=new T.Box3().setFromObject(convertedMesh);state.naturalCenter=bounds.getCenter(new T.Vector3()).toArray();state.dimensions=bounds.getSize(new T.Vector3()).toArray();
  cosmetic.add(convertedMesh);
  if(recenter)state.center=makeFit(convertedMesh,$('anchor').value).center;
  convertedMesh.position.fromArray(state.center).negate();
  convertedMesh.traverse(o=>{if(o.isMesh)o.material.wireframe=$('wireframe').checked;});
 }
 if(state.original)state.original.position.fromArray(state.center).negate();
 applyFit();refreshList();showComparison();$('welcome').hidden=!!(state.model||state.original);
 if(state.model||state.original)gizmo.attach(cosmetic);else gizmo.detach();
 updateButtons();
}
function showComparison(){
 const original=$('original').checked;
 if(convertedMesh)convertedMesh.visible=!original||!state.original;
 if(state.original)state.original.visible=original||!state.converted;
 $('previewBadge').textContent=state.original&&(!state.converted||original)?'GLB ORIGINAL · A CONVERTER':'MODELO DO KUSHMOD';
 if(selectionBox)selectionBox.visible=!original;
}
function disposeOriginal(){
 if(state.original){cosmetic.remove(state.original);disposeObject(state.original,{textures:true});state.original=null;}
 state.triangles=null;state.converted=false;$('glbPanel').hidden=true;$('original').checked=false;
}
function resetAnchor(){
 const name=$('anchor').value,anchor=ANCHORS[name],position=anchor.position.slice(),d=state.dimensions;
 if(name==='head')position[1]=32+d[1]/2;
 if(name==='face')position[2]=-4-d[2]/2-.1;
 if(name==='back')position[2]=2+d[2]/2+.1;
 if(name==='chest')position[2]=-2-d[2]/2-.1;
 state.center=state.naturalCenter.slice();convertedMesh?.position.fromArray(state.center).negate();state.original?.position.fromArray(state.center).negate();
 state.fit={position,rotation:[0,0,0],scale:1,attach:anchor.attach};$('category').value=anchor.category;applyFit();
}
const paletteColors=['#eee5ce','#27222c','#f1bd52','#b9314b','#f27ca5','#779bc9','#80b792','#f8f5ef'];
async function pngBytes(canvas){const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/png'));if(!blob)throw Error('Não foi possível criar a textura PNG.');return new Uint8Array(await blob.arrayBuffer());}
async function defaultPalette(){
 const canvas=document.createElement('canvas');canvas.width=16;canvas.height=16;const ctx=canvas.getContext('2d');paletteColors.forEach((c,i)=>{ctx.fillStyle=c;ctx.fillRect(i,0,1,1);});return pngBytes(canvas);
}
async function decodeTexture(bytes){
 K.checkPNG(bytes);const blob=new Blob([bytes],{type:'image/png'}),bitmap=await createImageBitmap(blob);
 const texture=new T.Texture(bitmap);texture.colorSpace=T.SRGBColorSpace;texture.magFilter=T.NearestFilter;texture.minFilter=T.NearestFilter;texture.generateMipmaps=false;texture.flipY=true;texture.needsUpdate=true;return texture;
}
function installTexture(bytes,texture){
 const old=state.texture;state.texture=texture;state.textureBytes=bytes;if(old&&old!==texture){old.image?.close?.();old.dispose();}
 $('textureInfo').textContent='TEXTURE.PNG · '+texture.image.width+' × '+texture.image.height+' px';
}
async function setTexture(bytes){const texture=await decodeTexture(bytes);installTexture(bytes,texture);}
async function preset(kind){
 if(state.busy)return;state.busy=true;updateButtons();try{loadRevision++;textureRevision++;
 disposeOriginal();state.model=simpleModel(kind);state.selected=-1;state.history=[];state.palette=true;
 await setTexture(await defaultPalette());
 $('anchor').value=kind==='hat'?'head':'back';
 const name={hat:'Meu chapéu',backpack:'Minha mochila',wings:'Minhas asas'}[kind];$('displayName').value=name;$('cosmeticId').value={hat:'meu_chapeu',backpack:'minha_mochila',wings:'minhas_asas'}[kind];
 rebuild({recenter:true});resetAnchor();$('category').value=kind==='wings'?'wings':ANCHORS[$('anchor').value].category;change();$('modelInfo').textContent='Modelo criado no Studio · '+state.model.elements.length+' cubos';say('Modelo pronto. Escolha um cubo para editar ou arraste as setas para ajustar no personagem.');
 }finally{state.busy=false;updateButtons();}
}
async function importModel(file){
 if(!file||state.busy)return;
 const stamp=++loadRevision;textureRevision++;state.busy=true;updateButtons();say('Abrindo '+file.name+'…');
 try{
  if(file.size>64*1024*1024)throw Error('O arquivo ultrapassa 64 MB.');
  const buffer=await file.arrayBuffer();if(stamp!==loadRevision)return;
  if(/\.glb$/i.test(file.name)){
   const json=inspectGLB(buffer);
   const manager=new T.LoadingManager();manager.setURLModifier(url=>{if(/^(blob:|data:|file:)/i.test(url))return url;throw Error('O GLB tentou carregar um recurso externo.');});
   const draco=new DRACOLoader(manager).setDecoderPath(new URL('decoders/draco/',location.href).href).setWorkerLimit(2);
   const ktx=new KTX2Loader(manager).setTranscoderPath(new URL('decoders/basis/',location.href).href).detectSupport(renderer).setWorkerLimit(2);
   let gltf;
   try{gltf=await new GLTFLoader(manager).setDRACOLoader(draco).setKTX2Loader(ktx).setMeshoptDecoder(MeshoptDecoder).parseAsync(buffer,'');}finally{draco.dispose();ktx.dispose();}
   if(stamp!==loadRevision){disposeObject(gltf.scene,{textures:true});return;}
   const raw=gltf.scene,box=new T.Box3().setFromObject(raw),center=box.getCenter(new T.Vector3()),size=box.getSize(new T.Vector3()),longest=Math.max(size.x,size.y,size.z);
   if(box.isEmpty()||!Number.isFinite(longest)||longest<1e-10){disposeObject(raw,{textures:true});throw Error('GLB sem malha visível.');}
   const normalized=new T.Group();normalized.scale.setScalar(12/longest);normalized.position.copy(center).multiplyScalar(-12/longest);normalized.add(raw);
   const wrapper=new T.Group();wrapper.add(normalized);
   let triangles;try{triangles=await prepareMesh(wrapper,renderer);}catch(error){disposeObject(wrapper,{textures:true});throw error;}
   if(stamp!==loadRevision){disposeObject(wrapper,{textures:true});return;}
   disposeOriginal();state.original=wrapper;state.triangles=triangles;cosmetic.add(wrapper);state.model=null;state.center=[0,0,0];state.naturalCenter=[0,0,0];state.dimensions=size.toArray().map(v=>v*12/longest);state.palette=false;state.selected=-1;state.history=[];
   $('glbPanel').hidden=false;$('original').checked=true;$('conversionInfo').textContent=triangles.length.toLocaleString('pt-BR')+' triângulos · escolha o detalhe e converta.';resetAnchor();rebuild();
   say('GLB aberto'+(gltf.animations.length?' na pose estática':'')+'. Ajuste no personagem e clique em “Converter para o KUSHMOD”.');
  }else if(/\.json$/i.test(file.name)){
   const model=K.checkModel(new Uint8Array(buffer));
   disposeOriginal();state.model=model;state.palette=false;state.selected=-1;state.history=[];
   await setTexture(await defaultPalette());state.textureBytes=null;$('textureInfo').textContent='Selecione a TEXTURE.PNG deste modelo para exportar.';
   if(model.kushStudio?.coordinateSpace==='player'){
    state.center=[0,0,0];state.fit={position:[0,0,0],rotation:[0,0,0],scale:1,attach:model.kushStudio.attach||'HEAD'};
    const match=Object.entries(ANCHORS).find(([,a])=>a.attach===state.fit.attach);if(match)$('anchor').value=match[0];rebuild();
   }else{rebuild({recenter:true});resetAnchor();}
   say('MODEL.JSON importado. Selecione sua TEXTURE.PNG e ajuste a peça no personagem.');
  }else throw Error('Escolha um arquivo .GLB ou .JSON.');
  $('modelInfo').textContent=file.name;change();
 }catch(error){say(error.message,'error');}finally{state.busy=false;updateButtons();}
}
async function convert(){
 if(!state.triangles||state.busy)return;
 const stamp=loadRevision;state.busy=true;state.controller=new AbortController();$('cancelConvert').hidden=false;$('progress').hidden=false;$('progress').value=0;updateButtons();gizmo.detach();
 try{
  say('Convertendo a superfície e as cores do GLB em cubos…');
  const result=await voxelize(state.triangles,{resolution:Number($('detail').value),signal:state.controller.signal,onProgress:p=>$('progress').value=p});
  if(stamp!==loadRevision)throw Error('O modelo mudou durante a conversão.');
  const canvas=document.createElement('canvas');canvas.width=result.width;canvas.height=result.height;canvas.getContext('2d').putImageData(new ImageData(result.pixels,result.width,result.height),0,0);
  const bytes=await pngBytes(canvas),modelBytes=K.jsonBytes(result.model);K.checkModel(modelBytes);
  await setTexture(bytes);state.model=result.model;state.converted=true;state.palette=false;state.selected=-1;state.history=[];$('original').checked=false;state.center=[0,0,0];rebuild();
  $('conversionInfo').textContent=result.cubes.toLocaleString('pt-BR')+' cubos · '+result.colors+' cores · pronto para exportar';change();say('Conversão concluída. Este é o modelo que irá para o jogo. Compare com o original e confira o encaixe.','success');
 }catch(error){say(error.name==='AbortError'?'Conversão cancelada.':error.message,'error');}
 finally{state.busy=false;state.controller=null;$('cancelConvert').hidden=true;$('progress').hidden=true;if(state.model||state.original)gizmo.attach(cosmetic);updateButtons();}
}
function dataURL(bytes){return new Promise((resolve,reject)=>{const r=new FileReader();r.onload=()=>resolve(r.result);r.onerror=reject;r.readAsDataURL(new Blob([bytes],{type:'image/png'}));});}
function download(bytes,name,type='application/octet-stream'){
 const url=URL.createObjectURL(new Blob([bytes],{type})),a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),60000);
}
function options(){
 const anchor=ANCHORS[$('anchor').value];return {id:$('cosmeticId').value.trim(),displayName:$('displayName').value.trim(),category:$('category').value,attach:anchor.attach,flags:525,replace:$('replace').checked};
}
async function exportFiles(withJar){
 if(!ready()||withJar&&!state.jar)return;
 try{
  state.busy=true;updateButtons();say(withJar?'Montando uma cópia do KUSHMOD…':'Preparando os arquivos do cosmético…');await new Promise(r=>setTimeout(r,30));
  const o=options(),fit={...state.fit,attach:o.attach},model=fitModel(state.model,state.center,fit),bytes=K.jsonBytes(model),assets=K.makeAssets(o,bytes,state.textureBytes);
  if(withJar){
   const result=K.build(o,state.jar,bytes,state.textureBytes);
   download(result.jar,state.jarName.replace(/\.jar$/i,'')+'-'+o.id+'.jar','application/java-archive');
   say('KUSHMOD gerado com '+result.count+' itens no catálogo. Escolha onde salvar a cópia.','success');
  }else{
   const packageItem={...assets.item,model:'MODEL.JSON',texture:'TEXTURE.PNG'};
   const files={'MODEL.JSON':bytes,'TEXTURE.PNG':state.textureBytes,'CATALOG.JSON':K.jsonBytes({schema:1,items:[packageItem]}),'catalog-item.json':K.jsonBytes(packageItem)};
   download(F.zipSync(files,{level:6}),o.id+'-KUSH.zip','application/zip');say('Pacote gerado: MODEL.JSON, CATALOG.JSON e TEXTURE.PNG, com o ajuste no personagem.','success');
  }
 }catch(error){say(error.message,'error');}finally{state.busy=false;updateButtons();}
}
async function saveProject(){
 if(!ready()){say('Crie um modelo ou converta o GLB antes de salvar o projeto.','error');return;}
 try{
  const project={format:'kushstudio',version:2,model:state.model,fit:state.fit,center:state.center,anchor:$('anchor').value,palette:state.palette,options:options()};
  download(F.zipSync({'project.json':K.jsonBytes(project),'texture.png':state.textureBytes},{level:6}),($('cosmeticId').value||'projeto')+'.kushstudio','application/zip');
  $('saveState').textContent='Projeto salvo';say('Projeto salvo com o modelo, a textura e o ajuste. O JAR original não faz parte do projeto.','success');
 }catch(error){say(error.message,'error');}
}
async function openProject(file){
 if(!file||state.busy)return;
 try{
  if(file.size>12*1024*1024)throw Error('Projeto maior que 12 MB.');
  const entries=F.unzipSync(new Uint8Array(await file.arrayBuffer()),{filter:e=>['project.json','texture.png'].includes(e.name)&&e.originalSize<=8*1024*1024});
  if(!entries['project.json']||!entries['texture.png'])throw Error('Projeto incompleto.');
  const p=JSON.parse(new TextDecoder().decode(entries['project.json']));
  if(p.format!=='kushstudio'||p.version!==2||!ANCHORS[p.anchor]||!Array.isArray(p.center)||p.center.length!==3||p.center.some(v=>!Number.isFinite(v)||Math.abs(v)>4096))throw Error('Formato do projeto não suportado.');
  K.checkModel(K.jsonBytes(p.model));fitModel(p.model,p.center,p.fit);K.makeAssets({...p.options,attach:p.fit.attach,flags:525},K.jsonBytes(fitModel(p.model,p.center,p.fit)),entries['texture.png']);
  const texture=await decodeTexture(entries['texture.png']);
  loadRevision++;textureRevision++;disposeOriginal();state.model=p.model;state.fit=p.fit;state.center=p.center;state.palette=!!p.palette;state.selected=-1;state.history=[];
  installTexture(entries['texture.png'],texture);$('anchor').value=p.anchor;$('category').value=p.options.category;$('displayName').value=p.options.displayName;$('cosmeticId').value=p.options.id;$('replace').checked=false;
  rebuild();change();$('saveState').textContent='Projeto aberto';say('Projeto aberto. Se quiser gerar um mod, selecione o KUSHMOD original.','success');
 }catch(error){say('Não foi possível abrir o projeto: '+error.message,'error');}
}
function skinFaces(u,v,w,h,d,size){
 const rects={east:[u,v+d,u+d,v+d+h],north:[u+d,v+d,u+d+w,v+d+h],west:[u+d+w,v+d,u+2*d+w,v+d+h],south:[u+2*d+w,v+d,u+2*d+2*w,v+d+h],up:[u+d,v,u+d+w,v+d],down:[u+d+w,v,u+d+2*w,v+d]};
 return Object.fromEntries(Object.entries(rects).map(([k,uv])=>[k,{texture:'#1',uv:uv.map((p,i)=>p/(i%2?size[1]:size[0])*16)}]));
}
function createPlayer(texture){
 if(player){scene.remove(player);disposeObject(player);}
 const size=[texture.image.width,texture.image.height],modern=size[1]===64;
 const specs=[
 [[-4,24,-4],[4,32,4],0,0,8,8,8],
 [[-4,12,-2],[4,24,2],16,16,8,12,4],
 [[4,12,-2],[8,24,2],40,16,4,12,4],
 [[-8,12,-2],[-4,24,2],modern?32:40,modern?48:16,4,12,4],
 [[0,0,-2],[3.8,12,2],0,16,4,12,4],
 [[-3.8,0,-2],[0,12,2],modern?16:0,modern?48:16,4,12,4]
 ];
 const elements=specs.map(([a,b,u,v,w,h,d])=>({from:[a[0]+8,a[1],a[2]+8],to:[b[0]+8,b[1],b[2]+8],faces:skinFaces(u,v,w,h,d,size)}));
 player=createModel({elements},texture);player.visible=$('showPlayer').checked;scene.add(player);
}
function defaultSkin(){
 const c=document.createElement('canvas');c.width=64;c.height=64;const x=c.getContext('2d');
 x.fillStyle='#c69e80';x.fillRect(0,0,64,64);
 x.fillStyle='#334a58';x.fillRect(16,16,24,16);x.fillStyle='#303440';x.fillRect(0,16,16,16);x.fillRect(16,48,16,16);
 x.fillStyle='#29323b';x.fillRect(40,16,16,16);x.fillRect(32,48,16,16);
 x.fillStyle='#b99377';x.fillRect(44,27,4,5);x.fillRect(36,59,4,5);
 x.fillStyle='#392d2d';x.fillRect(0,0,32,8);x.fillRect(8,8,8,2);x.fillRect(0,8,8,8);x.fillRect(24,8,8,8);
 x.fillStyle='#f4ede0';x.fillRect(9,11,2,1);x.fillRect(13,11,2,1);x.fillStyle='#414450';x.fillRect(10,11,1,1);x.fillRect(13,11,1,1);
 x.fillStyle='#8b6256';x.fillRect(11,14,3,1);
 const texture=new T.CanvasTexture(c);texture.colorSpace=T.SRGBColorSpace;texture.magFilter=texture.minFilter=T.NearestFilter;texture.generateMipmaps=false;return texture;
}
skinTexture=defaultSkin();createPlayer(skinTexture);
$('model').addEventListener('change',e=>importModel(e.target.files[0]));
$('texture').addEventListener('change',async e=>{
 const file=e.target.files[0];if(!file||state.busy)return;
 const stamp=++textureRevision;
 try{
  if(file.size>8*1024*1024)throw Error('Textura maior que 8 MB.');
  const bytes=new Uint8Array(await file.arrayBuffer()),texture=await decodeTexture(bytes);
  if(stamp!==textureRevision){texture.dispose();texture.image?.close?.();return;}
  installTexture(bytes,texture);state.palette=false;state.history=[];rebuild();change();say('Textura aplicada ao modelo.');
 }catch(error){say(error.message,'error');}
});
$('jar').addEventListener('change',async e=>{
 const file=e.target.files[0];if(!file)return;
 try{
  if(file.size>64*1024*1024)throw Error('JAR maior que 64 MB.');
  const bytes=new Uint8Array(await file.arrayBuffer()),base=K.readJar(bytes);
  state.jar=bytes;state.jarName=file.name;$('jarInfo').textContent=file.name+' · '+base.catalog.items.length+' cosméticos · versão '+base.manifest.version;updateButtons();say('KUSHMOD compatível carregado. Escolha um nome e ID para o novo cosmético.','success');
 }catch(error){state.jar=null;$('jarInfo').textContent='JAR não carregado.';say(error.message,'error');updateButtons();}
});
$('skin').addEventListener('change',async e=>{
 const file=e.target.files[0];if(!file)return;const stamp=++skinRevision;
 try{
  if(file.size>1024*1024)throw Error('A skin deve ter no máximo 1 MB.');
  const bytes=new Uint8Array(await file.arrayBuffer()),info=K.checkPNG(bytes);
  if(info.width!==64||![32,64].includes(info.height))throw Error('Use uma skin clássica de 64×64 ou 64×32 pixels.');
  const texture=await decodeTexture(bytes);if(stamp!==skinRevision){texture.dispose();return;}
  const old=skinTexture;skinTexture=texture;createPlayer(texture);old.dispose();old.image?.close?.();say('Skin aplicada ao personagem de referência.');
 }catch(error){say(error.message,'error');}
});
document.querySelectorAll('[data-preset]').forEach(b=>b.addEventListener('click',()=>preset(b.dataset.preset).catch(e=>say(e.message,'error'))));
$('detail').addEventListener('input',()=>{$('detailValue').value=$('detail').value;});
$('convert').addEventListener('click',convert);$('cancelConvert').addEventListener('click',()=>state.controller?.abort());
$('original').addEventListener('change',showComparison);
$('anchor').addEventListener('change',()=>{remember();resetAnchor();change();});
$('resetFit').addEventListener('click',()=>{remember();resetAnchor();change();});
for(const [prefix,key] of [['p','position'],['r','rotation']])for(const [i,axis] of ['x','y','z'].entries())$(prefix+axis).addEventListener('change',e=>{
 const value=Number(e.target.value);if(!Number.isFinite(value)||key==='position'&&Math.abs(value)>2048){say('Valor fora dos limites.','error');syncFitFields();return;}remember();state.fit[key][i]=value;applyFit();change();
});
$('scale').addEventListener('pointerdown',remember);$('scale').addEventListener('input',()=>{state.fit.scale=Number($('scale').value)/100;applyFit();change();});
function setTool(mode){gizmo.setMode(mode);$('move').classList.toggle('active',mode==='translate');$('rotate').classList.toggle('active',mode==='rotate');}
$('move').addEventListener('click',()=>setTool('translate'));$('rotate').addEventListener('click',()=>setTool('rotate'));
document.querySelectorAll('[data-view]').forEach(b=>b.addEventListener('click',()=>{
 const targets={front:[0,22,-65],back:[0,22,65],side:[65,22,0],iso:[46,30,-65]};camera.position.fromArray(targets[b.dataset.view]);orbit.target.set(0,18,0);orbit.update();
}));
$('showPlayer').addEventListener('change',()=>{player.visible=$('showPlayer').checked;});
$('wireframe').addEventListener('change',()=>convertedMesh?.traverse(o=>{if(o.isMesh)o.material.wireframe=$('wireframe').checked;}));
$('cubeList').addEventListener('change',()=>selectedCube(Number($('cubeList').value)));
$('addCube').addEventListener('click',async()=>{
 if(state.busy)return;
 if(!state.model){await preset('hat');state.model.elements=[];}
 if(state.model.elements.length>=2048){say('O limite é de 2.048 cubos.','error');return;}
 remember();const e=state.model.elements[state.selected>=0?state.selected:0];
 const cube=e?{name:'Novo cubo',from:[6,0,6],to:[10,4,10],faces:structuredClone(e.faces)}:simpleModel().elements[0];
 state.model.elements.push(cube);state.selected=state.model.elements.length-1;rebuild();change();
});
$('duplicateCube').addEventListener('click',()=>{
 if(state.busy||state.selected<0)return;if(state.model.elements.length>=2048){say('Limite de 2.048 cubos.','error');return;}
 remember();const e=structuredClone(state.model.elements[state.selected]);e.name=(e.name||'Cubo')+' · cópia';e.from=e.from.map((v,i)=>v+(i===0?1:0));e.to=e.to.map((v,i)=>v+(i===0?1:0));
 const index=state.model.elements.length;state.model.elements.push(e);
 // The duplicate follows the same group as its source.
 function addToGroup(groups){for(const g of groups||[]){if(typeof g==='object'){if(g.children?.includes(state.selected)){g.children.push(index);return true;}if(addToGroup(g.children))return true;}}return false;}
 addToGroup(state.model.groups);state.selected=index;rebuild();change();
});
function removeIndex(groups,index){return (groups||[]).filter(g=>g!==index).map(g=>typeof g==='number'?g>index?g-1:g:{...g,children:removeIndex(g.children,index)});}
$('deleteCube').addEventListener('click',()=>{
 if(state.busy||state.selected<0)return;if(state.model.elements.length===1){say('O modelo precisa de pelo menos um cubo.','error');return;}
 remember();const index=state.selected;state.model.elements.splice(index,1);if(state.model.groups)state.model.groups=removeIndex(state.model.groups,index);state.selected=-1;rebuild();change();
});
for(const id of ['cx','cy','cz','sx','sy','sz'])$(id).addEventListener('change',()=>{
 if(state.busy||state.selected<0)return;
 const center=['cx','cy','cz'].map(id=>Number($(id).value)),size=['sx','sy','sz'].map(id=>Number($(id).value));
 if(center.some(v=>!Number.isFinite(v)||Math.abs(v)>1024)||size.some(v=>!Number.isFinite(v)||v<=0||v>1024)){say('Use dimensões positivas e valores de até 1.024 pixels.','error');selectedCube(state.selected);return;}
 remember();const e=state.model.elements[state.selected];e.from=center.map((v,i)=>v-size[i]/2);e.to=center.map((v,i)=>v+size[i]/2);rebuild();change();
});
$('cubeColor').addEventListener('change',()=>{
 if(state.busy||!state.palette||state.selected<0)return;remember();const i=Number($('cubeColor').value);
 const e=state.model.elements[state.selected];for(const f of Object.values(e.faces))f.uv=[i,0,i+1,1];rebuild();change();
});
$('undo').addEventListener('click',()=>{
 if(state.busy)return;const previous=state.history.pop();if(!previous)return;
 Object.assign(state,previous);rebuild();change();say('Alteração desfeita.');
});
$('exportJar').addEventListener('click',()=>exportFiles(true));$('exportAssets').addEventListener('click',()=>exportFiles(false));$('projectSave').addEventListener('click',saveProject);$('project').addEventListener('change',e=>openProject(e.target.files[0]));
for(const id of ['displayName','cosmeticId','category','replace'])$(id).addEventListener('input',change);
let pointerStart;
renderer.domElement.addEventListener('pointerdown',e=>{pointerStart=[e.clientX,e.clientY];});
renderer.domElement.addEventListener('pointerup',e=>{
 if(!convertedMesh?.visible||gizmo.dragging||!pointerStart||Math.hypot(e.clientX-pointerStart[0],e.clientY-pointerStart[1])>4)return;
 const rect=renderer.domElement.getBoundingClientRect(),p=new T.Vector2((e.clientX-rect.left)/rect.width*2-1,-(e.clientY-rect.top)/rect.height*2+1),ray=new T.Raycaster();ray.setFromCamera(p,camera);
 const hit=ray.intersectObject(convertedMesh,true).find(h=>h.object.userData.element!=null);if(hit)selectedCube(hit.object.userData.element);
});
document.addEventListener('keydown',e=>{
 if(['INPUT','SELECT','TEXTAREA'].includes(document.activeElement?.tagName))return;
 if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='z'){e.preventDefault();$('undo').click();}
 if(e.key.toLowerCase()==='w')setTool('translate');if(e.key.toLowerCase()==='e')setTool('rotate');
});
viewport.addEventListener('dragover',e=>{e.preventDefault();});
viewport.addEventListener('drop',e=>{e.preventDefault();const file=e.dataTransfer.files[0];if(file)importModel(file);});
window.addEventListener('error',e=>say('Ocorreu um erro: '+e.message,'error'));
window.addEventListener('unhandledrejection',e=>say('Ocorreu um erro: '+(e.reason?.message||e.reason),'error'));
updateButtons();
