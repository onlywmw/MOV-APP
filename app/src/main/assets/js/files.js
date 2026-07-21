/* ============================================================
   files.js — 房间文件 tab: 文件树 + 预览 + 操作
   ============================================================ */
var _filesPath='';

function renderFileTree(roomId){
  var res=B.listRoomFiles(roomId,_filesPath);
  if(!res.ok){$('fileList').innerHTML='<div class="sysline">'+t('files.loadFail')+'</div>';return;}
  var files=res.files||[];
  var h='';
  if(_filesPath){
    h+='<div class="file-row" data-up="1"><span class="fic">..</span><span class="fn">..</span></div>';
  }
  files.forEach(function(f){
    var ext=f.isDir?'':f.name.split('.').pop().toUpperCase().slice(0,4);
    var icon=f.isDir?'<span class="fic dir">D</span>':'<span class="fic">'+esc(ext||'F')+'</span>';
    var size=f.isDir?'':'<span class="fs">'+formatFileSize(f.size)+'</span>';
    h+='<div class="file-row" data-file="'+esc(f.name)+'" data-dir="'+(f.isDir?'1':'0')+'">'
      +icon+'<span class="fn">'+esc(f.name)+'</span>'+size+'</div>';
  });
  if(files.length===0&&!_filesPath)h='<div class="sysline">'+t('files.empty')+'</div>';
  $('fileList').innerHTML=h;
  renderFilePathBar(roomId);

  document.querySelectorAll('#fileList .file-row').forEach(function(el){
    el.addEventListener('click',function(){
      var name=el.getAttribute('data-file');
      var isDir=el.getAttribute('data-dir')==='1';
      if(el.getAttribute('data-up')){
        _filesPath=_filesPath.substring(0,_filesPath.lastIndexOf('/'));
        renderFileTree(roomId);
      }else if(isDir){
        _filesPath=_filesPath?_filesPath+'/'+name:name;
        renderFileTree(roomId);
      }else{
        var fp=(_filesPath?_filesPath+'/':'')+name;
        var res2=B.readFile(roomId,fp);
        if(res2.ok){showFilePreview(name,res2.content);}
        else{B.toast(res2.error||t('files.loadFail'));}
      }
    });
    /* 长按: 文件删除 */
    if(el.getAttribute('data-dir')==='0'&&!el.getAttribute('data-up')){
      var fname=el.getAttribute('data-file');
      bindLongPress(el,{
        text:t('files.delete'),
        exec:function(){
          var fp=(_filesPath?_filesPath+'/':'')+fname;
          var r=B.deleteFile(roomId,fp);
          if(r.ok){B.toast(t('files.deleted'));renderFileTree(roomId);}
          else{B.toast(r.message||'');}
        }
      });
    }
  });
}

function renderFilePathBar(roomId){
  var parts=_filesPath?_filesPath.split('/'):[];
  var h='<span class="file-crumb" data-path="">~</span>';
  var acc='';
  parts.forEach(function(p){
    acc=acc?acc+'/'+p:p;
    h+=' / <span class="file-crumb" data-path="'+esc(acc)+'">'+esc(p)+'</span>';
  });
  $('filePathBar').innerHTML=h;
  document.querySelectorAll('#filePathBar .file-crumb').forEach(function(el){
    el.addEventListener('click',function(){
      _filesPath=el.getAttribute('data-path')||'';
      renderFileTree(roomId);
    });
  });
}

function showFilePreview(name,content){
  var b=$('chatBody');
  var d=document.createElement('div');
  d.className='msg wide';
  d.innerHTML='<div class="bubble" style="font-family:var(--font-mono);font-size:11px;white-space:pre-wrap;max-height:300px;overflow:auto">'
    +'<div style="font-weight:700;margin-bottom:6px;color:var(--acc-strong)">'+esc(name)+'</div>'
    +esc(content)+'</div>';
  b.appendChild(d);
  b.scrollTop=b.scrollHeight;
}

function formatFileSize(bytes){
  if(bytes<1024)return bytes+'B';
  if(bytes<1048576)return (bytes/1024).toFixed(1)+'KB';
  return (bytes/1048576).toFixed(1)+'MB';
}

/* 文件 tab 的 + 按钮: 上传文件到房间 */
function fileFabAction(roomId){
  B.pickFile(function(info){
    if(!info)return;
    /* 读取选中文件内容并写入房间 */
    B.toast(t('files.uploaded')+' '+info.name);
    renderFileTree(roomId);
  });
}
