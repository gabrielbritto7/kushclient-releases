import * as F from 'fflate';
import * as T from 'three';
const enc=new TextEncoder();
export const encode=value=>enc.encode(JSON.stringify(value));
export function png(r=255,g=70,b=100){
 const width=16,height=16,rows=new Uint8Array(height*(width*4+1));for(let y=0;y<height;y++)for(let x=0;x<width;x++)rows.set([r,g,b,255],y*(width*4+1)+1+x*4);
 const crc=bytes=>{let c=0xffffffff;for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return(c^0xffffffff)>>>0;};
 const chunk=(name,data)=>{const type=enc.encode(name),o=new Uint8Array(data.length+12),v=new DataView(o.buffer);v.setUint32(0,data.length);o.set(type,4);o.set(data,8);v.setUint32(o.length-4,crc(o.subarray(4,o.length-4)));return o;};
 const ih=new Uint8Array(13),view=new DataView(ih.buffer);view.setUint32(0,width);view.setUint32(4,height);ih[8]=8;ih[9]=6;
 const parts=[new Uint8Array([137,80,78,71,13,10,26,10]),chunk('IHDR',ih),chunk('IDAT',F.zlibSync(rows)),chunk('IEND',new Uint8Array())],out=new Uint8Array(parts.reduce((s,p)=>s+p.length,0));let at=0;for(const p of parts){out.set(p,at);at+=p.length;}return out;
}
export function jar(items=[{id:'original_hat',displayName:'Original',category:'hats',attach:'HEAD',flags:13,model:'builtin/hat/model.json',texture:'builtin/hat/texture.png'}]){
 return F.zipSync({
 'fabric.mod.json':encode({schemaVersion:1,id:'fastclient-hud',name:'KUSHMOD',version:'0.3.29'}),
 'assets/kush/cosmetics/catalog.json':encode({schema:1,items}),
 'net/fastclient/core/equip/KushCatalogProvider.class':enc.encode('fixture builtin/ /assets/kush/cosmetics/catalog.json'),
 'net/fastclient/Untouched.class':new Uint8Array([0xca,0xfe,0xba,0xbe,0,6]),
 'assets/kush/cosmetics/hat/model.json':encode({original:true}),
 'assets/kush/cosmetics/hat/texture.png':png()
 },{level:6});
}
export const options={id:'test_cosmetic',displayName:'Teste GLB',category:'back',attach:'BODY',flags:525,replace:false};
export function glbFixture(){
 const geometry=new T.BoxGeometry(1,1,1).toNonIndexed(),position=geometry.attributes.position.array,normal=geometry.attributes.normal.array,uv=geometry.attributes.uv.array,texture=png();
 const arrays=[new Uint8Array(position.buffer),new Uint8Array(normal.buffer),new Uint8Array(uv.buffer),texture],views=[],segments=[];let size=0;
 for(const a of arrays){const padded=new Uint8Array(Math.ceil(a.length/4)*4);padded.set(a);views.push({buffer:0,byteOffset:size,byteLength:a.length});segments.push(padded);size+=padded.length;}
 const json={asset:{version:'2.0'},buffers:[{byteLength:size}],bufferViews:views,accessors:[
 {bufferView:0,componentType:5126,count:position.length/3,type:'VEC3',min:[-.5,-.5,-.5],max:[.5,.5,.5]},
 {bufferView:1,componentType:5126,count:normal.length/3,type:'VEC3'},
 {bufferView:2,componentType:5126,count:uv.length/2,type:'VEC2'}],
 images:[{bufferView:3,mimeType:'image/png'}],textures:[{source:0}],materials:[{pbrMetallicRoughness:{baseColorTexture:{index:0},metallicFactor:0,roughnessFactor:1}}],
 meshes:[{primitives:[{attributes:{POSITION:0,NORMAL:1,TEXCOORD_0:2},material:0}]}],nodes:[{mesh:0}],scenes:[{nodes:[0]}],scene:0};
 return binaryGLB(json,segments);
}
export function binaryGLB(json,segments=[]){
 const source=encode(json),jsonBytes=new Uint8Array(Math.ceil(source.length/4)*4).fill(32);jsonBytes.set(source);
 const binSize=segments.reduce((n,v)=>n+v.length,0),total=12+8+jsonBytes.length+(binSize?8+binSize:0),out=new Uint8Array(total),view=new DataView(out.buffer);
 view.setUint32(0,0x46546c67,true);view.setUint32(4,2,true);view.setUint32(8,total,true);view.setUint32(12,jsonBytes.length,true);view.setUint32(16,0x4e4f534a,true);out.set(jsonBytes,20);
 if(binSize){let at=20+jsonBytes.length;view.setUint32(at,binSize,true);view.setUint32(at+4,0x004e4942,true);at+=8;for(const b of segments){out.set(b,at);at+=b.length;}}return out;
}
