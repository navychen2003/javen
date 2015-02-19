
var details_tags = {
  values: [],
  keyaction: null,
  
  init_value: function( value )
  {
    this.values = [];
    
    if (value != null && value.length > 0) {
      var vals = value.split( ',' );
      if (vals) {
        for (var key in vals) {
          var val = vals[key];
          this.add_value( val );
        }
      }
    }
    
    this.init_values();
  },
  add_value: function( value )
  {
    if (value == null || value.length == 0)
      return;
    
    for (var key in this.values) {
      var val = this.values[key];
      if (val == value) return;
    }
    
    this.values.push( value );
  },
  init_values: function()
  {
    $( '#details-tags-autogen' )
      .attr( 'onClick', 'javascript:details_tags.onfocus();return false;' );
    
    var values = this.values;
    if (values == null) values = [];
    
    var valueContent = [];
    var valueStr = '';
    
    for (var key in values) {
      var value = values[key];
      if (value == null || value.length == 0) 
        continue;
      
      if (valueStr.length > 0) valueStr += ',';
      valueStr += value;
      
      var clickto = 'javascript:details_tags.remove_value(' + key + ');return false;';
      var item = '<li class="select2-search-choice"><div>' + value.esc() + '</div><a onclick="' + clickto + '" class="select2-search-choice-close" tabindex="-1"></a></li>';
      
      valueContent.push( item );
    }
    
    var keydown = 'javascript:return details_tags.input_keydown(this,event);';
    var keyup = 'javascript:details_tags.input_keyup(this,event);';
    var focus = 'javascript:details_tags.input_focus(this,event);';
    var blur = 'javascript:details_tags.input_blur(this,event);';
    
    var item = '<li class="select2-search-field"><input id="details-tags-selectinput" type="text" autocomplete="off" class="select2-input" style="width: 10px;" onKeyDown="' + keydown + '" onKeyUp="' + keyup + '" onFocus="' + focus + '" onBlur="' + blur + '"></li>';
    
    valueContent.push( item );
    
    $( '#details-tags-list' ).html( valueContent.join( '\n' ) );
    $( '#details-tags-input' ).attr( 'value', valueStr.esc() );
    
    this.keyaction = null;
  },
  remove_value: function( key )
  {
    if (this.values == null) return;
    
    if (key >= 0 && key < this.values.length) {
      this.values.splice(key, 1);
      this.init_values();
    }
  },
  onfocus: function( element, event )
  {
    $( '#details-tags-autogen' )
      .addClass( 'select2-container-active' );
    $( '#details-tags-selectinput' )
      .addClass( 'select2-focused' )
      .focus();
  },
  input_keydown: function( element, event )
  {
    var code = event.keyCode;
    if (code == 13 || code == 32 || code == 188) {
      this.keyaction = 'add';
      return false;
    }
    if (code == 8) {
      var value = element.value;
      if (value != null && value.length > 0) return true;
      this.keyaction = 'remove';
      return false;
    }
    //if (code == 16 || code == 8 || code == 37 || code == 39) return true;
    //if (code >= 65 && code <= 90) return true;
    //if (code >= 48 && code <= 57) return true;
    //if (code == 190 || code == 188 || code == 189) return true;
    return true;
  },
  input_keyup: function( element, event )
  {
    var value = element.value;
    var action = this.keyaction;
    this.keyaction = null;
    
    if (value != null && value.length > 0) {
      var chr = value.charCodeAt(value.length-1);
      if (chr == 65292 || chr == 32) {
        value = value.substring(0,value.length-1);
        element.value = value;
        action = 'add';
      }
    }
    
    if (action == 'add') {
      if (value != null && value.length > 0) {
        this.add_value( value );
        this.init_values();
        this.onfocus();
      }
      return;
    } else if (action == 'remove') {
      if (this.values.length > 0) {
        this.values.splice( this.values.length -1, 1);
        this.init_values();
        this.onfocus();
      }
      return;
    }
    
    var len = 0;
    
    if (value != null && value.length > 0) {
      for (var i=0; i < value.length; i++) {
        var chr = value.charCodeAt(i);
        if (chr >= 0 && chr <= 127)
          len ++;
        else
          len += 2;
      }
    }
    
    var width = (len + 1) * 8;
    if (width <= 10) width = 10;
    if (width >= 392) width = 392;
    
    $( '#details-tags-selectinput' ).attr( 'style', 'width: ' + width + 'px;' );
  },
  input_focus: function( element, event )
  {
  },
  input_blur: function( element, event )
  {
    var value = element.value;
    if (value != null && value.length > 0) {
      this.add_value( value );
      this.init_values();
    }
    
    $( '#details-tags-autogen' )
      .removeClass( 'select2-container-active' );
    $( '#details-tags-selectinput' )
      .removeClass( 'select2-focused' );
  }
};

var listdetails = { 
  id_param: null,
  section: null,
  section_id: null,
  section_type: null,
  section_name: null,
  permissions: null,
  operations : null,
  root_id: null,
  library_id: null,
  library_type: null,
  library_name: null,
  extension_selects: null,
  selectlist: null,
  selectelement: null,
  lockelement: null,
  empty_poster: null,
  empty_background: null,
  slidephotos: [],
  
  changeposter: function()
  {
    var emptysrc = this.empty_poster;
    
    artwork.showselect('Public Files', function( section ) {
          if (section) {
            var id = section['id'];
            if (id == null || id.length <= 0) id = 'null';
            listdetails.save_artwork(id, null);
          }
        }, emptysrc);
  },
  changebackground: function()
  {
    var emptysrc = this.empty_background;
    
    artwork.showselect('Public Files', function( section ) {
          if (section) {
            var id = section['id'];
            if (id == null || id.length <= 0) id = 'null';
            listdetails.save_artwork(null, id);
          }
        }, emptysrc);
  },
  showinfo: function()
  {
    var section = this.section;
    if (section == null) return;
    
    fileinfo.showdetails( section );
  },
  click_poster: function()
  {
    var section = this.section;
    if (section == null) return;
    
    photoslide.show( listdetails.slidephotos, 0, false );
  },
  goplay: function()
  {
    var section = this.section;
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
    
    var section_id = this.section_id;
    if (section_id == null) return;
    
    var openlink = '#/~play/' + section_id;
    var context = system.context;
    context.redirect( openlink );
  },
  godetails: function()
  {
    var section_id = this.section_id;
    if (section_id == null) return;
    
    var openlink = '#/~details/' + section_id;
    var context = system.context;
    context.redirect( openlink );
  },
  goedit: function()
  {
    var section_id = this.section_id;
    if (section_id == null) return;
    
    var openlink = '#/~edit/' + section_id;
    var context = system.context;
    context.redirect( openlink );
  },
  showdetails: function()
  {
    this.showlist( 'details' );
  },
  showedit: function()
  {
    this.showlist( 'edit' );
  },
  showplay: function()
  {
    this.showlist( 'play' );
  },
  showshare: function()
  {
    var section = this.section;
    if (section == null) return;
    
    var items = [];
    items.push( section );
    
    compose.share( items );
  },
  save_submit: function()
  {
  },
  save_cancel: function()
  {
    this.godetails();
  },
  save_artwork: function(poster_id, background_id)
  {
    if (poster_id == null && background_id == null)
      return;
    
    var section_id = this.section_id;
    if (section_id == null) return;
    
    var poster = poster_id;
    var background = background_id;
    
    if (poster == null) poster = '';
    if (background == null) background = '';
    
    var params = '&action=updateposter&id=' + encodeURIComponent(section_id) 
               + '&poster=' + encodeURIComponent(poster) 
               + '&background=' + encodeURIComponent(background);
    
    $.ajax
    (
      {
        url : app.base_path + '/update?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
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
            sammy.refresh();
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
  showlist: function( action )
  {
    var editlink_element = $( '#action-edit-link' );
    var downloadlink_element = $( '#action-download-link' );
    var sharelink_element = $( '#action-share-link' );
    var infolink_element = $( '#action-info-link' );
    
    editlink_element
      .attr( 'title', strings('Edit') )
      .attr( 'href', 'javascript:listdetails.goedit();' );
    
    downloadlink_element
      .attr( 'title', strings('Download') )
      .attr( 'href', '' );
    
    sharelink_element
      .attr( 'title', strings('Share') )
      .attr( 'href', 'javascript:listdetails.showshare();' );
    
    infolink_element
      .attr( 'title', strings('Information') )
      .attr( 'href', 'javascript:listdetails.showinfo();' );
    
    this.slidephotos = [];
    
    var id = this.id_param;
    var params = '&action=details&id=' + encodeURIComponent(id);

    $.ajax
    (
      {
        url : app.base_path + '/sectioninfo?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
          listlibrary.initlist( function() { 
              navbar.init_title( listdetails.library_id );
            });
        },
        success : function( response, text_status, xhr )
        {
          listdetails.init_values( action, response );
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
  init_values: function( action, response )
  {
    var section = response;
    
    var section_id = section['id'];
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
    
    var extension = extname;
    var meta_author = '';
    var meta_genre = '';
    var meta_album = '';
    var meta_year = '';
    var meta_title = '';
    var meta_subtitle = '';
    var meta_summary = '';
    var meta_tags = '';
    
    var section_details = section['details'];
    if (section_details) {
      var metadata = section_details['metadata'];
      if (metadata) {
        meta_author = metadata['author'];
        meta_genre = metadata['genre'];
        meta_album = metadata['album'];
        meta_year = metadata['year'];
        meta_title = metadata['title'];
        meta_subtitle = metadata['subtitle'];
        meta_summary = metadata['summary'];
        meta_tags = metadata['tags'];
      }
    }
    
    if (meta_author == null) meta_author = '';
    if (meta_genre == null) meta_genre = '';
    if (meta_album == null) meta_album = '';
    if (meta_year == null) meta_year = '';
    if (meta_title == null) meta_title = '';
    if (meta_subtitle == null) meta_subtitle = '';
    if (meta_summary == null) meta_summary = '';
    if (meta_tags == null) meta_tags = '';
    
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
  
    listdetails.section = response;
    listdetails.section_type = section_type;
    listdetails.section_name = section_name;
    listdetails.section_id = section_id;
    listdetails.root_id = root_id;
    listdetails.permissions = section_perms;
    listdetails.operations = section_ops;
  
    listdetails.library_type = library_type;
    listdetails.library_name = library_name;
    listdetails.library_id = library_id;
    searchform.library_id = library_id;
  
    if (library_id != null) {
      if (library_type != null && library_type.indexOf('image') >= 0)
        section_title = strings( 'My Photo: ' ) + library_name + ' (' + library_hostname + ')';
      else
        section_title = strings( 'My Library: ' ) + library_name + ' (' + library_hostname + ')';
    } else { 
      section_title = strings( 'My Library' );
      if (system.friendlyName != null)
        section_title = section_title + ' (' + system.friendlyName + ')';
    }
  
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var backlink_element = $( '#back-link' );
  
    title_element
      .html( section_title.esc() );
  
    if (query == null && listsection && listsection.query_param != null && listsection.query_param.length > 0) 
      query = decodeURIComponent(listsection.query_param);
  
    if (query != null && query.length > 0) {
      headbar.backlinkto = '#/~browse/all/' + encodeURIComponent(query);
    } else if (parent_id == null || parent_id.length == 0) {
      if (isfolder == false)
        headbar.backlinkto = '#/~dashboard';
      else
        headbar.backlinkto = '#/~browse/' + section_id;
    } else {
      if (isfolder == false)
        headbar.backlinkto = '#/~browse/' + parent_id;
      else
        headbar.backlinkto = '#/~browse/' + section_id;
    }
  
    if (library_name == null) library_name = '';
    if (library_hostname == null) library_hostname = '';
  
    var sectionpath = null;
    var sectionmedia = null;
  
    if (parent_id != null && parent_id.length > 0 && parent_id != library_id) {
      if (parent_name != null && parent_name.length > 0) {
        var parentLink = '#/~browse/' + parent_id;
        var displayName = listdetails.toDisplaySectionName(parent_name, parent_type);
        sectionpath = {id:parent_id, name:displayName, type:parent_type, link:parentLink};
      }
    }
  
    if (section_id != null && section_id.length > 0 && section_id != library_id) {
      if (section_name != null && section_name.length > 0) {
        var sectionLink = ''; //'#/~details/' + section_id;
        var sectionClick = 'javascript:fileinfo.showdetailsid(\'' + section_id + '\');return false;';
        var displayName = listdetails.toDisplaySectionName(section_name, section_type);
        if (isfolder == true) sectionLink = '#/~browse/' + section_id;
        sectionmedia = {id:section_id, name:displayName, type:section_type, link:sectionLink, click:sectionClick};
      }
    }
  
    var sectionlib = {id:library_id, type:library_type, name:library_name, username:username, usertype:usertype, usertitle:usertitle, hostname:library_hostname};
    navbar.init_title0( sectionlib, sectionpath, sectionmedia );
  
    var openlink = '';
    var downlink = '';
    var postersrc = 'css/' + app.theme + '/images/posters/poster.png';
    var name = section_name;
  
    if (isfolder == false && name.indexOf('.') >= 0) { 
      var pos = name.lastIndexOf('.');
      if (pos > 0) name = name.substring(0, pos);
      if (name == null) name = '';
    }
  
    var canplay = false;
    var canplaymusic = false;
    var canposter = true;
    var playhtml = '';
    var detailhtml = '';
    var metaduration = '';
  
    if (section_type.indexOf('image') == 0) {
      postersrc = app.base_path + '/image/' + section_id + '_256.' + extension + '?token=' + app.token;
      openlink = app.base_path + '/image/' + section_id + '_0.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
      
      metaduration = width + ' x ' + height;
      canposter = false;
      
      listdetails.slidephotos = [];
      listdetails.slidephotos.push( section );
    
    } else if (section_type.indexOf('audio/') == 0) {
      postersrc = 'css/' + app.theme + '/images/posters/music.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
    
      metaduration = musicplayer.readableSeconds( timeLen / 1000 );
      
      canplay = true;
      canplaymusic = true;
      canposter = true;
    
    } else if (section_type.indexOf('video/') == 0) { 
      postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
    
      metaduration = musicplayer.readableSeconds( timeLen / 1000 );
      
      var videosrc = openlink;
      if (section_type.indexOf('/flv') >= 0) { 
        playhtml = '<object classid="clsid:d27cdb6e-ae6d-11cf-96b8-444553540000" codebase="http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,0,0" width="100%" height="100%">' + "\n" +
                   '<param name="movie" value="vcastr22.swf"><param name="quality" value="high">' + "\n" +
                   '<param name="menu" value="false"><param name="allowFullScreen" value="true" />' + "\n" +
                   '<param name="FlashVars" value="vcastr_file=' + videosrc.esc() + '" />' + "\n" +
                   '<embed src="vcastr22.swf" allowFullScreen="true" FlashVars="vcastr_file=' + videosrc.esc() + '" menu="false" quality="high" width="100%" height="100%" type="application/x-shockwave-flash" pluginspage="http://www.macromedia.com/go/getflashplayer" />' + "\n" +
                   '</object>';
      } else {
        playhtml = '<video controls src="' + videosrc + '" poster="" width="100%" height="100%">' + "\n" +
                   'Your browser does not support the <code>video</code> element.' + "\n" +
                   '</video>';
      }
      canplay = true;
      canposter = true;
    
    } else if (section_type.indexOf('text/') == 0) { 
      postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
    
      metaduration = readableBytes( length );
      canposter = true;
    
    } else if (isfolder == false) { 
      postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
      
      metaduration = readableBytes( length );
      canposter = true;
      
    } else if (isfolder == true) {
      postersrc = 'css/' + app.theme + '/images/posters/folder.png';
      canposter = true;
    }
  
    section['openlink'] = openlink;
    section['downlink'] = downlink;
  
    listdetails.empty_poster = postersrc;
    listdetails.empty_background = 'css/' + app.theme + '/images/background.png';
  
    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      postersrc = app.base_path + '/image/' + imgid + '_256t.' + imgext + '?token=' + app.token;
      
      listdetails.slidephotos = [];
      listdetails.slidephotos.push( section );
    }
  
    if (background != null && background.length > 0) {
      var imgid = background;
      var imgext = 'jpg';
      var src = app.base_path + '/image/' + imgid + '.' + imgext + '?token=' + app.token;
      
      $( '#background-image' )
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
  
    var editlink_element = $( '#action-edit-link' );
    var downloadlink_element = $( '#action-download-link' );
    var downloaditem_element = $( '#action-download-item' );
    var infolink_element = $( '#action-info-link' );
  
    var poster_element = $( '#details-poster' );
    var posterlink_element = $( '#details-poster-link' );
    var changeposter_element = $( '#details-change-poster' );
    var changebackground_element = $( '#details-change-background' );
    var postertext_element = $( '#details-change-poster-text' );
    var backgroundtext_element = $( '#details-change-background-text' );
    var playbutton_element = $( '#details-play-button' );
    var playlink_element = $( '#details-play-link' );
    var playtext_element = $( '#details-play-text' );
  
    if (downloadlink_element) {
      downloadlink_element
        .attr( 'href', downlink.esc() )
        .attr( 'target', '_blank' );
    }
  
    if (poster_element) {
      poster_element.attr( 'src', postersrc );
    }
    if (posterlink_element) {
      posterlink_element
        .attr( 'onclick', 'javascript:listdetails.click_poster();return false;' )
        .attr( 'href', '' );
    }
    if (postertext_element) {
      postertext_element.html( strings( 'Change Poster' ) );
    }
    if (backgroundtext_element) {
      backgroundtext_element.html( strings( 'Change Background' ) );
    }
    if (playtext_element) {
      playtext_element.html( strings( 'Play' ) );
    }
    if (changeposter_element) { 
      changeposter_element
        .attr( 'onClick', 'javascript:listdetails.changeposter();return false;' )
        .attr( 'href', '' )
        .addClass( 'hide' );
    }
    if (changebackground_element) { 
      changebackground_element
        .attr( 'onClick', 'javascript:listdetails.changebackground();return false;' )
        .attr( 'href', '' )
        .addClass( 'hide' );
    }
    if (playlink_element) {
      playlink_element
        .attr( 'onClick', 'javascript:listdetails.goplay();return false;' )
        .attr( 'href', '' );
    }
    if (playbutton_element) {
      playbutton_element.addClass( 'hide' );
    }
  
    if (action == 'edit') {
      var nametext_element = $( '#details-name-text' );
      var nameinput_element = $( '#details-name-input' );
      
      var extensiontext_element = $( '#details-extension-text' );
      var extensioninput_element = $( '#details-extension-input' );
      
      var authortext_element = $( '#details-author-text' );
      var authorinput_element = $( '#details-author-input' );
      
      var albumtext_element = $( '#details-album-text' );
      var albuminput_element = $( '#details-album-input' );
      
      var yeartext_element = $( '#details-year-text' );
      var yearinput_element = $( '#details-year-input' );
      
      var titletext_element = $( '#details-title-text' );
      var titleinput_element = $( '#details-title-input' );
      
      var subtitletext_element = $( '#details-subtitle-text' );
      var subtitleinput_element = $( '#details-subtitle-input' );
      
      var summarytext_element = $( '#details-summary-text' );
      var summaryinput_element = $( '#details-summary-input' );
      
      var tagstext_element = $( '#details-tags-text' );
      var tagsinput_element = $( '#details-tags-input' );
      
      var submitbutton_element = $( '#details-submit-button' );
      var cancelbutton_element = $( '#details-cancel-button' );
      
      nametext_element.html( strings('Name') );
      extensiontext_element.html( strings('Extension') );
      authortext_element.html( strings('Author') );
      albumtext_element.html( strings('Album') );
      yeartext_element.html( strings('Year') );
      titletext_element.html( strings('Title') );
      subtitletext_element.html( strings('Sub Title') );
      summarytext_element.html( strings('Summary') );
      tagstext_element.html( strings('Tags') );
      
      submitbutton_element
        //.attr( 'onclick', 'javascript:listdetails.save_submit();return false;' )
        .html( strings('Save') );
        
      cancelbutton_element
        .attr( 'onclick', 'javascript:listdetails.save_cancel();return false;' )
        .html( strings('Cancel') );
      
      listdetails.init_form();
      
      listdetails.init_lockelement( '#details-name-lock' );
      listdetails.init_lockelement( '#details-extension-lock' );
      listdetails.init_lockelement( '#details-author-lock' );
      listdetails.init_lockelement( '#details-album-lock' );
      listdetails.init_lockelement( '#details-year-lock' );
      listdetails.init_lockelement( '#details-title-lock' );
      listdetails.init_lockelement( '#details-subtitle-lock' );
      listdetails.init_lockelement( '#details-summary-lock' );
      listdetails.init_lockelement( '#details-tags-lock' );
  
      if (isfolder == true) {
        $( '#details-extension-group' ).addClass( 'hide' );
        $( '#details-author-group' ).addClass( 'hide' );
        $( '#details-album-group' ).addClass( 'hide' );
        $( '#details-year-group' ).addClass( 'hide' );
        $( '#details-title-group' ).addClass( 'hide' );
        $( '#details-subtitle-group' ).addClass( 'hide' );
        $( '#details-summary-group' ).addClass( 'hide' );
        $( '#details-tags-group' ).addClass( 'hide' );
      }
      
      if (nameinput_element) {
        nameinput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-name-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-name-lock\', true);' )
          .attr( 'value', name.esc() );
      }
      if (extensioninput_element) {
        extensioninput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-extension-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-extension-lock\', true);' )
          .attr( 'onFocus', 'javascript:listdetails.on_extension_focus(this);' )
          .attr( 'onBlur', 'javascript:listdetails.on_extension_blur(this);' )
          .attr( 'value', extname.esc() );
      }
      if (authorinput_element) {
        authorinput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-author-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-author-lock\', true);' )
          .attr( 'value', meta_author.esc() );
      }
      if (albuminput_element) {
        albuminput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-album-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-album-lock\', true);' )
          .attr( 'value', meta_album.esc() );
      }
      if (yearinput_element) {
        yearinput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-year-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-year-lock\', true);' )
          .attr( 'value', meta_year.esc() );
      }
      if (titleinput_element) {
        titleinput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-title-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-title-lock\', true);' )
          .attr( 'value', meta_title.esc() );
      }
      if (subtitleinput_element) {
        subtitleinput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-subtitle-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-subtitle-lock\', true);' )
          .attr( 'value', meta_subtitle.esc() );
      }
      if (summaryinput_element) {
        summaryinput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-summary-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-summary-lock\', true);' )
          .attr( 'value', meta_summary.esc() );
      }
      if (tagsinput_element) {
        tagsinput_element
          .attr( 'onChange', 'javascript:listdetails.lockchanged(\'#details-tags-lock\', true);' )
          .attr( 'onKeyDown', 'javascript:listdetails.lockchanged(\'#details-tags-lock\', true);' )
          .attr( 'value', '' );
      }
      
      details_tags.init_value( meta_tags );
      
      if (changeposter_element) { 
        changeposter_element.removeClass( 'hide' );
      }
      if (changebackground_element) { 
        changebackground_element.removeClass( 'hide' );
      }
      
      $( '#details-controls' ).removeClass( 'hide' );
      $( '#details-metadata' ).addClass( 'hide' );
      $( '#details-form' ).removeClass( 'hide' );
      $( '#player-container' ).addClass( 'hide' );
      $( '#player-details' ).addClass( 'hide' );
      
    } else if (action == 'details') {
      if (musicprogress) 
        musicprogress.add_callback( listdetails.onmusicevent );
      
      var year = meta_year;
      var title = meta_title;
      var genre = meta_genre;
      var duration = metaduration;
      var subtitle = meta_subtitle;
      var director = meta_author;
      var cast = meta_album;
      var summary = meta_summary;
      var tags = meta_tags;
      
      if (title == null || title.length == 0) title = section_name;
      if (year == null) year = '';
      if (genre == null) genre = '';
      if (duration == null) duration = '';
      if (subtitle == null) subtitle = '';
      if (director == null) director = '';
      if (cast == null) cast = '';
      if (summary == null) summary = '';
      if (tags == null) tags = '';
      
      var ownerinfo = '';
      if (owner != null && owner.length > 0) {
        if (ownertitle == null || ownertitle.length == 0)
          ownertitle = owner;
        
        var chatbutton = '';
        if (ownertype == 'group') {
          var chattitle = strings( 'Conversation in {0}' ).format(ownertitle);
          var chatclick = 'javascript:messageinfo.showconversation(\'' + owner + '\');';
          chatbutton = '<button class="player-btn " onclick="' + chatclick + '" title="' + chattitle.esc() + '" style="width: 20px;height: 20px;font-size: 18px;"><i class="glyphicon conversation"></i></button>';
        } else {
          var chattitle = strings( 'Chat with {0}' ).format(ownertitle);
          var chatclick = 'javascript:messageinfo.showchat(\'' + owner + '\');';
          chatbutton = '<button class="player-btn " onclick="' + chatclick + '" title="' + chattitle.esc() + '" style="width: 20px;height: 20px;font-size: 18px;"><i class="glyphicon chat"></i></button>';
        }
        
        ownerinfo = '<a href="" onClick="javascript:userinfo.showdetails(\'' + owner + '\');return false;">' + ownertitle + '</a>' + chatbutton;
      }
      
      $( '#details-metadata-owner-name' ).html( strings( 'Owner' ) );
      $( '#details-metadata-director-name' ).html( strings( 'Author' ) );
      $( '#details-metadata-cast-name' ).html( strings( 'Album' ) );
      $( '#details-metadata-tags-name' ).html( strings( 'Tags' ) );
      
      $( '#details-metadata-year' ).html( year.esc() );
      $( '#details-metadata-title' ).html( title.esc() );
      $( '#details-metadata-title2' ).html( genre.esc() );
      $( '#details-metadata-name' ).html( duration.esc() );
      $( '#details-metadata-subtitle' ).html( subtitle.esc() );
      $( '#details-metadata-director' ).html( director.esc() );
      $( '#details-metadata-cast' ).html( cast.esc() );
      $( '#details-metadata-summary' ).html( summary );
      $( '#details-metadata-tags' ).html( tags );
      $( '#details-metadata-owner' ).html( ownerinfo );
      
      if (playbutton_element) {
        playbutton_element.removeClass( 'hide' );
      }
      
      $( '#details-controls' ).removeClass( 'hide' );
      $( '#details-metadata' ).removeClass( 'hide' );
      $( '#details-form' ).addClass( 'hide' );
      $( '#player-container' ).addClass( 'hide' );
      $( '#player-details' ).addClass( 'hide' );
      
    } else if (action == 'play') {
      if (musicplayer) musicplayer.stop();
      
      var title = meta_title;
      var summary = meta_summary;
      var duration = metaduration;
      
      if (title == null || title.length == 0) title = section_name;
      if (summary == null) summary = '';
      if (duration == null) duration = '';
      
      var detailsClick = 'javascript:listdetails.clickdetails();return false;';
      var titleClick = 'javascript:listdetails.clicktitle();return false;';
      
      detailhtml = '<a href="" onclick="' + detailsClick + '" class="details-btn player-action-btn"><i class="glyphicon circle-info"></i> ' + strings( 'Details' ) + '</a>' + "\n" +
	        '<h2><a href="" onclick="' + titleClick + '">' + title.esc() + '</a></h2>' + "\n" +
	        '<div id="player-details-more" class="video-more-details">' + "\n" +
		    '<img class="poster player-poster" src="' + postersrc + '">' + "\n" +
		    '<div class="metadata">' + "\n" +
			'   <span>' + duration.esc() + '</span>' + "\n" +
			'   <span class="rating"><i class="glyphicon star"></i><i class="glyphicon star"></i><i class="glyphicon dislikes"></i><i class="glyphicon dislikes"></i><i class="glyphicon dislikes"></i></span>' + "\n" +
		    '</div>' + "\n" +
		    '<p class="summary">' + summary + '</p>' + "\n" +
	        '</div>';
      
      var playcontainer_element = $( '#player-container' );
      var playwrapper_element = $( '#player-wrapper' );
      var playdetails_element = $( '#player-details' );
    
      if (playwrapper_element) { 
        playwrapper_element.html( playhtml );
      }
      if (playcontainer_element) {
        if (playhtml == null || playhtml.length == 0)
          playcontainer_element.addClass( 'hide' );
      }
      if (playdetails_element) {
        playdetails_element.html( detailhtml );
        if (detailhtml == null || detailhtml.length == 0)
          playdetails_element.addClass( 'hide' );
      }
      
      $( '#details-controls' ).addClass( 'hide' );
      $( '#details-metadata' ).addClass( 'hide' );
      $( '#details-form' ).addClass( 'hide' );
      $( '#player-container' ).removeClass( 'hide' );
      $( '#player-details' ).removeClass( 'hide' );
    }
    
    if (isfolder) {
      if (downloaditem_element)
        downloaditem_element.addClass( 'hide' );
    }
    if (canplay == false) { 
      if (playbutton_element)
        playbutton_element.addClass( 'hide' );
    }
  
    if (canposter == false) {
      if (changeposter_element)
        changeposter_element.addClass( 'hide' );
      if (changebackground_element)
        changebackground_element.addClass( 'hide' );
    }
    
    if (fileinfo.can_modifysection(section)) {
      $( '#action-edit-item' ).removeClass( 'hide' );
    } else {
      $( '#action-edit-item' ).addClass( 'hide' );
    }
    
    if (fileinfo.can_sharesection(section)) {
      $( '#action-share-item' ).removeClass( 'hide' );
    } else {
      $( '#action-share-item' ).addClass( 'hide' );
    }
  },
  init_form: function()
  {
    var form_element = $( '#details-form' );
    
    var id = this.id_param;
    var params = '&action=update&id=' + encodeURIComponent(id);
    
    form_element
      .ajaxForm
      (
        {
          url : app.base_path + '/update?token=' + app.token + params + '&wt=json',
          dataType : 'json',
          beforeSubmit : function( array, form, options )
          {
            show_loading();
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
              listdetails.godetails();
            }
          },
          error : function( xhr, text_status, error_thrown )
          {
            request_error( xhr, text_status, error_thrown );
          },
          complete : function()
          {
            hide_loading();
          }
        }
      );
  },
  onmusicevent: function( eventname, event, section )
  {
    if (eventname == null || event == null || section == null)
      return;
    
    var section_id = section['id'];
    if (section_id == null || section_id.length == 0 || section_id != listdetails.section_id)
      return;
    
    var playtext_element = $( '#details-play-text' );
    if (playtext_element) {
      if (eventname == 'playing') playtext_element.html( strings( 'Playing' ) );
      else playtext_element.html( strings( 'Play' ) );
    }
  },
  clickdetails: function()
  {
    var more_element = $( '#player-details-more' );
    if (more_element) {
      if (more_element.hasClass( 'open' ))
        more_element.removeClass( 'open' );
      else
        more_element.addClass( 'open' );
    }
  },
  clicktitle: function()
  {
  },
  toDisplaySectionName: function( section_name, section_type )
  {
    return fileinfo.getdisplayname( section_name, section_type );
  },
  init_lockelement: function( idname )
  {
    if (idname == null) return;
    var lockelement = $( idname );
    if (lockelement && idname) {
      lockelement
        .attr( 'onClick', 'javascript:listdetails.changelock(\'' + idname + '\');' );
    }
  },
  changelock: function( idname )
  {
    if (idname == null) return;
    var lockelement = $( idname );
    if (lockelement && idname) {
      if (lockelement.hasClass( 'selected' )) {
        lockelement.removeClass( 'selected' );
      } else { 
        lockelement.addClass( 'selected' );
      }
    }
  },
  lockchanged: function( idname, changed )
  {
    if (idname == null) return;
    var lockelement = $( idname );
    if (lockelement && idname) {
      if (changed == false) {
        lockelement.removeClass( 'selected' );
      } else { 
        lockelement.addClass( 'selected' );
      }
    }
  },
  on_extension_focus: function( elem )
  {
    var section_type = this.section_type;
    var mimetype = '';
    var selectedval = '';
    
    if (section_type != null) {
      var pos = section_type.indexOf('/');
      if (pos > 0) {
        mimetype = section_type.substring(0, pos) + '/*';
        selectedval = section_type.substring(pos+1);
      }
    }
    
    if (mimetype == null || mimetype.length == 0)
      return;
    
    if (listdetails.extension_selects == null) listdetails.extension_selects = {};
    var extension_values = listdetails.extension_selects[mimetype];
    if (extension_values) {
      listdetails.selectelement = $( '#details-extension-input' );
      listdetails.lockelement = $( '#details-extension-lock' );
      listdetails.showselects( elem, extension_values, selectedval );
      return;
    }
    
    var params = '&name=file.extension&mimetype=' + encodeURIComponent(mimetype);

    $.ajax
    (
      {
        url : app.base_path + '/value?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var values = response['values'];
          if (listdetails.extension_selects == null) listdetails.extension_selects = {};
          listdetails.extension_selects[mimetype] = values;
          listdetails.selectelement = $( '#details-extension-input' );
          listdetails.lockelement = $( '#details-extension-lock' );
          listdetails.showselects( elem, values, selectedval );
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
  showselects: function( elem, values, selected_value )
  {
    if (elem == null || values == null)
      return;
    
    var offsetLeft = elem.offsetLeft + elem.offsetParent.offsetLeft + document.body.clientLeft;
    var offsetTop = elem.offsetTop + elem.offsetParent.offsetTop + document.body.clientTop + 10;
    var offsetHeight = elem.offsetHeight;
    var offsetWidth = elem.offsetWidth;
    
    var left = offsetLeft;
    var top = offsetTop + offsetHeight;
    var width = offsetWidth;
    
    var scrollelems = $( '#details-scroll' );
    if (scrollelems && scrollelems.length == 1) {
      var scrollelem = scrollelems[0];
      if (scrollelem && scrollelem.scrollTop > 0) 
        top = top - scrollelem.scrollTop;
    }
    
    var content = [];
    var selectlist = [];
    
    for (var key in values) {
      var value = values[key];
      
      var index = selectlist.length;
      var name = key;
      var title = key;
      var highlight = '';
      
      if (selected_value == key) {
        highlight = 'select2-highlighted';
        value['selected'] = true;
      }
      
      var item = 
        '<li id="selectlist-item-' + index + '" onMouseOver="javascript:listdetails.on_select_focus(' + index + ');" onMouseOut="javascript:listdetails.on_select_out(' + index + ');" onMouseDown="javascript:listdetails.on_select_click(' + index + ');" class="select2-results-dept-0 select2-result select2-result-selectable ' + highlight + '">' + "\n" +
        '  <div class="select2-result-label"><span class="select2-match"></span>' + title.esc() + '</div>' + "\n" + 
        '</li>';
      
      selectlist.push( name );
      content.push( item );
    }
    
    this.selectlist = selectlist;
    //this.selectelement = elem;
    
    if (content.length == 0)
      return;
    
    if ( $( '#javen' ).hasClass( 'show-music-player' ) ) {
      top += 62; // margin-top: 62px;
    }
    
    var html = 
      '<div class="select2-drop select2-drop-multi select2-drop-active" style="top: ' + top + 'px; left: ' + left + 'px; width: ' + width + 'px; display: block;">' + "\n" +
      '<ul class="select2-results">' + "\n" + content.join( '\n' ) +
      '</ul></div>';
    
    var selectlist_element = $( '#selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( html );
  },
  on_select_focus: function( index )
  {
    var selectlist = this.selectlist;
    if (selectlist == null || selectlist.length == 0)
      return;
    
    var value = selectlist[index];
    if (value == null) return;
    
    var item_element = $( '#selectlist-item-' + index );
    if (item_element)
      item_element.addClass( 'select2-highlighted' );
  },
  on_select_out: function( index )
  {
    var selectlist = this.selectlist;
    if (selectlist == null || selectlist.length == 0)
      return;
    
    var value = selectlist[index];
    if (value == null) return;
    
    var selected = value['selected'];
    if (selected == null) selected = false;
    
    var item_element = $( '#selectlist-item-' + index );
    if (item_element && selected == false)
      item_element.removeClass( 'select2-highlighted' );
  },
  on_select_click: function( index )
  {
    var selectlist = this.selectlist;
    if (selectlist == null || selectlist.length == 0)
      return;
    
    var value = selectlist[index];
    if (value == null) return;
    
    var selectelement = this.selectelement;
    var changed = false;
    if (selectelement) {
      selectelement.attr( 'value', value.esc() );
      changed = true;
    }
    
    var lockelement = this.lockelement;
    if (lockelement && changed) {
      lockelement.addClass( 'selected' );
    }
  },
  on_extension_blur: function( elem )
  {
    var selectlist_element = $( '#selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( '' );
  }
};

var detail_headbar = {
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

// #/~details
sammy.get
(
  /// /^#\/(~details)$/,
  new RegExp( '(~details)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var backbutton_element;
    var backlink_element;

    var path_param = this.path.slice(11);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    detail_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/details.html',
      function( template )
      {
        body_element
          .html( template );

        listdetails.id_param = id_param;
        listdetails.showdetails();

        statusbar.show();
      }
    );
  }
);

// #/~edit
sammy.get
(
  /// /^#\/(~edit)$/,
  new RegExp( '(~edit)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var backbutton_element;
    var backlink_element;

    var path_param = this.path.slice(8);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    detail_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/details.html',
      function( template )
      {
        body_element
          .html( template );

        listdetails.id_param = id_param;
        listdetails.showedit();

        statusbar.show();
      }
    );
  }
);

// #/~play
sammy.get
(
  /// /^#\/(~play)$/,
  new RegExp( '(~play)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var backbutton_element;
    var backlink_element;

    var path_param = this.path.slice(8);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    detail_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/details.html',
      function( template )
      {
        body_element
          .html( template );

        listdetails.id_param = id_param;
        listdetails.showplay();

        statusbar.show();
      }
    );
  }
);
