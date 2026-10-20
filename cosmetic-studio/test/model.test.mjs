import test from 'node:test';
import assert from 'node:assert/strict';
import * as T from 'three';
import K from '../src/core.mjs';
import {simpleModel,fitModel,modelVertices} from '../src/model.mjs';
test('exported hierarchy matches preview with nested group and cube rotations',()=>{
 const model=simpleModel();model.elements[0].rotation={origin:[8,1,8],angles:[12,23,-9]};
 model.groups=[{origin:[8,2,8],rotation:[5,-20,13],children:[0,{origin:[6,1,7],rotation:[31,8,-4],children:[1]}]}];
 const center=[2,3,-1],fit={position:[-6.5,18,-3],rotation:[-17,42,23],scale:1.7,attach:'LEFT_ARM'};
 const matrix=new T.Matrix4().compose(new T.Vector3(...fit.position),new T.Quaternion().setFromEuler(new T.Euler(...fit.rotation.map(T.MathUtils.degToRad),'ZYX')),new T.Vector3().setScalar(fit.scale));
 const expected=modelVertices(model).map(v=>v.sub(new T.Vector3(...center)).applyMatrix4(matrix));
 const exported=fitModel(model,center,fit);K.checkModel(K.jsonBytes(exported));
 const actual=modelVertices(exported);assert.equal(actual.length,expected.length);
 actual.forEach((v,i)=>assert.ok(v.distanceTo(expected[i])<1e-4,'vertex '+i+' differs'));
 assert.equal(exported.kushStudio.flags,525);assert.equal(exported.kushCosmetica,undefined);
});
test('fitted reimport and identity export preserve all vertices',()=>{
 const fit={position:[0,33,0],rotation:[15,8,3],scale:.8,attach:'HEAD'},first=fitModel(simpleModel(),[0,3,0],fit),second=fitModel(first,[0,0,0],{position:[0,0,0],rotation:[0,0,0],scale:1,attach:'HEAD'});
 const a=modelVertices(first),b=modelVertices(second);a.forEach((v,i)=>assert.ok(v.distanceTo(b[i])<1e-5));
});
