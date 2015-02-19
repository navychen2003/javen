
var browsecb_impl = {
  getResponseSectionId: function(response) { return response['id']; },
  getResponseSectionName: function(response) { return response['name']; },
  getResponseSectionType: function(response) { return response['type']; },
  getResponseSectionPermissions: function(response) { return response['perms']; },
  getResponseSectionOperations: function(response) { return response['ops']; },
  getResponseRootId: function(response) { return response['root_id']; },
  getResponseRootName: function(response) { return response['root_name']; },
  getResponseRootType: function(response) { return response['root_type']; },
  getResponseParentId: function(response) { return response['parent_id']; },
  getResponseParentName: function(response) { return response['parent_name']; },
  getResponseParentType: function(response) { return response['parent_type']; },
  getResponseLibraryId: function(response) { return response['library_id']; },
  getResponseLibraryName: function(response) { return response['library_name']; },
  getResponseLibraryType: function(response) { return response['library_type']; },
  getResponseUserId: function(response) { return response['userid']; },
  getResponseUserName: function(response) { return response['username']; },
  getResponseUserType: function(response) { return response['usertype']; },
  getResponseUserTitle: function(response) { return response['usertitle']; },
  getResponseHostname: function(response) { return response['hostname']; },
  getResponseTotalCount: function(response) { return response['total_count']; },
  getResponseSectionFrom: function(response) { return response['section_from']; },
  getResponseSectionCount: function(response) { return response['section_count']; },
  getResponseSubCount: function(response) { return response['subcount']; },
  getResponseSubLength: function(response) { return response['sublength']; },
  getResponseSections: function(response) { return response['sections']; },
  
  getResponseGroups: function(response) { return response['groups']; },
  getGroupName: function(group) { return group['name']; },
  getGroupTitle: function(group) { return group['title']; },
  
  getResponseSorts: function(response) { return response['sorts']; },
  getSortName: function(sort) { return sort['name']; },
  getSortTitle: function(sort) { return sort['title']; },
  getSortSorted: function(sort) { return sort['sorted']; },
  
  getResponseSectionDetails: function(response) { return response['details']; },
  getMediaDetails: function(details) { return details['media']; },
  
  getFileDetails: function(details) { return details['file']; },
  getFileDetailsName: function(filedetails) { return filedetails['name']; },
  getFileDetailsIsfolder: function(filedetails) { return filedetails['isfolder']; },
  getFileDetailsContentType: function(filedetails) { return filedetails['type']; },
  getFileDetailsContentLength: function(filedetails) { return filedetails['length']; },
  getFileDetailsExtensionName: function(filedetails) { return filedetails['extname']; },
  getFileDetailsDatadir: function(filedetails) { return filedetails['path']; },
  getFileDetailsCreatedTime: function(filedetails) { return filedetails['ctime']; },
  getFileDetailsModifiedTime: function(filedetails) { return filedetails['mtime']; },
  getFileDetailsIndexedTime: function(filedetails) { return filedetails['itime']; },
  getFileDetailsSubCount: function(filedetails) { return filedetails['subcount']; },
  getFileDetailsSubLength: function(filedetails) { return filedetails['sublen']; },
  getFileDetailsPermissions: function(filedetails) { return filedetails['perms']; },
  
  getLibraryId: function(library) { return library['id']; },
  getLibraryName: function(library) { return library['name']; },
  getLibraryContentType: function(library) { return library['type']; },
  getLibraryHostname: function(library) { return library['hostname']; },
  
  getSectionUri: function(section) { return section['uri']; },
  getSectionId: function(section) { return section['id']; },
  getSectionName: function(section) { return section['name']; },
  getSectionContentType: function(section) { return section['type']; },
  getSectionExtensionName: function(section) { return section['extname']; },
  getSectionPoster: function(section) { return section['poster']; },
  getSectionBackground: function(section) { return section['background']; },
  getSectionPath: function(section) { return section['path']; },
  getSectionIsfolder: function(section) { return section['isfolder']; },
  getSectionCreatedTime: function(section) { return section['ctime']; },
  getSectionModifiedTime: function(section) { return section['mtime']; },
  getSectionIndexedTime: function(section) { return section['itime']; },
  getSectionWidth: function(section) { return section['width']; },
  getSectionHeight: function(section) { return section['height']; },
  getSectionTimeLength: function(section) { return section['timelen']; },
  getSectionLength: function(section) { return section['length']; },
  getSectionSubCount: function(section) { return section['subcount']; },
  getSectionSubLength: function(section) { return section['sublen']; },
  getSectionOwner: function(response) { return response['owner']; },
  
  beforeFetchSections: function() {},
  afterFetchSections: function() {},
  getFilterContents: function(response, filterContent) {},
  
  getSectionListUrl: function( sortby, refresh ) { 
    var query = listsection.query_param;
    var q = query; //encodeURIComponent(query);
    
    var params = '&byfolder=' + listsection.listbyfolder;
    //if (refresh) params += '&action=refresh';
    
    listsection.sort_param = '';
    if (sortby) listsection.sort_param = sortby;
    params = '&sort=' + listsection.sort_param + params;
    
    var sectionid = listsection.id_param;
    if (sectionid == null) sectionid = ''
    if (q == null) q = '';
    
    params = '&id=' + sectionid + '&q=' + q + params;
    
    var request_address = listsection.request_address;
    var request_path = app.base_path;
    
    if (request_address != null && request_address.length > 0)
      request_path = request_address + request_path;
    
    var url = request_path + '/section?token=' + app.token + params + '&wt=json';
    
    return url;
  },
  getSectionListUrlMore: function( from ) { 
    var query = listsection.query_param;
    var q = query; //encodeURIComponent(query);
    
    var params = '&byfolder=' + listsection.listbyfolder;
    params = '&sort=' + listsection.sort_param + '&from=' + from + params;
    
    var sectionid = listsection.id_param;
    if (sectionid == null) sectionid = ''
    if (q == null) q = '';
    
    params = '&id=' + sectionid + '&q=' + q + params;
    
    var request_address = listsection.request_address;
    var request_path = app.base_path;
    
    if (request_address != null && request_address.length > 0)
      request_path = request_address + request_path;
    
    var url = request_path + '/section?token=' + app.token + params + '&wt=json';
    
    return url;
  }
};

// #/~browse
sammy.get
(
  /// /^#\/(~browse)$/,
  new RegExp( '(~browse)\\/' ),
  function( context )
  {
    sectioncb = browsecb_impl;
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(10);
    var id_param = path_param;
    var query_param = '';
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) { 
        id_param = path_param.substring(0, pos);
        query_param = path_param.substring(pos+1);
      }
    }

    if (id_param == 'all') {
      if (query_param == null || query_param.length == 0)
        query_param = encodeURIComponent('*:*');
    }

    if (globalApp.go_storage_library( id_param ) == true)
      return;

    section_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      sidebar.shown ? 'tpl/section2.html' : 'tpl/section.html',
      function( template )
      {
        body_element
          .html( template );
        
        var sectionlist_element = $( '#section-list' );
        var sectiontitle_element = $( '.section-list-title' );
        var sectioncount_element = $( '.well-header-count' );

        var sharename_element = $( '#share-button-name' );
        var emptyname_element = $( '#empty-button-name' );
        var deletename_element = $( '#delete-button-name' );
        var movename_element = $( '#move-button-name' );
        var copyname_element = $( '#copy-button-name' );
        var selallname_element = $( '#selall-button-name' );

        var sharebutton_element = $( '#share-button' );
        var emptybutton_element = $( '#empty-button' );
        var deletebutton_element = $( '#delete-button' );
        var movebutton_element = $( '#move-button' );
        var copybutton_element = $( '#copy-button' );
        var selallbutton_element = $( '#selall-button' );
        
        var selectbutton_element = $( '#select-button' );
        var editbutton_element = $( '#edit-button' );
        var refreshbutton_element = $( '#refresh-button' );
        
        var section_title = strings( 'My Library' );
        var refreshbutton_title = strings( 'Refresh' );
        
        if (id_param == 'all')
          $( '#edit-list-item' ).addClass( 'hide' );
        else
          $( '#edit-list-item' ).removeClass( 'hide' );
        
        title_element
          .html( section_title.esc() );

        editbutton_element
          .attr( 'title', strings( 'Edit' ) )
          .attr( 'onclick', 'javascript:listsection.edit();return false;');

        refreshbutton_element
          .attr( 'title', refreshbutton_title.esc() )
          .attr( 'onclick', 'javascript:listsection.scan();return false;');

        selectbutton_element
          .attr( 'onclick', 'javascript:listsection.select();return false;')
          .attr( 'title', strings( 'Select Items' ).esc() );

        selallbutton_element
          .attr( 'onclick', 'javascript:listsection.select_all();return false;')
          .attr( 'title', strings( 'Select All Items' ).esc() );

        copybutton_element
          .attr( 'onclick', 'javascript:listsection.copy_selected();return false;')
          .attr( 'title', strings( 'Copy Items' ).esc() );

        movebutton_element
          .attr( 'onclick', 'javascript:listsection.move_selected();return false;')
          .attr( 'title', strings( 'Move Items' ).esc() );

        deletebutton_element
          .attr( 'onclick', 'javascript:listsection.delete_selected();return false;')
          .attr( 'title', strings( 'Delete Items' ).esc() );

        emptybutton_element
          .attr( 'onclick', 'javascript:listsection.empty_folder();return false;')
          .attr( 'title', strings( 'Empty Folder' ).esc() );

        sharebutton_element
          .attr( 'onclick', 'javascript:listsection.share_selected();return false;')
          .attr( 'title', strings( 'Share Items' ).esc() );

        selallname_element
          .html( strings( 'Select All' ).esc() );

        copyname_element
          .html( strings( 'Copy' ).esc() );

        movename_element
          .html( strings( 'Move' ).esc() );

        deletename_element
          .html( strings( 'Delete' ).esc() );

        emptyname_element
          .html( strings( 'Empty' ).esc() );

        sharename_element
          .html( strings( 'Share' ).esc() );

        sidebar.init();

        listsection.request_address = null; 
        listsection.id_param = id_param;
        listsection.query_param = query_param;
        listsection.showlist(false);
        
        statusbar.show();
      }
    );
  }
);