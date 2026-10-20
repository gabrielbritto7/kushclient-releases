const {app,BrowserWindow,Menu,session,dialog}=require('electron');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const appDirectory=path.join(__dirname,'app');
app.setAppUserModelId('com.kush.cosmeticstudio');
let window;
app.whenReady().then(()=>{
 Menu.setApplicationMenu(null);
 session.defaultSession.setPermissionRequestHandler((_wc,_permission,respond)=>respond(false));
 session.defaultSession.webRequest.onBeforeRequest((details,callback)=>{
  const url=details.url;
  const allowed=url.startsWith('file:')||url.startsWith('data:')||url.startsWith('blob:')||url.startsWith('devtools:');
  callback({cancel:!allowed});
 });
 window=new BrowserWindow({width:1450,height:940,minWidth:1100,minHeight:720,backgroundColor:'#101115',title:'KUSH Cosmetic Studio',show:false,webPreferences:{nodeIntegration:false,contextIsolation:true,sandbox:true,webSecurity:true}});
 const entry=pathToFileURL(path.join(appDirectory,'index.html')).href;
 window.webContents.on('will-navigate',(event,url)=>{if(url!==entry)event.preventDefault();});
 window.webContents.setWindowOpenHandler(()=>({action:'deny'}));
 session.defaultSession.on('will-download',(_event,item)=>{
  item.setSaveDialogOptions({title:'Salvar arquivo do KUSH Cosmetic Studio',defaultPath:path.join(app.getPath('downloads'),path.basename(item.getFilename()))});
 });
 window.once('ready-to-show',()=>window.show());
 window.loadURL(entry).catch(error=>dialog.showErrorBox('Não foi possível abrir o Studio',error.message));
});
app.on('window-all-closed',()=>app.quit());
