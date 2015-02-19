
var fileproperty = {
  filedetails_dialog: null,
  responsedetails: null,
  fileitem: null,
  
  showdetails: function( section )
  {
    if (section == null) return;
    var sec_id = section['id'];
    if (sec_id == null) return;
    
    this.fileitem = section;
    this.showdetailsid( sec_id );
  },
  showdetailsid: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    var params = '&action=property&id=' + encodeURIComponent(id);
    
    $.ajax
    (
      {
        url : app.base_path + '/sectioninfo?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : $( '#fileproperty-text' ),
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var details = response['details'];
          fileproperty.responsedetails = details;
          if (fileproperty.fileitem == null) fileproperty.fileitem = response;
          dialog.show( fileproperty.filedetails_dialog );
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
  gettitles: function()
  {
    var section = this.fileitem;
    return this.getsectiontitles( section, true );
  },
  getsectiontitles: function( section, isproperty )
  {
    if (section) { 
      var sectionid = section['id'];
      var contentType = section['type'];
      var isfolder = section['isfolder'];
      if (contentType == null) contentType = '';
      
      var title = null;
      var opentitle = null;
      var iconclass = null;
      
      if (isfolder == true) { 
        title = strings( isproperty == true ? 'Folder Property' : 'Folder Information' );
        iconclass = 'glyphicon folder-open';
        opentitle = '';
        
      } else if (contentType.indexOf('image/') == 0) {
        title = strings( isproperty == true ? 'Photo Property' : 'Photo Information' );
        opentitle = strings( 'View original photo' );
        iconclass = 'glyphicon picture';
        
      } else if (contentType.indexOf('video/') == 0) {
        title = strings( isproperty == true ? 'Movie Property' : 'Movie Information' );
        opentitle = null;
        iconclass = 'glyphicon film';
        
      } else if (contentType.indexOf('audio/') == 0) {
        title = strings( isproperty == true ? 'Music Property' : 'Music Information' );
        opentitle = null;
        iconclass = 'glyphicon music';
      }
      
      opentitle = strings( 'Open' );
      
      var context = system.context;
      var path = context.path;
      if (isfolder == true) {
        if (path.indexOf('#/~browse/') == 0 && path.indexOf(sectionid) >= 0)
          opentitle = '';
      } else {
        if (path.indexOf('#/~details/') == 0 && path.indexOf(sectionid) >= 0)
          opentitle = '';
      }
      
      return {dialog_title: title, open_title: opentitle, icon_class: iconclass};
    }
    return null;
  },
  open: function()
  {
    var section = this.fileitem;
    this.opensection( section, false );
  },
  opensection: function( section, editmode )
  {
    if (section) { 
      var sectionid = section['id'];
      var isfolder = section['isfolder'];
      
      //var openlink = section['openlink'];
      //if (openlink != null && openlink.length > 0) { 
        //window.open(openlink, '_blank');
        //return;
      //}
      
      if (sectionid != null && sectionid.length > 0) {
        var openlink = '#/~details/' + sectionid;
        if (isfolder == true)
          openlink = '#/~browse/' + sectionid;
        else if (editmode == true)
          openlink = '#/~edit/' + sectionid;
        
        var context = system.context;
        context.redirect( openlink );
      }
    }
  },
  getdetails: function()
  {
    var details = this.responsedetails;
    var mediainfo = '';
    var fileinfo = '';
    
    if (details) {
      var mediadetails = details['media'];
      if (mediadetails) {
        var content = [];
        
        for (var key in mediadetails) { 
          var val = mediadetails[key];
          
          content.push( this.getdetailitem( ''+key, ''+val ) );
        }
        
        if (content.length > 0) {
          mediainfo = '<div class="well">' + "\n" +
            '   <h4>' + strings( 'Media' ).esc() + '</h4>' + "\n" +
            '   <ul>' + "\n" + content.join( '\n' ) +
            '   </ul>' + "\n" +
            '</div>' + "\n";
        }
      }
      
      var filedetails = details['file'];
      if (filedetails) {
        var content = [];
        
        var name = filedetails['name'];
        var isfolder = filedetails['isfolder'];
        var contentType = filedetails['type'];
        var contentLength = filedetails['length'];
        var extensionName = filedetails['extname'];
        var dataDir = filedetails['path'];
        //var ctime = filedetails['ctime'];
        var mtime = filedetails['mtime'];
        var itime = filedetails['itime'];
        var subcount = filedetails['subcount'];
        var sublen = filedetails['sublen'];
        var perms = filedetails['perms'];
        var userid = filedetails['userid'];
        var username = filedetails['username'];
        var owner = filedetails['owner'];
        var checksum = filedetails['checksum'];
        
        if (username == null) username = '';
        if (owner == null || owner.length == 0) owner = username;
        if (checksum == null) checksum = '';
        
        //var createdTime = format_time(ctime);
        var modifiedTime = format_time(mtime);
        var indexedTime = format_time(itime);
        
        if (isfolder) { 
          if (dataDir != null && dataDir.length > 0) { 
            var lastChr = dataDir.charAt(dataDir.length - 1);
            if (lastChr != '/' && lastChr != '\\')
              dataDir = dataDir + '/';
          }
          dataDir = dataDir + name;
          contentLength = sublen;
          
          var countText = strings( '{0} Items' ).format( subcount );
          
          content.push( this.getdetailitem( 'Path', dataDir ) );
          content.push( this.getdetailitem( 'Type', contentType ) );
          content.push( this.getdetailitem( 'Contains', countText ) );
          
        } else { 
          content.push( this.getdetailitem( 'Name', name ) );
          content.push( this.getdetailitem( 'Path', dataDir ) );
          content.push( this.getdetailitem( 'Type', contentType ) );
        }
        
        content.push( this.getdetailitem( 'Length', readableBytes(contentLength) ) );
        //content.push( this.getdetailitem( 'Created', createdTime ) );
        content.push( this.getdetailitem( 'Updated', modifiedTime ) );
        
        if (itime != null && itime > 0) {
          content.push( this.getdetailitem( 'Indexed', indexedTime ) );
        }
        
        content.push( this.getdetailitem( 'Owner', owner ) );
        
        if (checksum != null && checksum.length > 0) {
          content.push( this.getdetailitem( 'Checksum', checksum ) );
        }
        
        content.push( this.getdetailitem( 'Permission', this.getpermissions(perms) ) );
        
        fileinfo = '<div class="well">' + "\n" +
            '   <h4>' + strings( isfolder ? 'Folder' : 'File' ).esc() + '</h4>' + "\n" +
            '   <ul>' + "\n" + content.join( '\n' ) +
            '   </ul>' + "\n" +
            '</div>' + "\n";
      }
    }
    
    return fileinfo + mediainfo;
  },
  getdisplayname: function( section_name, section_type )
  {
    if (section_name == null)
      section_name = '';
    if (section_type == null)
      section_type = '';
    
    if (section_type == 'application/x-recycle-root' || section_type == 'application/x-share-root' || section_type == 'application/x-upload-root') {
      section_name = strings( section_name );
    } else if (section_type == 'application/x-search') {
      section_name = strings( 'Search Results' );
    }
    
    return section_name;
  },
  getpermissions: function( perms )
  {
    if (perms == null) perms = '';
    var str = '';
    str += perms.indexOf('r') >= 0 ? strings( 'Read enabled' ) : strings( 'Read disabled' );
    str += ', ';
    str += perms.indexOf('w') >= 0 ? strings( 'Write enabled' ) : strings( 'Write disabled' );
    str += ', ';
    str += perms.indexOf('d') >= 0 ? strings( 'Delete enabled' ) : strings( 'Delete disabled' );
    str += ', ';
    str += perms.indexOf('m') >= 0 ? strings( 'Move enabled' ) : strings( 'Move disabled' );
    str += ', ';
    str += perms.indexOf('c') >= 0 ? strings( 'Copy enabled' ) : strings( 'Copy disabled' );
    return str;
  },
  getdetailitem: function( name, value )
  {
    if (name == null) name = '';
    if (value == null) value = '';
    
    name = strings( name );
    
    return '        <li>' + "\n" +
           '            <span class="media-info-detail-label">' + name.esc() + '</span>' + "\n" +
           '            <span class="media-info-detail">' + value.esc() + '</span>' + "\n" +
           '        </li>';
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/fileproperty.html',
      function( template )
      {
        fileproperty.filedetails_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#fileproperty-title' ).html( strings( 'File Property' ) );
            $( '#fileproperty-ok' ).html( strings( 'Open' ) );
            $( '#fileproperty-no' ).html( strings( 'Close' ) );
            
            $( '#fileproperty-container' )
              .attr( 'class', 'modal modal-large media-info-modal fade in' );
            
            $( '#fileproperty-icon' )
              .attr( 'class', 'glyphicon file' );
            
            var titles = fileproperty.gettitles();
            
            var icon = titles ? titles.icon_class : null;
            if (icon != null && icon.length > 0) {
              $( '#fileproperty-icon' ).attr( 'class', icon );
            }
            
            var title = titles ? titles.dialog_title : null;
            if (title != null && title.length > 0) {
              $( '#fileproperty-title' ).html( title.esc() );
            }
            
            var opentitle = titles ? titles.open_title : null;
            if (opentitle != null) {
              if (opentitle.length > 0) 
                $( '#fileproperty-ok' ).html( opentitle.esc() );
              else
                $( '#fileproperty-ok' ).addClass( 'hide' );
            }
            
            var html = fileproperty.getdetails();
            if (html == null) html = "";
            
            $( '#fileproperty-text' )
              .html( html );
            
            $( '#fileproperty-ok' )
              .attr( 'onclick', 'javascript:fileproperty.open();return false;' )
              .addClass( 'hide' );
            
            $( '#fileproperty-no' )
              .attr( 'onclick', 'javascript:dialog.hide();return false;' );
            
            $( '#fileproperty-close' )
              .attr( 'onclick', 'javascript:dialog.hide();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
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

var fileinfo = {
  filedetails_dialog: null,
  filedetails_showcb: null,
  slidephotos: [],
  section: null,
  
  showdetails: function( section )
  {
    if (section == null) return;
    var sec_id = section['id'];
    if (sec_id == null) return;
    
    this.showdetailsid( sec_id );
  },
  showdetailsid: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    this.section = null;
    this.slidephotos = [];
    
    var params = '&action=details&id=' + encodeURIComponent(id);
    
    $.ajax
    (
      {
        url : app.base_path + '/sectioninfo?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : $( '#fileinfo-text' ),
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          fileinfo.section = response;
          dialog.show( fileinfo.filedetails_dialog );
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
  getdisplayname: function( section_name, section_type )
  {
    return fileproperty.getdisplayname( section_name, section_type );
  },
  getpermissions: function( perms )
  {
    return fileproperty.getpermissions( perms );
  },
  can_modify: function()
  {
    var section = this.section;
    return fileinfo.can_modifysection( section );
  },
  can_modifysection: function( section )
  {
    if (section == null) return false;
    
    var id = section['id'];
    if (id == null || id.length == 0)
      return false;
    
    var perms = section['perms'];
    var owner = section['owner'];
    var username = section['username'];
    var usertype = section['usertype'];
    var gmrole = section['gmrole'];
    var me = globalApp.get_username();
    
    if (perms != null && perms.indexOf('w') >= 0) {
      if (me != null && me == username)
        return true;
      
      if (usertype == 'group') {
        if (gmrole == 'owner' || gmrole == 'manager')
          return true;
      }
    }
    
    return false;
  },
  can_share: function()
  {
    var section = this.section;
    return fileinfo.can_sharesection( section );
  },
  can_sharesection: function( section )
  {
    if (section == null) return false;
    
    var id = section['id'];
    if (id == null || id.length == 0)
      return false;
    
    var isfolder = section['isfolder'];
    if (isfolder) return false;
    
    var perms = section['perms'];
    if (perms != null && perms.indexOf('r') >= 0) {
      return true;
    }
    
    return false;
  },
  init_details: function()
  {
    var section = this.section;
    if (section == null) return;
    
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
  
    var root_id = section['root_id'];
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
  
    if (library_name == null) library_name = '';
    if (library_hostname == null) library_hostname = '';
  
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
      
      fileinfo.slidephotos = [];
      fileinfo.slidephotos.push( section );
    
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
      
      canplay = true;
      canposter = true;
    
    } else if (section_type.indexOf('text/') == 0) { 
      postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
    
      metaduration = readableBytes2( length );
      canposter = true;
    
    } else if (isfolder == false) { 
      postersrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
      downlink = app.base_path + '/download/' + section_id + '.' + extension + '?token=' + app.token;
      
      metaduration = readableBytes2( length );
      canposter = true;
      
    } else if (isfolder == true) {
      postersrc = 'css/' + app.theme + '/images/posters/folder.png';
      canposter = true;
    }
  
    section['openlink'] = openlink;
    section['downlink'] = downlink;
  
    var empty_poster = postersrc;
    var empty_background = 'css/' + app.theme + '/images/background.png';
  
    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      postersrc = app.base_path + '/image/' + imgid + '_256t.' + imgext + '?token=' + app.token;
      
      fileinfo.slidephotos = [];
      fileinfo.slidephotos.push( section );
    }
  
    if (background != null && background.length > 0) {
      var imgid = background;
      var imgext = 'jpg';
      var src = app.base_path + '/image/' + imgid + '.' + imgext + '?token=' + app.token;
      
      //$( '#background-image' )
      //  .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
  
    var poster_element = $( '#fileinfo-poster' );
    var posterlink_element = $( '#fileinfo-poster-link' );
    var playbutton_element = $( '#fileinfo-play-button' );
    var playlink_element = $( '#fileinfo-play-link' );
    var playtext_element = $( '#fileinfo-play-text' );
  
    if (poster_element) {
      poster_element.attr( 'src', postersrc );
    }
    if (posterlink_element) {
      posterlink_element
        .attr( 'onclick', 'javascript:fileinfo.click_poster();return false;' )
        .attr( 'href', '' );
    }
    if (playlink_element) {
      playlink_element
        .attr( 'onClick', 'javascript:fileinfo.goplay();return false;' )
        .attr( 'href', '' );
    }
    if (playbutton_element) {
      playbutton_element.addClass( 'hide' );
    }
    if (playtext_element) {
      playtext_element.html( strings( 'Play' ) );
    }
  
    if (section) {
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
      
      var longtitle = section_name;
      if (longtitle == null) longtitle = '';
      longtitle = longtitle.replaceAll( '"', '' );
      
      title = title.replaceAll( '"', '' );
      if (title.length > 10) {
        //longtitle = title;
        title = trim_line( title, 10 );
      }
      
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
      
      $( '#fileinfo-metadata-owner-name' ).html( strings( 'Owner' ) );
      $( '#fileinfo-metadata-director-name' ).html( strings( 'Author' ) );
      $( '#fileinfo-metadata-cast-name' ).html( strings( 'Album' ) );
      $( '#fileinfo-metadata-tags-name' ).html( strings( 'Tags' ) );
      
      $( '#fileinfo-metadata-year' ).html( year.esc() );
      $( '#fileinfo-metadata-title' ).html( title.esc() ).attr( 'title', longtitle.esc() );
      $( '#fileinfo-metadata-title2' ).html( genre.esc() );
      $( '#fileinfo-metadata-name' ).html( duration.esc() );
      $( '#fileinfo-metadata-subtitle' ).html( subtitle.esc() );
      $( '#fileinfo-metadata-director' ).html( director.esc() );
      $( '#fileinfo-metadata-cast' ).html( cast.esc() );
      $( '#fileinfo-metadata-summary' ).html( summary );
      $( '#fileinfo-metadata-tags' ).html( tags );
      $( '#fileinfo-metadata-owner' ).html( ownerinfo );
      
      if (ownerinfo != null && ownerinfo.length > 0) $( '#fileinfo-metadata-owner-item' ).removeClass( 'hide' );
      if (director != null && director.length > 0) $( '#fileinfo-metadata-director-item' ).removeClass( 'hide' );
      if (cast != null && cast.length > 0) $( '#fileinfo-metadata-cast-item' ).removeClass( 'hide' );
      if (tags != null && tags.tags > 0) $( '#fileinfo-metadata-tags-item' ).removeClass( 'hide' );
      
      if (playbutton_element && canplay) {
        playbutton_element.removeClass( 'hide' );
      }
      
      $( '#fileinfo-controls' ).removeClass( 'hide' );
      $( '#fileinfo-metadata' ).removeClass( 'hide' );
    }
    
    if (musicprogress && canplaymusic) 
      musicprogress.add_callback( fileinfo.onmusicevent );
  },
  onmusicevent: function( eventname, event, section )
  {
    if (eventname == null || event == null || section == null)
      return;
    
    var thissection = fileinfo.section;
    var thissection_id = null;
    if (thissection) thissection_id = thissection['id'];
    
    var section_id = section['id'];
    if (section_id == null || section_id.length == 0 || section_id != thissection_id)
      return;
    
    var playtext_element = $( '#fileinfo-play-text' );
    if (playtext_element) {
      if (eventname == 'playing') playtext_element.html( strings( 'Playing' ) );
      else playtext_element.html( strings( 'Play' ) );
    }
  },
  init_dialog: function( dialog_element ) 
  {
    fileproperty.init_dialog( dialog_element );
    $.get
    (
      'tpl/fileinfo.html',
      function( template )
      {
        fileinfo.filedetails_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#fileinfo-title' ).html( strings( 'File Information' ) );
            $( '#fileinfo-property' ).html( strings( 'Property' ) );
            $( '#fileinfo-edit' ).html( strings( 'Edit' ) );
            $( '#fileinfo-share' ).html( strings( 'Share' ) );
            $( '#fileinfo-ok' ).html( strings( 'Open' ) );
            $( '#fileinfo-no' ).html( strings( 'Close' ) );
            
            $( '#fileinfo-details-show' ).html( strings( 'Show More Actions' ) );
            $( '#fileinfo-details-hide' ).html( strings( 'Hide More Actions' ) );
            
            fileinfo.init_details();
            
            var titles = fileproperty.getsectiontitles( fileinfo.section, false );
            
            var icon = titles ? titles.icon_class : null;
            if (icon != null && icon.length > 0) {
              $( '#fileinfo-icon' ).attr( 'class', icon );
            }
            
            var title = titles ? titles.dialog_title : null;
            if (title != null && title.length > 0) {
              $( '#fileinfo-title' ).html( title.esc() );
            }
            
            $( '#fileinfo-details-show' )
              .attr( 'onclick', 'javascript:fileinfo.showactions();return false;' )
              .attr( 'href', '' );
            
            $( '#fileinfo-details-hide' )
              .attr( 'onclick', 'javascript:fileinfo.hideactions();return false;' )
              .attr( 'href', '' );
            
            $( '#fileinfo-property' )
              .attr( 'onclick', 'javascript:fileinfo.showproperty();return false;' );
            
            $( '#fileinfo-edit' )
              .attr( 'onclick', 'javascript:fileinfo.edit();return false;' );
            
            $( '#fileinfo-share' )
              .attr( 'onclick', 'javascript:fileinfo.share();return false;' );
            
            $( '#fileinfo-ok' )
              .attr( 'onclick', 'javascript:fileinfo.open();return false;' );
            
            $( '#fileinfo-no' )
              .attr( 'onclick', 'javascript:fileinfo.close();return false;' );
            
            $( '#fileinfo-close' )
              .attr( 'onclick', 'javascript:fileinfo.close();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            fileinfo.hideactions();
            
            var cb = fileinfo.filedetails_showcb;
            if (cb) cb.call(fileinfo);
          },
          hidecb: function()
          {
          },
          shown: false
        };
      }
    );
  },
  showactions: function()
  {
    $( '#fileinfo-details-show' ).addClass( 'hide' );
    $( '#fileinfo-details-hide' ).removeClass( 'hide' );
    
    $( '#fileinfo-property' ).removeClass( 'hide' );
    $( '#fileinfo-ok' ).removeClass( 'hide' );
    $( '#fileinfo-no' ).removeClass( 'hide' );
    
    if (this.can_modify()) {
      $( '#fileinfo-edit' ).removeClass( 'hide' );
    }
    
    if (this.can_share()) {
      $( '#fileinfo-share' ).removeClass( 'hide' );
    }
  },
  hideactions: function()
  {
    $( '#fileinfo-details-show' ).removeClass( 'hide' );
    $( '#fileinfo-details-hide' ).addClass( 'hide' );
    
    $( '#fileinfo-property' ).addClass( 'hide' );
    $( '#fileinfo-edit' ).addClass( 'hide' );
    $( '#fileinfo-share' ).addClass( 'hide' );
    $( '#fileinfo-ok' ).removeClass( 'hide' );
    $( '#fileinfo-no' ).removeClass( 'hide' );
  },
  click_poster: function()
  {
    if (fileinfo.slidephotos != null && fileinfo.slidephotos.length > 0) {
      fileinfo.close();
      photoslide.show( fileinfo.slidephotos, 0, false );
    }
  },
  goplay: function()
  {
    var section = fileinfo.section;
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
  showproperty: function()
  {
    fileproperty.showdetails( fileinfo.section );
  },
  edit: function()
  {
    fileproperty.opensection( fileinfo.section, true );
  },
  share: function()
  {
    var section = fileinfo.section;
    if (section == null) return;
    
    var items = [];
    items.push( section );
    
    compose.share( items );
  },
  open: function()
  {
    fileproperty.opensection( fileinfo.section, false );
  },
  close: function()
  {
    dialog.hide();
  }
};
