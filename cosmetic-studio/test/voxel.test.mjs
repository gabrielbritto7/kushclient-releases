import test from 'node:test';
import assert from 'node:assert/strict';
import * as T from 'three';
import K from '../src/core.mjs';
import {inspectGLB,prepareMesh,voxelize} from '../src/voxel.mjs';
import {binaryGLB,glbFixture} from './fixtures.mjs';
test('valid GLB parses and external resource GLB is rejected',()=>{
 const glb=glbFixture();assert.equal(inspectGLB(glb.buffer).asset.version,'2.0');
 const external=binaryGLB({asset:{version:'2.0'},buffers:[{uri:'https://example.invalid/a.bin',byteLength:10}]});
 assert.throws(()=>inspectGLB(external.buffer),/externos/);
 assert.throws(()=>inspectGLB(new ArrayBuffer(33)),/inválido/);
});
test('voxelizes transformed mesh and bakes vertex/material color to an atlas',async()=>{
 const geometry=new T.BoxGeometry(1,2,.25),mesh=new T.Mesh(geometry,new T.MeshStandardMaterial({color:'#ed4565'}));mesh.position.set(3,-5,2);mesh.rotation.z=.1;
 const root=new T.Group();root.add(mesh);const triangles=await prepareMesh(root);
 assert.equal(triangles.length,12);const result=await voxelize(triangles,{resolution:14});
 assert.ok(result.cubes>0&&result.cubes<2048);assert.ok(result.colors>0);assert.equal(result.pixels[3],255);
 assert.ok(result.pixels[0]>result.pixels[1]);K.checkModel(K.jsonBytes(result.model));
});
test('samples embedded texture base color and refuses excessive cube count',async()=>{
 const texture=new T.DataTexture(new Uint8Array([20,100,230,255]),1,1);texture.colorSpace=T.SRGBColorSpace;
 const mesh=new T.Mesh(new T.BoxGeometry(1,1,1),new T.MeshStandardMaterial({map:texture,color:0xffffff})),root=new T.Group();root.add(mesh);
 const tris=await prepareMesh(root),result=await voxelize(tris,{resolution:8});
 assert.ok(result.pixels[2]>result.pixels[0]);assert.ok(Math.abs(result.pixels[1]-100)<10);
 await assert.rejects(voxelize(tris,{resolution:20,maxCubes:5}),/ultrapassa/);
});
test('conversion cancellation is honored',async()=>{
 const mesh=new T.Mesh(new T.BoxGeometry(1,1,1),new T.MeshStandardMaterial()),root=new T.Group();root.add(mesh);
 const c=new AbortController();c.abort();await assert.rejects(voxelize(await prepareMesh(root),{signal:c.signal}),/cancelada/);
});
