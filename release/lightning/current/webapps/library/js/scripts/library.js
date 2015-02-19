
var librarymode = {
  editmode: false,
  librarylist: [],
  
  change: function()
  {
    if ($( '#dashboard-library-actions' ).hasClass( 'hide' )) 
      this.show();
    else
      this.hide();
  },
  show: function()
  {
    $( '#dashboard-library-list' )
      .removeClass( 'tile-list section-tile-list' )
      .addClass( 'list media-list section-list' );
      
    $( '#dashboard-library-actions' )
      .removeClass( 'hide' );
    
    for (var i=0; i < this.librarylist.length; i++)
    {
      var lib = this.librarylist[i];
      
      $( '#' + lib.linkid )
        .removeAttr( 'href' );
    }
    
    this.editmode = true;
  },
  hide: function()
  {
    $( '#dashboard-library-list' )
      .removeClass( 'list media-list section-list' )
      .addClass( 'tile-list section-tile-list' );
    
    $( '#dashboard-library-actions' )
      .addClass( 'hide' );
    
    for (var i=0; i < this.librarylist.length; i++)
    {
      var lib = this.librarylist[i];
      
      $( '#' + lib.linkid )
        .attr( 'href', lib.linkhref );
    }
    
    this.editmode = false;
  }
};

var editlibrary = {
  library_name: null,
  library_id: null,
  delete_cb: null,
  delete_confirm_dialog: null,
  rebuild_confirm_dialog: null,
  rescan_confirm_dialog: null,
  
  edit: function( key )
  {
    editlibrary.edit0( key, null );
  },
  edit0: function( key, cb )
  {
    if (key == null || key.length == 0) return;
    addlibrary.selecttype_cb = function() { editlibrary.init_library( key ); };
    addlibrary.submit_cb = cb;
    dialog.show( addlibrary.selecttype_dialog );
  },
  init_library: function( key )
  {
    var library = globalApp.libraries[key];
    if (library == null) return;
    
    $( '#addlibrary-title' ).html( strings( 'Edit Library' ) );
    $( '#addlibrary-icon' ).attr( 'class', 'glyphicon pencil' );
    
    var id = library['id'];
    var name = library['name'];
    var hostname = library['hostname'];
    var contentType = library['type'];
    var sections = library['sections'];
    
    var paths = [];
    
    if (sections)
    {
      for (var skey in sections)
      {
        var section = sections[skey];
        
        var sid = section['id'];
        var sdataDir = section['path'];
        
        //if (sdataDir != null && sdataDir.length > 0)
        //  paths.push( sdataDir );
      }
    }
    
    if (contentType.indexOf( 'image' ) >= 0)
      addlibrary.edit_photo( id, name, paths );
    else if (contentType.indexOf( 'audio' ) >= 0)
      addlibrary.edit_music( id, name, paths );
    else if (contentType.indexOf( 'video' ) >= 0)
      addlibrary.edit_video( id, name, paths );
    else if (contentType.indexOf( 'computer' ) >= 0)
      addlibrary.edit_computer( id, name, paths );
    else
      addlibrary.edit_file( id, name, paths );
    
  },
  delete: function( key, cb )
  {
    var library = globalApp.libraries[key];
    if (library == null) return;
    
    this.library_id = library['id'];
    this.library_name = library['name'];
    this.delete_cb = cb;
    
    dialog.show( this.delete_confirm_dialog );
  },
  delete_library: function()
  {
    dialog.hide();
    
    var id = this.library_id;
    var cb = this.delete_cb;
    
    var action = 'delete';
    var params = '&id=' + id + '&token=' + app.token;
    
    $.ajax
    (
      {
        url : app.base_path + '/library?action=' + action + params + '&wt=json',
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
            if (cb) cb.call(editlibrary);
            else listlibrary.showlist();
          }
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
  indexLibrary: function( id, deepscan )
  {
    if (id == null) return;
    
    var cmd = deepscan ? 'full-import' : 'delta-import';
    var entity = 'datum';
    var deep = 'true'; //deepscan ? 'true' : 'false';
    
    var params = 'command=' + cmd + '&commit=true&optimize=true&entity=' + entity;
    var custom = 'deepscan=' + deep;
    
    if (id == 'all') { 
      var libraries = globalApp.libraries;
      if (libraries) { 
        for (var key in libraries) { 
          var library = libraries[key];
          var libid = library['id'];
          
          custom = custom + '&id=' + libid;
        }
      }
    } else { 
      custom = custom + '&id=' + id;
    }
    
    params = params + '&' + custom;
    
    $.ajax
    (
      {
        url : app.index_path + '/dataimport?' + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          listlibrary.showlist();
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
  refreshLibrary: function( id, deepscan )
  {
    if (id == null) return;
    
    var deep = deepscan ? 'true' : 'false';
    var params = 'action=refresh' + '&token=' + app.token;
    var custom = 'deepscan=' + deep;
    
    if (id == 'all') { 
      var libraries = globalApp.libraries;
      if (libraries) { 
        for (var key in libraries) { 
          var library = libraries[key];
          var libid = library['id'];
          
          custom = custom + '&id=' + libid;
        }
      }
    } else { 
      custom = custom + '&id=' + id;
    }
    
    params = params + '&' + custom;
    
    $.ajax
    (
      {
        url : app.base_path + '/library?' + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          listlibrary.showlist();
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
  refresh: function( id )
  {
    this.refreshLibrary( id, false );
  },
  scan: function()
  {
    this.refreshLibrary( 'all', false );
  },
  deepscan: function()
  {
    this.refreshLibrary( 'all', true );
  },
  rebuild: function()
  {
  },
  rebuild_library: function()
  {
  },
  rescan: function()
  {
    this.rescan_library();
  },
  rescan_library: function()
  {
    dialog.hide();
    this.scan();
  }
};

var addlibrary = {
  selecttype_dialog: null,
  selecttype_cb: null,
  submit_cb: null,
  input_folders: [],
  input_type: null,
  input_action: null,
  input_id: null,
  
  add_file: function()
  {
    this.input_action = 'add';
    this.input_type = 'file';
    this.input_id = null;
    this.input_folders = [];
    
    this.show_details( strings( 'My File' ), strings( 'File' ) );
  },
  edit_file: function( id, name, paths )
  {
    this.input_action = 'edit';
    this.input_type = 'file';
    this.input_id = id;
    this.input_folders = [];
    
    if (paths)
    {
      for (var i=0; i < paths.length; i++)
      {
        var folder = {
            path: paths[i],
            id: this.input_folders.length
          };
        
        this.input_folders.push( folder );
      }
    }
    
    this.show_details( name, strings( 'File' ) );
  },
  add_music: function()
  {
    this.input_action = 'add';
    this.input_type = 'music';
    this.input_id = null;
    this.input_folders = [];
    
    this.show_details( strings( 'My Music' ), strings( 'Music' ) );
  },
  edit_music: function( id, name, paths )
  {
    this.input_action = 'edit';
    this.input_type = 'music';
    this.input_id = id;
    this.input_folders = [];
    
    if (paths)
    {
      for (var i=0; i < paths.length; i++)
      {
        var folder = {
            path: paths[i],
            id: this.input_folders.length
          };
        
        this.input_folders.push( folder );
      }
    }
    
    this.show_details( name, strings( 'Music' ) );
  },
  add_video: function()
  {
    this.input_action = 'add';
    this.input_type = 'video';
    this.input_id = null;
    this.input_folders = [];
    
    this.show_details( strings( 'My Video' ), strings( 'Video' ) );
  },
  edit_video: function( id, name, paths )
  {
    this.input_action = 'edit';
    this.input_type = 'video';
    this.input_id = id;
    this.input_folders = [];
    
    if (paths)
    {
      for (var i=0; i < paths.length; i++)
      {
        var folder = {
            path: paths[i],
            id: this.input_folders.length
          };
        
        this.input_folders.push( folder );
      }
    }
    
    this.show_details( name, strings( 'Video' ) );
  },
  add_photo: function()
  {
    this.input_action = 'add';
    this.input_type = 'photo';
    this.input_id = null;
    this.input_folders = [];
    
    this.show_details( strings( 'My Photo' ), strings( 'Photo' ) );
  },
  edit_photo: function( id, name, paths )
  {
    this.input_action = 'edit';
    this.input_type = 'photo';
    this.input_id = id;
    this.input_folders = [];
    
    if (paths)
    {
      for (var i=0; i < paths.length; i++)
      {
        var folder = {
            path: paths[i],
            id: this.input_folders.length
          };
        
        this.input_folders.push( folder );
      }
    }
    
    this.show_details( name, strings( 'Photo' ) );
  },
  add_computer: function()
  {
    this.input_action = 'add';
    this.input_type = 'computer';
    this.input_id = null;
    this.input_folders = [];
    
    this.show_details( strings( 'My Computer' ), strings( 'Computer' ) );
  },
  edit_computer: function( id, name, paths )
  {
    this.input_action = 'edit';
    this.input_type = 'computer';
    this.input_id = id;
    this.input_folders = [];
    
    if (paths)
    {
      for (var i=0; i < paths.length; i++)
      {
        var folder = {
            path: paths[i],
            id: this.input_folders.length
          };
        
        this.input_folders.push( folder );
      }
    }
    
    this.show_details( name, strings( 'Computer' ) );
  },
  submit: function()
  {
    var input_error = false;
    
    var input_name = $( '#addlibrary-name-input' )
      .attr( 'value' ).trim();
    
    var input_store = $( '#addlibrary-store-input' )
      .attr( 'value' ).trim();
    
    if (input_name == null || input_name.length == 0)
    {
      $( '#addlibrary-required-name' )
        .removeClass( 'hide' );
      
      $( '#addlibrary-name-group' )
        .addClass( 'error' );
      
      input_error = true;
    }
    else
    {
      $( '#addlibrary-required-name' )
        .addClass( 'hide' );
      
      $( '#addlibrary-name-group' )
        .removeClass( 'error' );
    }
    
    if (true)
    {
      if (this.input_folders.length == 0)
      {
        //$( '#addlibrary-required-folder' )
        //  .removeClass( 'hide' );
      
        //input_error = true;
      }
      else
      {
        $( '#addlibrary-required-folder' )
          .addClass( 'hide' );
      }
    }
    
    if (input_error) return;
    
    if (input_store == null || input_store.length == 0)
      input_store = "file://";
    
    var id = this.input_id;
    var action = this.input_action;
    var params = 'token=' + app.token + '&type=' + this.input_type 
               + '&name=' + encodeURIComponent(input_name) 
               + '&store=' + encodeURIComponent(input_store);
    
    if (action == 'edit')
      params = 'id=' + id + '&' + params;
    
    for (var i=0; i < this.input_folders.length; i++)
    {
      var folder = this.input_folders[i];
      params = params + '&path=' + encodeURIComponent(folder.path);
    }
    
    $.ajax
    (
      {
        url : app.base_path + '/library?action=' + action + '&' + params + '&wt=json',
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
            dialog.hide();
            
            var cb = addlibrary.submit_cb;
            if (cb) {
              cb.call(addlibrary);
              
            } else {
              var libid = null;
              if (library) 
                libid = library['id'];
            
              if (libid)
                editlibrary.refresh( libid );
              else
                editlibrary.scan();
            }
          }
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
  init_advanced: function() 
  {
    var stores = globalApp.stores;
    var storeContent = [];
    if (stores) {
      for (var key in stores) { 
        var store = stores[key];
        var title = null;
        var value = key;
        
        if (key.indexOf("file:") >= 0) {
          title = strings( 'Folder: {0}' );
          value = store;
        } else if (key.indexOf('dfs') >= 0) {
          title = strings( 'Cloud Storage: {0}' );
          value = store;
        }
        
        if (title && value) { 
          title = title.format( store );
          var item = '<option value="' + value.esc() + '">' + title.esc() + '</option>' + "\n";
          storeContent.push( item );
        }
      }
    }
    
    var stores_element = $( '#addlibrary-store-input' );
    stores_element
      .html( storeContent.join( "\n" ) );
    
  },
  show_advanced: function() 
  {
    $( '#addlibrary-advanced' )
      .removeClass( 'hide' );
    $( '#addlibrary-advanced-show' )
      .addClass( 'hide' );
    $( '#addlibrary-advanced-hide' )
      .removeClass( 'hide' );
  },
  hide_advanced: function() 
  {
    $( '#addlibrary-advanced' )
      .addClass( 'hide' );
    $( '#addlibrary-advanced-show' )
      .removeClass( 'hide' );
    $( '#addlibrary-advanced-hide' )
      .addClass( 'hide' );
  },
  show_details: function( name, type )
  {
    var addfolder_string = strings( 'Add folders where your {0} is located' );
    var myfiles_string = strings( 'My File' );
    var myphotos_string = strings( 'My Photo' );
    var file_string = strings( 'File' );
    var photo_string = strings( 'Photo' );
    
    $( '#addlibrary-addfolder-title' )
      .html( addfolder_string.format( type ) );
    
    $( '#addlibrary-name-input' )
      .attr( 'value', name );
    
    $( '#addlibrary-types' )
      .addClass( 'hide' );
    
    $( '#addlibrary-details' )
      .removeClass( 'hide' );
    
    $( '#addlibrary-footer' )
      .removeClass( 'hide' );
    
    var libraryid = this.input_id;
    if (libraryid != null && libraryid.length > 0) {
      $( '#addlibrary-delete' )
        .attr( 'onclick', 'javascript:addlibrary.delete(\'' + libraryid.esc() + '\');return false;' )
        .removeClass( 'hide' );
    }
    
    this.init_advanced();
    
    if (globalApp.is_admin() == false) { 
      $( '#addlibrary-folder-group' )
        .addClass( 'hide' );
        
    } else {
      this.update_folders();
    }
  },
  show_selecttype: function()
  {
    addlibrary.selecttype_cb = null;
    addlibrary.submit_cb = null;
    dialog.show( addlibrary.selecttype_dialog );
  },
  show_add: function()
  {
    addlibrary.selecttype_cb = null;
    addlibrary.submit_cb = addlibrary.addcb;
    dialog.show( addlibrary.selecttype_dialog );
  },
  addcb: function()
  {
    listlibrary.libraries_inited = false;
    sammy.refresh();
  },
  show_selectfolder: function()
  {
    selectfolder.show_localfolder( function( pathid, path ) {
          addlibrary.add_folder( path );
        } );
  },
  add_folder: function( folderpath )
  {
    var folder = {
        path: folderpath,
        id: this.input_folders.length
      };
    
    this.input_folders.push( folder );
    this.update_folders();
  },
  update_folders: function()
  {
    var folders_element = $( '#addlibrary-folder-list' );
    var folderContent = [];
    
    for (var i=0; i < this.input_folders.length; i++)
    {
      var folder = this.input_folders[i];
      folder.id = i;
      
      var item = '    <div class="path-container" id="addlibrary-folder' + folder.id + '">' + "\n" +
                 '      <input type="text" name="path" class="span12" id="folderpath' + folder.id + '" value="' + folder.path.esc() + '" readonly>' + "\n" +
                 '      <div class="path-actions">' + "\n" +
                 '        <a class="remove-path-btn btn btn-danger btn-icon" onclick="javascript:addlibrary.remove_folder(' + folder.id + ');return false;" data-focus="keyboard">'  + "\n" +
                 '          <i class="glyphicon circle-remove"></i>' + "\n" +
                 '        </a>' + "\n" +
                 '      </div>' + "\n" +
                 '    </div>' + "\n";
      
      folderContent.push( item );
    }
    
    folders_element
      .html( folderContent.join( "\n" ) );
  },
  remove_folder: function( id )
  {
    this.input_folders.splice( id, 1 );
    this.update_folders();
  },
  delete: function( id )
  {
    editlibrary.delete( id, addlibrary.deletecb );
  },
  deletecb: function()
  {
    listlibrary.libraries_inited = false;
    
    var context = system.context;
    var username = globalApp.get_username();
    context.redirect( '#/~browse/' + encodeURIComponent(globalApp.get_browsekey(username)) );
  }
};

var listlibrary = {
  body_element: null,
  libraries_inited: false,
  
  initlist: function( cb )
  {
    if (this.libraries_inited) 
      return;
    
    var params = '&action=library';
    
    $.ajax
    (
      {
        url : app.base_path + '/dashboard?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var announcements = response['announcements'];
          var libraries = response['libraries'];
          var storages = response['storages'];
          var stores = response['stores'];
          
          globalApp.announcements = announcements;
          globalApp.libraries = libraries;
          globalApp.storages = storages;
          globalApp.stores = stores;
          listlibrary.libraries_inited = true;
          
          if (cb) cb.call(listlibrary);
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
  showlist: function()
  {
    this.showlist0( null );
  },
  showlist0: function( cb )
  {
    var librarylist_element = $( '#dashboard-library-list' );
    var libraryactions_element = $( '#dashboard-library-actions' );
    
    var edit_title = strings( 'Edit' );
    var refresh_title = strings( 'Refresh' );
    var delete_title = strings( 'Delete' );
    
    var params = '&action=list';
    
    $.ajax
    (
      {
        url : app.base_path + '/dashboard?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : $( '#dashboard-library-list', this.body_element ),
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var announcements = response['announcements'];
          var histories = response['histories'];
          var libraries = response['libraries'];
          var storages = response['storages'];
          var stores = response['stores'];
          var libraryContent = [];
          
          globalApp.announcements = announcements;
          globalApp.libraries = libraries;
          globalApp.storages = storages;
          globalApp.stores = stores;
          
          listlibrary.libraries_inited = true;
          librarymode.librarylist = [];
          
          for (var key in libraries) { 
            var library = libraries[key];
            
            var name = library['name'];
            var hostname = library['hostname'];
            var contentType = library['type'];
            var mtime = library['mtime'];
            var indextime = library['itime'];
            var subcount = library['subcount'];
            var sublen = library['sublen'];
            
            var imagesrc = 'css/' + app.theme + '/images/sections/folder.png';
            
            if (contentType.indexOf('image') >= 0)
              imagesrc = 'css/' + app.theme + '/images/sections/photo.png';
            else if (contentType.indexOf('audio') >= 0)
              imagesrc = 'css/' + app.theme + '/images/sections/music.png';
            else if (contentType.indexOf('video') >= 0)
              imagesrc = 'css/' + app.theme + '/images/sections/video.png';
            
            var edit_onclick = 'javascript:editlibrary.edit(\'' + key + '\');return false;';
            var refresh_onclick = 'javascript:editlibrary.refresh(\'' + key + '\');return false;';
            var delete_onclick = 'javascript:editlibrary.delete(\'' + key + '\');return false;';
            
            var linkid = 'library_' + key;
            var linkhref = '#/~browse/' + key;
            
            var lib = {};
            lib.linkid = linkid;
            lib.linkhref = linkhref;
            lib.key = key;
            
            librarymode.librarylist.push( lib );
            
            var hrefattr = ' href="' + linkhref + '"';
            if (librarymode.editmode)
              hrefattr = '';
            
            var infoContent = '';
            var sizeInfo = '';
        
            if (subcount != null && subcount > 0) { 
              var countTitle = strings( 'Contains' );
              var countText = strings( '{0} Items' ).format( subcount );
              if (sizeInfo.length > 0) sizeInfo += ' ';
              sizeInfo += countTitle + ': ' + countText;
            }
            if (sublen != null && sublen > 0) { 
              var lengthTitle = strings( 'Length' );
              var lengthText = readableBytes2(sublen);
              if (sizeInfo.length > 0) sizeInfo += ' ';
              sizeInfo += lengthTitle + ': ' + lengthText;
            }
            if (sizeInfo != null && sizeInfo.length > 0) {
              infoContent += '            <h4>' + sizeInfo.esc() + '</h4>' + "\n";
            }
            if (mtime != null && mtime > 0) { 
              var updatedTitle = strings( 'Updated' );
              var updatedText = format_time(mtime);
              infoContent += '            <h4>' + updatedTitle.esc() + ': ' + updatedText.esc() + '</h4>' + "\n";
            }
            
            var item = '		<li><a class="clearfix" id="' + linkid + '"' + hrefattr + '" data-focus="keyboard">' + "\n" +
		               '            <div class="actions">' + "\n" +
			           '                <button type="button" class="edit-section-btn btn btn-icon" rel="tooltip" title="' + edit_title.esc() + '" onclick="' + edit_onclick.esc() + '"><i class="glyphicon pencil"></i></button>' + "\n" +
			           '                <button type="button" class="refresh-section-btn btn btn-icon " rel="tooltip" title="' + refresh_title.esc() + '" onclick="' + refresh_onclick.esc() + '"><i class="glyphicon repeat"></i></button>' + "\n" +
	                   '                <button type="button" class="delete-section-btn btn btn-danger btn-icon" rel="tooltip" title="' + delete_title.esc() + '" onclick="' + delete_onclick.esc() + '"><i class="glyphicon ban"></i></button>' + "\n" +
		               '            </div>' + "\n" +
		               '            <img class="poster section-poster custom-section-poster" src="' + imagesrc + '">' + "\n" +
		               '            <h3>' + "\n" +
			           '                <span class="section-title">' + name.esc() + '</span>' + "\n" +
			           '                <span class="source-title secondary">' + hostname.esc() + '</span>' + "\n" +
		               '            </h3>' + "\n" +
		               infoContent +
	                   '         </a></li>' + "\n";
	        
	        libraryContent.push( item );
          }
          
          librarylist_element
            .html( libraryContent.join( "\n" ) );
          
          if (libraryContent.length == 0) {
            $( '#dashboard-history-empty' ).removeClass( 'hide' );
            librarylist_element.addClass( 'hide' );
            addlibrary.show_selecttype();
          }
          
          if (cb) cb.call(listlibrary, histories);
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
  }
};

var library_dialogs = { 
  init_addlibrary: function( dialog_element ) 
  {
    $.get
    (
      'tpl/addlibrary.html',
      function( template )
      {
        addlibrary.selecttype_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          {
            $( '#addlibrary-title' ).html( strings( 'Add Library' ) );
            $( '#addlibrary-select' ).html( strings( 'Select library type' ) );
            $( '#addlibrary-file-label' ).html( strings( 'File' ) );
            $( '#addlibrary-music-label' ).html( strings( 'Music' ) );
            $( '#addlibrary-video-label' ).html( strings( 'Video' ) );
            $( '#addlibrary-photo-label' ).html( strings( 'Photo' ) );
            $( '#addlibrary-computer-label' ).html( strings( 'Computer' ) );
            $( '#addlibrary-required-name' ).html( strings( 'A library name is required.' ) );
            $( '#addlibrary-addfolder-title' ).html( strings( 'Add folders where your {0} is located' ) );
            $( '#addlibrary-required-folder' ).html( strings( 'At least one folder is required.' ) );
            $( '#addlibrary-advanced-show' ).html( strings( 'Show Advanced Options' ) );
            $( '#addlibrary-advanced-hide' ).html( strings( 'Hide Advanced Settings' ) );
            $( '#addlibrary-store-title' ).html( strings( 'Store System' ) );
            $( '#addlibrary-name-label' ).html( strings( 'Name' ) );
            $( '#addlibrary-addfolder-label' ).html( strings( 'Add Folder' ) );
            $( '#addlibrary-delete' ).html( strings( 'Delete' ) );
            $( '#addlibrary-save' ).html( strings( 'Save' ) );
            $( '#addlibrary-cancel' ).html( strings( 'Cancel' ) );
            
            $( '#addlibrary-file' )
              .attr( 'onclick', 'javascript:addlibrary.add_file();return false;' );
            
            $( '#addlibrary-music' )
              .attr( 'onclick', 'javascript:addlibrary.add_music();return false;' );
            
            $( '#addlibrary-video' )
              .attr( 'onclick', 'javascript:addlibrary.add_video();return false;' );
            
            $( '#addlibrary-photo' )
              .attr( 'onclick', 'javascript:addlibrary.add_photo();return false;' );
            
            $( '#addlibrary-computer' )
              .attr( 'onclick', 'javascript:addlibrary.add_computer();return false;' );
            
            $( '#addlibrary-addfolder' )
              .attr( 'onclick', 'javascript:addlibrary.show_selectfolder();return false;' );
            
            $( '#addlibrary-advanced-show' )
              .attr( 'onclick', 'javascript:addlibrary.show_advanced();return false;' )
              .attr( 'href', '' );
            
            $( '#addlibrary-advanced-hide' )
              .attr( 'onclick', 'javascript:addlibrary.hide_advanced();return false;' )
              .attr( 'href', '' );
            
            $( '#addlibrary-save' )
              .attr( 'onclick', 'javascript:addlibrary.submit();return false;' );
            
            $( '#addlibrary-cancel' )
              .attr( 'onclick', 'javascript:dialog.hide();return false;' );
            
            $( '#addlibrary-close' )
              .attr( 'onclick', 'javascript:dialog.hide();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            var cb = addlibrary.selecttype_cb;
            if (cb) cb.call(library_dialogs);
          },
          hidecb: function()
          {
          },
          shown: false
        };
      }
    );
  },
  init_message: function( dialog_element, template ) 
  {
    editlibrary.delete_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Library' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to delete {0}? This cannot be undone.' );
        if (msg == null) msg = "";
        
        msg = msg.format( editlibrary.library_name );
        
        $( '#message-text' )
          .html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:editlibrary.delete_library();return false;' )
          .addClass( 'btn-danger' );
        
        $( '#message-no' )
          .attr( 'onclick', 'javascript:dialog.hide();return false;' );
        
        $( '#message-close' )
          .attr( 'onclick', 'javascript:dialog.hide();return false;' )
          .attr( 'title', strings( 'Close' ) );
        
      },
      hidecb: function()
      {
      },
      shown: false,
      alertdialog: true
    };
    
    editlibrary.rebuild_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Build Library' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon circle-question-mark' );
        
        var msg = strings( 'Are you sure you want to rebuild all libraries?' );
        if (msg == null) msg = "";
        
        $( '#message-text' ).html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:editlibrary.rebuild_library();return false;' );
        
        $( '#message-no' )
          .attr( 'onclick', 'javascript:dialog.hide();return false;' );
        
        $( '#message-close' )
          .attr( 'onclick', 'javascript:dialog.hide();return false;' )
          .attr( 'title', strings( 'Close' ) );
        
      },
      hidecb: function()
      {
      },
      shown: false,
      alertdialog: true
    };
    
    editlibrary.rescan_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Refresh Library' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon circle-question-mark' );
        
        var msg = strings( 'Are you sure you want to refresh all libraries?' );
        if (msg == null) msg = "";
        
        $( '#message-text' ).html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:editlibrary.rescan_library();return false;' );
        
        $( '#message-no' )
          .attr( 'onclick', 'javascript:dialog.hide();return false;' );
        
        $( '#message-close' )
          .attr( 'onclick', 'javascript:dialog.hide();return false;' )
          .attr( 'title', strings( 'Close' ) );
        
      },
      hidecb: function()
      {
      },
      shown: false,
      alertdialog: true
    };
  }
};
