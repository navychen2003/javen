
var announcement_dialogs = { 
  delete_confirm_dialog: null,
  
  init_message: function( dialog_element, template ) 
  {
    announcement_dialogs.delete_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Announcement' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to remove this announcement?' );
        if (msg == null) msg = "";
        
        $( '#message-text' ).html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:announcementform.delete_submit();return false;' )
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
  }
};

var announcementform = {
  announcementkey: null,
  successcb: null,
  
  add_announcement: function()
  {
    var context = system.context;
    context.redirect( '#/~announcement/new' );
  },
  action_submit: function( announcementkey, action )
  {
    if (announcementkey == null || action == null) return;
    
    var params = '&action=' + encodeURIComponent(action) + 
                 '&key=' + encodeURIComponent(announcementkey);
    
    $.ajax
    (
      {
        url : app.user_path + '/announcement?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
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
            var cb = announcementform.successcb;
            if (cb) cb.call(announcementform);
            else sammy.refresh();
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
  delete_announcement: function( announcementkey )
  {
    if (announcementkey == null|| announcementkey.length == 0)
      return;
    
    this.announcementkey = announcementkey;
    dialog.show( announcement_dialogs.delete_confirm_dialog );
  },
  delete_submit: function()
  {
    dialog.hide();
    
    var announcementkey = this.announcementkey;
    if (announcementkey== null|| announcementkey.length == 0)
      return;
    
    this.action_submit( announcementkey, 'delete' );
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var listannouncement = {
  showlist: function( lang )
  {
    navbar.init_name( strings( 'Setting' ), null, '#/~settings' );
    
    if (lang == null) lang = '';
    var params = '&action=list&lang=' + encodeURIComponent(lang);
    
    $.ajax
    (
      {
        url : app.user_path + '/announcement?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var announcements = response['announcements'];
          var langs = response['langs'];
          var the_lang = response['lang'];
          listannouncement.init_content( announcements, langs, the_lang );
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
  init_content: function( announcements, langs, thelang )
  {
    if (announcements == null) announcements = {};
    if (langs == null) langs = globalApp.languages;
    if (thelang == null) thelang = '';
    
    var langCount = 0;
    var langContent = [];
    
    if (true) {
      var title = strings( 'All' );
      var clickto = 'javascript:listannouncement.showlist(\'all\');return false;';
        
      var activeItem = '';
      if (thelang == null || thelang == '' || thelang == 'all') 
        activeItem = 'active';
        
      var item = '<li><a class="settings-filter ' + activeItem + '" onclick="' + clickto + '" href="">' + title.esc() + '</a></li>' + "\n";
      langContent.push( item );
      langCount ++;
    }
    
    for (var ckey in langs) {
      var title = langs[ckey];
      if (ckey == 'all') continue;
      if (title != null && ckey != null) {
        var clickto = 'javascript:listannouncement.showlist(\'' + encodeURIComponent(ckey) + '\');return false;';
        
        var activeItem = '';
        if (thelang == ckey) activeItem = 'active';
        
        var item = '<li><a class="settings-filter ' + activeItem + '" onclick="' + clickto + '" href="">' + title.esc() + '</a></li>' + "\n";
        langContent.push( item );
        langCount ++;
      }
    }
    
    var announcementCount = 0;
    var announcementContent = [];
    
    for (var akey in announcements) {
      var announcement = announcements[akey];
      if (announcement != null) {
        var key = announcement['key'];
        var lang = announcement['lang'];
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
        if (lang == null) lang = '';
        
        var modifiedTime = format_time(mtime);
        
        var thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
        var editClick = 'javascript:listannouncement.editannouncement(\'' + key + '\');return false;';
        var deleteClick = 'javascript:listannouncement.deleteannouncement(\'' + key + '\');return false;';
        
        var imghide = '';
        if (poster != null && poster.length > 0) {
          var id = poster;
          var extension = 'jpg';
      
          var src = app.base_path + '/image/' + id + '_64t.' + extension + '?token=' + app.token;
          thumbsrc = src;
        } else { 
          imghide = 'hide';
        }
        
        var langtitle = null;
        var langimg = null;
        
        if (langs && lang != null && lang.length > 0) {
          langtitle = langs[lang];
          if (langtitle != null)
            langimg = 'img/flag/' + lang + '.png';
        }
        
        if (langtitle == null) {
          langtitle = 'English';
          langimg = 'img/flag/en.png';
        }
        
        var actionhide = 'hide';
        if (globalApp.is_admin()) actionhide = '';
        
        var item = 
            '		<li>' + "\n" +
            '	    	<div class="dashboard-announcements-container well">' + "\n" +
            '			<div class="dashboard-well-header dashboard-well-header-secondary well-header">' + "\n" +
            '				<ul class="well-header-actions ' + actionhide + '">' + "\n" +
            '					<li><button type="button" class="edit-btn btn btn btn-icon" rel="tooltip" title="' + strings('Edit') + '" onClick="' + editClick + '"><i class="glyphicon pencil"></i></button></li>' + "\n" +
            '                   <li><button type="button" class="delete-btn btn btn-danger btn-icon" title="' + strings('Remove') + '" onClick="' + deleteClick + '"><i class="glyphicon ban"></i></button></li>' + "\n" +
            '				</ul>' + "\n" +
            '               <img src="' + langimg + '" title="' + langtitle.esc() + '" style="margin-top: -8px;" />' + "\n" + 
            '				<h3></h3>' + "\n" +
            '			</div>' + "\n" +
            '			<div class="dashboard-carousel-container">' + "\n" +
            '			<div class="dashboard-carousel-content">' + "\n" +
            '			<ul class="dashboard-announcement-list" style="width: 100%; left: 0px;">' + "\n" +
            '				<li style="width: 100%;">' + "\n" +
            '					<h3><a target="_blank" tabindex="-1" href="' + link.esc() + '">' + title.esc() + '</a></h3>' + "\n" +
            '					<p class="date">' + modifiedTime + '</p>' + "\n" +
            '					<img class="poster pull-left ' + imghide + '" src="' + thumbsrc + '"><p class="description">' + body + '</p>' + "\n" +
            '				</li>' + "\n" +
            '			</ul>' + "\n" +
            '			</div></div>' + "\n" +
            '			</div>' + "\n" +
            '		</li>';
        
        announcementContent.push( item );
        announcementCount ++;
      }
    }
    
    $( '#announcement-folder-list' ).html( langContent.join( "\n" ) );
    $( '#announcement-list' ).html( announcementContent.join( "\n" ) );
    
    if (announcementContent.length == 0) {
      $( '#announcement-empty' )
        .html( strings( 'No announcements :(' ) )
        .removeClass( 'hide' );
    } else {
      $( '#announcement-empty' )
        .addClass( 'hide' );
    }
  },
  editannouncement: function( key )
  {
    if (key == null || key.length == 0)
      return;
    
    var context = system.context;
    context.redirect( '#/~announcement/' + encodeURIComponent(key) );
  },
  deleteannouncement: function( key )
  {
    if (key == null || key.length == 0)
      return;
    
    announcementform.delete_announcement( key );
  }
};

var announcementdetails = {
  announcementkey: null,
  showcb: null,
  selectlist: null,
  selectelement: null,
  lockelement: null,
  empty_poster: null,
  
  edit: function( announcementkey )
  {
    announcementdetails.show0( announcementkey, true );
  },
  show: function( announcementkey )
  {
    announcementdetails.show0( announcementkey, false );
  },
  show0: function( announcementkey, editmode )
  {
    if (announcementkey == null || announcementkey.length == 0)
      announcementkey = 'new';
    
    announcementform.successcb = null;
    navbar.init_name( strings( 'Announcements' ), null, '#/~announcements/all' );
    
    var editlink_element = $( '#action-edit-link' );
    
    if (editlink_element) {
      editlink_element
        .attr( 'title', strings('Edit') )
        .attr( 'onClick', 'javascript:announcementdetails.edit(\'' + announcementkey + '\');return false;' )
        .attr( 'href', '' );
    }
    
    this.announcementkey = announcementkey;
    this.init_form( announcementkey );
    
    if (announcementkey == 'new') {
      this.init_values( announcementkey, {} );
      return;
    }
    
    var params = '&action=info&key=' + encodeURIComponent(announcementkey);
    
    $.ajax
    (
      {
        url : app.user_path + '/announcement?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var key = response['key'];
          var profile = response['announcement'];
          //if (editmode == true)
            announcementdetails.init_values( key, profile );
          //else
          //  announcementdetails.init_details( key, profile );
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
  get_value: function( profile, name )
  {
    if (profile && name) {
      var val = profile[name];
      if (val == null) val = '';
      return val;
    }
    return '';
  },
  changeposter: function()
  {
    var announcementkey = this.announcementkey;
    if (announcementkey == null || announcementkey.length == 0)
      return;
    
    var emptysrc = this.empty_poster;
    
    artwork.showselect0( announcementkey, 'Public Files', 
      function( section ) {
        if (section) {
          var id = section['id'];
          if (id == null || id.length <= 0) id = 'null';
          announcementdetails.save_artwork(id);
        }
      }, emptysrc);
  },
  save_artwork: function(poster_id)
  {
    if (poster_id == null) return;
    
    var announcementkey = this.announcementkey;
    if (announcementkey == null || announcementkey.length == 0)
      return;
    
    var poster = poster_id;
    if (poster == null) poster = '';
    
    var params = '&action=update&key=' + encodeURIComponent(announcementkey) 
               + '&poster=' + encodeURIComponent(poster);
    
    $.ajax
    (
      {
        url : app.user_path + '/announcement?token=' + app.token + params + '&wt=json',
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
            var key = response['key'];
            if (key != null && key.length > 0) {
              if (announcementkey == 'new') {
                var context = system.context;
                context.redirect( '#/~announcement/' + encodeURIComponent(key) );
              } else
                sammy.refresh();
            } else {
              sammy.refresh();
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
  init_form: function( announcementkey )
  {
    if (announcementkey == null || announcementkey.length == 0)
      return;
    
    var form_element = $( '#announcement-form' );
    var submitbutton_element = $( '#announcement-submit-button' );
    var cancelbutton_element = $( '#announcement-cancel-button' );
    
    var poster_element = $( '#announcement-poster' );
    var changeposter_element = $( '#announcement-change-poster' );
    
    var langinput_element = $( '#announcement-lang-input' );
    var titleinput_element = $( '#announcement-title-input' );
    var linkinput_element = $( '#announcement-link-input' );
    var bodyinput_element = $( '#announcement-body-input' );
    
    $( '#announcement-change-poster-text' ).html( strings('Change Poster') );
    $( '#announcement-metadata-title-name' ).html( strings('Title') );
    
    $( '#announcement-lang-text' ).html( strings('Language') );
    $( '#announcement-title-text' ).html( strings('Title') );
    $( '#announcement-link-text' ).html( strings('Link') );
    $( '#announcement-body-text' ).html( strings('Body') );
    
    $( '#announcement-submit-button' ).html( strings('Save') );
    $( '#announcement-cancel-button' ).html( strings('Cancel') );
    
    changeposter_element
      .attr( 'onClick', 'javascript:announcementdetails.changeposter();return false;' );
    
    langinput_element
      .attr( 'onChange', 'javascript:announcementdetails.lockchanged(\'#announcement-lang-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:announcementdetails.lockchanged(\'#announcement-lang-lock\', true);' )
      .attr( 'onFocus', 'javascript:announcementdetails.on_lang_focus(this);' )
      .attr( 'onBlur', 'javascript:announcementdetails.on_lang_blur(this);' )
      .attr( 'value', '' );
    
    titleinput_element
      .attr( 'onChange', 'javascript:announcementdetails.lockchanged(\'#announcement-title-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:announcementdetails.lockchanged(\'#announcement-title-lock\', true);' )
      .attr( 'value', '' );
    
    linkinput_element
      .attr( 'onChange', 'javascript:announcementdetails.lockchanged(\'#announcement-link-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:announcementdetails.lockchanged(\'#announcement-link-lock\', true);' )
      .attr( 'value', '' );
    
    bodyinput_element
      .attr( 'onChange', 'javascript:announcementdetails.lockchanged(\'#announcement-body-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:announcementdetails.lockchanged(\'#announcement-body-lock\', true);' )
      .attr( 'value', '' );
    
    this.init_lockelement( '#announcement-lang-lock' );
    this.init_lockelement( '#announcement-title-lock' );
    this.init_lockelement( '#announcement-link-lock' );
    this.init_lockelement( '#announcement-body-lock' );
    
    announcementdetails.empty_poster = 'css/' + app.theme + '/images/posters/poster.png';
    
    var params = '&action=update&key=' + encodeURIComponent(announcementkey);
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/announcement?token=' + app.token + params + '&wt=json',
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
              var key = response['key'];
              if (key != null && key.length > 0) {
                if (announcementkey == 'new') {
                  var context = system.context;
                  context.redirect( '#/~announcement/' + encodeURIComponent(key) );
                } else
                  sammy.refresh();
              } else {
                sammy.refresh();
              }
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
    
    //submitbutton_element
    //  .attr( 'onClick', 'javascript:announcementdetails.save_submit();return false;' );
    
    cancelbutton_element
      .attr( 'onClick', 'javascript:announcementdetails.save_cancel();return false;' );
    
  },
  init_values: function( announcementkey, profile )
  {
    //$( '#announcement-metadata' ).addClass( 'hide' );
    $( '#announcement-form' ).removeClass( 'hide' );
    $( '#announcement-change-poster' ).removeClass( 'hide' );
    
    if (announcementkey == null || profile == null) return;
    
    var lang = this.get_value(profile, 'lang');
    var title = this.get_value(profile, 'title');
    var link = this.get_value(profile, 'link');
    var body = this.get_value(profile, 'body');
    var poster = this.get_value(profile, 'poster');
    
    var langs = globalApp.languages;
    if (langs && lang && lang.length > 0) {
      var langtitle = langs[lang];
      if (langtitle != null && langtitle.length > 0) {
        var navtitle = strings( 'Announcements' ) + ': ' + langtitle;
        navbar.init_name( navtitle, null, '#/~announcements/' + encodeURIComponent(lang) );
      }
    }
    
    $( '#announcement-lang-input' ).attr( 'value', lang );
    $( '#announcement-title-input' ).attr( 'value', title );
    $( '#announcement-link-input' ).attr( 'value', link );
    $( '#announcement-body-input' ).attr( 'value', body );
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
    if (poster != null && poster.length > 0) {
      var id = poster;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
    }
    
    $( '#announcement-poster' ).attr( 'src', thumbsrc);
    
    var cb = announcementdetails.showcb;
    if (cb) cb.call(this, announcementkey, profile);
  },
  init_details: function( announcementkey, profile )
  {
    var cb = announcementdetails.showcb;
    if (cb) cb.call(this, announcementkey, profile);
  },
  init_lockelement: function( idname )
  {
    if (idname == null) return;
    var lockelement = $( idname );
    if (lockelement && idname) {
      this.lockchanged( idname, false );
      lockelement
        .attr( 'onClick', 'javascript:announcementdetails.changelock(\'' + idname + '\');return false;' );
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
    
    var scrollelems = $( '#announcement-scroll' );
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
      var title = value;
      var highlight = '';
      
      if (selected_value == key) {
        highlight = 'select2-highlighted';
        value['selected'] = true;
      }
      
      var item = 
        '<li id="selectlist-item-' + index + '" onMouseOver="javascript:announcementdetails.on_select_focus(' + index + ');" onMouseOut="javascript:announcementdetails.on_select_out(' + index + ');" onMouseDown="javascript:announcementdetails.on_select_click(' + index + ');" onClick="javascript:return false;" class="select2-results-dept-0 select2-result select2-result-selectable ' + highlight + '">' + "\n" +
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
  on_lang_focus: function( elem )
  {
    var values = {};
    var selectedval = '';
    
    var langs = globalApp.languages;
    if (langs) {
      for (var key in langs) {
        var title = langs[key];
        if (title == null) continue;
        
        values[key] = title;
      }
    }
    
    this.selectelement = $( '#announcement-lang-input' );
    this.lockelement = $( '#announcement-lang-lock' );
    this.showselects( elem, values, selectedval );
  },
  on_lang_blur: function( elem )
  {
    var selectlist_element = $( '#selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( '' );
  },
  save_submit: function()
  {
    var form_element = $( '#announcement-form' );
    
    form_element.submit();
  },
  save_cancel: function()
  {
    announcementdetails.show( announcementdetails.announcementkey );
  }
};

var announcement_headbar = {
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
        $( '#settings-link' ).addClass( 'active' );
        
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

// #/~announcements
sammy.get
(
  // /^#\/(~announcements)$/,
  new RegExp( '(~announcements)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(17);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    announcement_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/announcements.html',
      function( template )
      {
        body_element
          .html( template );

        $( '#announcement-add-submit-text' ).html( strings( 'Add Announcement' ) );

        $( '#announcement-add-submit' )
          .attr( 'onClick', 'javascript:announcementform.add_announcement();return false;' )
          .attr( 'title', strings( 'Add Announcement' ) );

        $( '#setting-title' )
          .attr( 'href', '#/~settings' )
          .html( strings( 'Setting' ) );

        $( '#cluster-title' )
          .attr( 'href', '#/~clusters' )
          .html( strings( 'Host' ) );

        $( '#announcement-title' )
          .attr( 'href', '#/~announcements/' + encodeURIComponent(id_param) )
          .html( strings( 'Announcement' ) );

        $( '#publish-title' )
          .attr( 'href', '#/~featured/' + encodeURIComponent('system') )
          .html( strings( 'Publish' ) );

        if (globalApp.is_admin()) {
          $( '#publish-subnav' ).removeClass( 'hide' );
          $( '#cluster-subnav' ).removeClass( 'hide' );
          $( '#announcement-subnav' ).removeClass( 'hide' );
          $( '#announcement-add-submit' ).removeClass( 'hide' );
          
        } else {
          $( '#publish-subnav' ).addClass( 'hide' );
          $( '#cluster-subnav' ).addClass( 'hide' );
          $( '#announcement-subnav' ).addClass( 'hide' );
          $( '#announcement-add-submit' ).addClass( 'hide' );
        }

        listannouncement.showlist( id_param );

        statusbar.show();
      }
    );
  }
);

// #/~announcement
sammy.get
(
  // /^#\/(~announcement)$/,
  new RegExp( '(~announcement)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(16);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    announcement_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/announcement.html',
      function( template )
      {
        body_element
          .html( template );

        announcementdetails.edit( id_param );

        statusbar.show();
      }
    );
  }
);
