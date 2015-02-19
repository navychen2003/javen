
var searchcb_impl = {
  library_id: null,
  library_name: null,
  library_type: null,
  library_hostname: null,
  
  getResponseSectionId: function(response) { return ''; },
  getResponseSectionName: function(response) { return this.getSearchQuery(); },
  getResponseSectionType: function(response) { return 'application/x-search'; },
  getResponseSectionPermissions: function(response) { return ''; },
  getResponseSectionOperations: function(response) { return ''; },
  getResponseRootId: function(response) { return ''; },
  getResponseRootName: function(response) { return ''; },
  getResponseRootType: function(response) { return ''; },
  getResponseParentId: function(response) { return ''; },
  getResponseParentName: function(response) { return ''; },
  getResponseParentType: function(response) { return ''; },
  getResponseLibraryId: function(response) { return this.library_id; },
  getResponseLibraryName: function(response) { return this.library_name; },
  getResponseLibraryType: function(response) { return this.library_type; },
  getResponseUserId: function(response) { return ''; },
  getResponseUserName: function(response) { return ''; },
  getResponseUserType: function(response) { return ''; },
  getResponseUserTitle: function(response) { return ''; },
  getResponseHostname: function(response) { return this.library_hostname; },
  getResponseTotalCount: function(response) { return response['response']['numFound']; },
  getResponseSectionFrom: function(response) { return response['response']['start']; },
  getResponseSectionCount: function(response) { return response['response']['section_count']; },
  getResponseSubCount: function(response) { return 0; },
  getResponseSubLength: function(response) { return 0; },
  getResponseSections: function(response) { return response['response']['docs']; },
  
  getResponseGroups: function(response) { return this.getSearchGroups(); },
  getGroupName: function(group) { return group['name']; },
  getGroupTitle: function(group) { return group['title']; },
  
  getResponseSorts: function(response) { return this.getSearchSorts(); },
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
  getSectionName: function(section) { return section['title']; },
  getSectionContentType: function(section) { return section['content_type']; },
  getSectionExtensionName: function(section) { return section['extname']; },
  getSectionPoster: function(section) { return section['poster']; },
  getSectionBackground: function(section) { return section['background']; },
  getSectionPath: function(section) { return section['path']; },
  getSectionIsfolder: function(section) { return section['isfolder']; },
  getSectionCreatedTime: function(section) { return section['updated']; },
  getSectionModifiedTime: function(section) { return section['updated']; },
  getSectionIndexedTime: function(section) { return section['timestamp']; },
  getSectionWidth: function(section) { return section['width']; },
  getSectionHeight: function(section) { return section['height']; },
  getSectionTimeLength: function(section) { return section['timelen']; },
  getSectionLength: function(section) { return section['length']; },
  getSectionSubCount: function(section) { return section['subcount']; },
  getSectionSubLength: function(section) { return section['sublen']; },
  getSectionOwner: function(response) { return 'owner'; },
  
  beforeFetchSections: function() { 
    var id = this.library_id;
    if (id == null) return;
    
    $.ajax
    (
      {
        url : app.base_path + '/library?token=' + app.token + '&id=' + id + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var library = response['library'];
          
          searchcb_impl.library_name = library['name'];
          searchcb_impl.library_type = library['type'];
          searchcb_impl.library_hostname = library['hostname'];
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
  afterFetchSections: function() {
    var searchinput_element = $( '#search-input' );
    
    var value = this.getSearchQuery();
    
    searchinput_element
      .attr( 'value', value );
  },
  getFilterContents: function(response, filterContent) {
    var facet_fields = response['facet_counts']['facet_fields'];
    var id_param = listsection.id_param;
    
    for (var field in facet_fields) { 
      var facets = facet_fields[field];
      var content = [];
      
      for (var i=0; i < facets.length; i=i+2) { 
        var name = facets[i];
        var count = facets[i+1];
        if (count <= 0) continue;
        
        var title = name + ' (' + count + ')';
        
        var query = field + ':"' + name + '"';
        var q = encodeURIComponent(query);
        
        var href = 'href="#/~search/' + id_param + '/' + q + '"';
        var item = '		<li><a class="gray filter-toggle" data-focus="keyboard" ' + href + '>' + title.esc() + '</a></li>';
        
        content.push( item );
      }
      
      if (content.length == 0)
        continue;
      
      filterContent.push( listsection.getfilteritem(
        field, strings(field).esc(), content.join( "\n" )) );
    }
  },
  getSectionListUrl: function( sortby, refresh ) { 
    var query = listsection.query_param;
    var q = query; //encodeURIComponent(query);
    
    var fq = '';
    var id_param = listsection.id_param;
    if (id_param && id_param != 'all') {
      fq = 'library:' + id_param;
      fq = encodeURIComponent(fq);
    }
    
    var sort = '';
    if (sortby) { 
      sortby = sortby.replace('\.', ' ');
      sort = encodeURIComponent(sortby);
    }
    
    var facet = '&facet=true&facet.field=make_s&facet.field=model_s';
    
    var request_address = listsection.request_address;
    var request_path = app.index_path;
    
    if (request_address != null && request_address.length > 0)
      request_path = request_address + request_path;
    
    var url = request_path + '/select?q=' + q + '&fq=' + fq + '&sort=' + sort + facet + '&start=0&rows=20&wt=json';
    
    return url;
  },
  getSectionListUrlMore: function( from ) { 
    var query = listsection.query_param;
    var q = query; //encodeURIComponent(query);
    
    var fq = '';
    var id_param = listsection.id_param;
    if (id_param && id_param != 'all') {
      fq = 'library:' + id_param;
      fq = encodeURIComponent(fq);
    }
    
    var sort = '';
    var sortby = listsection.sort_param;
    if (sortby) { 
      sortby = sortby.replace('\.', ' ');
      sort = encodeURIComponent(sortby);
    }
    
    var request_address = listsection.request_address;
    var request_path = app.index_path;
    
    if (request_address != null && request_address.length > 0)
      request_path = request_address + request_path;
    
    var url = request_path + '/select?q=' + q + '&fq=' + fq + '&sort=' + sort + '&start=' + from + '&rows=20&wt=json';
    
    return url;
  },
  getSearchQuery: function() { 
    var value = listsection.query_param;
    if (value == null) value = '';
    
    value = decodeURIComponent(value);
    
    return value;
  },
  getSearchGroups: function() { 
    var groups = [];
    var group = {};
    
    group['name'] = 'all';
    group['title'] = 'All';
    groups.push(group);
    
    return groups;
  },
  getSearchSorts: function() { 
    var sorts = [];
    
    sorts.push(this.newSearchSort('updated', 'Date Updated'));
    sorts.push(this.newSearchSort('taken', 'Date Taken'));
    sorts.push(this.newSearchSort('title', 'Name'));
    sorts.push(this.newSearchSort('length', 'Size'));
    sorts.push(this.newSearchSort('score', 'Rating'));
    
    return sorts;
  },
  newSearchSort: function( name, title ) { 
    var sort_param = listsection.sort_param;
    var sort = {};
    
    sort['name'] = name;
    sort['title'] = title;
    
    if (sort_param && sort_param.indexOf( name ) >= 0)
      sort['sorted'] = sort_param;
    
    return sort;
  }
};

// #/~search
sammy.get
(
  /// /^#\/(~search)$/,
  new RegExp( '(~search)\\/' ),
  function( context )
  {
    sectioncb = searchcb_impl;
    if (init_page(context) == false) return;
    
    searchcb_impl.library_id = null;
    searchcb_impl.library_name = null;
    searchcb_impl.library_type = null;
    searchcb_impl.library_hostname = null;
    
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
        
        if (id_param != null && id_param != 'all')
          searchcb_impl.library_id = id_param;
      }
    }

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
        
        $( '#edit-list-item' ).addClass( 'hide' );
        
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
          .attr( 'title', strings( 'Select Ttems' ).esc() );

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
        listsection.id_param = 'all'; //id_param;
        listsection.query_param = query_param;
        listsection.sort_param = 'score.desc';
        listsection.showlist(false);
        
        statusbar.show();
      }
    );
  }
);