
var selectfolder = {
  body_element: null,
  selectfolder_dialog: null,
  foldertype: null,
  selectfolderid: null,
  selectcb: null,
  
  show_localfolder: function( cb )
  {
    this.foldertype = 'local';
    this.selectfolderid = '';
    this.selectcb = cb;
    dialog.show( this.selectfolder_dialog );
  },
  show_movefolder: function( folderid, cb )
  {
    this.foldertype = 'move';
    this.selectfolderid = folderid;
    this.selectcb = cb;
    dialog.show( this.selectfolder_dialog );
  },
  changefolder: function( folderid )
  {
    if (folderid == null)
      folderid = '';
    
    var type = '';
    if (this.foldertype && this.foldertype.length > 0)
      type = this.foldertype;
    
    $.ajax
    (
      {
        url : app.base_path + '/folder?token=' + app.token + '&type=' + type + '&id=' + folderid + '&wt=json',
        dataType : 'json',
        context : $( '#selectfolder-drivers', this.body_element ),
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          selectfolder.init_content( response );
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
  init_content: function( response )
  {
    if (response == null) response = {};
    
    var driverlist_element = $( '#selectfolder-drivers' );
    var folderlist_element = $( '#selectfolder-folders' );
    var path_element = $( '#selectfolder-path' );
    var pathid_element = $( '#selectfolder-pathid' );
  
    var selectFolder = response['selected'];
    var folders = response['folders'];
    var drivers = response['drivers'];
  
    if (selectFolder == null) selectFolder = {};
    if (folders == null) folders = {};
    if (drivers == null) drivers = {};
  
    var folderContent = [];
    var driverContent = [];
  
    var selectId = selectFolder['id'];
    var selectPath = selectFolder['path'];
    var back_string = strings( '[Back]' );
  
    if (selectId == null) selectId = '';
    if (selectPath == null) selectPath = '';
  
    pathid_element
      .attr( 'value', selectId.esc() );
  
    path_element
      .attr( 'value', selectPath.esc() );
  
    for (var key in drivers) { 
      var driver = drivers[key];
    
      var name = driver['name'];
      var dataDir = driver['path'];
      var userHome = driver['ishome'];
      var selected = driver['selected'];
    
      var selectedClass = '';
      if (selected) selectedClass = ' class="selected"';
    
      if (userHome)
      {
        var item = '		<li class="browse-directory-list-item">' + "\n" +
                   '          <a data-key="' + key + '"' + selectedClass + ' onclick="javascript:selectfolder.changefolder(\'' + key + '\');return false;" href="">' + "\n" +
                   '            <i class="folder-icon glyphicon home"></i>' + "\n" +
                   '            ' + name.esc() + ' <div class="spinner-container"></div>' + "\n" +
                   '          </a>' + "\n" +
                   '        </li>' + "\n";
    
        driverContent.push( item );
      }
      else 
      {
        var item = '		<li class="browse-directory-list-item">' + "\n" +
                   '          <a data-key="' + key + '"' + selectedClass + ' onclick="javascript:selectfolder.changefolder(\'' + key + '\');return false;" href="">' + "\n" +
                   '            <i class="folder-icon glyphicon hdd"></i>' + "\n" +
                   '            ' + name.esc() + ' <div class="spinner-container"></div>' + "\n" +
                   '          </a>' + "\n" +
                   '        </li>' + "\n";
    
        driverContent.push( item );
      }
    }
  
    for (var key in folders) { 
      var folder = folders[key];
    
      var name = folder['name'];
      var dataDir = folder['path'];
      var contentType = folder['type'];
    
      if (contentType == null) contentType = '';
      if (contentType.indexOf('recycle') >= 0)
        continue;
    
      if (name == '..')
        name = name + ' ' + back_string;
    
      var item = '		<li class="browse-directory-list-item">' + "\n" +
                 '          <a data-key="' + key + '" onclick="javascript:selectfolder.changefolder(\'' + key + '\');return false;" href="">' + "\n" +
                 '            <i class="folder-icon glyphicon folder-open"></i>' + "\n" +
                 '            ' + name.esc() + ' <div class="spinner-container"></div>' + "\n" +
                 '          </a>' + "\n" +
                 '        </li>' + "\n";
    
      folderContent.push( item );
    }
  
    driverlist_element
      .html( driverContent.join( "\n" ) );
  
    folderlist_element
      .html( folderContent.join( "\n" ) );
  },
  submit: function()
  {
    var path_element = $( '#selectfolder-path' );
    var path = path_element
      .attr( 'value' );
    
    var pathid_element = $( '#selectfolder-pathid' );
    var pathid = pathid_element
      .attr( 'value' );
    
    if (pathid != null && pathid.length > 0 && path != null && path.length > 0) {
      var cb = this.selectcb;
      if (cb) cb.call(this, pathid, path);
    }
    
    dialog.hide();
  },
  hide: function()
  {
    this.foldertype = null;
    dialog.hide();
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/selectfolder.html',
      function( template )
      {
        selectfolder.selectfolder_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            var body_element = $( '#content-body' );
            
            $( '#selectfolder-title' ).html( strings( 'Select Folder' ) );
            $( '#selectfolder-submit' ).html( strings( 'Ok' ) );
            $( '#selectfolder-cancel' ).html( strings( 'Cancel' ) );
            
            $( '#selectfolder-submit' )
              .attr( 'onclick', 'javascript:selectfolder.submit();return false;' );
            
            $( '#selectfolder-cancel' )
              .attr( 'onclick', 'javascript:selectfolder.hide();return false;' );
            
            $( '#selectfolder-close' )
              .attr( 'onclick', 'javascript:selectfolder.hide();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            var folderid = selectfolder.selectfolderid;
            if (folderid == null) folderid = '';
            
            selectfolder.body_element = body_element;
            selectfolder.changefolder( folderid );
            
          },
          hidecb: function()
          {
          },
          shown: false
        };
      }
    );
  }
};
