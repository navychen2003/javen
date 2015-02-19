
var artwork = {
  artwork_dialog: null,
  library_id: null,
  library_type: null,
  library_name: null,
  section_id: null,
  section_type: null,
  section_name: null,
  sections: null,
  username: null,
  selectname: null,
  selectcb: null,
  emptysrc: null,
  accepttype: null,
  suffix: null,
  downloading: false,
  dialog_shown: false,
  
  showselect: function( name, selectcb, emptysrc, suffix, accept )
  {
    artwork.showselect0( null, name, selectcb, emptysrc, suffix, accept );
  },
  showselect0: function( username, name, selectcb, emptysrc, suffix, accept )
  {
    if (this.dialog_shown == true) {
      artwork.showerror('Please close current artwork select dialog.');
      return;
    }
    
    if (name == null || name.length == 0) {
      artwork.showerror('Artwork root name is empty');
      return;
    }
    
    if (username == null) username = '';
    if (suffix == null) suffix = '.jpg';
    
    this.library_id = null;
    this.library_type = null;
    this.library_name = null;
    this.section_id = null;
    this.section_type = null;
    this.section_name = null;
    this.username = username;
    this.selectname = name;
    this.selectcb = selectcb;
    this.emptysrc = emptysrc;
    this.suffix = suffix;
    this.accepttype = accept;
    
    var uname = encodeURIComponent(username);
    var pname = encodeURIComponent(name);
    var psuffix = encodeURIComponent(suffix);
    var params = '&action=list&username=' + uname + '&rootname=' + pname 
               + '&path=&prefix=&suffix=' + psuffix;
    
    $.ajax
    (
      {
        url : app.base_path + '/artwork?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          artwork.library_id = response['library_id'];
          artwork.library_type = response['library_type'];
          artwork.library_name = response['library_name'];
          
          artwork.section_id = response['section_id'];
          artwork.section_type = response['section_type'];
          artwork.section_name = response['section_name'];
          
          artwork.sections = response['artworks'];
          
          if (artwork.section_id != null && artwork.section_id.length > 0)
            artwork.showdialog();
          else
            artwork.showerror('There is no library found, please add one first.');
        },
        error : function( xhr, text_status, error_thrown)
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function( xhr, text_status )
        {
          hide_loading();
        }
      }
    );
  },
  showdialog: function()
  {
    //if (is_empty_object(globalApp.libraries)) {
    //  artwork.showerror('There is no library found, please add one first.');
    //  return;
    //}
    
    this.dialog_shown = true;
    dialog.show( artwork.artwork_dialog );
  },
  hidedialog: function()
  {
    if (this.dialog_shown == true) {
      this.dialog_shown = false;
      dialog.hide();
    }
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/artwork.html',
      function( template )
      {
        artwork.artwork_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#artwork-close' )
              .attr( 'onclick', 'javascript:artwork.hidedialog();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            artwork.init_artwork();
          },
          hidecb: function()
          {
            artwork.dialog_shown = false;
          },
          shown: false
        };
      }
    );
  },
  init_artwork: function()
  {
    $( '#artwork-title' ).html( strings( 'Select Artwork' ) );
    $( '#artwork-upload' ).html( strings( 'upload an image' ) );
    $( '#artwork-drapdrop' ).html( strings( 'drag and drop' ) );
    $( '#artwork-enterurl' ).html( strings( 'enter a url' ) );
    $( '#artwork-uploading-text' ).html( strings( 'Uploading artwork...' ) );
    $( '#artwork-error-text' ).html( strings( 'There was an error uploading this artwork.' ) );
    $( '#artwork-tryagain' ).html( strings( 'Try again' ) );
    $( '#artwork-url-input' ).attr( 'placeholder', strings( 'Enter a url to upload an image from the web' ) );
    $( '#artwork-loading-text' ).html( strings( 'Searching for artwork...' ) );
    $( '#artwork-notfound-text' ).html( strings( 'No artwork found' ) );
    
    $( '#artwork-upload' )
      .attr( 'onClick', 'javascript:artwork.upload_image();return false;' )
      .attr( 'href', '' );
    
    $( '#artwork-enterurl' )
      .attr( 'onClick', 'javascript:artwork.show_enterurl();return false;' )
      .attr( 'href', '' );
    
    $( '#artwork-url-input' )
      .attr( 'onBlur', 'javascript:artwork.hide_enterurl();' );
    
    var accept = artwork.accepttype;
    if (accept != null && accept.length > 0) {
      accept = accept.toLowerCase();
      if (accept.indexOf('image/') < 0) {
        $( '#artwork-title' ).html( strings( 'Select File' ) );
        $( '#artwork-upload' ).html( strings( 'upload a file' ) );
        $( '#artwork-uploading-text' ).html( strings( 'Uploading file...' ) );
        $( '#artwork-error-text' ).html( strings( 'There was an error uploading this file.' ) );
        $( '#artwork-url-input' ).attr( 'placeholder', strings( 'Enter a url to upload a file from the web' ) );
        $( '#artwork-loading-text' ).html( strings( 'Searching for file...' ) );
        $( '#artwork-notfound-text' ).html( strings( 'No file found' ) );
      }
    }
    
    var username = artwork.username;
    if (username == null) username = '';
    
    var name = artwork.selectname;
    if (name == null) name = '';
    
    var urlform_element = $( '#artwork-urlform' );
    
    var uname = encodeURIComponent(username);
    var pname = encodeURIComponent(name);
    var params = '&action=download&username=' + uname + '&rootname=' + pname;
    
    urlform_element
      .ajaxForm
      (
        {
          url : app.base_path + '/artwork?token=' + app.token + params + '&wt=json',
          dataType : 'json',
          beforeSubmit : function( array, form, options )
          {
            var input_error = false;
            if (!input_error) { 
                show_loading();
                artwork.download_start();
            }
            
            return !input_error;
          },
          success : function( response, status_text, xhr, form )
          {
            var error = response['error'];
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
              artwork.download_done();
            }
          },
          error : function( xhr, text_status, error_thrown )
          {
            request_error( xhr, text_status, error_thrown );
          },
          complete : function()
          {
            hide_loading();
            artwork.download_end();
          }
        }
      );
    
    var dragarea_element = $( '#artwork-container' );
    dragarea_element = dragarea_element[0];
    
    dragarea_element.addEventListener('drop', function(event) {
          event.stopPropagation();
          event.preventDefault();
          artwork.handle_drop(event);
        }, false);
    
    dragarea_element.addEventListener('dragover', function(event) {
          event.stopPropagation();
          event.preventDefault();
          artwork.handle_dragover(event);
        }, false);
    
    var sections = this.sections;
    if (sections == null) sections = {};
    
    var content = [];
    var emptyItem = this.buildEmpty();
    if (emptyItem != null && emptyItem.length > 0)
      content.push( emptyItem );
    
    for (var key in sections) {
      var section = sections[key];
      if (section == null) continue;
      
      var item = this.buildItem(section);
      if (item != null)
        content.push( item );
    }
    
    $( '#artwork-options' ).html( content.join('\n') );
    
    this.dialog_shown = true;
  },
  buildEmpty: function()
  {
    var emptysrc = this.emptysrc;
    if (emptysrc == null || emptysrc.length == 0)
      return null;
    
    var thumbsrc = emptysrc;
    var clickto = 'javascript:artwork.clickempty();return false;';
    
    var item = '<span><a class="artwork-option" data-rating-key="" data-focus="keyboard">' +
               '<img class="poster poster-poster" src="' + thumbsrc.esc() + '" onClick="' + clickto + '">' +
               '</a></span>';
    
    return item;
  },
  clickempty: function()
  {
    this.hidedialog();
    
    var section = {id:''};
    var selectcb = this.selectcb;
    
    if (selectcb && section)
      selectcb.call(this, section);
  },
  buildItem: function( section )
  {
    if (section == null) return null;
    
    var sec_id = section['id'];
    var sec_name = section['name'];
    var contentType = section['type'];
    var extension = section['extname'];
    var poster = section['poster'];
    var path = section['path'];
    var isfolder = section['isfolder'];
    //var ctime = section['ctime'];
    var mtime = section['mtime'];
    //var indextime = section['itime'];
    var width = section['width'];
    var height = section['height'];
    var timelen = section['timelen'];
    var length = section['length'];
    var subcount = section['subcount'];
    var sublength = section['sublen'];
    
    if (extension == null && path != null) { 
      var pos = path.lastIndexOf('.');
      if (pos >= 0) 
        extension = path.substring(pos+1);
    }

    var acceptImage = true;
    var accept = artwork.accepttype;
    if (accept != null && accept.length > 0) {
      accept = accept.toLowerCase();
      if (accept.indexOf('image/') < 0) {
        acceptImage = false;
      }
    }

    if (sec_name == null) sec_name = '';
    if (contentType == null) contentType = '';
    if (extension == null || extension.length == 0) extension = 'dat';
    if (isfolder == null) isfolder = false;
    
    var titleText = fileinfo.getdisplayname( sec_name, contentType );
    var lengthText = readableBytes2(length);
    
    var thumbsrc = null;
    var openlink = null;
    
    var buttonicon = 'glyphicon picture';
    
    if (contentType.indexOf('image/') == 0) {
      thumbsrc = app.base_path + '/image/' + sec_id + '_192.' + extension + '?token=' + app.token;
      openlink = app.base_path + '/image/' + sec_id + '_0.' + extension + '?token=' + app.token;
      
      lengthText = '' + width + ' x ' + height;
      buttonicon = 'glyphicon picture';
      
    } else if (contentType.indexOf('audio/') == 0) { 
      thumbsrc = 'css/' + app.theme + '/images/posters/music.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      
      buttonicon = 'glyphicon music';
      
    } else if (contentType.indexOf('video/') == 0) { 
      thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      
      buttonicon = 'glyphicon film';
      
    } else if (contentType.indexOf('text/') == 0) { 
      thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      
      buttonicon = 'glyphicon notes';
      
    } else {
      thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      
      buttonicon = 'glyphicon file';
    }
    
    if (isfolder) {
      openlink = '#/~browse/' + key;
      thumbsrc = 'css/' + app.theme + '/images/posters/folder.png';
      
      buttonicon = 'glyphicon folder-open';
    }

    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + imgid + '_192t.' + imgext + '?token=' + app.token;
    }
    
    if (thumbsrc == null || thumbsrc.length == 0)
      return null;
    
    if (titleText == null) titleText = '';
    if (lengthText != null && lengthText.length > 0) 
      titleText = titleText + ' (' + lengthText + ')';
    
    var clickto = 'javascript:artwork.clickitem(\'' + sec_id + '\');return false;';
    var buttonclick = 'javascript:return false;';
    var buttonhide = '';
    if (acceptImage) buttonhide = 'hide';
    
    var item = 
        '<span>' + "\n" +
        '  <a class="artwork-option" data-rating-key="" data-focus="keyboard">' + "\n" +
        '    <img class="poster poster-poster" src="' + thumbsrc.esc() + '" onClick="' + clickto + '" title="' + titleText.esc() + '" />' + "\n" +
        '  </a>' + "\n" +
        '  <button type="button" class="hover-menu-btn poster-info-btn ' + buttonhide + '" style="margin-left: -40px;margin-top: 5px;padding: 1px 8px 3px;" onclick="' + buttonclick + '"><i class="' + buttonicon + '"></i></button>' + "\n" +
        '</span>';
    
    return item;
  },
  clickitem: function( key ) 
  {
    this.hidedialog();
    
    if (key == null || this.sections == null) 
      return;
    
    var section = this.sections[key];
    var selectcb = this.selectcb;
    
    if (selectcb && section)
      selectcb.call(this, section);
  },
  upload_image: function()
  {
    var section_id = this.section_id;
    var section_type = this.section_type;
    var section_name = this.section_name;
    var accept = this.accepttype; //'image/jpeg';
    
    var username = artwork.username;
    if (username == null) username = '';
    
    if (accept == null || accept.length == 0)
      accept = 'image/jpeg';
    else if (accept == '*')
      accept = null;
    
    var target = {id: section_id, type: section_type, name: section_name, username: username};
    uploader.select_files( accept, target, function( tg ) {
          if (tg && tg.id && tg.id == section_id && artwork.dialog_shown == true) {
            if (uploader.uploading_count == 0)
              artwork.upload_done();
            else
              artwork.upload_start();
          }
        }, false);
  },
  show_enterurl: function()
  {
    $( '#artwork-prompt' ).addClass( 'hide' );
    $( '#artwork-uploading-text' ).addClass( 'hide' );
    $( '#artwork-error' ).addClass( 'hide' );
    $( '#artwork-urlform' ).removeClass( 'hide' );
    $( '#artwork-url-input' ).focus();
  },
  hide_enterurl: function()
  {
    if (artwork.downloading == true) return;
    
    $( '#artwork-prompt' ).removeClass( 'hide' );
    $( '#artwork-uploading-text' ).addClass( 'hide' );
    $( '#artwork-error' ).addClass( 'hide' );
    $( '#artwork-urlform' ).addClass( 'hide' );
  },
  upload_start: function()
  {
    $( '#artwork-uploading-text' ).html( strings( 'Uploading artwork...' ) );
    
    var accept = artwork.accepttype;
    if (accept != null && accept.length > 0) {
      accept = accept.toLowerCase();
      if (accept.indexOf('image/') < 0) {
        $( '#artwork-uploading-text' ).html( strings( 'Uploading file...' ) );
      }
    }
    
    $( '#artwork-prompt' ).addClass( 'hide' );
    $( '#artwork-uploading-text' ).removeClass( 'hide' );
    $( '#artwork-error' ).addClass( 'hide' );
    $( '#artwork-urlform' ).addClass( 'hide' );
  },
  upload_done: function()
  {
    $( '#artwork-prompt' ).removeClass( 'hide' );
    $( '#artwork-uploading-text' ).addClass( 'hide' );
    $( '#artwork-error' ).addClass( 'hide' );
    $( '#artwork-urlform' ).addClass( 'hide' );
    
    var username = this.username;
    var name = this.selectname;
    var cb = this.selectcb;
    var emptysrc = this.emptysrc;
    var suffix = this.suffix;
    var accept = this.accepttype;
    
    artwork.hidedialog();
    artwork.showselect0(username, name, cb, emptysrc, suffix, accept);
  },
  download_start: function()
  {
    artwork.downloading = true;
    
    $( '#artwork-uploading-text' ).html( strings( 'Downloading artwork...' ) );
    
    var accept = artwork.accepttype;
    if (accept != null && accept.length > 0) {
      accept = accept.toLowerCase();
      if (accept.indexOf('image/') < 0) {
        $( '#artwork-uploading-text' ).html( strings( 'Downloading file...' ) );
      }
    }
    
    $( '#artwork-prompt' ).addClass( 'hide' );
    $( '#artwork-uploading-text' ).removeClass( 'hide' );
    $( '#artwork-error' ).addClass( 'hide' );
    $( '#artwork-urlform' ).addClass( 'hide' );
  },
  download_end: function()
  {
    artwork.downloading = false;
    
    $( '#artwork-prompt' ).removeClass( 'hide' );
    $( '#artwork-uploading-text' ).addClass( 'hide' );
    $( '#artwork-error' ).addClass( 'hide' );
    $( '#artwork-urlform' ).addClass( 'hide' );
  },
  download_done: function()
  {
    this.upload_done();
  },
  handle_drop: function(event)
  {
    var files = event.dataTransfer.files;
    if (this.check_files(files) == false) {
      this.showerror('Please select JPG image file.');
      return;
    }
    
    var section_id = this.section_id;
    var section_type = this.section_type;
    var section_name = this.section_name;
    
    var target = {id: section_id, type: section_type, name: section_name};
    uploader.upload_files( files, target, function( tg ) {
          if (tg && tg.id && tg.id == section_id && artwork.dialog_shown) {
            if (uploader.uploading_count == 0)
              artwork.upload_done();
            else
              artwork.upload_start();
          }
        }, false);
  },
  handle_dragover: function(event)
  {
  },
  check_files: function( files )
  {
    if (files == null || files.length == 0)
      return true;
    
    var accept = artwork.accepttype;
    if (accept == '*') return true;
    
    for (var i=0; i < files.length; i++) { 
      var file = files[i];
      if (file == null) continue;
      
      var name = file.name;
      var type = file.type;
      var size = file.size;
      
      if (name == null) name = '';
      var ext = '';
      var pos = name.lastIndexOf('.');
      if (pos >= 0) ext = name.substring(pos+1);
      if (ext == null) ext = '';
      ext = ext.toLowerCase();
      
      if (type != 'image/jpeg' || ext != 'jpg')
        return false;
    }
    
    return true;
  }
};
