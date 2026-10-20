import * as T from 'three';
import {FACE_NAMES} from './model.mjs';
export function inspectGLB(buffer){
 if(!(buffer instanceof ArrayBuffer)||buffer.byteLength<20||buffer.byteLength>64*1024*1024)throw Error('Escolha um GLB de até 64 MB.');
 const view=new DataView(buffer);
 if(view.getUint32(0,true)!==0x46546c67||view.getUint32(4,true)!==2||view.getUint32(8,true)!==buffer.byteLength)throw Error('GLB inválido. É necessário glTF 2.0 binário.');
 let at=12,json;
 while(at+8<=buffer.byteLength){
  const size=view.getUint32(at,true),type=view.getUint32(at+4,true);at+=8;
  if(size%4||at+size>buffer.byteLength)throw Error('GLB truncado.');
  if(type===0x4e4f534a){
   if(json)throw Error('GLB contém mais de um bloco JSON.');
   try{json=JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(new Uint8Array(buffer,at,size)).trim());}catch{throw Error('JSON interno do GLB inválido.');}
  }at+=size;
 }
 if(at!==buffer.byteLength||!json||json.asset?.version!=='2.0')throw Error('Estrutura GLB não suportada.');
 for(const resource of [...json.buffers||[],...json.images||[]]){
  if(resource.uri&&!/^data:/i.test(resource.uri))throw Error('O GLB precisa incluir suas texturas e buffers. Recursos externos não são carregados.');
 }
 if((json.buffers||[]).reduce((n,b)=>n+(b.byteLength||0),0)>256*1024*1024)throw Error('O GLB descompactado ultrapassa 256 MB.');
 for(const a of json.accessors||[])if(!Number.isInteger(a.count)||a.count<0||a.count>2000000)throw Error('O GLB contém uma malha grande demais.');
 return json;
}
export async function imagePixels(texture,renderer){
 if(!texture)return null;
 const img=texture.source?.data||texture.image;
 if(img?.data&&img.width&&img.height&&!texture.isCompressedTexture){
  if(img.width*img.height>16777216)throw Error('Textura maior que 16 megapixels.');
  return {data:img.data,width:img.width,height:img.height};
 }
 if(img&&!texture.isCompressedTexture){
  const w=img.width||img.videoWidth,h=img.height||img.videoHeight;
  if(!w||!h||w*h>16777216)throw Error('Dimensão da textura não suportada.');
  const canvas=document.createElement('canvas');canvas.width=w;canvas.height=h;
  const ctx=canvas.getContext('2d',{willReadFrequently:true});ctx.drawImage(img,0,0);
  return {data:ctx.getImageData(0,0,w,h).data,width:w,height:h};
 }
 if(!renderer)throw Error('Não foi possível ler a textura comprimida do GLB.');
 const w=img?.width||texture.mipmaps?.[0]?.width,h=img?.height||texture.mipmaps?.[0]?.height;
 if(!w||!h||w*h>16777216)throw Error('Dimensão da textura comprimida não suportada.');
 const target=new T.WebGLRenderTarget(w,h,{depthBuffer:false});target.texture.colorSpace=T.SRGBColorSpace;
 const scene=new T.Scene(),camera=new T.OrthographicCamera(-1,1,1,-1,0,2),geometry=new T.PlaneGeometry(2,2),material=new T.MeshBasicMaterial({map:texture,transparent:true});
 const plane=new T.Mesh(geometry,material);plane.position.z=-1;scene.add(plane);
 const old=renderer.getRenderTarget(),oldClear=renderer.getClearColor(new T.Color()),alpha=renderer.getClearAlpha();renderer.setRenderTarget(target);renderer.setClearColor(0,0);renderer.clear();renderer.render(scene,camera);
 const raw=new Uint8Array(w*h*4);renderer.readRenderTargetPixels(target,0,0,w,h,raw);renderer.setRenderTarget(old);renderer.setClearColor(oldClear,alpha);
 target.dispose();geometry.dispose();material.dispose();
 const data=new Uint8Array(raw.length);for(let y=0;y<h;y++)data.set(raw.subarray((h-1-y)*w*4,(h-y)*w*4),y*w*4);
 return {data,width:w,height:h};
}
export async function prepareMesh(root,renderer){
 root.updateMatrixWorld(true);
 const meshes=[],cache=new Map();let total=0;
 root.traverse(o=>{
  if(!o.isMesh||!o.visible)return;
  if(o.isInstancedMesh)throw Error('GLB com instâncias: converta as instâncias para malhas comuns antes de importar.');
  const position=o.geometry.attributes.position;
  if(!position)return;
  const count=(o.geometry.index?.count||position.count)/3;
  if(!Number.isInteger(count))throw Error('O GLB precisa conter triângulos.');
  total+=count;meshes.push(o);
 });
 if(!total)throw Error('O GLB não contém uma malha de triângulos.');
 if(total>200000)throw Error('O GLB ultrapassa 200 mil triângulos. Simplifique a malha antes de importar.');
 for(const mesh of meshes){
  for(const mat of Array.isArray(mesh.material)?mesh.material:[mesh.material]){
   if(mat?.map&&!cache.has(mat.map))cache.set(mat.map,await imagePixels(mat.map,renderer));
  }
 }
 const triangles=[];
 const vertex=new T.Vector3(),uv=new T.Vector2();
 for(const mesh of meshes){
  const geometry=mesh.geometry,position=geometry.attributes.position,index=geometry.index,mats=Array.isArray(mesh.material)?mesh.material:[mesh.material];
  const limit=index?.count||position.count;
  for(let i=0;i<limit;i+=3){
   let materialIndex=0;
   if(Array.isArray(mesh.material)){const g=geometry.groups.find(g=>i>=g.start&&i<g.start+g.count);materialIndex=g?.materialIndex||0;}
   const material=mats[materialIndex];if(!material||material.visible===false)continue;
   const ids=[0,1,2].map(j=>index?index.getX(i+j):i+j);
   const vertices=ids.map(id=>{if(mesh.getVertexPosition)mesh.getVertexPosition(id,vertex);else vertex.fromBufferAttribute(position,id);return vertex.clone().applyMatrix4(mesh.matrixWorld);});
   const uvAttr=geometry.attributes[(material.map?.channel||0)?'uv'+material.map.channel:'uv'];
   const uvs=ids.map(id=>uvAttr?uv.fromBufferAttribute(uvAttr,id).clone():new T.Vector2());
   const colorAttr=material.vertexColors?geometry.attributes.color:null;
   const colors=ids.map(id=>colorAttr?new T.Color().fromBufferAttribute(colorAttr,id):new T.Color(1,1,1));
   triangles.push({vertices,uvs,colors,material,pixels:cache.get(material.map)});
  }
 }
 return triangles;
}
function pixelColor(item,bary){
 const mat=item.material,col=(mat.color||new T.Color(1,1,1)).clone(),vertex=new T.Color(0,0,0);
 item.colors.forEach((c,i)=>{vertex.r+=c.r*bary.getComponent(i);vertex.g+=c.g*bary.getComponent(i);vertex.b+=c.b*bary.getComponent(i);});col.multiply(vertex);
 let alpha=mat.opacity??1;
 if(mat.map&&item.pixels){
  const uv=new T.Vector2();
  item.uvs.forEach((v,i)=>uv.addScaledVector(v,bary.getComponent(i)));mat.map.updateMatrix();mat.map.transformUv(uv);
  const p=item.pixels,x=Math.min(p.width-1,Math.max(0,Math.floor(uv.x*p.width))),y=Math.min(p.height-1,Math.max(0,Math.floor(uv.y*p.height))),offset=(y*p.width+x)*4;
  const sample=new T.Color(p.data[offset]/255,p.data[offset+1]/255,p.data[offset+2]/255);
  if(mat.map.colorSpace===T.SRGBColorSpace)sample.convertSRGBToLinear();
  col.multiply(sample);alpha*=p.data[offset+3]/255;
 }
 if(alpha<Math.max(.2,mat.alphaTest||0))return null;
 col.convertLinearToSRGB();
 return [col.r,col.g,col.b].map(v=>Math.min(255,Math.max(0,Math.round(v*31)/31*255|0)));
}
export async function voxelize(triangles,{resolution=20,maxCubes=2048,onProgress=()=>{},signal}={}){
 if(!Number.isInteger(resolution)||resolution<6||resolution>48)throw Error('Detalhe deve ficar entre 6 e 48.');
 const bounds=new T.Box3();for(const t of triangles)for(const p of t.vertices)bounds.expandByPoint(p);
 const size=bounds.getSize(new T.Vector3()),longest=Math.max(size.x,size.y,size.z);
 if(!Number.isFinite(longest)||longest<=1e-10)throw Error('A malha está vazia ou tem tamanho zero.');
 const step=longest/resolution,center=bounds.getCenter(new T.Vector3()),dims=size.toArray().map(v=>Math.max(1,Math.ceil(v/step))),min=bounds.min.clone();
 const cells=new Map(),triangle=new T.Triangle(),box=new T.Box3(),p=new T.Vector3(),nearest=new T.Vector3(),bary=new T.Vector3();
 let work=0,lastYield=performance.now();
 for(let ti=0;ti<triangles.length;ti++){
  if(signal?.aborted)throw new DOMException('Conversão cancelada.','AbortError');
  const item=triangles[ti];triangle.set(...item.vertices);
  const tb=new T.Box3().setFromPoints(item.vertices);
  const lo=tb.min.clone().sub(min).divideScalar(step).floor().toArray().map((v,a)=>Math.max(0,Math.min(dims[a]-1,v)));
  const hi=tb.max.clone().sub(min).divideScalar(step).floor().toArray().map((v,a)=>Math.max(0,Math.min(dims[a]-1,v)));
  for(let x=lo[0];x<=hi[0];x++)for(let y=lo[1];y<=hi[1];y++)for(let z=lo[2];z<=hi[2];z++){
   if(++work>15000000)throw Error('Conversão complexa demais. Reduza o detalhe ou simplifique a malha.');
   box.min.set(x,y,z).multiplyScalar(step).add(min);box.max.copy(box.min).addScalar(step);
   if(!box.intersectsTriangle(triangle))continue;
   box.getCenter(p);triangle.closestPointToPoint(p,nearest);const distance=nearest.distanceToSquared(p),key=x+','+y+','+z;
   if(cells.has(key)&&cells.get(key).distance<=distance)continue;
   triangle.getBarycoord(nearest,bary);if(!Number.isFinite(bary.x))continue;
   const color=pixelColor(item,bary);if(color)cells.set(key,{x,y,z,color,distance});
   if(cells.size>maxCubes)throw Error('O resultado ultrapassa '+maxCubes+' cubos. Diminua o detalhe e converta novamente.');
  }
  if(performance.now()-lastYield>18){
   onProgress(Math.round(ti/triangles.length*95));await new Promise(resolve=>setTimeout(resolve,0));lastYield=performance.now();
  }
 }
 if(!cells.size)throw Error('Não foi encontrada nenhuma superfície visível para converter.');
 const palette=[],paletteIndex=new Map();
 for(const cell of cells.values()){const k=cell.color.join(',');if(!paletteIndex.has(k)){paletteIndex.set(k,palette.length);palette.push([...cell.color,255]);}cell.colorIndex=paletteIndex.get(k);}
 const atlasSize=Math.max(16,2**Math.ceil(Math.log2(Math.ceil(Math.sqrt(palette.length)))));
 const pixels=new Uint8ClampedArray(atlasSize*atlasSize*4);palette.forEach((color,i)=>pixels.set(color,i*4));
 const offsets={north:[0,0,-1],south:[0,0,1],east:[1,0,0],west:[-1,0,0],up:[0,1,0],down:[0,-1,0]};
 const targetSize=12,scale=targetSize/longest,unit=step*scale,elements=[];
 for(const cell of cells.values()){
  const from=new T.Vector3(cell.x,cell.y,cell.z).multiplyScalar(step).add(min).sub(center).multiplyScalar(scale);
  const px=cell.colorIndex%atlasSize,py=Math.floor(cell.colorIndex/atlasSize);
  // A single atlas texel supplies each voxel's baked base color.
  const uv=[(px+.15)/atlasSize*16,(py+.15)/atlasSize*16,(px+.85)/atlasSize*16,(py+.85)/atlasSize*16];
  const faces={};for(const side of FACE_NAMES){const d=offsets[side];if(!cells.has([cell.x+d[0],cell.y+d[1],cell.z+d[2]].join(',')))faces[side]={uv:uv.slice(),texture:'#1'};}
  if(!Object.keys(faces).length)continue;
  elements.push({from:[from.x+8,from.y,from.z+8],to:[from.x+8+unit,from.y+unit,from.z+8+unit],faces});
 }
 onProgress(100);
 return {model:{credit:'GLB convertido no KUSH Cosmetic Studio',texture_size:[atlasSize,atlasSize],textures:{'1':'texture'},elements},pixels,width:atlasSize,height:atlasSize,cubes:elements.length,colors:palette.length};
}
