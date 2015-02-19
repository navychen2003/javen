
var navbar = {
  init: function()
  {
    $( '#server-dropdown-computer' ).html( strings( 'Computer' ) );
    $( '#account-dropdown-signin' ).html( strings( 'Sign In' ) );
    
    var homelink_element = $( '#home-link' );
    var searchinput_element = $( '#search-input' );
    var searchform_element = $( '#search-form' );
    
    var musiclistlink_element = $( '#musiclist-link' );
    var librarylink_element = $( '#library-link' );
    var settingslink_element = $( '#settings-link' );
    var accountlink_element = $( '#account-link' );
    
    var backbutton_element = $( '#back-button' );
    var backlink_element = $( '#back-link' );
    var sectiondivider_element = $( '#section-divider' );
    var libraries_element = $( '#section-breadcrumb' );
    
    var username = globalApp.get_username();
    if (username == null) username = '';
    
    var hostlocation = system.get_hostlocation();
    if (hostlocation == null) hostlocation = '';
    
    var libraryTitle = strings( 'Library' ) + '(' 
      + globalApp.get_remainingtitle() + ')';
    
    searchform.init( searchform_element );
    
    backbutton_element
      .removeClass( 'hide' );
    
    backlink_element
      .attr( 'title', strings( 'Back' ) )
      .attr( 'href', 'javascript:headbar.onback();' );
    
    homelink_element
      .attr( 'title', strings( 'Home' ) )
      .attr( 'href', hostlocation + '#/~dashboard' )
      .removeClass( 'active' );
    
    searchinput_element
      .attr( 'placeholder', strings( 'Search' ) );
    
    musiclistlink_element
      .attr( 'title', strings( 'Music' ) )
      .attr( 'href', hostlocation + '#/~playlist' );
    
    librarylink_element
      .attr( 'title', libraryTitle )
      .attr( 'href', hostlocation + '#/~browse/' + encodeURIComponent('me@') );
    
    settingslink_element
      .attr( 'title', strings( 'Setting' ) )
      .attr( 'href', hostlocation + '#/~settings' );
    
    accountlink_element
      .attr( 'title', strings( 'Account' ) )
      .attr( 'onclick', 'javascript:opener.toggle(\'#account-dropdown\');return false;' )
      .attr( 'href', '' );
    
    if (hostlocation != null && hostlocation.length > 0) {
      accountlink_element
        .attr( 'onclick', '' )
        .attr( 'href', hostlocation + '#/~profile' );
    }
    
    musicplayer.init_navicon();
  },
  init_metitle: function( clickto, linkto )
  {
    var title = strings( 'Me' );
    var username = globalApp.get_username();
    var nickname = globalApp.get_nickname();
    if (nickname != null && nickname.length > 0) {
      title = title + '(' + nickname + ')';
    } else if (username != null && username.length > 0) {
      title = title + '(' + username + ')';
    }
    this.init_name( title, clickto, linkto );
  },
  init_grouptitle: function( groupname, nickname, clickto, linkto )
  {
    var title = strings( 'Group' );
    if (nickname != null && nickname.length > 0) {
      title = title + '(' + nickname + ')';
    } else if (groupname != null && groupname.length > 0) {
      title = title + '(' + groupname + ')';
    }
    this.init_name( title, clickto, linkto );
  },
  init_usertitle: function( username, nickname, clickto, linkto )
  {
    var title = strings( 'User' );
    if (nickname != null && nickname.length > 0) {
      title = title + '(' + nickname + ')';
    } else if (username != null && username.length > 0) {
      title = title + '(' + username + ')';
    }
    this.init_name( title, clickto, linkto );
  },
  init_name: function( name, clickto, linkto )
  {
    if (name == null || name.length == 0)
      return;
    
    var hrefItem = '';
    var clickItem = '';
    
    if (linkto != null && linkto.length > 0)
      hrefItem = 'href="' + linkto + '"';
    
    if (clickto != null && clickto.length > 0) {
      clickItem = 'onClick="' + clickto + ' return false;"';
      hrefItem = 'href=""';
    }
    
    var mediadivider_element = $( '#media-divider' );
    var media_element = $( '#media-breadcrumb' );
    
    if (name != null) {
      var html = '    <a class="breadcrumb-placeholder breadcrumb-btn"><i class="glyphicon more"></i></a>' + "\n" +
                 '    <div class="breadcrumb-content"><a class="breadcrumb-btn" data-focus="keyboard" ' + clickItem + ' ' + hrefItem + '>' + name.esc() + '</a></div>';
      
      mediadivider_element
        .removeClass( 'hide' );
      
      media_element
        .html( html )
        .removeClass( 'hide' );
    }
  },
  init_title: function(libid, sectionpath, sectionmedia)
  {
    var sectionlib = {id:libid, type:null, name:null, username:null, usertype:null, usertitle:null, hostname:null};
    navbar.init_title0(sectionlib, sectionpath, sectionmedia);
  },
  init_title0: function(sectionlib, sectionpath, sectionmedia)
  {
    if (sectionlib == null) sectionlib = {};
    var libid = sectionlib.id;
    var libtype = sectionlib.type;
    var libname = sectionlib.name;
    var libhostname = sectionlib.hostname;
    var libusername = sectionlib.username;
    var libusertype = sectionlib.usertype;
    var libusertitle = sectionlib.usertitle;
    
    var library_id = libid;
    var library_type = libtype;
    var library_name = libname;
    var library_hostname = libhostname;
    var library_param = libid;
    var library_title = '';
    
    var applibraries = globalApp.libraries; //response['libraries'];
    var appstorages = globalApp.storages;
    var libraryContent = [];
    var foundLibrary = false;
    
    var libraries = {};
    if (applibraries != null) {
      for (var key in applibraries) { 
        var library = applibraries[key];
        if (library == null) continue;
        libraries[key] = library;
      }
    }
    if (appstorages != null) {
      for (var skey in appstorages) {
        var storage = appstorages[skey];
        if (storage == null) continue;
        var storage_libs = storage['libraries'];
        if (storage_libs == null) continue;
        for (var lkey in storage_libs) {
          var library = storage_libs[lkey];
          if (library == null) continue;
          libraries[lkey] = library;
        }
      }
    }
    
    if (library_id == 'all') {
      library_type = 'application/x-library-all';
      library_name = strings( 'All' );
      library_param = 'all/' + encodeURIComponent('*:*');
      foundLibrary = true;
    }
    
    for (var key in libraries) { 
      var library = libraries[key];
      if (library == null) continue;
    
      var lib_id = library['id'];
      var lib_name = library['name'];
      var lib_type = library['type'];
      var lib_hostname = library['hostname'];
      var subcount = library['subcount'];
      var sublen = library['sublen'];
    
      if (lib_id == null || lib_id.length == 0)
        continue;
    
      var lib_param = lib_id;
      if (lib_id == 'all')
        lib_param = 'all/' + encodeURIComponent('*:*');
    
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
    
      if (lib_id == library_id) {
        library_type = lib_type;
        library_name = lib_name;
        library_hostname = lib_hostname;
        library_param = lib_param;
        library_title = sizeInfo;
        foundLibrary = true;
      }
    
      var iconClass = 'javen-icon-show-mini';
      if (lib_type != null) {
        if (lib_type.indexOf('image') >= 0)
          iconClass = 'javen-icon-photo-mini';
        else if (lib_type.indexOf('audio') >= 0)
          iconClass = 'javen-icon-artist-mini';
        else if (lib_type.indexOf('video') >= 0)
          iconClass = 'javen-icon-movie-mini';
      }
      
      if (lib_name == null) lib_name = '';
      if (lib_hostname == null) lib_hostname = '';
      
      var item = '        <li><a href="#/~browse/' + lib_param + '" data-focus="keyboard" title="' + sizeInfo.esc() + '">' + "\n" +
                 '          <div class="spinner-container">' + "\n" +
                 '            <i class="javen-icon-section-mini ' + iconClass + ' javen-icon-dark"></i>' + "\n" +
                 '          </div>' + "\n" +
                 '          &nbsp;' + lib_name.esc() + ' <span class="source-title ">(' + lib_hostname.esc() + ')</span>' + "\n" +
                 '        </a></li>' + "\n";
    
      libraryContent.push( item );
    }

    if (foundLibrary == true) {
      var addtitle = strings( 'Add Library' );
      var clickto = 'javascript:opener.hide(\'#section-dropdown-list\');addlibrary.show_add();return false;';
      
      var item = '        <li><a href="" onclick="' + clickto + '" data-focus="keyboard">' + "\n" +
                 '          <div class="spinner-container">' + "\n" +
                 '            <i class="glyphicon database-plus"></i>' + "\n" +
                 '          </div>' + "\n" +
                 '          &nbsp;' + addtitle.esc() + ' <span class="source-title "></span>' + "\n" +
                 '        </a></li>' + "\n";
    
      libraryContent.push( item );
    } else { 
      libraryContent = [];
      
      var lib_id = library_id;
      var lib_name = library_name;
      var lib_type = library_type;
      var lib_hostname = library_hostname;
    
      var lib_param = lib_id;
      if (lib_id == 'all')
        lib_param = 'all/' + encodeURIComponent('*:*');
    
      var iconClass = 'javen-icon-show-mini';
      if (lib_type != null) {
        if (lib_type.indexOf('image') >= 0)
          iconClass = 'javen-icon-photo-mini';
        else if (lib_type.indexOf('audio') >= 0)
          iconClass = 'javen-icon-artist-mini';
        else if (lib_type.indexOf('video') >= 0)
          iconClass = 'javen-icon-movie-mini';
      }
      
      if (lib_name == null) lib_name = '';
      if (lib_hostname == null) lib_hostname = '';
      
      var item = '        <li><a href="#/~browse/' + lib_param + '" data-focus="keyboard">' + "\n" +
                 '          <div class="spinner-container">' + "\n" +
                 '            <i class="javen-icon-section-mini ' + iconClass + ' javen-icon-dark"></i>' + "\n" +
                 '          </div>' + "\n" +
                 '          &nbsp;' + lib_name.esc() + ' <span class="source-title ">(' + lib_hostname.esc() + ')</span>' + "\n" +
                 '        </a></li>' + "\n";
    
      libraryContent.push( item );
    }

    var iconClass = 'javen-icon-show-mini';
    if (library_type != null) {
      if (library_type.indexOf('image') >= 0)
        iconClass = 'javen-icon-photo-mini';
      else if (library_type.indexOf('audio') >= 0)
        iconClass = 'javen-icon-artist-mini';
      else if (library_type.indexOf('video') >= 0)
        iconClass = 'javen-icon-movie-mini';
    }
    
    if (library_name == null) library_name = '';
    if (library_hostname == null) library_hostname = '';
    
    var userme = globalApp.get_username();
    if (userme != libusername && libusername != null && libusername.length > 0) {
      var libtitle = libusertype == 'group' ? strings( 'Group' ) : strings( 'User' );
      if (libusertitle != null && libusertitle.length > 0) {
        libtitle = libtitle + ':' + libusertitle;
      } else {
        libtitle = libtitle + ':' + libusername;
      }
      if (library_title != null && library_title.length > 0) {
        library_title = libtitle + '(' + library_title + ')';
      } else {
        library_title = libtitle;
      }
    }
    
    var dropdownclick = 'javascript:opener.toggle(\'#section-dropdown-list\');return false;';
    
    var dropdown = '    <a class="breadcrumb-placeholder breadcrumb-btn">' + "\n" +
                   '      <i class="glyphicon more"></i>' + "\n" +
                   '    </a>' + "\n" +
                   '    <div id="section-dropdown-list" class="breadcrumb-content dropdown">' + "\n" +
                   '    <a class="breadcrumb-btn split-left" href="#/~browse/' + library_param + '" data-focus="keyboard" title="' + library_title.esc() + '">' + "\n" +
                   '      <div class="breadcrumb-spinner-container spinner-container">' + "\n" +
                   '        <i class="javen-icon-section-mini ' + iconClass + '"></i>' + "\n" +
                   '      </div>' + "\n" +
                   '      ' + library_name.esc() + ' <span class="source-title ">(' + library_hostname.esc() + ')</span>' + "\n" +
                   '    </a>' + "\n" +
                   '    <a class="breadcrumb-btn split-right dropdown-toggle" data-focus="keyboard" data-toggle="dropdown" href="" onclick="' + dropdownclick + '">' + "\n" +
                   '      <i class="glyphicon-caret"></i>' + "\n" +
                   '    </a>' + "\n" +
                   '    <ul class="dropdown-menu dropdown-menu-large">' + "\n" + 
                   libraryContent.join( "\n" ) +
                   '    </ul>' + "\n" +
                   '    </div>';
  
    
    if (library_id != null) {
      var divider_element = $( '#section-divider' );
      var libraries_element = $( '#section-breadcrumb' );

      divider_element.removeClass( 'hide' );
      libraries_element.html( dropdown ).removeClass( 'hide' );
    }
    
    if (sectionpath) {
      var pathdivider_element = $( '#mediapath-divider' );
      var path_element = $( '#mediapath-breadcrumb' );
      
      var id = sectionpath.id;
      var name = sectionpath.name;
      var link = sectionpath.link;
      var click = sectionpath.click;
      
      if (id != null && id.length > 0 && name != null && name.length > 0) {
        var clickhtml = '';
        if (link != null && link.length > 0)
          clickhtml = 'href="' + link.esc() + '"';
        else if (click !=null && click.length > 0)
          clickhtml = 'href="" onclick="' + click.esc() + '"';
        
        var html = '    <a class="breadcrumb-placeholder breadcrumb-btn"><i class="glyphicon more"></i></a>' + "\n" +
                   '    <div class="breadcrumb-content"><a class="breadcrumb-btn" data-focus="keyboard" ' + clickhtml + '>' + name.esc() + '</a></div>';
      
        pathdivider_element.removeClass( 'hide' );
        path_element.html( html ).removeClass( 'hide' );
      }
    }
    
    if (sectionmedia) {
      var mediadivider_element = $( '#media-divider' );
      var media_element = $( '#media-breadcrumb' );
      
      var id = sectionmedia.id;
      var name = sectionmedia.name;
      var link = sectionmedia.link;
      var click = sectionmedia.click;
      
      if (id != null && id.length > 0 && name != null && name.length > 0) {
        var clickhtml = '';
        if (link != null && link.length > 0)
          clickhtml = 'href="' + link.esc() + '"';
        else if (click !=null && click.length > 0)
          clickhtml = 'href="" onclick="' + click.esc() + '"';
        
        var html = '    <a class="breadcrumb-placeholder breadcrumb-btn"><i class="glyphicon more"></i></a>' + "\n" +
                   '    <div class="breadcrumb-content"><a class="breadcrumb-btn" data-focus="keyboard" ' + clickhtml + '>' + name.esc() + '</a></div>';
      
        mediadivider_element.removeClass( 'hide' );
        media_element.html( html ).removeClass( 'hide' );
      }
    }
  },
  oninited: function()
  {
    if (globalApp.user != null)
    {
      var username = globalApp.get_username();
      var nickname = globalApp.user['nick'];
      if (username == null) username = '';
      if (nickname == null) nickname = '';
      
      var invite_count = globalApp.invite_count;
      var message_count = globalApp.message_count;
      var join_count = 0;
      var join_hide = 'hide';
      var invite_hide = 'hide';
      var message_hide = 'hide';
      
      if (invite_count > 0) invite_hide = '';
      if (message_count > 0) message_hide = '';
      if (join_count > 0) join_hide = '';
      
      var content = [];
      
      var title = username;
      if (username.length < 15) {
        if (nickname != null && nickname.length > 0) {
          var len = 15 - username.length - 2;
          if (len > 0) {
            if (nickname.length > len)
              nickname = nickname.substring(0, len) + '...';
            title = title + '(' + nickname + ')';
          }
        }
      }
      
      var item = '<li><a data-focus="keyboard" href="" onClick="javascript:navbar.go_profile();return false;"><i class="glyphicon user"></i> ' + title.esc() + '</a></li>';
      content.push( item );
      
      item = '<li class="divider"></li>';
      content.push( item );
      
      title = strings( 'Friends' );
      item = '<li><a data-focus="keyboard" href="" onClick="javascript:navbar.go_friends();return false;"><i class="glyphicon parents"></i> ' + title.esc() + ' <span class="friend-requests-badge ' + invite_hide + '">' + invite_count + '</span></a></li>';
      content.push( item );
      
      title = strings( 'Groups' );
      item = '<li><a data-focus="keyboard" href="" onClick="javascript:navbar.go_groups();return false;"><i class="glyphicon parents"></i> ' + title.esc() + ' <span class="friend-requests-badge ' + join_hide + '">' + join_count + '</span></a></li>';
      content.push( item );
      
      item = '<li class="divider"></li>';
      content.push( item );
      
      title = strings( 'Publications' );
      item = '<li><a data-focus="keyboard" href="" onClick="javascript:navbar.go_publication();return false;"><i class="glyphicon globe"></i> ' + title.esc() + '</a></li>';
      content.push( item );
      
      title = strings( 'Messages' );
      item = '<li><a data-focus="keyboard" href="" onClick="javascript:navbar.go_messags();return false;"><i class="glyphicon message-new"></i> ' + title.esc() + ' <span class="friend-requests-badge ' + message_hide + '">' + message_count + '</span></a></li>';
      content.push( item );
      
      item = '<li class="divider"></li>';
      content.push( item );
      
      title = strings( 'Sign Out' );
      item = '<li><a class="signout-btn" data-focus="keyboard" href="" onClick="javascript:navbar.signout();return false;"><i class="glyphicon exit"></i> ' + title.esc() + '</a></li>';
      content.push( item );
      
      $( '#account-dropdown-list' )
        .html( content.join("\n") );
    }
    
    uploader.init_taskicon();
  },
  go_profile: function() 
  {
    var context = system.context;
    context.redirect( '#/~profile' );
  },
  go_friends: function() 
  {
    var context = system.context;
    context.redirect( '#/~friends' );
  },
  go_groups: function() 
  {
    var context = system.context;
    context.redirect( '#/~groups' );
  },
  go_publication: function()
  {
    var context = system.context;
    context.redirect( '#/~posts/me' );
  },
  go_messags: function() 
  {
    var context = system.context;
    context.redirect( '#/~messages' );
  },
  signout: function() 
  {
    if (logoutform)
      logoutform.logout();
  }
};

var searchform = {
  callback: null,
  library_id: null,
  
  init: function( form_element ) 
  { 
    form_element
      .attr('onsubmit', 'javascript:return searchform.submit();');
  },
  initcb: function( form_element, callback ) 
  { 
    this.callback = callback;
    this.init( form_element );
  },
  submit: function() 
  { 
    var searchinput_element = $( '#search-input' );
    var query = searchinput_element.val();
    if (query == null) query = '';
    query = query.trim();
    
    if (query == '' || query == '*')
      query = '*:*';
    
    var library = this.library_id;
    //if (this.callback != null) 
    //  library = this.callback.call( this );
    //if (library == null || library.length == 0)
      library = 'all';
    
    if (query != null && query.length > 0) {
      var href = '#/~browse/' + library + '/' + encodeURIComponent(query);
      var context = system.context;
      context.redirect( href );
    }
    
    return false;
  }
};
