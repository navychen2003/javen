
var listsection = {
  request_address: null,
  id_param: null,
  query_param: null,
  sort_param: null,
  section: null,
  section_id: null,
  section_type: null,
  section_name: null,
  permissions: null,
  operations : null,
  parent_id: null,
  parent_type: null,
  parent_name: null,
  root_id: null,
  root_type: null,
  root_name: null,
  library_id: null,
  library_type: null,
  library_name: null,
  sections: [],
  section_list: [],
  slidephotos: [],
  listtype: null,
  listbyfolder: true,
  selectmode: null,
  total_count: 0,
  sub_count: 0,
  sub_length: 0,
  fetched_from: 0,
  fetched_count: 0,
  fetching: false,
  clicking: 0,
  
  is_shown: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('browse') >= 0 || 
        path.indexOf('search') >= 0) {
      return true;
    }
    
    return false;
  },
  can_read: function()
  {
    return this.can_permission('r');
  },
  can_write: function()
  {
    return this.can_permission('w');
  },
  can_delete: function()
  {
    return this.can_permission('d');
  },
  can_move: function()
  {
    return this.can_permission('m');
  },
  can_copy: function()
  {
    return this.can_permission('c');
  },
  support_copy: function()
  {
    return this.support_operation('c');
  },
  support_move: function()
  {
    return this.support_operation('m');
  },
  support_delete: function()
  {
    return this.support_operation('d');
  },
  support_upload: function()
  {
    return this.support_operation('u');
  },
  support_newfolder: function()
  {
    return this.support_operation('n');
  },
  support_empty: function()
  {
    return this.support_operation('e');
  },
  support_select: function()
  {
    return this.support_delete() || this.support_move() || this.support_copy();
  },
  support_share: function()
  {
    var root_type = this.root_type;
    if (root_type == 'application/x-share-root')
      return true;
    
    return false;
  },
  can_permission: function( perm )
  {
    var perms = this.permissions;
    if (perms != null && perms.indexOf(perm) >= 0)
      return true;
    return false;
  },
  support_operation: function( op )
  {
    var ops = this.operations;
    if (ops != null && ops.indexOf(op) >= 0)
      return true;
    return false;
  },
  getmorefrom: function()
  {
    var fetchfrom = this.fetched_from + this.fetched_count;
  
    if (fetchfrom < this.total_count)
      return fetchfrom;
    
    return -1;
  },
  showmore: function( from )
  {
    this.showmorecb( from, null );
  },
  showmorecb: function( from, callback )
  {
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    
    var sectionscroll_element = $( '#section-scroll' );
    var sectionlist_element = $( '#section-list' );
    var sectiontitle_element = $( '.section-list-title' );
    var sectioncount_element = $( '.well-header-count' );
    
    var moreurl = sectioncb.getSectionListUrlMore( from );
    
    $.ajax
    (
      {
        url : moreurl, //app.base_path + '/section?id=' + sectionid + params + '&wt=json',
        dataType : 'json',
        context : $( '#section-list', body_element ),
        beforeSend : function( xhr, settings )
        {
          listsection.fetching = true;
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var total_count = sectioncb.getResponseTotalCount(response); //response['total_count'];
          var section_from = sectioncb.getResponseSectionFrom(response); //response['section_from'];
          var section_count = sectioncb.getResponseSectionCount(response); //response['section_count'];
          
          listsection.total_count = total_count;
          listsection.fetched_from = section_from;
          listsection.fetched_count = section_count;
          
          var sections = sectioncb.getResponseSections(response); //response['sections'];
          var sectionContent = listsection.buildContent( sections );
          
          sectionlist_element
            .append( sectionContent.join( "\n" ) );
          
        },
        error : function( xhr, text_status, error_thrown)
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function( xhr, text_status )
        {
          listsection.fetching = false;
          hide_loading();
          
          if (callback)
            callback.call( this );
        }
      }
    );
  },
  showlist: function( refresh )
  {
    var sortby = '';
    if (this.sort_param) sortby = this.sort_param;
    this.showlistsortby( sortby, refresh );
  },
  getfilteritem: function( name, title, list ) { 
    return '	<h2 class="sidebar-header sort-header">' + title.esc() + '</h2>' + "\n" + 
           '	<ul class="sidebar-list" id="' + name + '-list">' + list + '</ul>';
  },
  showsectioninfo: function()
  {
    var section = this.section;
    if (section == null) return;
    
    fileinfo.showdetails( section );
  },
  showlistsortby: function( sortby, refresh )
  {
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var backlink_element = $( '#back-link' );
    
    var sectionscroll_element = $( '#section-scroll' );
    var sectionlist_element = $( '#section-list' );
    var sectiontitle_element = $( '.section-list-title' );
    var sectioncount_element = $( '.well-header-count' );

    var selectbutton_element = $( '#select-button' );
    var editbutton_element = $( '#edit-button' );
    var refreshbutton_element = $( '#refresh-button' );
    
    var grouptitle_element = $( '#group-title' );
    var grouplist_element = $( '#group-list' );
    var filter_element = $( '#filter-container' );
    
    this.sort_param = '';
    if (sortby) this.sort_param = sortby;
    
    this.section = null;
    this.slidephotos = [];
    
    var listurl = sectioncb.getSectionListUrl( sortby, refresh );
    
    $.ajax
    (
      {
        url : listurl, //app.base_path + '/section?id=' + sectionid + params + '&wt=json',
        dataType : 'json',
        context : $( '#section-list', body_element ),
        beforeSend : function( xhr, settings )
        {
          listsection.fetching = true;
          show_loading();
          
          listlibrary.initlist( function() { 
              navbar.init_title( listsection.library_id );
            });
          sectioncb.beforeFetchSections();
        },
        success : function( response, text_status, xhr )
        {
          var section_id = sectioncb.getResponseSectionId(response); //response['id'];
          var section_name = sectioncb.getResponseSectionName(response); //response['name'];
          var section_type = sectioncb.getResponseSectionType(response); //response['type'];
          var section_perms = sectioncb.getResponseSectionPermissions(response); //response['perms'];
          var section_ops = sectioncb.getResponseSectionOperations(response); //response['ops'];
          
          var root_id = sectioncb.getResponseRootId(response); //response['root_id'];
          var root_name = sectioncb.getResponseRootName(response); //response['root_name'];
          var root_type = sectioncb.getResponseRootType(response); //response['root_type'];
          
          var parent_id = sectioncb.getResponseParentId(response); //response['parent_id'];
          var parent_name = sectioncb.getResponseParentName(response); //response['parent_name'];
          var parent_type = sectioncb.getResponseParentType(response); //response['parent_type'];
          
          var library_id = sectioncb.getResponseLibraryId(response); //response['library_id'];
          var library_name = sectioncb.getResponseLibraryName(response); //response['library_name'];
          var library_type = sectioncb.getResponseLibraryType(response); //response['library_type'];
          var library_hostname = sectioncb.getResponseHostname(response); //response['hostname'];
          
          var userid = sectioncb.getResponseUserId(response); //response['userid'];
          var username = sectioncb.getResponseUserName(response); //response['username'];
          var usertype = sectioncb.getResponseUserType(response); //response['usertype'];
          var usertitle = sectioncb.getResponseUserTitle(response); //response['usertitle'];
          
          var total_count = sectioncb.getResponseTotalCount(response); //response['total_count'];
          var section_from = sectioncb.getResponseSectionFrom(response); //response['section_from'];
          var section_count = sectioncb.getResponseSectionCount(response); //response['section_count'];
          var sub_count = sectioncb.getResponseSubCount(response); //response['subcount'];
          var sub_length = sectioncb.getResponseSubLength(response); //response['sublength'];
          
          if (parent_id == null || parent_id.length == 0) {
            var me = globalApp.get_username();
            var hostlocation = system.get_hostlocation();
            if (hostlocation == null) hostlocation = '';
            if (username == null || username.length == 0 || me == username) {
              headbar.backlinkto = hostlocation + '#/~dashboard';
            } else if (userid != null && userid.length > 0) {
              var lastchr = userid.charAt(userid.length-1);
              if (lastchr == '1')
                headbar.backlinkto = '#/~group/' + encodeURIComponent(username);
              else
                headbar.backlinkto = '#/~user/' + encodeURIComponent(username);
            } else {
              headbar.backlinkto = hostlocation + '#/~dashboard';
            }
          } else {
            headbar.backlinkto = '#/~browse/' + parent_id;
          }
          
          if (library_name == null) library_name = '';
          if (library_hostname == null) library_hostname = '';
          
          if (section_name == null) section_name = '';
          var selectmode = null;
          
          if (library_id == null) library_id = '';
          if (library_type == null) library_type = '';
          if (library_type.indexOf('search') >= 0) {
            library_name = strings( 'Search Results' );
          }
          
          if (library_id != null && library_id != '') {
            if (library_type.indexOf('image') >= 0)
              section_title = strings( 'My Photo: ' ) + library_name + ' (' + library_hostname + ')';
            else
              section_title = strings( 'My Library: ' ) + library_name + ' (' + library_hostname + ')';
          } else { 
            section_title = strings( 'My Library' );
            if (system.friendlyName != null)
              section_title = section_title + ' (' + system.friendlyName + ')';
          }
          
          listsection.total_count = total_count;
          listsection.fetched_from = section_from;
          listsection.fetched_count = section_count;
          listsection.sub_count = sub_count;
          listsection.sub_length = sub_length;
          
          listsection.section = response;
          listsection.section_type = section_type;
          listsection.section_name = section_name;
          listsection.section_id = section_id;
          listsection.permissions = section_perms;
          listsection.operations = section_ops;
          
          listsection.root_id = root_id;
          listsection.root_name = root_name;
          listsection.root_type = root_type;
          listsection.parent_id = parent_id;
          listsection.parent_name = parent_name;
          listsection.parent_type = parent_type;
          
          listsection.library_type = library_type;
          listsection.library_name = library_name;
          listsection.library_id = library_id;
          searchform.library_id = library_id;
          
          if (section_type != null) { 
            section_name = listsection.getSectionName();
            if (listsection.support_upload()) {
              $( '#actionbar-upload' ).removeClass( 'hide' );
              //$( '#actionbar-import' ).removeClass( 'hide' );
            }
            if (listsection.support_newfolder())
              $( '#actionbar-newfolder' ).removeClass( 'hide' );
            if (listsection.support_select()) 
              selectmode = 'enable';
          }
          
          title_element
            .html( section_title.esc() );
          
          grouptitle_element
            .html( section_name.esc() ); 
          
          //sorttitle_element
          //  .html( strings( 'SORT' ).esc() );
          
          sectiontitle_element
            .html( section_name.esc() );
          
          sectioncount_element
            .html( total_count );
          
          var sectionpath = null;
          var sectionmedia = null;
          
          if (parent_id != null && parent_id.length > 0 && parent_id != library_id) {
            if (parent_name != null && parent_name.length > 0) {
              var parentLink = '#/~browse/' + parent_id;
              var displayName = listsection.toDisplaySectionName(parent_name, parent_type);
              sectionpath = {id:parent_id, name:displayName, type:parent_type, link:parentLink};
            }
          }
          
          if (section_id != null && section_id.length > 0 && section_id != library_id) {
            if (section_name != null && section_name.length > 0) {
              var sectionClick = 'javascript:listsection.showsectioninfo();return false;';
              var displayName = listsection.toDisplaySectionName(section_name, section_type);
              sectionmedia = {id:section_id, name:displayName, type:section_type, click:sectionClick};
            }
          }
          
          var sectionlib = {id:library_id, type:library_type, name:library_name, username:username, usertype:usertype, usertitle:usertitle, hostname:library_hostname};
          navbar.init_title0( sectionlib, sectionpath, sectionmedia );
          
          var groups = sectioncb.getResponseGroups(response); //response['groups'];
          var groupContent = [];
          
          for (var i=0; groups != null && i < groups.length; i++) { 
            var group = groups[i];
            
            var name = sectioncb.getGroupName(group); //group['name'];
            var title = sectioncb.getGroupTitle(group); //group['title'];
            
            if (title == null) title = '';
            title = strings( title );
            
            var onclick = 'javascript:return false;';
            
            var item = '	<li><a class="gray filter-toggle " data-focus="keyboard" href="" onclick="' + onclick + '">' + title.esc() + '</a></li>';
            groupContent.push( item );
          }
          
          grouplist_element
            .html( groupContent.join( "\n" ) );
          
          var filterContent = [];
          
          var sorts = sectioncb.getResponseSorts(response); //response['sorts'];
          var sortContent = [];
          
          for (var i=0; sorts != null && i < sorts.length; i++) { 
            var sort = sorts[i];
            
            var name = sectioncb.getSortName(sort); //sort['name'];
            var title = sectioncb.getSortTitle(sort); //sort['title'];
            var sorted = sectioncb.getSortSorted(sort); //sort['sorted'];
            
            if (name == null) name = '';
            if (title == null) title = '';
            title = strings( title );
            
            var onclick = 'onclick="javascript:listsection.showlistsortby(\'' + name.esc() + '.asc\',true);return false;"';
            
            var sortedIcon = '';
            if (sorted) { 
              if (sorted.indexOf('.desc') > 0)
                sortedIcon = ' <i class="glyphicon-caret small"></i>';
              else {
                sortedIcon = ' <i class="glyphicon-caret reverse small"></i>';
                onclick = 'onclick="javascript:listsection.showlistsortby(\'' + name.esc() + '.desc\',true);return false;"';
              }
            }
            
            var item = '		<li><a class="gray filter-toggle" data-focus="keyboard" href="" ' + onclick + '>' + title.esc() + sortedIcon + '</a></li>';
            sortContent.push( item );
          }
          
          if (sortContent.length > 0) {
            //sortlist_element
            //  .html( sortContent.join( "\n" ) );
          
            filterContent.push( listsection.getfilteritem(
              'sort', strings( 'SORT' ).esc(), sortContent.join( "\n" )) );
          }
          
          sectioncb.getFilterContents(response, filterContent);
          
          filter_element
            .html( filterContent.join( "\n" ) );
          
          listsection.slidephotos = [];
          listsection.sections = [];
          listsection.section_list = [];
          
          var sections = sectioncb.getResponseSections(response); //response['sections'];
          var sectionContent = listsection.buildContent( sections );
          
          listsection.init_select( selectmode );
          
          sectionlist_element
            .html( sectionContent.join( "\n" ) );
          
          $(document).ready( function() { 
                sectionscroll_element.scroll( function() {
                  listsection.scroll_callback( sectionscroll_element, null );
                });
              });
          
        },
        error : function( xhr, text_status, error_thrown)
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function( xhr, text_status )
        {
          listsection.fetching = false;
          hide_loading();
          
          sectioncb.afterFetchSections();
          listsection.scroll_more( sectionscroll_element );
        }
      }
    );
  },
  scroll_callback: function( sectionscroll_element, cb )
  {
    if (sectionscroll_element == null || sectionscroll_element[0] == null)
      return false;
    
    var divHeight = sectionscroll_element.height();
    var scrollHeight = sectionscroll_element[0].scrollHeight;
    var scrollTop = sectionscroll_element[0].scrollTop;
  
    if (scrollTop + divHeight >= scrollHeight) {
      if (listsection.fetching == false) {
        var fetchfrom = listsection.getmorefrom();
      
        if (fetchfrom > 0) {
          listsection.showmore( fetchfrom, cb );
          return true;
        }
      }
    }
    
    return false;
  },
  scroll_more: function( sectionscroll_element )
  {
    listsection.scroll_callback( sectionscroll_element, function() {
          var divHeight = sectionscroll_element.height();
          var scrollHeight = sectionscroll_element[0].scrollHeight;
          var scrollTop = sectionscroll_element[0].scrollTop;
  
          if (scrollTop + divHeight >= scrollHeight) 
            listsection.scroll_more( sectionscroll_element );
        });
  },
  relist: function()
  {
    if (listsection.sections)
    {
      var sectionlist_element = $( '#section-list' );
      var sectionContent = [];
      
      photoslide.stop();
      listsection.slidephotos = [];
      listsection.section_list = [];
      
      for (var key in listsection.sections) {
        var section = listsection.sections[key];
        var item = this.buildItem( section );
        sectionContent.push( item );
      }
      
      sectionlist_element
        .html( sectionContent.join( "\n" ) );
    }
  },
  buildContent: function( sections )
  {
    var sectionContent = [];
    var sectionCount = 0;
  
    for (var key in sections) { 
      var section = sections[key];
      var item = this.buildItem( section );

      listsection.sections.push( section );
      sectionContent.push( item );
      sectionCount ++;
    }
    
    if (this.fetched_count == null) 
      this.fetched_count = sectionCount;
    
    return sectionContent;
  },
  clickitem: function( idx )
  {
    if (listsection.clicking == 1) {
      this.edititem( idx );
      return;
    } else if (listsection.clicking == 2) { 
      this.showinfo( idx );
      return;
    } else if (listsection.clicking == 3) {
      this.playitem( idx );
      return;
    } else if (listsection.clicking == 4) {
      this.shareitem( idx );
      return;
    }
    
    listsection.clicking = 0;
    
    var mode = this.selectmode;
    if (this.section_list && idx >= 0 && idx < this.section_list.length) { 
      var section = this.section_list[idx];
      if (section) {
        var sec_id = sectioncb.getSectionId(section); //section['id'];
        var isfolder = sectioncb.getSectionIsfolder(section); //section['isfolder'];
        var contentType = sectioncb.getSectionContentType(section); //section['type'];
        var openlink = section['openlink'];
        var slideIndex = section['slideIndex'];
        
        if (contentType == null) contentType = '';
        
        if (mode == 'selecting') {
          var itemid = section['itemIndex'];
          var selected = section['selected'];
          if (selected == null) selected = false;
          var newselected = !selected;
          section['selected'] = newselected;
          if (itemid >= 0) { 
            var sectionlink_element = $( '#section-link-' + itemid );
            if (newselected)
              sectionlink_element.addClass( 'selected' );
            else
              sectionlink_element.removeClass( 'selected' );
          }
          this.update_title();
          
        } else if (isfolder) { 
          var context = system.context;
          context.redirect( openlink );
          //window.open( openlink );
          
        } else if (slideIndex >= 0) {
          photoslide.show( listsection.slidephotos, slideIndex, true );
          
        } else {
          if (contentType.indexOf('audio/') == 0) { 
            openlink = '#/~details/' + sec_id;
          } else if (contentType.indexOf('video/') == 0) {
            openlink = '#/~details/' + sec_id;
          } else {
            openlink = '#/~details/' + sec_id;
          }
          var context = system.context;
          context.redirect( openlink );
          //fileinfo.showdetails( section );
        }
      }
    }
  },
  edititem: function( idx )
  {
    listsection.clicking = 0;
    
    var mode = this.selectmode;
    if (this.section_list && idx >= 0 && idx < this.section_list.length) { 
      var section = this.section_list[idx];
      if (section) {
        var sec_id = sectioncb.getSectionId(section); //section['id'];
        var isfolder = sectioncb.getSectionIsfolder(section); //section['isfolder'];
        var contentType = sectioncb.getSectionContentType(section); //section['type'];
        var openlink = section['openlink'];
        var slideIndex = section['slideIndex'];
        
        if (contentType == null) contentType = '';
        
        if (isfolder) { 
          openlink = '#/~edit/' + sec_id;
          
          var context = system.context;
          context.redirect( openlink );
        } else {
          openlink = '#/~edit/' + sec_id;
          
          var context = system.context;
          context.redirect( openlink );
        }
      }
    }
  },
  playitem: function( idx )
  {
    listsection.clicking = 0;
    
    var mode = this.selectmode;
    if (this.section_list && idx >= 0 && idx < this.section_list.length) { 
      var section = this.section_list[idx];
      if (section) {
        var sec_id = sectioncb.getSectionId(section); //section['id'];
        var isfolder = sectioncb.getSectionIsfolder(section); //section['isfolder'];
        var contentType = sectioncb.getSectionContentType(section); //section['type'];
        
        if (contentType == null) contentType = '';
        
        if (isfolder == false) { 
          if (contentType.indexOf('audio/') == 0) { 
            if (musicplayer) {
              musicplayer.playid( sec_id );
              if (mode == 'selecting') {
                for (var key in this.section_list) {
                  var sec = this.section_list[key];
                  var id = sectioncb.getSectionId(sec); //section['id'];
                  var type = sectioncb.getSectionContentType(sec); //section['type'];
                  var selected = sec['selected'];
                  if (id != null && type != null && selected == true) {
                    if (type.indexOf('audio/') == 0) { 
                      musicplayer.addid( id );
                    }
                  }
                }
              }
              return;
            }
          } else if (contentType.indexOf('video/') == 0) {
            openlink = '#/~play/' + sec_id;
          
            var context = system.context;
            context.redirect( openlink );
          }
        }
      }
    }
  },
  shareitem: function( idx )
  {
    listsection.clicking = 0;
    
    if (this.section_list && idx >= 0 && idx < this.section_list.length) { 
      var section = this.section_list[idx];
      if (section) {
        var items = [];
        items.push( section );
        compose.share( items );
      }
    }
  },
  showinfo: function( idx )
  {
    listsection.clicking = 0;
    
    if (this.section_list && idx >= 0 && idx < this.section_list.length) { 
      var section = this.section_list[idx];
      if (section) {
        fileinfo.showdetails( section );
      }
    }
  },
  buildItem: function( section )
  {
    //var sec_uri = sectioncb.getSectionUri(section); //section['uri'];
    var sec_id = sectioncb.getSectionId(section); //section['id'];
    var sec_name = sectioncb.getSectionName(section); //section['name'];
    var contentType = sectioncb.getSectionContentType(section); //section['type'];
    var extension = sectioncb.getSectionExtensionName(section); //section['extname'];
    var poster = sectioncb.getSectionPoster(section); //section['poster'];
    var background = sectioncb.getSectionBackground(section); //section['background'];
    var path = sectioncb.getSectionPath(section); //section['path'];
    var isfolder = sectioncb.getSectionIsfolder(section); //section['isfolder'];
    //var ctime = sectioncb.getSectionCreatedTime(section); //section['ctime'];
    var mtime = sectioncb.getSectionModifiedTime(section); //section['mtime'];
    //var indextime = sectioncb.getSectionIndexedTime(section); //section['itime'];
    var width = sectioncb.getSectionWidth(section); //section['width'];
    var height = sectioncb.getSectionHeight(section); //section['height'];
    var timelen = sectioncb.getSectionTimeLength(section); //section['timelen'];
    var length = sectioncb.getSectionLength(section); //section['length'];
    var subcount = sectioncb.getSectionSubCount(section); //section['subcount'];
    var sublength = sectioncb.getSectionSubLength(section); //section['sublen'];
    var owner = sectioncb.getSectionOwner(section); //section['owner'];

    var key = sec_id;
    var thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
    var iconclass = 'glyphicon file';
    var linkclass = '';
    var linkto = '';
    var openlink = '';
    var playlink = '';
    var editlink = '';
    var sharelink = '';
    var infolink = '';
    var target = '';

    if (extension == null && path != null) { 
      var pos = path.lastIndexOf('.');
      if (pos >= 0) 
        extension = path.substring(pos+1);
    }

    if (sec_name == null) sec_name = '';
    if (contentType == null) contentType = '';
    if (extension == null || extension.length == 0) extension = 'dat';
    if (isfolder == null) isfolder = false;

    var itemid = listsection.section_list.length;
    listsection.section_list.push( section );
    
    linkto = 'javascript:listsection.clickitem(' + itemid + ');';
    playlink = 'javascript:listsection.playitem(' + itemid + ');return false;';
    editlink = 'javascript:listsection.edititem(' + itemid + ');return false;';
    sharelink = 'javascript:listsection.shareitem(' + itemid + ');return false;';
    infolink = 'javascript:listsection.showinfo(' + itemid + ');return false;';
    
    section['itemIndex'] = itemid;
    section['slideIndex'] = -1;

    if (this.selectmode == 'selecting' && section['selected'] == true)
      linkclass = 'selected';

    var canplay = false;
    var mediaSize = null;
    var clickto = linkto + 'return false;';
    
    playlink = 'javascript:listsection.clicking=3;';
    editlink = 'javascript:listsection.clicking=1;';
    sharelink = 'javascript:listsection.clicking=4;';
    infolink = 'javascript:listsection.clicking=2;';
    
    listsection.clicking = 0;

    if (contentType.indexOf('image/') == 0) {
      iconclass = 'glyphicon picture';
      thumbsrc = app.base_path + '/image/' + sec_id + '_192.' + extension + '?token=' + app.token;
      openlink = app.base_path + '/image/' + sec_id + '_0.' + extension + '?token=' + app.token;
      
      section['slideIndex'] = listsection.slidephotos.length;
      listsection.slidephotos.push( section );
      
      mediaSize = '' + width + ' x ' + height;
    
    } else if (contentType.indexOf('audio/') == 0) { 
      iconclass = 'glyphicon music';
      thumbsrc = 'css/' + app.theme + '/images/posters/audio.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      target = 'target="_blank"';
      canplay = true;
    
    } else if (contentType.indexOf('video/') == 0) { 
      iconclass = 'glyphicon film';
      thumbsrc = 'css/' + app.theme + '/images/posters/video.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      target = 'target="_blank"';
      canplay = true;
    
    } else if (contentType.indexOf('text/') == 0) { 
      iconclass = 'glyphicon file';
      thumbsrc = 'css/' + app.theme + '/images/posters/text.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      target = 'target="_blank"';
    
    } else { 
      iconclass = 'glyphicon file';
      thumbsrc = 'css/' + app.theme + '/images/posters/file.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
    }

    if (isfolder) {
      openlink = '#/~browse/' + key;
      thumbsrc = 'css/' + app.theme + '/images/posters/folder.png';
    }

    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + imgid + '_256t.' + imgext + '?token=' + app.token;
    }

    section['openlink'] = openlink;

    sec_name = this.toDisplaySectionName( sec_name, contentType );

    var lengthText = readableBytes2(length);
    var updated = format_time(mtime);
    //var indexed = format_time(indextime);
    
    var edithide = 'hide';
    var mename = globalApp.get_username();
    if (mename != null && mename == owner) edithide = '';

    if (listsection.listtype == 'compact') {
      var subtitle = '';
      var playhide = 'hide';
      if (canplay == true) playhide = '';
      
      if (isfolder) { 
        var item = '		<li>' + "\n" +
                   '          <a id="section-link-' + itemid + '" href="' + linkto + '" onClick="' + clickto + '" data-focus="keyboard" class="' + linkclass + '" ' + target + '>' + "\n" +
                   '          <div class="media-details">' + "\n" +
                   '          <input type="checkbox" class="multiselect" name="selected">' + "\n" +
                   '          <span class="media-rating rating sorted-by"><span class="rating hide"></span></span>' + "\n" +
                   '          <i class="folder-icon glyphicon folder-open"></i>' + "\n" +
                   '          <span class="media-title title">' + sec_name.esc() + '</span>' + "\n" +
                   '          <span class="media-year subtitle">' + subtitle.esc() + '</span>' + "\n" +
		           '          <div class="media-details-right">' + "\n" +
		           '            <div class="hover-menu">' + "\n" +
			       '              <button type="button" onclick="' + playlink + '" class="hover-menu-btn poster-play-btn ' + playhide + '" tabindex="-1"><i class="glyphicon play"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + editlink + '" class="hover-menu-btn poster-edit-btn ' + edithide + '" tabindex="-1"><i class="glyphicon pencil"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + infolink + '" class="hover-menu-btn poster-info-btn" tabindex="-1"><i class="glyphicon circle-info"></i></button>' + "\n" +
		           '            </div>' + "\n" +
		           '          </div>' + "\n" +
		           '          </div>' + "\n" +
                   '          </a>' + "\n" +
                   '        </li>' + "\n";
    
        return item;
      
      } else { 
        if (mediaSize != null && mediaSize.length > 0) 
          subtitle = mediaSize;
        else if (lengthText != null)
          subtitle = lengthText;
        
        var item = '		<li>' + "\n" +
                   '          <a id="section-link-' + itemid + '" href="' + linkto + '" onClick="' + clickto + '" data-focus="keyboard" class="' + linkclass + '" ' + target + '>' + "\n" +
                   '          <div class="media-details">' + "\n" +
                   '          <input type="checkbox" class="multiselect" name="selected">' + "\n" +
                   '          <span class="media-rating rating sorted-by"><span class="rating hide"></span></span>' + "\n" +
                   '          <i class="folder-icon ' + iconclass + '"></i>' + "\n" +
                   '          <span class="media-title title">' + sec_name.esc() + '</span>' + "\n" +
                   '          <span class="media-year subtitle">' + subtitle.esc() + '</span>' + "\n" +
		           '          <div class="media-details-right">' + "\n" +
		           '            <div class="hover-menu">' + "\n" +
			       '              <button type="button" onclick="' + playlink + '" class="hover-menu-btn poster-play-btn ' + playhide + '" tabindex="-1"><i class="glyphicon play"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + editlink + '" class="hover-menu-btn poster-edit-btn ' + edithide + '" tabindex="-1"><i class="glyphicon pencil"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + sharelink + '" class="hover-menu-btn poster-share-btn" tabindex="-1"><i class="glyphicon share-alt"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + infolink + '" class="hover-menu-btn poster-info-btn" tabindex="-1"><i class="glyphicon circle-info"></i></button>' + "\n" +
		           '            </div>' + "\n" +
		           '          </div>' + "\n" +
		           '          </div>' + "\n" +
                   '          </a>' + "\n" +
                   '        </li>' + "\n";
    
        return item;
      }
    }
    else if (listsection.listtype == 'detail') 
    {
      var updatedTitle = strings( 'Updated' );
      var lengthTitle = strings( 'Length' );
      var playhide = 'hide';
      if (canplay == true) playhide = '';
      
      if (isfolder) { 
        var folderContent = '';
        
        if (subcount != null && subcount > 0) { 
          var countTitle = strings( 'Contains' );
          var countText = strings( '{0} Items' ).format( subcount );
          folderContent += '            <h4 class="media-tagline">' + countTitle.esc() + ': ' + countText.esc() + '</h4>' + "\n";
        }
        if (sublength != null && sublength > 0) { 
          var sublenText = readableBytes2(sublength);
          folderContent += '            <h4 class="media-tagline">' + lengthTitle.esc() + ': ' + sublenText.esc() + '</h4>' + "\n";
        }
        
        var item = '		<li>' + "\n" +
	               '          <a id="section-link-' + itemid + '" href="' + linkto + '" onClick="' + clickto + '" data-focus="keyboard" class="' + linkclass + '" ' + target + '>' + "\n" +
	               '          <div class="poster-container">' + "\n" +
		           '            <img class="poster media-list-poster" src="' + thumbsrc + '">' + "\n" +
	               '          </div>' + "\n" +
	               '          <div class="media-details">' + "\n" +
		           '            <div class="media-details-right">' + "\n" +
		           '            <div class="hover-menu">' + "\n" +
			       '              <button type="button" onclick="' + playlink + '" class="hover-menu-btn poster-play-btn ' + playhide + '" tabindex="-1"><i class="glyphicon play"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + editlink + '" class="hover-menu-btn poster-edit-btn ' + edithide + '" tabindex="-1"><i class="glyphicon pencil"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + infolink + '" class="hover-menu-btn poster-info-btn" tabindex="-1"><i class="glyphicon circle-info"></i></button>' + "\n" +
		           '            </div>' + "\n" +
		           '            <div class="media-rating rating sorted-by"><span class="rating hide"></span></div>' + "\n" +
		           '            </div>' + "\n" +
		           '            <h3>' + "\n" +
			       '              <input type="checkbox" class="multiselect" name="selected">' + "\n" +
			       '              <span class="media-title">' + sec_name.esc() + '</span>' + "\n" +
		           '            </h3>' + "\n" +
		           folderContent +
		           '            <h4 class="media-tagline">' + updatedTitle.esc() + ': ' + updated.esc() + '</h4>' + "\n" +
		           '            <div class="media-summary summary"></div>' + "\n" +
	               '          </div>' + "\n" +
	               '          </a>' + "\n" +
                   '        </li>' + "\n";
    
        return item;
      
      } else { 
        var mediaContent = '';
        
        if (mediaSize != null && mediaSize.length > 0) { 
          var photoTitle = strings( 'Photo' );
          mediaContent += '            <h4 class="media-tagline">' + photoTitle.esc() + ': ' + mediaSize.esc() + '</h4>' + "\n";
        }
        
        var item = '		<li>' + "\n" +
	               '          <a id="section-link-' + itemid + '" href="' + linkto + '" onClick="' + clickto + '" data-focus="keyboard" class="' + linkclass + '" ' + target + '>' + "\n" +
	               '          <div class="poster-container">' + "\n" +
		           '            <img class="poster media-list-poster" src="' + thumbsrc + '">' + "\n" +
	               '          </div>' + "\n" +
	               '          <div class="media-details">' + "\n" +
		           '            <div class="media-details-right">' + "\n" +
		           '            <div class="hover-menu">' + "\n" +
			       '              <button type="button" onclick="' + playlink + '" class="hover-menu-btn poster-play-btn ' + playhide + '" tabindex="-1"><i class="glyphicon play"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + editlink + '" class="hover-menu-btn poster-edit-btn ' + edithide + '" tabindex="-1"><i class="glyphicon pencil"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + sharelink + '" class="hover-menu-btn poster-share-btn" tabindex="-1"><i class="glyphicon share-alt"></i></button>' + "\n" +
			       '              <button type="button" onclick="' + infolink + '" class="hover-menu-btn poster-info-btn" tabindex="-1"><i class="glyphicon circle-info"></i></button>' + "\n" +
		           '            </div>' + "\n" +
		           '            <div class="media-rating rating sorted-by"><span class="rating hide"></span></div>' + "\n" +
		           '            </div>' + "\n" +
		           '            <h3>' + "\n" +
			       '              <input type="checkbox" class="multiselect" name="selected">' + "\n" +
			       '              <span class="media-title">' + sec_name.esc() + '</span>' + "\n" +
		           '            </h3>' + "\n" +
		           mediaContent +
		           '            <h4 class="media-tagline">' + lengthTitle.esc() + ': ' + lengthText.esc() + '</h4>' + "\n" +
		           '            <h4 class="media-tagline">' + updatedTitle.esc() + ': ' + updated.esc() + '</h4>' + "\n" +
		           '            <div class="media-summary summary"></div>' + "\n" +
	               '          </div>' + "\n" +
	               '          </a>' + "\n" +
                   '        </li>' + "\n";
    
        return item;
      }
    }
    else
    {
      var playhide = 'hide';
      if (canplay == true) playhide = '';
      
      if (isfolder) { 
        var item = '		<li class="photo hide-hover-overlay">' + "\n" +
                   '            <a id="section-link-' + itemid + '" href="' + linkto + '" onClick="' + clickto + '" data-focus="keyboard" class="' + linkclass + '" ' + target + '>' + "\n" +
                   '            <div class="poster-container">' + "\n" +
                   '            <div class="poster-card">' + "\n" +
                   '            <div class="poster-face front">' + "\n" +
                   '                <img class="poster media-tile-list-poster placeholder" src="' + thumbsrc + '">' + "\n" +
                   '                <div class="hover-overlay card-overlay" style="padding: 4px 4px 4px;">' + "\n" +
                   '                    <button type="button" onclick="' + playlink + '" class="hover-menu-btn poster-play-btn ' + playhide + '" tabindex="-1"><i class="glyphicon play"></i></button>' + "\n" +
                   '                    <button type="button" onclick="' + editlink + '" class="hover-menu-btn poster-edit-btn ' + edithide + '" tabindex="-1"><i class="glyphicon pencil"></i></button>' + "\n" +
                   '                    <button type="button" onclick="' + infolink + '" class="hover-menu-btn poster-info-btn" tabindex="-1"><i class="glyphicon circle-info"></i></button>' + "\n" +
                   '                </div>' + "\n" +
                   '            </div></div></div>' + "\n" +
                   '            <div class="poster-title">' + sec_name.esc() + '</div>' + "\n" +
                   '            </a>' + "\n" +
                   '        </li>' + "\n";
    
        return item;
      
      } else { 
        var item = '		<li class="photo hide-hover-overlay">' + "\n" +
                   '            <a id="section-link-' + itemid + '" href="' + linkto + '" onClick="' + clickto + '" data-focus="keyboard" class="' + linkclass + '" ' + target + '>' + "\n" +
                   '            <div class="poster-container">' + "\n" +
                   '            <div class="poster-card">' + "\n" +
                   '            <div class="poster-face front">' + "\n" +
                   '                <img class="poster media-tile-list-poster" src="' + thumbsrc + '">' + "\n" +
                   '                <div class="hover-overlay card-overlay" style="padding: 4px 4px 4px;">' + "\n" +
                   '                    <button type="button" onclick="' + playlink + '" class="hover-menu-btn poster-play-btn ' + playhide + '" tabindex="-1"><i class="glyphicon play"></i></button>' + "\n" +
                   '                    <button type="button" onclick="' + editlink + '" class="hover-menu-btn poster-edit-btn ' + edithide + '" tabindex="-1"><i class="glyphicon pencil"></i></button>' + "\n" +
                   '                    <button type="button" onclick="' + sharelink + '" class="hover-menu-btn poster-share-btn" tabindex="-1"><i class="glyphicon share-alt"></i></button>' + "\n" +
                   '                    <button type="button" onclick="' + infolink + '" class="hover-menu-btn poster-info-btn" tabindex="-1"><i class="glyphicon circle-info"></i></button>' + "\n" +
                   '                </div>' + "\n" +
                   '            </div></div></div>' + "\n" +
                   '            <div class="poster-title">' + sec_name.esc() + '</div>' + "\n" +
                   '            </a>' + "\n" +
                   '        </li>' + "\n";
    
        return item;
      }
    }
  },
  refresh: function( deepscan )
  {
    var sectionid = this.id_param;
    if (sectionid == null) return;
    if (sectionid.length == 16) deepscan = true;
    deepscan = false;
    
    if (!deepscan) { 
      listsection.showlist(true);
      return;
    }
    
    var cmd = 'delta-import';
    var entity = 'datum';
    var deep = deepscan ? 'true' : 'false';
    
    var params = 'command=' + cmd + '&commit=true&optimize=true&entity=' + entity;
    var custom = 'id=' + sectionid + '&deepscan=' + deep;
    
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
        },
        error : function( xhr, text_status, error_thrown)
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function( xhr, text_status )
        {
          hide_loading();
          listsection.showlist(true);
        }
      }
    );
  },
  scan: function()
  {
    this.refresh( false );
  },
  deepscan: function()
  {
    this.refresh( true );
  },
  edit: function()
  {
    var sectionid = this.section_id;
    var sectiontype = this.section_type;
    
    if (sectionid != null && sectionid.length > 0 && sectiontype != null) {
      if (sectiontype.indexOf('library') >= 0) {
        editlibrary.edit0( sectionid, listsection.editcb );
      } else { 
        var context = system.context;
        context.redirect( '#/~edit/' + sectionid );
      }
    }
  },
  editcb: function()
  {
    listlibrary.libraries_inited = false;
    sammy.refresh();
  },
  select: function()
  {
    var mode = this.selectmode;
    if (mode == 'enable') {
      this.change_select( 'selecting' );
      
    } else if (mode == 'selecting') {
      this.change_select( 'enable' );
      
    }
  },
  change_select: function( mode )
  {
    if (mode == 'selecting') {
      this.init_select( 'selecting' );
      this.relist();
      this.update_title();
      
    } else if (mode == 'enable') {
      this.init_select( 'enable' );
      this.relist();
      this.update_title();
      
    }
  },
  getSelectedCount: function()
  {
    var mode = this.selectmode;
    if (mode == 'selecting') {
      var count = 0;
      var sections = this.sections;
      
      for (var key in sections) { 
        var section = sections[key];
        if (section && section['selected'] == true)
          count ++;
      }
      
      return count;
    }
    
    return 0;
  },
  getSelectedItemsName: function()
  {
    var text = strings( '{0} Items' );
    var count = this.getSelectedCount();
      
    return text.format( count );
  },
  getSelectedItemIds: function()
  {
    var mode = this.selectmode;
    var items = [];
    
    if (mode == 'selecting') {
      var sections = this.sections;
      
      for (var key in sections) { 
        var section = sections[key];
        if (section && section['selected'] == true)
          items.push( section['id'] );
      }
      
      this.change_select( 'enable' );
    }
    
    return items;
  },
  getSelectedItems: function()
  {
    var mode = this.selectmode;
    var items = [];
    
    if (mode == 'selecting') {
      var sections = this.sections;
      
      for (var key in sections) { 
        var section = sections[key];
        if (section && section['selected'] == true)
          items.push( section );
      }
      
      this.change_select( 'enable' );
    }
    
    return items;
  },
  getSectionName: function()
  {
    var section_name = this.section_name;
    var section_type = this.section_type;
    return this.toDisplaySectionName( section_name, section_type );
  },
  toDisplaySectionName: function( section_name, section_type )
  {
    return fileinfo.getdisplayname( section_name, section_type );
  },
  update_title: function()
  {
    var sectiontitle_element = $( '.section-list-title' );
    var sectioncount_element = $( '.well-header-count' );
    
    var sectiontitle = this.getSectionName();
    var sectioncount = this.total_count;
    
    var mode = this.selectmode;
    if (mode == 'selecting') {
      sectiontitle = strings( 'Selected Items' );
      sectioncount = this.getSelectedCount();
    }
    
    if (sectiontitle == null)
      sectiontitle = '';
    if (sectioncount == null)
      sectioncount = 0;
    
    sectiontitle_element
      .html( sectiontitle.esc() );
  
    sectioncount_element
      .html( sectioncount );
  },
  init_select: function( mode )
  {
    if (mode == 'enable') { 
      this.selectmode = mode;
      
      $( '#select-list-item' ).removeClass( 'hide' );
      $( '#selall-list-item' ).addClass( 'hide' );
      $( '#copy-list-item' ).addClass( 'hide' );
      $( '#move-list-item' ).addClass( 'hide' );
      $( '#delete-list-item' ).addClass( 'hide' );
      $( '#empty-list-item' ).addClass( 'hide' );
      $( '#share-list-item' ).addClass( 'hide' );
      
      $( '#select-button' )
        .attr( 'title', strings( 'Select Items' ).esc() );
      
      $( '#select-button-icon' )
        .attr( 'class', 'glyphicon check' );
      
    } else if (mode == 'selecting') { 
      this.selectmode = mode;
      
      $( '#select-list-item' ).removeClass( 'hide' );
      $( '#selall-list-item' ).removeClass( 'hide' );
      if (this.support_copy()) $( '#copy-list-item' ).removeClass( 'hide' );
      if (this.support_move()) $( '#move-list-item' ).removeClass( 'hide' );
      if (this.support_delete()) $( '#delete-list-item' ).removeClass( 'hide' );
      if (this.support_empty()) $( '#empty-list-item' ).removeClass( 'hide' );
      if (this.support_share()) $( '#share-list-item' ).removeClass( 'hide' );
      
      $( '#select-button' )
        .attr( 'title', strings( 'Unselect Items' ).esc() );
      
      $( '#select-button-icon' )
        .attr( 'class', 'glyphicon unchecked' );
      
    } else {
      this.selectmode = null;
      
      $( '#select-list-item' ).addClass( 'hide' );
      $( '#selall-list-item' ).addClass( 'hide' );
      $( '#copy-list-item' ).addClass( 'hide' );
      $( '#move-list-item' ).addClass( 'hide' );
      $( '#delete-list-item' ).addClass( 'hide' );
      $( '#empty-list-item' ).addClass( 'hide' );
      $( '#share-list-item' ).addClass( 'hide' );
    }
  },
  select_all: function()
  {
    var mode = this.selectmode;
    if (mode != 'selecting') return;
    
    if (listsection.sections)
    {
      for (var key in listsection.sections) {
        var section = listsection.sections[key];
        
        var selected = section['selected'];
        if (selected == null) selected = false;
        var newselected = true; //!selected;
        section['selected'] = newselected;
      }
      
      this.relist();
      this.update_title();
    }
  },
  copy_selected: function()
  {
    if (this.getSelectedCount() <= 0) {
      messager.showerror( strings( 'You should select some items first' ) );
      return;
    }
    
    var folderid = this.section_id;
    if (folderid == null) folderid = '';
    
    selectfolder.show_movefolder( folderid, function( pathid, path ) {
          listsection.copy_submit( pathid, path );
        } );
  },
  move_selected: function()
  {
    if (this.getSelectedCount() <= 0) {
      messager.showerror( strings( 'You should select some items first' ) );
      return;
    }
    
    var folderid = this.section_id;
    if (folderid == null) folderid = '';
    
    selectfolder.show_movefolder( folderid, function( pathid, path ) {
          listsection.move_submit( pathid, path );
        } );
  },
  delete_selected: function()
  {
    if (this.getSelectedCount() <= 0) {
      messager.showerror( strings( 'You should select some items first' ) );
      return;
    }
    
    var section_type = this.section_type;
    if (section_type == null)
      section_type = '';
    
    if (section_type == 'application/x-recycle-root' || section_type.indexOf('application/x-library-') >= 0)
      dialog.show( section_dialogs.delete_confirm_dialog );
    else
      dialog.show( section_dialogs.trash_confirm_dialog );
  },
  share_selected: function()
  {
    if (this.getSelectedCount() <= 0) {
      messager.showerror( strings( 'You should select some items first' ) );
      return;
    }
    
    var items = this.getSelectedItems();
    if (items == null || items.length == 0)
      return;
    
    compose.share(items);
  },
  empty_folder: function()
  {
    if (this.total_count <= 0) {
      messager.showerror( strings( 'No items to clean' ) );
      return;
    }
    
    var section_type = this.section_type;
    if (section_type == null)
      section_type = '';
    
    if (section_type == 'application/x-recycle-root')
      dialog.show( section_dialogs.empty_confirm_dialog );
  },
  empty_submit: function()
  {
    dialog.hide();
    
    var sectionid = this.section_id;
    if (sectionid == null || sectionid.length == 0)
      return;
    
    var items = [];
    items.push( sectionid );
    
    uploader.empty_folder( items, function( res ) {
          if (res && res.error_code != 0) { 
            messager.error_code = res.error_code;
            messager.error_msg = res.error_msg;
            dialog.show( messager.message_dialog );
          } else {
            listsection.scan();
          }
        });
  },
  delete_submit: function()
  {
    dialog.hide();
    
    var items = this.getSelectedItemIds();
    
    uploader.delete_file( items, function( res ) {
          if (res && res.error_code != 0) { 
            messager.error_code = res.error_code;
            messager.error_msg = res.error_msg;
            dialog.show( messager.message_dialog );
          } else {
            listsection.scan();
          }
        });
  },
  trash_submit: function()
  {
    dialog.hide();
    
    var items = this.getSelectedItemIds();
    
    uploader.trash_file( items, function( res ) {
          if (res && res.error_code != 0) { 
            messager.error_code = res.error_code;
            messager.error_msg = res.error_msg;
            dialog.show( messager.message_dialog );
          } else {
            listsection.scan();
          }
        });
  },
  move_submit: function( pathid, path )
  {
    var items = this.getSelectedItemIds();
    
    uploader.move_file( items, pathid, function( res ) { 
          if (res && res.error_code != 0) { 
            messager.error_code = res.error_code;
            messager.error_msg = res.error_msg;
            dialog.show( messager.message_dialog );
          } else {
            listsection.scan();
          }
        });
  },
  copy_submit: function( pathid, path )
  {
    var items = this.getSelectedItemIds();
    
    uploader.copy_file( items, pathid, function( res ) { 
          if (res && res.error_code != 0) { 
            messager.error_code = res.error_code;
            messager.error_msg = res.error_msg;
            dialog.show( messager.message_dialog );
          } else {
            listsection.scan();
          }
        });
  }
};

var sidebar = {
  shown: false,
  newfolder_dialog: null,
  
  init: function()
  {
    var sidebar_element = $( '#action-sidebar' );
    var options_element = $( '#action-options' );
    var upload_element = $( '#action-upload' );
    var import_element = $( '#action-import' );
    var newfolder_element = $( '#action-newfolder' );
    
    var sectionlist_element = $( '#section-list' );
    var tilelist_element = $( '#action-tilelist' );
    var detaillist_element = $( '#action-detaillist' );
    var compactlist_element = $( '#action-compactlist' );
    var byfolder_element = $( '#action-byfolder' );
    
    options_element
      .attr( 'onclick', 'javascript:sidebar.toggle();return false;')
      .attr( 'href', '' )
      .attr( 'title', strings( 'Options' ).esc() );
    
    upload_element
      .attr( 'onclick', 'javascript:sidebar.upload();return false;')
      .attr( 'href', '' )
      .attr( 'title', strings( 'Upload' ).esc() );
    
    import_element
      .attr( 'onclick', 'javascript:sidebar.import();return false;')
      .attr( 'href', '' )
      .attr( 'title', strings( 'Import' ).esc() );
    
    newfolder_element
      .attr( 'onclick', 'javascript:sidebar.newfolder();return false;')
      .attr( 'href', '' )
      .attr( 'title', strings( 'New Folder' ).esc() );
    
    tilelist_element
      .attr( 'onclick', 'javascript:sidebar.showtile();return false;')
      .attr( 'title', strings( 'Tiles' ).esc() );
    
    detaillist_element
      .attr( 'onclick', 'javascript:sidebar.showdetail();return false;')
      .attr( 'title', strings( 'Details' ).esc() );
    
    compactlist_element
      .attr( 'onclick', 'javascript:sidebar.showcompact();return false;')
      .attr( 'title', strings( 'List' ).esc() );
    
    byfolder_element
      .attr( 'onclick', 'javascript:sidebar.showbyfolder();return false;')
      .attr( 'title', strings( 'By Folder' ).esc() );
    
    if (listsection.listtype == 'tile')
      this.inittile();
    else if (listsection.listtype == 'detail')
      this.initdetail();
    else if (listsection.listtype == 'compact')
      this.initcompact();
    
    this.initbyfolder();
    
    if (this.shown == false) {
      var val = system.get_cookie( 'sidebar.shown' );
      if (val == 'true') this.shown = true;
    }
    
    if (this.shown) this.show();
    else this.hide();
  },
  initbyfolder: function()
  {
    var byfolder_element = $( '#action-byfolder' );
    if (listsection.listbyfolder) { 
      byfolder_element.addClass( 'selected active' );
    } else { 
      byfolder_element.removeClass( 'selected active' );
    }
  },
  inittile: function()
  {
    var sectionlist_element = $( '#section-list' );
    var tilelist_element = $( '#action-tilelist' );
    var detaillist_element = $( '#action-detaillist' );
    var compactlist_element = $( '#action-compactlist' );
    
    sectionlist_element
      .attr( 'class', 'tile-list media-tile-list' );
    
    tilelist_element.addClass( 'active' );
    detaillist_element.removeClass( 'active' );
    compactlist_element.removeClass( 'active' );
    
  },
  initdetail: function()
  {
    var sectionlist_element = $( '#section-list' );
    var tilelist_element = $( '#action-tilelist' );
    var detaillist_element = $( '#action-detaillist' );
    var compactlist_element = $( '#action-compactlist' );
    
    sectionlist_element
      .attr( 'class', 'list media-list virtual-media-list' );
    
    tilelist_element.removeClass( 'active' );
    detaillist_element.addClass( 'active' );
    compactlist_element.removeClass( 'active' );
    
  },
  initcompact: function()
  {
    var sectionlist_element = $( '#section-list' );
    var tilelist_element = $( '#action-tilelist' );
    var detaillist_element = $( '#action-detaillist' );
    var compactlist_element = $( '#action-compactlist' );
    
    sectionlist_element
      .attr( 'class', 'list media-list media-compact-list' );
    
    tilelist_element.removeClass( 'active' );
    detaillist_element.removeClass( 'active' );
    compactlist_element.addClass( 'active' );
    
  },
  showtile: function()
  {
    this.inittile();
    
    listsection.listtype = 'tile';
    listsection.relist();
  },
  showdetail: function()
  {
    this.initdetail();
    
    listsection.listtype = 'detail';
    listsection.relist();
  },
  showcompact: function()
  {
    this.initcompact();
    
    listsection.listtype = 'compact';
    listsection.relist();
  },
  toggle: function()
  {
    if (this.hashide())
      this.show();
    else
      this.hide();
  },
  hashide: function()
  {
    var sidebar_container = $( '#sidebar-container' );
    var sidebar_element = $( '#action-sidebar' );
    
    if (sidebar_element && sidebar_element.hasClass( 'hide' ))
      return true;
    
    return false;
  },
  show: function()
  {
    if (!this.hashide()) return;
    
    var section_container = $( '#section-container' );
    var sidebar_container = $( '#sidebar-container' );
    var sidebar_element = $( '#action-sidebar' );
    var options_element = $( '#action-options' );
    
    if (!section_container.hasClass( 'sidebar-open' )) {
      section_container
        .addClass( 'sidebar-open' );
    }
    
    if (!sidebar_container.hasClass( 'open' )) {
      sidebar_container
        .addClass( 'open' );
    }
    
    if (sidebar_element.hasClass( 'hide' )) {
      sidebar_element
        .removeClass( 'hide' );
    }
    
    this.shown = true;
    system.set_cookie( 'sidebar.shown', 'true' );
  },
  hide: function()
  {
    if (this.hashide()) return;
    
    var section_container = $( '#section-container' );
    var sidebar_container = $( '#sidebar-container' );
    var sidebar_element = $( '#action-sidebar' );
    var options_element = $( '#action-options' );
    
    if (section_container.hasClass( 'sidebar-open' )) {
      section_container
        .removeClass( 'sidebar-open' );
    }
    
    if (sidebar_container.hasClass( 'open' )) {
      sidebar_container
        .removeClass( 'open' );
    }
    
    if (!sidebar_element.hasClass( 'hide' )) {
      sidebar_element
        .addClass( 'hide' );
    }
    
    this.shown = false;
    system.set_cookie( 'sidebar.shown', null );
  },
  showbyfolder: function()
  {
    var rootId = listsection.root_id;
    var sectionId = listsection.section_id;
    
    if (rootId != null && rootId.length > 0) { 
      if (listsection.listbyfolder) {
        listsection.listbyfolder = false;
        if (rootId == sectionId) { 
          sidebar.initbyfolder();
          listsection.scan();
        } else {
          var context = system.context;
          context.redirect( '#/~browse/' + rootId );
        }
      } else { 
        listsection.listbyfolder = true;
        sidebar.initbyfolder();
        listsection.scan();
      }
    }
  },
  upload: function()
  {
    var section_id = listsection.section_id;
    var section_type = listsection.section_type;
    var section_name = listsection.section_name;
    var library_type = listsection.library_type;
    var accept = null;
    
    if (library_type != null && library_type.indexOf('image') >= 0)
      accept = 'image/*';
    
    var target = {id: section_id, type: section_type, name: section_name};
    uploader.select_files( accept, target, function( tg ) {
          if (tg && tg.id && tg.id == listsection.section_id && uploader.uploading_count == 0 && uploader.dialog_shown == false) {
            var context = system.context;
            if (context && context.path.indexOf( '#/~browse/' ) == 0)
              listsection.scan();
          }
        }, true);
  },
  import: function()
  {
  },
  newfolder: function()
  {
    dialog.show( this.newfolder_dialog );
  },
  newfolder_html: function()
  {
    var name = strings( 'Folder Name' );
    var required = strings( 'A folder name is required.' );
    
    return '<div class="add-section-details" id="newfolder-details">' + "\n" +
		   '  <div class="row-fluid">' + "\n" +
		   '	<div class="name-group control-group" id="newfolder-name-group">' + "\n" +
		   '	  <span class="help-inline hide" id="newfolder-required-name">' + required + '</span>' + "\n" +
		   '	  <label id="newfolder-name-label">' + name.esc() + '</label>' + "\n" +
		   '	  <input type="text" name="name" class="span12" id="newfolder-name-input" value="">' + "\n" +
		   '    </div>' + "\n" +
		   '  </div>' + "\n" +
		   '</div>';
  },
  newfolder_submit: function()
  {
    var input_error = false;
    
    var input_name = $( '#newfolder-name-input' )
      .attr( 'value' ).trim();
    
    if (input_name == null || input_name.length == 0)
    {
      $( '#newfolder-required-name' )
        .removeClass( 'hide' );
      
      $( '#newfolder-name-group' )
        .addClass( 'error' );
      
      input_error = true;
    }
    else
    {
      $( '#newfolder-required-name' )
        .addClass( 'hide' );
      
      $( '#newfolder-name-group' )
        .removeClass( 'error' );
    }
    
    if (input_error) return;
    
    var section_id = listsection.section_id;
    var section_type = listsection.section_type;
    var section_name = listsection.section_name;
    
    var target = {id: section_id, type: section_type, name: section_name};
    dialog.hide();
    
    uploader.create_folder( input_name, target, function() {
          listsection.scan();
        } );
  }
};

var section_headbar = {
  backlinkto: null,
  
  init: function( header_element ) 
  { 
    headbar = this;
    $.get
    (
      'tpl/navbar.html',
      function( template )
      {
        header_element
          .html( template );
        
        navbar.init();
        
        $( '#back-button' ).removeClass( 'hide' );
        $( '#library-link' ).addClass( 'active' );
        
        navbar.oninited();
      }
    );
  },
  onback: function()
  {
    if (popover.isshown()) { 
      popover.hide();
      return;
    }
    
    var context = system.context;
    var linkto = this.backlinkto;
    
    if (linkto != null && linkto.length > 0) {
      context.redirect( linkto );
      return;
    }
    
    back_page();
  }
};

var section_dialogs = { 
  delete_confirm_dialog: null,
  trash_confirm_dialog: null,
  empty_confirm_dialog: null,
  
  init_message: function( dialog_element, template ) 
  {
    section_dialogs.empty_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Empty Folder' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to clean up {0}? This cannot be undone.' );
        if (msg == null) msg = "";
        
        msg = msg.format( listsection.getSectionName() );
        
        $( '#message-text' )
          .html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:listsection.empty_submit();return false;' )
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
    
    section_dialogs.delete_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Items' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to delete {0}? This cannot be undone.' );
        if (msg == null) msg = "";
        
        msg = msg.format( listsection.getSelectedItemsName() );
        
        $( '#message-text' )
          .html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:listsection.delete_submit();return false;' )
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
    
    section_dialogs.trash_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Items' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon circle-question-mark' );
        
        var msg = strings( 'Are you sure to move {0} to Recycle Bin?' );
        if (msg == null) msg = "";
        
        msg = msg.format( listsection.getSelectedItemsName() );
        
        $( '#message-text' )
          .html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:listsection.trash_submit();return false;' );
        
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
    
    sidebar.newfolder_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'New Folder' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-container' )
          .attr( 'class', 'edit-section-modal modal fade in' );
        
        $( '#message-icon' )
          .attr( 'class', 'glyphicon folder-new' );
        
        var html = sidebar.newfolder_html();;
        if (html == null) html = "";
        
        $( '#message-text' )
          .html( html );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:sidebar.newfolder_submit();return false;' );
        
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
