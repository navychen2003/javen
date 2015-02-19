
var listdashboard = {
  slidephotos: [],
  historyitems: null,
  historyclick: 0,
  announcement_count: 0,
  announcement_idx: 0,
  
  listcb: function( histories )
  {
    listdashboard.slidephotos = [];
    listdashboard.init_histories( histories );
    listdashboard.showannouncement( 0 );
  },
  listhistories: function( count )
  {
    if (count == null || count <= 0) 
      count = 50;
    
    var params = '&action=history&count=' + count;
    
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
          var histories = response['histories'];
          listdashboard.init_histories( histories );
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
  init_histories: function( histories )
  {
    if (histories == null) histories = {};
    var sections = histories['sections'];
    if (sections == null) sections = {};
    
    var items = [];
    
    for (var key in sections) {
      var section = sections[key];
      if (section == null) continue;
      
      items.push( section );
    }
    
    listdashboard.historyitems = items;
    listdashboard.showhistories( 1 );
  },
  showhistories: function( page )
  {
    var sections = listdashboard.historyitems;
    if (sections == null) sections = [];
    
    if (page == null || page < 1) page = 1;
    
    var pagesize = 5;
    var totalpage = Math.ceil(sections.length / pagesize);
    
    var pageitem = '<span class="page">' + page + '</span> / <span class="pages">' + totalpage + '</span>';
    $( '#dashboard-history-page' ).html( pageitem );
    
    if (page > 1) {
      $( '#dashboard-history-prev' )
        .attr( 'onclick', 'javascript:listdashboard.showhistories(' + (page-1) + ');return false;' )
        .removeClass( 'disabled' );
    } else {
      $( '#dashboard-history-prev' )
        .addClass( 'disabled' );
    }
    
    if (page < totalpage) {
      $( '#dashboard-history-next' )
        .attr( 'onclick', 'javascript:listdashboard.showhistories(' + (page+1) + ');return false;' )
        .removeClass( 'disabled' );
    } else {
      $( '#dashboard-history-next' )
        .addClass( 'disabled' );
    }
    
    var content = [];
    var startidx = (page -1) * pagesize;
    var endidx = startidx + pagesize;
    
    for (var key=startidx; key < sections.length && key < endidx; key++) {
      var section = sections[key];
      if (section == null) continue;
      
      var item = this.buildHistoryItem( section, content.length );
      if (item != null)
        content.push( item );
    }
    
    $( '#dashboard-history-list' )
      .html( content.join("\n") );
    
    $( '#dashboard-history' ).removeClass( 'hide' );
    
    if (content.length == 0) {
      //$( '#dashboard-library' ).removeClass( 'hide' );
      $( '#dashboard-history-empty' ).removeClass( 'hide' );
      $( '#dashboard-history-list' ).addClass( 'hide' );
    } else {
      $( '#dashboard-library' ).addClass( 'hide' );
    }
  },
  getsection: function( id )
  {
    if (id == null || id.length == 0) 
      return null;
    
    var sections = listdashboard.historyitems;
    if (sections == null) return null;
    
    for (var key in sections) {
      var section = sections[key];
      if (section == null) continue;
      
      var section_id = section['id'];
      if (section_id == id) 
        return section;
    }
    
    return null;
  },
  playitem: function( id )
  {
    var section = listdashboard.getsection( id );
    if (section == null) return;
    
    if (section != null) {
        var section_type = section['type'];
        if (section_type == null) section_type = '';
        if (section_type.indexOf('audio/') == 0) {
          if (musicplayer) {
            musicplayer.play( section );
            return;
          }
        }
    }
    
    var section_id = section['id'];
    if (section_id == null || section_id.length == 0) 
      return;
    
    var openlink = '#/~play/' + section_id;
    var context = system.context;
    context.redirect( openlink );
  },
  clickitem: function( id )
  {
    var clicktype = listdashboard.historyclick;
    listdashboard.historyclick = 0;
    if (clicktype == 1) {
      listdashboard.playitem( id );
      return;
    }
    
    var section = listdashboard.getsection( id );
    if (section == null) return;
    
    var section_id = section['id'];
    if (section_id == null || section_id.length == 0) 
      return;
    
    fileinfo.showdetailsid( section_id );
  },
  showslide: function( id, idx )
  {
    if (idx == null || idx < 0) idx = 0;
    var slidephotos = listdashboard.slidephotos;
    if (slidephotos != null && slidephotos.length > 0) {
      photoslide.show( slidephotos, idx, false );
      return;
    }
    
    listdashboard.clickitem( id );
  },
  addslide: function( section )
  {
    if (section == null) return;
    var section_id = section['id'];
    if (section_id == null || section_id.length == 0)
      return -1;
    
    if (listdashboard.slidephotos == null)
      listdashboard.slidephotos = [];
    
    for (var key=0; key < listdashboard.slidephotos.length; key++) {
      var photo = listdashboard.slidephotos[key];
      if (photo == null) continue;
      var photo_id = photo['id'];
      if (photo_id == section_id) return key;
    }
    
    var idx = listdashboard.slidephotos.length;
    listdashboard.slidephotos.push( section );
    
    return idx;
  },
  buildHistoryItem: function( section, idx )
  {
    if (section == null) return;
    
    var section_id = section['id'];
    if (section_id == null || section_id.length == 0)
      return;
    
    var section_name = section['name'];
    var section_type = section['type'];
    var section_perms = section['perms'];
    var section_ops = section['ops'];
    var extname = section['extname'];
    var isfolder = section['isfolder'];
    var length = section['length'];
    var width = section['width'];
    var height = section['height'];
    var timeLen = section['timelen'];
    var query = section['query'];
    var poster = section['poster'];
    var background = section['background'];
  
    var username = section['username'];
    var usertype = section['usertype'];
    var usertitle = section['usertitle'];
    
    var root_id = section['root_id'];
    var root_name = section['root_name'];
    var root_type = section['root_type'];
    
    var parent_id = section['parent_id'];
    var parent_name = section['parent_name'];
    var parent_type = section['parent_type'];
    
    var library_id = section['library_id'];
    var library_name = section['library_name'];
    var library_type = section['library_type'];
    var library_hostname = section['hostname'];
    
    var owner = section['owner'];
    var ownertype = section['ownertype'];
    var ownertitle = section['ownertitle'];
    
    if (section_type == null) section_type = '';
    if (section_name == null) section_name = '';
    if (section_id == null) section_id = '';
    if (extname == null) extname = '';
    if (extension == null || extension.length == 0) extension = 'dat';
    if (isfolder == null) isfolder = false;
    if (length == null || length < 0) length = 0;
    if (width == null || width < 0) width = 0;
    if (height == null || height < 0) height = 0;
    if (timeLen == null || timeLen < 0) timeLen = 0;
    
    var openlink = '';
    var downlink = '';
    var postersrc = 'css/' + app.theme + '/images/posters/poster.png';
    var name = section_name;
    var extension = extname;
  
    var postersize = '192t';
    if (idx == 0) {
      postersrc = 'css/' + app.theme + '/images/posters/poster_wide.png';
      postersize = '256t';
    }
  
    if (isfolder == false && name.indexOf('.') >= 0) { 
      var pos = name.lastIndexOf('.');
      if (pos > 0) name = name.substring(0, pos);
      if (name == null) name = '';
    }
  
    var canplay = false;
    var canplaymusic = false;
    var canposter = true;
    var playclick = 'javascript:listdashboard.playitem(\'' + section_id + '\');return false;';
    var itemclick = 'javascript:listdashboard.clickitem(\'' + section_id + '\');return false;';
    var metaduration = '';
  
    if (section_type.indexOf('image') == 0) {
      postersrc = app.base_path + '/image/' + section_id + '_' + postersize + '.' + extension + '?token=' + app.token;
      openlink = app.base_path + '/image/' + section_id + '_0.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
      
      metaduration = width + ' x ' + height;
      canposter = false;
      
      var slideidx = listdashboard.addslide( section );
      if (slideidx != null && slideidx >= 0) {
        itemclick = 'javascript:listdashboard.showslide(\'' + section_id + '\',' + slideidx + ');return false;';
      }
    
    } else if (section_type.indexOf('audio/') == 0) {
      if (idx > 0) postersrc = 'css/' + app.theme + '/images/posters/music.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
    
      metaduration = musicplayer.readableSeconds( timeLen / 1000 );
      
      canplay = true;
      canplaymusic = true;
      canposter = true;
    
    } else if (section_type.indexOf('video/') == 0) { 
      if (idx > 0) postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
    
      metaduration = musicplayer.readableSeconds( timeLen / 1000 );
      
      canplay = true;
      canposter = true;
    
    } else if (section_type.indexOf('text/') == 0) { 
      if (idx > 0) postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
    
      metaduration = readableBytes2( length );
      canposter = true;
    
    } else if (isfolder == false) { 
      if (idx > 0) postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
      
      metaduration = readableBytes2( length );
      canposter = true;
      
    } else if (isfolder == true) {
      if (idx > 0) postersrc = 'css/' + app.theme + '/images/posters/folder.png';
      canposter = true;
    }
  
    section['openlink'] = openlink;
    section['downlink'] = downlink;
  
    var empty_poster = postersrc;
    var empty_background = 'css/' + app.theme + '/images/background.png';
  
    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      postersrc = app.base_path + '/image/' + imgid + '_' + postersize + '.' + imgext + '?token=' + app.token;
      
      var slideidx = listdashboard.addslide( section );
      if (slideidx != null && slideidx >= 0) {
        //itemclick = 'javascript:listdashboard.showslide(\'' + section_id + '\',' + slididx + ');return false;';
      }
    }
  
    if (background != null && background.length > 0) {
      var imgid = background;
      var imgext = 'jpg';
      var src = app.base_path + '/image/' + imgid + '.' + imgext + '?token=' + app.token;
      
      //$( '#background-image' )
      //  .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
    
    var title = section_name;
    var item = null;
    var playhide = 'hide';
    if (canplay) playhide = '';
    
    playclick = 'javascript:listdashboard.historyclick=1;';
    
    if (idx == 0) {
      item = 
        '<li class="primary">' + "\n" +
        '   <a class="card-container" data-focus="keyboard" onclick="' + itemclick + '" href="">' + "\n" +
        '       <div class="poster-container">' + "\n" +
        '       <div class="poster-card">' + "\n" +
        '       <div class="poster-face front">' + "\n" +
        '           <img class="poster placeholder" src="' + postersrc + '" />' + "\n" +
        '           <ul class="poster-actions">' + "\n" +
        '               <li><button class="play-btn poster-action-btn ' + playhide + '" onclick="' + playclick + '"><i class="glyphicon play"></i></button></li>' + "\n" +
        '           </ul>' + "\n" +
        '           <div class="card-overlay card-progress-overlay">' + "\n" +
        '               <h4>' + title.esc() + '</h4>' + "\n" +
        '               <p class="card-details">' + metaduration.esc() + '</p>' + "\n" +
        '           </div>' + "\n" +
        '       </div>' + "\n" +
        '       </div>' + "\n" +
        '       </div>' + "\n" +
        '   </a>' + "\n" +
        '</li>';
        
    } else {
      item = 
        '<li>' + "\n" +
        '   <a class="card-container" data-focus="keyboard" onclick="' + itemclick + '" href="">' + "\n" +
        '       <div class="poster-container">' + "\n" +
        '       <div class="poster-card">' + "\n" +
        '       <div class="poster-face front">' + "\n" +
        '           <img class="poster placeholder" src="' + postersrc + '" />' + "\n" +
        '           <ul class="poster-actions">' + "\n" +
        '               <li><button class="play-btn poster-action-btn poster-action-small-btn ' + playhide + '" onclick="' + playclick + '"><i class="glyphicon play"></i></button></li>' + "\n" +
        '           </ul>' + "\n" +
        '           <div class="card-overlay card-progress-overlay">' + "\n" +
        '               <h4>' + title.esc() + '</h4>' + "\n" +
        '               <p class="card-details">' + metaduration.esc() + '</p>' + "\n" +
        '           </div>' + "\n" +
        '       </div>' + "\n" +
        '       </div>' + "\n" +
        '       </div>' + "\n" +
        '   </a>' + "\n" +
        '</li>';
    }
    
    return item;
  },
  showannouncementnext: function()
  {
    var context = system.context;
    if (context.path != '#/~dashboard') return;
    
    var count = listdashboard.announcement_count;
    var idx = listdashboard.announcement_idx;
    
    if (count == null || count < 0) count = 0;
    if (idx == null || idx < 0) idx = 0;
    
    if (count <= 0) return;
    idx ++;
    
    if (idx >= count) idx = 0;
    listdashboard.showannouncement( idx );
  },
  showannouncement: function( idx )
  {
    if (idx == null || idx < 0) idx = 0;
    
    var announcements = globalApp.announcements;
    if (announcements == null) announcements = {};
    
    var navigationContent = [];
    var announcement = null;
    var lastone = null;
    var count = 0;
    
    for (var key in announcements) {
      var anno = announcements[key];
      if (anno == null) continue;
      
      var title = anno['title'];
      var body = anno['body'];
      
      if (title == null || title.length == 0) continue;
      if (body == null || body.length == 0) continue;
      
      var showClick = 'javascript:listdashboard.showannouncement(' + count + ');return false;';
      
      if (idx == count) {
        announcement = anno;
        
        var item = '<a class="dashboard-carousel-btn active" href="" onclick="' + showClick + '"></a>';
        navigationContent.push( item );
        
      } else { 
        var item = '<a class="dashboard-carousel-btn" href="" onclick="' + showClick + '"></a>';
        navigationContent.push( item );
      }
      
      lastone = anno;
      count ++;
    }
    
    listdashboard.announcement_count = count;
    listdashboard.announcement_idx = idx;
    
    if (announcement == null) announcement = lastone;
    if (announcement == null) return;
    
    var key = announcement['key'];
    var title = announcement['title'];
    var link = announcement['link'];
    var body = announcement['body'];
    var poster = announcement['poster'];
    var mtime = announcement['mtime'];
    var status = announcement['status'];
    
    if (key == null) key = '';
    if (title == null) title = '';
    if (link == null) link = '';
    if (poster == null) poster = '';
    if (status == null) status = '';
    
    var modifiedTime = format_time(mtime);
    var moreClick = 'javascript:listdashboard.showannouncements();return false;';
    var thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
    
    var imghide = '';
    if (poster != null && poster.length > 0) {
      var id = poster;
      var extension = 'jpg';
  
      var src = app.base_path + '/image/' + id + '_64t.' + extension + '?token=' + app.token;
      thumbsrc = src;
    } else { 
      imghide = 'hide';
    }
    
    var linkAtt = '';
    if (link != null && link.length > 0)
      linkAtt = 'href="' + link.esc() + '"';
    
    var item = 
        '	<div class="row-fluid module">' + "\n" +
        '	<div class="dashboard-announcements-container well span12">' + "\n" +
        '	<div class="dashboard-well-header dashboard-well-header-secondary well-header">' + "\n" +
        '	<ul class="well-header-actions">' + "\n" +
        '		<li><button type="button" class="mark-read-btn btn btn-inverse btn-icon" rel="tooltip" title="' + strings( 'More' ) + '" onClick="' + moreClick + '"><i class="glyphicon more"></i></button></li>' + "\n" +
        '	</ul>' + "\n" +
        '	<h3>' + strings( 'Announcements' ) + '</h3>' + "\n" +
        '	</div>' + "\n" +
        '	<div class="dashboard-carousel-container">' + "\n" +
        '	<div class="dashboard-carousel-container">' + "\n" +
        '	<div class="dashboard-carousel-content">' + "\n" +
        '	<ul class="dashboard-announcement-list" style="width: 600%; left: 0px;">' + "\n" +
        '		<li style="width: 16.666666666666668%;">' + "\n" +
        '			<h3><a target="_blank" tabindex="-1" ' + linkAtt + '>' + title.esc() + '</a></h3>' + "\n" +
        '			<p class="date">' + modifiedTime + '</p>' + "\n" +
        '			<img class="poster pull-left ' + imghide + '" src="' + thumbsrc + '"><p class="description">' + body + '</p>' + "\n" +
        '		</li>' + "\n" +
        '	</ul>' + "\n" +
        '	</div>' + "\n" +
        '	<div class="dashboard-carousel-navigation">' + "\n" + navigationContent.join( "\n" ) +
        '	</div>' + "\n" +
        '	</div></div>' + "\n" +
        '	</div></div>';
	
	$( '#content-sidebar' ).html( item );
  },
  showannouncements: function()
  {
    var lang = globalApp.get_language();
    if (lang == null || lang.length == 0)
      lang = 'all';
    
    var context = system.context;
    context.redirect( '#/~announcements/' + encodeURIComponent(lang) );
  },
  uploadfile: function()
  {
    var username = globalApp.get_username();
    var section_id = globalApp.get_browsekey(username);
    var accept = null;
    
    var target = {id: section_id, type: 'application/x-library', name: ''};
    uploader.select_files( accept, target, function( tg ) {
          if (tg && tg.id && uploader.dialog_shown == false) 
            sammy.refresh(); //listdashboard.listhistories();
        }, true);
  }
};

var dashboard_headbar = {
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
        
        $( '#back-button' ).addClass( 'hide' );
        $( '#home-link' ).addClass( 'active' );
        
        navbar.oninited();
      }
    );
  },
  onback: function()
  {
    var context = system.context;
    var linkto = this.backlinkto;
    
    if (linkto != null && linkto.length > 0) {
      context.redirect( linkto );
      return;
    }
    
    back_page();
  }
};

// #/~dashboard
sammy.get
(
  /^#\/(~dashboard)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );

    listlibrary.body_element = body_element;

    dashboard_headbar.init( header_element );
    message_dialogs.init( dialog_element );
    
    $.get
    (
      'tpl/dashboard.html',
      function( template )
      {
        body_element
          .html( template );

        var dashboardtitle_element = $( '#dashboard-library-title' );
        var historytitle_element = $( '#dashboard-history-title' );
        var librarylist_element = $( '#dashboard-library-list' );
        var libraryactions_element = $( '#dashboard-library-actions' );

        var addbutton_element = $( '#dashboard-add-button' );
        var editbutton_element = $( '#dashboard-edit-button' );
        var completebutton_element = $( '#dashboard-complete-button' );
        var refreshbutton_element = $( '#dashboard-refresh-button' );

        var page_title = strings( 'My Library' );
        var dashboard_title = strings( 'My Library' );
        var addbutton_title = strings( 'Add a library' );
        var editbutton_title = strings( 'Edit libraries' );
        var completebutton_title = strings( 'Complete!' );
        var refreshbutton_title = strings( 'Refresh Libraries' );
        var edit_title = strings( 'Edit' );
        var refresh_title = strings( 'Refresh' );
        var delete_title = strings( 'Delete' );
        
        if (system.friendlyName != null)
          page_title = page_title + ' (' + system.friendlyName + ')';
        
        title_element
          .html( page_title.esc() );

        dashboardtitle_element
          .html( dashboard_title.esc() );

        historytitle_element.html( strings( 'Recently Added' ) );

        addbutton_element
          .attr( 'title', addbutton_title.esc() )
          .attr( 'onclick', 'javascript:addlibrary.show_selecttype();return false;' );

        editbutton_element
          .attr( 'title', editbutton_title.esc() )
          .attr( 'onclick', 'javascript:librarymode.change();return false;' );

        completebutton_element
          .html( completebutton_title.esc() )
          .attr( 'onclick', 'javascript:librarymode.hide();return false;' );

        refreshbutton_element
          .attr( 'title', refreshbutton_title.esc() )
          .attr( 'onclick', 'javascript:editlibrary.rescan();return false;' );

        var libraryempty = strings( 'There are no available library sections.' ) 
          + ' <a class="add-btn" href="" onclick="javascript:addlibrary.show_selecttype();return false;">' 
          + strings( 'Add a library now' ) + '</a>';
        
        var historyempty = strings( 'There are no recently added sections.' ) 
          + ' <a class="add-btn" href="" onclick="javascript:listdashboard.uploadfile();return false;">' 
          + strings( 'Upload a file now' ) + '</a>';
        
        $( '#dashboard-library-empty' ).html( libraryempty );
        $( '#dashboard-history-empty' ).html( historyempty );

        listlibrary.showlist0( listdashboard.listcb );
        
        statusbar.show();
      }
    );
  }
);