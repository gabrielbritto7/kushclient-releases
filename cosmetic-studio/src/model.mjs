import * as T from 'three';
export const FACE_NAMES=['north','south','east','west','up','down'];
export const ANCHORS={
 head:{label:'Cabeça',attach:'HEAD',category:'hats',position:[0,33,0]},
 face:{label:'Rosto',attach:'HEAD',category:'face',position:[0,28,-4.5]},
 back:{label:'Costas',attach:'BODY',category:'back',position:[0,19,3.5]},
 chest:{label:'Peito',attach:'BODY',category:'back',position:[0,19,-3]},
 left_arm:{label:'Braço esquerdo',attach:'LEFT_ARM',category:'arm',position:[-6.5,18,0]},
 right_arm:{label:'Braço direito',attach:'RIGHT_ARM',category:'arm',position:[6.5,18,0]},
 left_leg:{label:'Perna esquerda',attach:'LEFT_LEG',category:'boots',position:[-1.9,5,0]},
 right_leg:{label:'Perna direita',attach:'RIGHT_LEG',category:'boots',position:[1.9,5,0]}
};
const radians=v=>v.map(T.MathUtils.degToRad);
const point=p=>new T.Vector3(p[0]-8,p[1],p[2]-8);
const jsonPoint=p=>[p.x+8,p.y,p.z+8].map(v=>Math.round(v*1e6)/1e6);
function angles(r){if(!r)return[0,0,0];if(r.angles)return r.angles;const v=[0,0,0];v['xyz'.indexOf(r.axis)]=r.angle;return v;}
export function faceVertices(e,side){
 const [x1,y1,z1]=e.from,[x2,y2,z2]=e.to;
 return {
 north:[[x2,y2,z1],[x2,y1,z1],[x1,y1,z1],[x1,y2,z1]],
 south:[[x1,y2,z2],[x1,y1,z2],[x2,y1,z2],[x2,y2,z2]],
 east:[[x2,y2,z2],[x2,y1,z2],[x2,y1,z1],[x2,y2,z1]],
 west:[[x1,y2,z1],[x1,y1,z1],[x1,y1,z2],[x1,y2,z2]],
 up:[[x1,y2,z1],[x1,y2,z2],[x2,y2,z2],[x2,y2,z1]],
 down:[[x1,y1,z2],[x1,y1,z1],[x2,y1,z1],[x2,y1,z2]]
 }[side];
}
export function createModel(model,texture){
 const root=new T.Group(),claimed=new Set();
 const material=new T.MeshStandardMaterial({map:texture,color:0xffffff,roughness:.85,metalness:0,alphaTest:.1,side:T.DoubleSide});
 const cube=(index,parent,parentOrigin)=>{
  const e=model.elements[index],positions=[],uvs=[],indices=[];
  for(const [side,f] of Object.entries(e.faces)){
   const corners=faceVertices(e,side),u=f.uv||[0,0,16,16],tex=[[u[0]/16,1-u[1]/16],[u[0]/16,1-u[3]/16],[u[2]/16,1-u[3]/16],[u[2]/16,1-u[1]/16]];
   const start=positions.length/3;
   corners.forEach((p,i)=>{const v=point(p);positions.push(v.x,v.y,v.z);uvs.push(...tex[(i+(f.rotation||0)/90)%4]);});
   indices.push(start,start+1,start+2,start,start+2,start+3);
  }
  const geometry=new T.BufferGeometry();geometry.setAttribute('position',new T.Float32BufferAttribute(positions,3));geometry.setAttribute('uv',new T.Float32BufferAttribute(uvs,2));geometry.setIndex(indices);geometry.computeVertexNormals();
  const mesh=new T.Mesh(geometry,material);mesh.userData.element=index;
  if(e.rotation){
   const origin=point(e.rotation.origin),g=new T.Group();g.position.copy(origin).sub(parentOrigin);g.rotation.set(...radians(angles(e.rotation)),'ZYX');mesh.position.copy(origin).negate();g.add(mesh);parent.add(g);
  }else{mesh.position.copy(parentOrigin).negate();parent.add(mesh);}
 };
 const visit=(node,parent,parentOrigin)=>{
  if(typeof node==='number'){claimed.add(node);cube(node,parent,parentOrigin);return;}
  const origin=point(node.origin||[8,0,8]),g=new T.Group();
  g.position.copy(origin).sub(parentOrigin);g.rotation.set(...radians(node.rotation||[0,0,0]),'ZYX');parent.add(g);
  for(const child of node.children||[])visit(child,g,origin);
 };
 for(const g of model.groups||[])visit(g,root,new T.Vector3());
 model.elements.forEach((e,i)=>{if(!claimed.has(i))cube(i,root,new T.Vector3());});
 root.updateMatrixWorld(true);return root;
}
export function disposeObject(object,{textures=false}={}){
 const materials=new Set(),maps=new Set();
 object.traverse(o=>{o.geometry?.dispose();for(const m of (Array.isArray(o.material)?o.material:o.material?[o.material]:[])){materials.add(m);if(textures&&m.map)maps.add(m.map);}});
 for(const m of materials)m.dispose();for(const t of maps)t.dispose();
}
export function fitModel(model,center,fit){
 const output=structuredClone(model),p=new T.Vector3(...fit.position),c=new T.Vector3(...center),s=fit.scale;
 if(!Number.isFinite(s)||s<=0||s>100||fit.position.some(v=>!Number.isFinite(v)||Math.abs(v)>2048)||fit.rotation.some(v=>!Number.isFinite(v)))throw Error('Ajuste fora dos limites.');
 const transform=v=>jsonPoint(point(v).sub(c).multiplyScalar(s).add(p));
 for(const e of output.elements){e.from=transform(e.from);e.to=transform(e.to);if(e.rotation)e.rotation.origin=transform(e.rotation.origin);}
 const claimed=new Set();
 function group(g){
  if(typeof g==='number'){claimed.add(g);return g;}
  return {...g,origin:transform(g.origin||[8,0,8]),children:(g.children||[]).map(group)};
 }
 const children=(output.groups||[]).map(group);output.elements.forEach((_,i)=>{if(!claimed.has(i))children.push(i);});
 output.groups=[{name:'KUSH Studio · ajuste no personagem',origin:jsonPoint(p),rotation:fit.rotation.slice(),children}];
 delete output.kushCosmetica;delete output.display;
 output.kushStudio={version:2,coordinateSpace:'player',attach:fit.attach,flags:525};
 return output;
}
export function makeFit(mesh,anchor='head'){
 const box=new T.Box3().setFromObject(mesh),center=box.getCenter(new T.Vector3()),size=box.getSize(new T.Vector3());
 if(box.isEmpty()||!Number.isFinite(size.length()))throw Error('O modelo não contém geometria visível.');
 return {center:center.toArray(),fit:{position:ANCHORS[anchor].position.slice(),rotation:[0,0,0],scale:1,attach:ANCHORS[anchor].attach}};
}
export function modelVertices(model){
 const object=createModel(model,null),out=[];object.updateMatrixWorld(true);
 object.traverse(o=>{if(o.isMesh){const a=o.geometry.attributes.position;for(let i=0;i<a.count;i++)out.push(new T.Vector3().fromBufferAttribute(a,i).applyMatrix4(o.matrixWorld));}});
 disposeObject(object);return out;
}
export function simpleModel(kind='hat'){
 const face=(i)=>Object.fromEntries(FACE_NAMES.map(side=>[side,{uv:[i,0,i+1,1],texture:'#1'}]));
 const cube=(name,from,to,color)=>({name,from,to,faces:face(color)});
 let elements;
 if(kind==='backpack')elements=[cube('Mochila',[4,0,5],[12,10,9],3),cube('Bolso',[5,1,9],[11,5,11],4),cube('Fecho',[7.5,6,9],[8.5,8,10],2)];
 else if(kind==='wings')elements=[cube('Asa esquerda',[-4,0,7],[5,2,9],4),cube('Ponta esquerda',[-6,2,7],[1,6,9],4),cube('Asa direita',[11,0,7],[20,2,9],4),cube('Ponta direita',[15,2,7],[22,6,9],4),cube('Centro',[5,-1,7],[11,4,10],2)];
 else elements=[cube('Aba',[2,0,2],[14,1,14],1),cube('Topo',[4,1,4],[12,6,12],3),cube('Faixa',[3.9,1,3.9],[12.1,2,12.1],4)];
 return {credit:'Criado no KUSH Cosmetic Studio',texture_size:[16,16],textures:{'1':'texture'},elements};
}
