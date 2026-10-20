import {build} from 'esbuild';
import {mkdir,cp,copyFile,readFile,writeFile} from 'node:fs/promises';
import path from 'node:path';
await mkdir('app',{recursive:true});
await build({entryPoints:['src/app.mjs'],bundle:true,format:'iife',platform:'browser',target:'chrome140',outfile:'app/studio.js',sourcemap:false,legalComments:'eof'});
await copyFile('src/index.html','app/index.html');await copyFile('src/style.css','app/style.css');
await cp('node_modules/three/examples/jsm/libs/draco/gltf','app/decoders/draco',{recursive:true});
await cp('node_modules/three/examples/jsm/libs/basis','app/decoders/basis',{recursive:true});
const licenses=[];
for(const dep of ['three','fflate','electron']){
 const base=path.join('node_modules',dep);
 for(const file of ['LICENSE','LICENSE.txt','LICENSE.md']){
  try{licenses.push(dep+'\n'+await readFile(path.join(base,file),'utf8'));break;}catch{}
 }
}
await writeFile('app/THIRD-PARTY-NOTICES.txt',licenses.join('\n\n----------------\n\n'));
