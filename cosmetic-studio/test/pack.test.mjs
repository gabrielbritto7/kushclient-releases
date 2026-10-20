import test from 'node:test';
import assert from 'node:assert/strict';
import * as T from 'three';
import {simpleModel,modelVertices} from '../src/model.mjs';
import {packParts,modelGroups,removeModelElements,removePackParts,normalizePack,restorePackEdit,splitPackParts,localPackBounds} from '../src/pack.mjs';

function sourcePack(){const root=new T.Group(),group=new T.Group();group.name='Pack';root.add(group);for(let i=0;i<3;i++){const mesh=new T.Mesh(new T.BoxGeometry(1,2,1),new T.MeshStandardMaterial({color:0xffffff}));mesh.name=['Chapéu','Mochila','Asas'][i];mesh.position.x=i*3;group.add(mesh);}return root;}
function coordinates(root){root.updateWorldMatrix(true,true);return packParts(root).map(({object})=>{const p=new T.Vector3(),result=[];for(let i=0;i<object.geometry.attributes.position.count;i++){object.getVertexPosition(i,p);result.push(p.clone().applyMatrix4(object.matrixWorld).toArray());}return result;});}
test('bulk JSON deletion remaps nested groups, removes empty groups and preserves original input',()=>{
 const model=simpleModel();model.groups=[{name:'Chapéu',children:[0]},{name:'Outro cosmético',children:[{name:'Interior',children:[1,2]}]}];
 const original=structuredClone(model),edited=removeModelElements(model,[0,1]);
 assert.equal(edited.elements.length,1);assert.equal(edited.groups.length,1);assert.equal(edited.groups[0].name,'Outro cosmético');assert.deepEqual(edited.groups[0].children[0].children,[0]);assert.deepEqual(model,original);
 const only=removeModelElements(model,[0],{keepOnly:true});assert.deepEqual(only.elements,[model.elements[0]]);assert.deepEqual(only.groups[0].children,[0]);
 assert.deepEqual(modelGroups(model).map(group=>group.indices),[[0],[1,2],[1,2]]);
 assert.equal(modelVertices(edited).length,24);
});
test('rejects deleting the entire JSON model and empty/invalid selections',()=>{
 const model=simpleModel();assert.throws(()=>removeModelElements(model,[0,1,2]),/pelo menos um/);assert.throws(()=>removeModelElements(model,[-1,99]),/Selecione/);
});
test('deletes GLB objects and undo restores geometry, hierarchy, order and placement',()=>{
 const root=sourcePack();root.position.set(3,20,-5);root.rotation.y=.45;root.scale.setScalar(.8);
 const before=coordinates(root),parts=packParts(root),edit=removePackParts(root,[parts[1].id],{keepOnly:true});
 assert.equal(packParts(root).length,1);assert.ok(packParts(root)[0].name.includes('Mochila'));
 edit.normalization=normalizePack(root);const bounds=localPackBounds(root);
 assert.ok(bounds.getCenter(new T.Vector3()).length()<1e-8);assert.ok(Math.abs(Math.max(...bounds.getSize(new T.Vector3()).toArray())-12)<1e-8);
 restorePackEdit(edit);assert.deepEqual(coordinates(root),before);assert.deepEqual(packParts(root).map(p=>p.id),parts.map(p=>p.id));
});
test('GLB multi-delete removes only selected objects and prevents an empty pack',()=>{
 const root=sourcePack(),parts=packParts(root),edit=removePackParts(root,[parts[0].id,parts[2].id]);
 assert.equal(packParts(root).length,1);assert.throws(()=>removePackParts(root,[parts[1].id]),/pelo menos uma/);restorePackEdit(edit);assert.equal(packParts(root).length,3);
});
function mergedMesh(){
 const a=new T.BoxGeometry(1,1,1).toNonIndexed().translate(-2,0,0),b=new T.BoxGeometry(1,1,1).toNonIndexed().translate(2,0,0),geometry=new T.BufferGeometry();
 for(const name of ['position','uv']){const first=a.attributes[name],second=b.attributes[name],array=new Float32Array(first.array.length+second.array.length);array.set(first.array);array.set(second.array,first.array.length);geometry.setAttribute(name,new T.BufferAttribute(array,first.itemSize));}
 geometry.addGroup(0,a.attributes.position.count,0);geometry.addGroup(a.attributes.position.count,b.attributes.position.count,1);geometry.computeVertexNormals();
 const mesh=new T.Mesh(geometry,[new T.MeshStandardMaterial({color:'red'}),new T.MeshStandardMaterial({color:'blue'})]);mesh.name='Pack unido';return mesh;
}
test('splits a merged pack across UV seams without changing material, UV or placement; undo restores it',async()=>{
 const root=new T.Group(),mesh=mergedMesh();mesh.rotation.z=.35;mesh.position.set(2,3,0);root.add(mesh);const before=coordinates(root);
 const edit=await splitPackParts(root,[]);assert.equal(edit.entries.length,1);const parts=packParts(root);assert.equal(parts.length,2);
 assert.equal(parts[0].object.material,mesh.material);assert.equal(parts[0].object.geometry.attributes.uv.count,36);assert.equal(parts[1].object.geometry.groups[0].materialIndex,1);
 assert.deepEqual(coordinates(root).flat(),before.flat());restorePackEdit(edit);assert.equal(packParts(root)[0].object,mesh);assert.deepEqual(coordinates(root),before);
});
test('a box with UV seams remains one connected piece',async()=>{
 const root=new T.Group();root.add(new T.Mesh(new T.BoxGeometry(1,1,1),new T.MeshBasicMaterial()));const edit=await splitPackParts(root,[]);assert.equal(edit.entries.length,0);assert.equal(packParts(root).length,1);
});
test('cancelled or excessive splits leave the source untouched',async()=>{
 const root=new T.Group(),mesh=mergedMesh();root.add(mesh);const controller=new AbortController();controller.abort();
 await assert.rejects(splitPackParts(root,[],{signal:controller.signal}),/cancelada/);assert.equal(packParts(root)[0].object,mesh);
 await assert.rejects(splitPackParts(root,[],{maxParts:1}),/ultrapassa/);assert.equal(packParts(root)[0].object,mesh);
});
