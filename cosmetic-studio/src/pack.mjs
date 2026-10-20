import * as T from 'three';

// Every edit changes the source geometry; hidden objects are never used as deletion.
export function packParts(root){
 const parts=[];
 root?.traverse(object=>{
  if(!object.isMesh||!object.geometry?.attributes.position)return;
  const names=[];let parent=object;
  while(parent&&parent!==root){if(parent.name)names.unshift(parent.name);parent=parent.parent;}
  parts.push({id:object.uuid,object,name:names.join(' / ')||'Elemento '+(parts.length+1),triangles:Math.floor((object.geometry.index?.count||object.geometry.attributes.position.count)/3)});
 });
 return parts;
}

export function removeModelElements(model,indices,{keepOnly=false}={}){
 const chosen=new Set(indices.filter(i=>Number.isInteger(i)&&i>=0&&i<model.elements.length));
 if(!chosen.size)throw Error('Selecione pelo menos um elemento.');
 const kept=model.elements.map((_,i)=>i).filter(i=>keepOnly?chosen.has(i):!chosen.has(i));
 if(!kept.length)throw Error('Mantenha pelo menos um elemento no modelo.');
 const mapping=new Map(kept.map((old,index)=>[old,index])),result=structuredClone(model);
 result.elements=kept.map(i=>result.elements[i]);
 const remap=nodes=>nodes.flatMap(node=>{
  if(typeof node==='number')return mapping.has(node)?[mapping.get(node)]:[];
  const children=remap(node.children||[]);return children.length?[{...node,children}]:[];
 });
 if(result.groups)result.groups=remap(result.groups);
 return result;
}

export function modelGroups(model){
 const groups=[];
 function visit(node,path){
  if(typeof node==='number')return [node];
  const label=[...path,node.name||'Grupo '+(groups.length+1)],entry={name:label.join(' / '),indices:[]};groups.push(entry);
  entry.indices=[...new Set((node.children||[]).flatMap(child=>visit(child,label)))];return entry.indices;
 }
 for(const node of model?.groups||[])if(typeof node!=='number')visit(node,[]);
 return groups.filter(group=>group.indices.length);
}

export function removePackParts(root,ids,{keepOnly=false}={}){
 const parts=packParts(root),selected=new Set(ids);
 for(const part of parts){let parent=part.object.parent;while(parent&&parent!==root){if(selected.has(parent.uuid)){selected.add(part.id);break;}parent=parent.parent;}}
 if(!parts.some(p=>selected.has(p.id)))throw Error('Selecione pelo menos uma peça do pack.');
 const remove=parts.filter(p=>keepOnly?!selected.has(p.id):selected.has(p.id));
 if(remove.length===parts.length)throw Error('Mantenha pelo menos uma peça do pack.');
 if(!remove.length)throw Error('A seleção já contém todas as peças.');
 const entries=remove.map(({object})=>({object,parent:object.parent,index:object.parent.children.indexOf(object)}));
 entries.forEach(({object,parent})=>parent.remove(object));
 return {type:'remove',entries};
}

export function localPackBounds(root){
 root.updateWorldMatrix(true,true);
 const inverse=root.matrixWorld.clone().invert(),bounds=new T.Box3(),position=new T.Vector3();
 for(const {object} of packParts(root)){
  const matrix=new T.Matrix4().multiplyMatrices(inverse,object.matrixWorld),attribute=object.geometry.attributes.position;
  for(let i=0;i<attribute.count;i++){
   object.getVertexPosition(i,position);bounds.expandByPoint(position.applyMatrix4(matrix));
  }
 }
 return bounds;
}

export function normalizePack(root){
 const bounds=localPackBounds(root),size=bounds.getSize(new T.Vector3()),center=bounds.getCenter(new T.Vector3()),longest=Math.max(size.x,size.y,size.z);
 if(bounds.isEmpty()||!Number.isFinite(longest)||longest<=1e-10)throw Error('O pack não contém uma peça com tamanho válido.');
 const factor=12/longest;
 const transforms=root.children.map(object=>({object,position:object.position.clone(),scale:object.scale.clone()}));
 for(const {object} of transforms){object.position.sub(center).multiplyScalar(factor);object.scale.multiplyScalar(factor);object.updateMatrix();}
 root.updateWorldMatrix(true,true);
 return {transforms,dimensions:size.multiplyScalar(factor).toArray()};
}

export function restorePackEdit(edit){
 for(const saved of edit.normalization?.transforms||[]){saved.object.position.copy(saved.position);saved.object.scale.copy(saved.scale);saved.object.updateMatrix();}
 if(edit.type==='split')for(const row of edit.entries)row.parts.forEach(part=>row.parent.remove(part));
 for(const row of [...edit.entries].sort((a,b)=>a.index-b.index)){
  row.parent.add(row.object);
  const current=row.parent.children.indexOf(row.object);row.parent.children.splice(current,1);row.parent.children.splice(row.index,0,row.object);
 }
}

async function connectedTriangles(mesh,{signal,onProgress=()=>{}}={}){
 const geometry=mesh.geometry,attribute=geometry.attributes.position,index=geometry.index,count=Math.floor((index?.count||attribute.count)/3);
 const parent=new Int32Array(count);for(let i=0;i<count;i++)parent[i]=i;
 const find=n=>{while(parent[n]!==n){parent[n]=parent[parent[n]];n=parent[n];}return n;};
 const join=(a,b)=>{a=find(a);b=find(b);if(a!==b)parent[b]=a;};
 geometry.computeBoundingBox();const size=geometry.boundingBox.getSize(new T.Vector3());
 const tolerance=Math.max(1e-8,Math.max(size.x,size.y,size.z)*1e-7),weld=new Map();
 for(let triangle=0;triangle<count;triangle++){
  for(let corner=0;corner<3;corner++){
   const vertex=index?index.getX(triangle*3+corner):triangle*3+corner;
   const key=[attribute.getX(vertex),attribute.getY(vertex),attribute.getZ(vertex)].map(v=>Math.round(v/tolerance)).join(',');
   const other=weld.get(key);if(other!==undefined)join(triangle,other);else weld.set(key,triangle);
  }
  if(triangle%4096===0){if(signal?.aborted)throw new DOMException('Separação cancelada.','AbortError');onProgress(triangle/count);await new Promise(resolve=>setTimeout(resolve,0));}
 }
 const groups=new Map();for(let triangle=0;triangle<count;triangle++){const key=find(triangle);if(!groups.has(key))groups.set(key,[]);groups.get(key).push(triangle);}
 return [...groups.values()];
}

function extractPart(mesh,triangles,number){
 const source=mesh.geometry,index=source.index,geometry=new T.BufferGeometry(),sourceIndices=triangles.flatMap(triangle=>[0,1,2].map(corner=>index?index.getX(triangle*3+corner):triangle*3+corner));
 for(const [name,attribute] of Object.entries(source.attributes)){
  if(['skinIndex','skinWeight','normal','tangent'].includes(name))continue;
  const values=new Float32Array(sourceIndices.length*attribute.itemSize);
  if(name==='position'){
   const point=new T.Vector3();sourceIndices.forEach((id,i)=>{mesh.getVertexPosition(id,point);point.toArray(values,i*3);});
  }else sourceIndices.forEach((id,i)=>{for(let c=0;c<attribute.itemSize;c++)values[i*attribute.itemSize+c]=attribute.getComponent(id,c);});
  geometry.setAttribute(name,new T.BufferAttribute(values,attribute.itemSize));
 }
 let last=-1,start=0;
 triangles.forEach((triangle,i)=>{
  const material=source.groups.find(g=>triangle*3>=g.start&&triangle*3<g.start+g.count)?.materialIndex||0;
  if(material!==last){if(last>=0)geometry.addGroup(start*3,(i-start)*3,last);last=material;start=i;}
 });
 if(last>=0)geometry.addGroup(start*3,(triangles.length-start)*3,last);
 geometry.computeVertexNormals();
 const part=new T.Mesh(geometry,mesh.material);part.name=(mesh.name||'Elemento')+' · peça '+number;
 part.position.copy(mesh.position);part.quaternion.copy(mesh.quaternion);part.scale.copy(mesh.scale);part.visible=mesh.visible;part.updateMatrix();
 return part;
}

export async function splitPackParts(root,ids,{maxParts=2048,signal,onProgress=()=>{}}={}){
 const selected=new Set(ids),all=packParts(root),targets=all.filter(p=>!selected.size||selected.has(p.id)),entries=[];
 let total=all.length;
 try{
  for(let i=0;i<targets.length;i++){
   const mesh=targets[i].object;if(mesh.children.length)continue;
   const groups=await connectedTriangles(mesh,{signal,onProgress:p=>onProgress(Math.round((i+p)/targets.length*100))});
   if(groups.length<=1)continue;
   total+=groups.length-1;if(total>maxParts)throw Error('O pack ultrapassa '+maxParts+' peças separadas. Selecione menos objetos para separar.');
   entries.push({object:mesh,parent:mesh.parent,index:mesh.parent.children.indexOf(mesh),parts:groups.map((triangles,index)=>extractPart(mesh,triangles,index+1))});
  }
  if(signal?.aborted)throw new DOMException('Separação cancelada.','AbortError');
 }catch(error){for(const row of entries)for(const part of row.parts)part.geometry.dispose();throw error;}
 for(const row of entries){row.parent.remove(row.object);row.parts.forEach(part=>row.parent.add(part));}
 onProgress(100);return {type:'split',entries};
}
