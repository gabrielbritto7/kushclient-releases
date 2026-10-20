import test from 'node:test';
import assert from 'node:assert/strict';
import * as F from 'fflate';
import K from '../src/core.mjs';
import {simpleModel,fitModel} from '../src/model.mjs';
import {jar,png,options} from './fixtures.mjs';
test('JAR preserves every original byte entry and adds fitted assets to catalog',()=>{
 const input=jar(),model=fitModel(simpleModel(),[0,3,0],{position:[0,19,3.5],rotation:[10,20,-30],scale:.8,attach:'BODY'}),modelBytes=K.jsonBytes(model),texture=png();
 const result=K.build(options,input,modelBytes,texture),before=F.unzipSync(input),after=F.unzipSync(result.jar),cat=JSON.parse(new TextDecoder().decode(after['assets/kush/cosmetics/catalog.json']));
 assert.equal(cat.items.length,2);assert.equal(cat.items[0].id,'original_hat');
 assert.equal(cat.items[1].attach,'BODY');assert.equal(cat.items[1].flags,525);
 for(const [name,bytes] of Object.entries(before))if(name!=='assets/kush/cosmetics/catalog.json')assert.deepEqual(after[name],bytes,name);
 assert.deepEqual(after['assets/kush/cosmetics/custom_test_cosmetic/model.json'],modelBytes);
 assert.deepEqual(after['assets/kush/cosmetics/custom_test_cosmetic/texture.png'],texture);
});
test('duplicate IDs require explicit replacement; replacement retains other items',()=>{
 const m=K.jsonBytes(simpleModel()),p=png(),first=K.build(options,jar(),m,p);
 assert.throws(()=>K.build(options,first.jar,m,p),/já existe/);
 const second=K.build({...options,replace:true},first.jar,m,p);assert.equal(second.count,2);
});
test('rejects non-KUSH JAR, invalid models, texture and unsafe IDs',()=>{
 assert.throws(()=>K.readJar(F.zipSync({'fabric.mod.json':K.jsonBytes({id:'different'})})),/catálogo/);
 assert.throws(()=>K.checkModel(K.jsonBytes({parent:'minecraft:item/generated'})),/parent/);
 assert.throws(()=>K.checkPNG(new Uint8Array(50)),/PNG/);
 assert.throws(()=>K.makeAssets({...options,id:'../bad'},K.jsonBytes(simpleModel()),png()),/ID/);
});
