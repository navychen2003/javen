
var uploader = {
  upload_queue: [],
  upload_target: null,
  refresh_cb: null,
  refresh_show: false,
  input_element: null,
  tasks_dialog: null,
  dialog_shown: false,
  dialog_uploaded_count: 0,
  uploaded_count: 0,
  uploading_count: 0,
  
  reset: function()
  {
    this.upload_queue = [];
  },
  select_files: function( accept, target, cb, show )
  {
    if (this.upload_queue == null)
      this.upload_queue = [];
    
    //this.removeDones();
    
    var acceptAttr = '';
    if (accept) acceptAttr = 'accept="' + accept.esc() + '"';
    this.upload_target = target;
    this.refresh_cb = cb;
    this.refresh_show = show;
    
    var elem = this.input_element;
    this.input_element = null;
    if (elem) elem.remove();
    
    var item = $('<input type="file" onchange="javascript:uploader.submit_files(this.files);" style="visibility:hidden" ' + acceptAttr + ' multiple />');
    item.appendTo(document.body);
    this.input_element = item;
    item.click();
  },
  upload_files: function( files, target, cb, show )
  {
    if (this.upload_queue == null)
      this.upload_queue = [];
    
    //this.removeDones();
    
    this.upload_target = target;
    this.refresh_cb = cb;
    this.refresh_show = show;
    
    this.submit_files( files );
  },
  submit_files: function( files )
  {
    var elem = this.input_element;
    this.input_element = null;
    if (elem) elem.remove();
    
    if (files == null || files.length == 0)
      return;
    
    var target = this.upload_target;
    //this.upload_target = null;
    if (target == null)
      return;
    
    this.removeDones();
    
    var queue = this.upload_queue;
    
    for (var i=0; i < files.length; i++) { 
      var file = files[i];
      if (file == null) continue;
      
      var queue = this.upload_queue;
      var found = false;
      
      for (var j=0; queue && j < queue.length; j++) { 
        var queueItem = queue[j];
        var queueFile = queueItem['file'];
        
        if (queueFile) { 
          if (queueFile.name == file.name && queueFile.size == file.size) { 
            found = true;
            break;
          }
        }
      }
      
      if (found) continue;
      
      var item = {};
      item['file'] = file;
      item['target'] = target;
      item['status'] = 'waiting';
      
      queue.push( item );
    }
    
    var uploadingCount = this.uploadFiles();
    
    if (queue && queue.length > 0 && this.refresh_show)
      this.show_dialog();
    
    if (uploadingCount > 0) 
      this.refreshCall();
  },
  uploadFiles: function()
  {
    var queue = this.upload_queue;
    var uploadingCount = 0;
    
    for (var j=0; queue && j < queue.length; j++) { 
      var item = queue[j];
      if (item) {
        var status = item['status'];
        if (status == 'uploading') { 
          uploadingCount ++;
          continue;
        }
        if (this.uploadFile( item ))
          uploadingCount ++;
      }
      if (uploadingCount >= 3)
        break;
    }
    
    this.uploading_count = uploadingCount;
    this.initTaskIcon( uploadingCount );
    
    return uploadingCount;
  },
  uploadFile: function( item )
  {
    if (item == null) return;
    
    var file = item['file'];
    var target = item['target'];
    var status = item['status'];
    
    if (file == null || target == null)
      return false;
    
    var targetId = target['id'];
    var username = target['username'];
    if (username == null) username = '';
    
    if (targetId == null || status != 'waiting')
      return false;
    
    item['status'] = 'uploading';
    
    var uploadURL = app.base_path + '/upload?action=upload&token=' + app.token + '&wt=json';
    var fd = new FormData();
    fd.append( 'username', username );
    fd.append( 'target', targetId );
    fd.append( 'file', file );

    /* event listners */
    var xhr = new XMLHttpRequest();
    item['request'] = xhr;
    item['startDate'] = new Date();
    
    xhr.upload.addEventListener("progress", 
      function( event ) { 
        if (event.lengthComputable) {
          //var percentComplete = event.loaded / event.total;
          item['progressLoaded'] = event.loaded;
          item['progressTotal'] = event.total;
          item['updateDate'] = new Date();
          uploader.updateItem(item);
        }
      }, false);
    
    xhr.addEventListener("load", 
      function( event ) {
        if (item['status'] == 'uploading') {
          item['status'] = 'done';
          item['updateDate'] = new Date();
          uploader.updateItem(item);
          uploader.uploadFiles();
          uploader.uploadDone(item);
        }
      }, false);
    
    xhr.addEventListener("error", 
      function( event ) {
        if (item['status'] == 'uploading') {
          item['status'] = 'error';
          item['updateDate'] = new Date();
          uploader.updateItem(item);
          uploader.uploadFiles();
        }
      }, false);
    
    xhr.addEventListener("abort", 
      function( event ) {
        if (item['status'] == 'uploading') {
          item['status'] = 'abort';
          item['updateDate'] = new Date();
          uploader.updateItem(item);
          uploader.uploadFiles();
        }
      }, false);
    
    xhr.onreadystatechange = 
      function() {
        if (xhr.readyState == 4) { 
          item['statusCode'] = xhr.status;
          item['statusText'] = xhr.statusText;
          if (xhr.status == 200) {
            // success
          } else { 
            item['responseText'] = xhr.responseText;
            var response = JSON.parse( xhr.responseText );
            if (response) { 
              var error = response['error'];
              if (error) { 
                item['errorCode'] = error['code'];
                item['errorMsg'] = error['msg'];
              }
            }
            if (item['status'] == 'uploading') {
              item['status'] = 'error';
              item['updateDate'] = new Date();
              uploader.updateItem(item);
              uploader.uploadFiles();
            }
          }
        }
      };
    
    /* Be sure to change the url below to the url of your upload server side script */
    xhr.open("POST", uploadURL, true);
    xhr.send(fd);
    
    return true;
  },
  initTaskIcon: function( uploadingCount )
  {
    var queue = this.upload_queue;
    if (queue && queue.length > 0)
    {
      var title;
      
      if (uploadingCount > 0) {
        title = strings( 'Uploading' );
        $( '#task-icon' ).removeClass( 'hide' );
        
      } else { 
        title = strings( 'Upload Tasks' );
        $( '#task-icon' ).addClass( 'hide' );
      }
      
      $( '#task-title' ).html( title );
      $( '#iconbar-task' ).removeClass( 'hide' );
      $( '#task-link' )
        .attr( 'onclick', 'javascript:uploader.show_dialog();return false;' )
        .attr( 'href', '' );
    }
    else
    {
      //$( '#task-title' ).html( strings( 'Uploading' ) );
      $( '#iconbar-task' ).addClass( 'hide' );
    }
  },
  init_taskicon: function()
  {
    var queue = this.upload_queue;
    var uploadingCount = 0;
    
    for (var j=0; queue && j < queue.length; j++) { 
      var item = queue[j];
      if (item) {
        var status = item['status'];
        if (status == 'uploading') { 
          uploadingCount ++;
          continue;
        }
      }
      if (uploadingCount >= 3)
        break;
    }
    
    this.initTaskIcon( uploadingCount );
  },
  show_dialog: function()
  {
    dialog.show( this.tasks_dialog );
    this.dialog_shown = true;
    
    var queue = this.upload_queue;
    
    for (var j=0; queue && j < queue.length; j++) { 
      var item = queue[j];
      if (item) 
        this.updateItem( item );
    }
  },
  hide_dialog: function()
  {
    this.dialog_shown = false;
    dialog.hide();
    
    var count = this.dialog_uploaded_count;
    this.dialog_uploaded_count = this.uploaded_count;
    
    if (count != this.dialog_uploaded_count)
      this.refreshCall();
  },
  init_dialog: function( dialog_element )
  {
    $.get
    (
      'tpl/tasks.html',
      function( template )
      {
        uploader.tasks_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#tasks-title' ).html( strings( 'Upload Tasks' ) );
            $( '#tasks-ok' ).remove(); //html( strings( 'Ok' ) );
            $( '#tasks-no' ).html( strings( 'Close' ) );
            
            $( '#tasks-container' )
              .attr( 'class', 'modal modal-large media-info-modal fade in' );
            
            $( '#tasks-icon' )
              .attr( 'class', 'glyphicon cloud-upload' );
            
            var html = uploader.initTasks();
            if (html == null) html = "";
            
            var remainingTitle = globalApp.get_remainingtitle();
            var remainingClick = 'javascript:return false;';
            var remainingHtml = '<a class="standard-options" onclick="' + remainingClick + '" href="">' + remainingTitle.esc() + '</a>';
            
            $( '#tasks-text' ).html( html );
            $( '#tasks-options' ).html( remainingHtml );
            
            //$( '#tasks-ok' )
            //  .attr( 'onclick', 'javascript:uploader.hide_dialog();return false;' );
            
            $( '#tasks-no' )
              .attr( 'onclick', 'javascript:uploader.hide_dialog();return false;' );
            
            $( '#tasks-close' )
              .attr( 'onclick', 'javascript:uploader.hide_dialog();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            uploader.initThumbs();
          },
          hidecb: function()
          {
            uploader.dialog_shown = false;
          },
          shown: false
        };
      }
    );
  },
  uploadDone: function( item )
  {
    listlibrary.libraries_inited = false;
    this.uploaded_count ++;
    this.refreshCall();
  },
  refreshCall: function()
  {
    var cb = uploader.refresh_cb;
    var target = uploader.upload_target;
    if (cb) cb.call(uploader, target);
  },
  removeDones: function()
  {
    while (this.removeDone()) {}
  },
  removeDone: function()
  {
    var queue = this.upload_queue;
    var foundIdx = -1;
    var foundItem = null;
    
    for (var j=0; queue && j < queue.length; j++) { 
      var queueItem = queue[j];
      var status = queueItem['status'];
      if (queueItem && (status == 'done' || status == 'abort')) { 
        foundIdx = j;
        foundItem = queueItem;
        break;
      }
    }
    
    if (queue && foundItem && foundIdx >= 0) {
      queue.splice(foundIdx, 1);
      foundItem['status'] = 'removed';
      
      return true;
    }
    
    return false;
  },
  remove_item: function( index )
  {
    if (index == null) return;
    
    var queue = this.upload_queue;
    var foundIdx = -1;
    var foundItem = null;
    
    for (var j=0; queue && j < queue.length; j++) { 
      var queueItem = queue[j];
      var queueIdx = queueItem['index'];
      if (queueItem && queueIdx == index) { 
        foundIdx = j;
        foundItem = queueItem;
        break;
      }
    }
    
    if (queue && foundItem && foundIdx >= 0) {
      var status = foundItem['status'];
      var xhr = foundItem['request'];
      
      if (xhr && status == 'uploading') {
        xhr.abort();
        return;
      }
      
      queue.splice(foundIdx, 1);
      foundItem['status'] = 'removed';
      
      var item_element = $( '#upload-item-' + index ); 
      if (item_element) 
        item_element.remove();
    }
  },
  update_thumb: function( index )
  {
    if (index == null) return;
    
    var queue = this.upload_queue;
    var foundIdx = -1;
    var foundItem = null;
    
    for (var j=0; queue && j < queue.length; j++) { 
      var queueItem = queue[j];
      var queueIdx = queueItem['index'];
      if (queueItem && queueIdx == index) { 
        foundIdx = j;
        foundItem = queueItem;
        break;
      }
    }
    
    if (queue && foundItem && foundIdx >= 0) 
      this.initThumb( foundItem );
  },
  initThumb: function( item )
  {
    if (item == null) return;
    
    var file = item['file'];
    var thumbId = item['thumbId'];
    var thumbSrc = item['thumbSrc'];
      
    if (file && thumbId && thumbSrc == null) {
      file.makeThumb({
          width: 40,
          height: 40,
          mark: {},
          success: function(dataURL, tSize, file, sSize, fEvt, oItem) {
            var elementId = oItem['thumbId'];
            oItem['thumbSrc'] = dataURL;
            if (elementId)
              $( '#' + elementId ).attr( 'src', dataURL );
          }
        }, item);
    }
  },
  initThumbs: function()
  {
    var queue = this.upload_queue;
    if (queue == null || queue.length == 0)
      return;
    
    for (var i=0; i < queue.length; i++) { 
      var item = queue[i];
      var file = item['file'];
      if (file == null || file.size > 51200)
        continue;
      
      this.initThumb( item );
    }
  },
  initTasks: function()
  {
    var queue = this.upload_queue;
    var content = [];
    
    if (queue == null) queue = [];
    
    for (var i=0; i < queue.length; i++) {
      var item = queue[i];
      var itemHtml = this.buildItem( item, i );
      if (item && itemHtml) {
        item['index'] = i;
        var file = item['file'];
        if (file) {
          var contentItem = '<li id="upload-item-' + i + '">' + itemHtml + '</li>';
          content.push( contentItem );
        }
      }
    }
    
    var html = '<div class="well">' + "\n" +
            '<ul = class="list">' + content.join('\n') + '</ul>' + "\n" +
            '</div>' + "\n";
    
    return html;
  },
  updateItem: function( item )
  {
    if (item == null) return;
    if (this.dialog_shown == false) return;
    
    var thumbId = item['thumbId'];
    var statusId = item['statusId'];
    var doneSizeId = item['doneSizeId'];
    var removeId = item['removeId'];
    var progressThumbId = item['progressThumbId'];
    var progressBarId = item['progressBarId'];
    
    if (thumbId == null || statusId == null || doneSizeId == null || 
        removeId == null || progressThumbId == null || progressBarId == null)
      return;
    
    var file = item['file'];
    if (file == null) return;
    
    var fileName = file.name;
    var fileType = file.type;
    var fileSize = file.size;
    
    var target = item['target'];
    var targetName = target['name'];
    
    var loaded = item['progressLoaded'];
    var total = item['progressTotal'];
    var startDate = item['startDate'];
    var updateDate = item['updateDate'];
    
    var status = item['status'];
    var statusCode = item['statusCode'];
    var statusText = item['statusText'];
    var errorMsg = item['errorMsg'];
    
    var statusStr = status;
    var statusMsg = '';
    var statusTitle = '';
    var removeTitle = '';
    var elapsedInfo = '';
    
    if (loaded == null || loaded < 0) loaded = 0;
    if (total == null || total <= 0) total = fileSize;
    
    if (errorMsg && errorMsg.length > 0)
      statusText = errorMsg;
    if (statusCode && statusText)
      statusMsg = ': ' + strings(statusText) + '(' + statusCode + ')';
    
    if (startDate && updateDate) { 
      var elapsedTime = updateDate.getTime() - startDate.getTime();
      if (elapsedTime > 0) { 
        var elapsedStr = uploader.readableSeconds( elapsedTime / 1000 );
        
        var speed = 1000 * loaded / elapsedTime;
        var speedStr = uploader.readableBytes( speed ) + '/s';
        
        var leftSecs = 0;
        if (speed > 0 && loaded >= 0 && total > 0 && loaded <= total) 
          leftSecs = (total - loaded) / speed;
        var leftStr = uploader.readableSeconds( leftSecs );
        
        var text = strings( 'elapsed {0} and {1} left and speed {2}' );
        elapsedInfo = ' (' + text.format(elapsedStr, leftStr, speedStr) + ')';
      }
    }
    
    if (status == 'waiting') { 
      var text = strings( 'Waiting to upload {0} to {1}' );
      statusTitle = text.format(fileName, targetName) + elapsedInfo;
      removeTitle = strings( 'Cancel Upload' );
      statusStr = strings( 'Waiting' );
      
    } else if (status == 'uploading') { 
      var text = strings( 'Uploading {0} to {1}' );
      statusTitle = text.format(fileName, targetName) + elapsedInfo;
      removeTitle = strings( 'Cancel Upload' );
      statusStr = strings( 'Uploading' );
      
      if (total > 0 && loaded >= total)
        statusStr = strings( 'Analyzing and Storing' );
      
    } else if (status == 'done') { 
      var text = strings( 'Upload {0} to {1} completed' );
      statusTitle = text.format(fileName, targetName) + elapsedInfo;
      removeTitle = strings( '' );
      statusStr = strings( 'Completed' );
      
    } else if (status == 'error') { 
      var text = strings( 'Upload {0} to {1} failed' );
      statusTitle = text.format(fileName, targetName) + elapsedInfo + statusMsg;
      removeTitle = strings( '' );
      statusStr = strings( 'Failed' );
      
    } else if (status == 'abort') { 
      var text = strings( 'Upload {0} to {1} canceled' );
      statusTitle = text.format(fileName, targetName) + elapsedInfo;
      removeTitle = strings( '' );
      statusStr = strings( 'Canceled' );
    }
    
    var status_element = $( '#' + statusId );
    var donesize_element = $( '#' + doneSizeId );
    var remove_element = $( '#' + removeId );
    var progressthumb_element = $( '#' + progressThumbId );
    var progressbar_element = $( '#' + progressBarId );
      
    if (status_element) {
      status_element
        .attr( 'title', statusTitle.esc() )
        .html( statusStr.esc() );
    }
    
    if (remove_element)
      remove_element.attr( 'title', removeTitle.esc() );
    
    if (donesize_element && progressthumb_element && progressbar_element) { 
      if (loaded >= 0 && total > 0) { 
        if (loaded < 0) loaded = 0;
        if (loaded > total) loaded = total;
          
        var percent = 100.0 * loaded / total;
        var thumb = percent - 1.0;
        if (thumb <= 1.0) thumb = 1.0;
        if (percent >= 100.0) thumb = 98.0;
          
        var doneSize = this.readableBytes( loaded );
          
        donesize_element
          .html( doneSize.esc() );
          
        progressthumb_element
          .attr( 'style', 'left: ' + thumb + '%;' );
          
        progressbar_element
          .attr( 'style', 'width: ' + percent + '%;' );
      }
    }
  },
  buildItem: function( item, index )
  {
    if (item == null) return null;
    
    var file = item['file'];
    var fileName = file.name;
    var fileType = file.type;
    var fileSize = this.readableBytes( file.size );
    var doneSize = this.readableBytes( 0 );
    
    var target = item['target'];
    var targetId = target['id'];
    var targetType = target['type'];
    var targetName = target['name'];
    
    var statusId = item['statusId'];
    var doneSizeId = item['doneSizeId'];
    var removeId = item['removeId'];
    var progressThumbId = item['progressThumbId'];
    var progressBarId = item['progressBarId'];
    
    var thumbId = item['thumbId'];
    var thumbSrc = item['thumbSrc'];
    var thumbTitle = '';
    
    var status = item['status'];
    var statusStr = status;
    var statusTitle = '';
    var removeTitle = '';
    
    if (status == 'waiting') { 
      var text = strings( 'Waiting to upload {0} to {1}' );
      statusTitle = text.format(fileName, targetName);
      removeTitle = strings( 'Cancel Upload' );
      statusStr = strings( 'Waiting' );
      
    } else if (status == 'uploading') { 
      var text = strings( 'Uploading {0} to {1}' );
      statusTitle = text.format(fileName, targetName);
      removeTitle = strings( 'Cancel Upload' );
      statusStr = strings( 'Uploading' );
      
    } else if (status == 'done') { 
      var text = strings( 'Upload {0} to {1} completed' );
      statusTitle = text.format(fileName, targetName);
      removeTitle = strings( 'Delete' );
      statusStr = strings( 'Completed' );
      
    } else if (status == 'error') { 
      var text = strings( 'Upload {0} to {1} failed' );
      statusTitle = text.format(fileName, targetName);
      removeTitle = strings( 'Delete' );
      statusStr = strings( 'Failed' );
      
    } else if (status == 'abort') { 
      var text = strings( 'Upload {0} to {1} canceled' );
      statusTitle = text.format(fileName, targetName);
      removeTitle = strings( 'Delete' );
      statusStr = strings( 'Canceled' );
    }
    
    if (file) { 
      var text = strings( 'File name: {0} File type: {1} File size: {2}' );
      thumbTitle = text.format(fileName, fileType, readableBytes(file.size));
      
      if (fileType == null) fileType = '';
      var filethumb = 'css/' + app.theme + '/images/posters/poster.png';
      
      if (fileType.indexOf('video/') == 0) {
        filethumb = 'css/' + app.theme + '/images/posters/poster.png';
        
      } else if (fileType.indexOf('audio/') == 0) {
        filethumb = 'css/' + app.theme + '/images/posters/music.png';
        
      } else if (fileType.indexOf('image/') == 0) {
        filethumb = 'css/' + app.theme + '/images/posters/photo.png';
      }
      
      if (thumbSrc == null || thumbSrc.length == 0)
        thumbSrc = filethumb;
    }
    
    thumbId = 'upload-file-thumb-' + index;
    statusId = 'upload-status-' + index;
    doneSizeId = 'upload-done-size-' + index;
    removeId = 'upload-remove-' + index;
    progressThumbId = 'upload-progress-thumb-' + index;
    progressBarId = 'upload-progress-' + index;
    
    item['thumbId'] = thumbId;
    item['statusId'] = statusId;
    item['doneSizeId'] = doneSizeId;
    item['removeId'] = removeId;
    item['progressThumbId'] = progressThumbId;
    item['progressBarId'] = progressBarId;
    
    if (thumbSrc == null || thumbSrc.length == 0) { 
      thumbSrc = 'src="' + 'css/' + app.theme + '/images/posters/poster.png' + '"';
    } else { 
      thumbSrc = 'src="' + thumbSrc + '"';
    }
    
    if (removeTitle == null || removeTitle.length == 0)
      removeTitle = strings( 'Delete' );
    
    return '<div class="upload-task">' + "\n" +
           '<div class="task-controls">' + "\n" +
	       '    <a class="now-playing-album" title="' + thumbTitle.esc() + '"><img class="poster task-icon-poster placeholder" onMouseOver="javascript:uploader.update_thumb(' + index + ');" id="' + thumbId + '" ' + thumbSrc + '></a>' + "\n" +
	       '    <div class="now-playing">' + "\n" +
		   '        <a class="now-playing-title" title="' + statusTitle.esc() + '" id="' + statusId + '">' + statusStr.esc() + '</a>' + "\n" +
		   '        <span class="now-playing-subtitle">' + fileName.esc() + '</span>' + "\n" +
	       '    </div>' + "\n" +
           '	<div class="task-controls-right">' + "\n" +
           '		<div class="player-time" style="padding-top: 2px;">' + "\n" +
           '            <span class="player-current-time" id="' + doneSizeId + '">' + doneSize + '</span> / <span class="player-duration">' + fileSize + '</span>' + "\n" +
           '		    <button class="player-btn player-slider-btn glyphicon" onClick="javascript:uploader.remove_item(' + index + ');" title="' + removeTitle.esc() + '" id="' + removeId + '"><i class="glyphicon remove-2"></i></button>' + "\n" +
           '        </div>' + "\n" +
           '	</div>' + "\n" +
           '	<div class="task-controls-center-wrapper">' + "\n" +
           '	<div class="task-controls-center">' + "\n" +
           '	<div class="player-slider-track player-seek-bar">' + "\n" +
           '		<div class="player-slider-thumb player-progress-thumb" style="left: 0.0%;" id="' + progressThumbId + '"></div>' + "\n" +
           '		<div class="player-slider-bar player-progress-bar" style="width: 0.0%;" id="' + progressBarId + '"></div>' + "\n" +
           '		<div class="player-buffer-bar" style="left: 0px; width: 100%;"></div>' + "\n" +
           '	</div></div>' + "\n" +
           '	</div>' + "\n" +
           '</div>' + "\n" +
           '</div>';
  },
  readableSeconds: function( seconds )
  {
    seconds = parseInt( seconds || 0, 10 );
    var minutes = Math.floor( seconds / 60 );
    var hours = Math.floor( minutes / 60 );

    var text = [];
    if (0 !== hours)
    {
      text.push( hours + 'h' );
      seconds -= hours * 60 * 60;
      minutes -= hours * 60;
    }

    if (0 !== minutes)
    {
      text.push( minutes + 'm' );
      seconds -= minutes * 60;
    }

    if (0 !== seconds || text.length == 0)
    {
      text.push( seconds + 's' );
    }

    return text.join( ' ' );
  },
  readableBytes: function( bytes )
  {
    var bb = bytes;
    var kb = Math.floor( bb / 1024 );
    var mb = Math.floor( kb / 1024 );
    var gb = Math.floor( mb / 1024 );

    var text = [];
    if (gb > 0)
    {
      text.push( gb + ' GB' );
      bb -= gb * 1024 * 1024 * 1024;
      kb -= gb * 1024 * 1024;
      mb -= gb * 1024;
    }

    if (mb > 0)
    {
      text.push( mb + ' MB' );
      bb -= mb * 1024 * 1024;
      kb -= mb * 1024;
    }

    if (kb > 0)
    {
      text.push( kb + ' KB' );
      bb -= kb * 1024;
    }

    if (text.length == 0)
    {
      text.push( bb + ' ' + strings( 'Bytes' ) );
    }

    return text[0]; //text.join( ' ' );
  },
  create_folder: function( foldername, target, cb )
  {
    //dialog.hide();
    
    if (foldername == null || target == null) 
      return;
    
    var targetId = target['id'];
    var name = encodeURIComponent(foldername);
    
    var action = 'newfolder';
    var params = '&target=' + targetId + '&foldername=' + name + '&token=' + app.token;
    
    $.ajax
    (
      {
        url : app.base_path + '/upload?action=' + action + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var error = response['error'];
          var library = response['library'];
          
          if (error)
          {
            var code = error['code'];
            var msg = error['msg'];
            
            messager.error_code = code;
            messager.error_msg = msg;
            
            dialog.show( messager.message_dialog );
          }
          else
          {
            listlibrary.showlist();
          }
        },
        error : function( xhr, text_status, error_thrown)
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function( xhr, text_status )
        {
          hide_loading();
          if (cb) cb.call(uploader);
        }
      }
    );
  },
  action_submit: function( action, params, items, cb )
  {
    if (items == null || items.length == 0)
      return;
    
    if (params == null) params = '';
    
    var fd = null;
    
    if (items.length > 20) {
      fd = new FormData();
    
      for (var i=0; i < items.length; i++) {
        var item = items[i];
        if (item && item.length > 0)
          fd.append( 'id', item );
      }
    }
    else { 
      for (var i=0; i < items.length; i++) {
        var item = items[i];
        if (item && item.length > 0)
          params += '&id=' + item;
      }
    }
    
    var requestURL = app.base_path + '/upload?action=' + action + params + '&token=' + app.token + '&wt=json';
    
    $.ajax(
      {
        type : 'POST', 
        url : requestURL, 
        data : fd, 
        contentType : false, 
        processData : false,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var res = { error_code: 0, error_msg: text_status, resp: response };
          var error = response['error'];
          if (error)
          {
            res.error_code = error['code'];
            res.error_msg = error['msg'];
          }
          
          listlibrary.libraries_inited = false;
          if (cb) cb.call( uploader, res );
        },
        error : function( xhr, text_status, error_thrown)
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function( xhr, text_status )
        {
          hide_loading();
        }
      });
  },
  empty_folder: function( items, cb )
  {
    var action = 'empty';
    var params = '';
    
    this.action_submit( action, params, items, cb );
  },
  delete_file: function( items, cb )
  {
    var action = 'delete';
    var params = '';
    
    this.action_submit( action, params, items, cb );
  },
  trash_file: function( items, cb )
  {
    var action = 'trash';
    var params = '';
    
    this.action_submit( action, params, items, cb );
  },
  move_file: function( items, pathid, cb )
  {
    if (pathid == null || pathid.length == 0)
      return;
    
    var action = 'move';
    var params = '&target=' + pathid;
    
    this.action_submit( action, params, items, cb );
  },
  copy_file: function( items, pathid, cb )
  {
    if (pathid == null || pathid.length == 0)
      return;
    
    var action = 'copy';
    var params = '&target=' + pathid;
    
    this.action_submit( action, params, items, cb );
  }
};