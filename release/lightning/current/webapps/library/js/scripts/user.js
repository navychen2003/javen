
var user_tags = {
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
    $( '#user-tags-autogen' )
      .attr( 'onClick', 'javascript:user_tags.onfocus();return false;' );
    
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
      
      var clickto = 'javascript:user_tags.remove_value(' + key + ');return false;';
      var item = '<li class="select2-search-choice"><div>' + value.esc() + '</div><a onclick="' + clickto + '" class="select2-search-choice-close" tabindex="-1"></a></li>';
      
      valueContent.push( item );
    }
    
    var keydown = 'javascript:return user_tags.input_keydown(this,event);';
    var keyup = 'javascript:user_tags.input_keyup(this,event);';
    var focus = 'javascript:user_tags.input_focus(this,event);';
    var blur = 'javascript:user_tags.input_blur(this,event);';
    
    var item = '<li class="select2-search-field"><input id="user-tags-selectinput" type="text" autocomplete="off" class="select2-input" style="width: 10px;" onKeyDown="' + keydown + '" onKeyUp="' + keyup + '" onFocus="' + focus + '" onBlur="' + blur + '"></li>';
    
    valueContent.push( item );
    
    $( '#user-tags-list' ).html( valueContent.join( '\n' ) );
    $( '#user-tags-input' ).attr( 'value', valueStr.esc() );
    
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
    $( '#user-tags-autogen' )
      .addClass( 'select2-container-active' );
    $( '#user-tags-selectinput' )
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
    
    $( '#user-tags-selectinput' ).attr( 'style', 'width: ' + width + 'px;' );
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
    
    $( '#user-tags-autogen' )
      .removeClass( 'select2-container-active' );
    $( '#user-tags-selectinput' )
      .removeClass( 'select2-focused' );
  }
};

var listuser = {
  username: null,
  showcb: null,
  slidephotos: [],
  selectlist: null,
  selectelement: null,
  lockelement: null,
  empty_avatar: null,
  empty_background: null,
  
  gopublish: function()
  {
    var username = this.username;
    if (username == null) return;
    var context = system.context;
    context.redirect( '#/~posts/' + encodeURIComponent(username) );
  },
  showchat: function()
  {
    var username = this.username;
    if (username == null) return;
    var context = system.context;
    context.redirect( '#/~chat/' + encodeURIComponent(username) );
  },
  showconversation: function()
  {
    var username = this.username;
    if (username == null) return;
    var context = system.context;
    context.redirect( '#/~conversation/' + encodeURIComponent(username) );
  },
  sendmessage: function()
  {
    var username = this.username;
    if (username == null) return;
    compose.show( 'mail', username );
  },
  showinfo: function()
  {
    var username = this.username;
    if (username == null) return;
    userinfo.showdetails( username );
  },
  showgroupinfo: function()
  {
    var username = this.username;
    if (username == null) return;
    groupinfo.showdetails( username );
  },
  showlist: function()
  {
    this.showcb = this.user_showcb;
    this.showlist0( this.username, false );
  },
  showlist0: function( username, editmode )
  {
    var editlink_element = $( '#action-edit-link' );
    var publishlink_element = $( '#action-publish-link' );
    var sendlink_element = $( '#action-send-link' );
    var chatlink_element = $( '#action-chat-link' );
    var memberlink_element = $( '#action-member-link' );
    var librarylink_element = $( '#action-library-link' );
    var infolink_element = $( '#action-info-link' );
    
    if (editlink_element) {
      editlink_element
        .attr( 'title', strings('Edit') )
        .attr( 'onClick', 'javascript:listuser.edit();return false;' )
        .attr( 'href', '' );
    }
    if (publishlink_element) {
      publishlink_element
        .attr( 'title', strings('Publish') )
        .attr( 'onClick', 'javascript:listuser.gopublish();return false;' )
        .attr( 'href', '' );
    }
    if (sendlink_element) {
      sendlink_element
        .attr( 'title', strings('Chat') )
        .attr( 'onClick', 'javascript:listuser.showchat();return false;' )
        .attr( 'href', '' );
    }
    if (chatlink_element) {
      chatlink_element
        .attr( 'title', strings('Conversation') )
        .attr( 'onClick', 'javascript:listuser.showconversation();return false;' )
        .attr( 'href', '' );
    }
    if (memberlink_element) {
      memberlink_element
        .attr( 'title', strings('Members') );
    }
    if (librarylink_element) {
      librarylink_element
        .attr( 'title', strings('Library') );
    }
    if (infolink_element) {
      infolink_element
        .attr( 'title', strings('Information') )
        .attr( 'onClick', 'javascript:listuser.showinfo();return false;' )
        .attr( 'href', '' );
    }
    
    this.slidephotos = [];
    
    if (username == null) return;
    this.init_form(username);
    
    var params = '&action=info&username=' + encodeURIComponent(username);
    
    $.ajax
    (
      {
        url : app.user_path + '/userinfo?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var profile = response['profile'];
          var user = response['user'];
          var group = response['group'];
          var avatarSection = response['avatar'];
          var backgroundSection = response['background'];
          if (editmode == true)
            listuser.init_values( username, user, group, profile, avatarSection, backgroundSection );
          else
            listuser.init_details( username, user, group, profile, avatarSection, backgroundSection );
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
  click_avatar: function()
  {
    var username = this.username;
    if (username == null || username.length == 0)
      return;
    
    photoslide.show( listuser.slidephotos, 0, false );
  },
  changeavatar: function()
  {
    var username = this.username;
    if (username == null || username.length == 0)
      return;
    
    var emptysrc = this.empty_avatar;
    
    artwork.showselect0( username, 'Public Files', 
      function( section ) {
        if (section) {
          var id = section['id'];
          if (id == null || id.length <= 0) id = 'null';
          listuser.save_artwork(id, null);
        }
      }, emptysrc);
  },
  changebackground: function()
  {
    var username = this.username;
    if (username == null || username.length == 0)
      return;
    
    var emptysrc = this.empty_background;
    
    artwork.showselect0( username, 'Public Files', 
      function( section ) {
        if (section) {
          var id = section['id'];
          if (id == null || id.length <= 0) id = 'null';
          listuser.save_artwork(null, id);
        }
      }, emptysrc);
  },
  save_artwork: function(avatar_id, background_id)
  {
    if (avatar_id == null && background_id == null)
      return;
    
    var username = this.username;
    if (username == null || username.length == 0)
      return;
    
    var avatar = avatar_id;
    var background = background_id;
    
    if (avatar == null) avatar = '';
    if (background == null) background = '';
    
    var params = '&action=update&username=' + encodeURIComponent(username) 
               + '&avatar=' + encodeURIComponent(avatar) 
               + '&background=' + encodeURIComponent(background);
    
    $.ajax
    (
      {
        url : app.user_path + '/userinfo?token=' + app.token + params + '&wt=json',
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
  init_form: function( username )
  {
    if (username == null || username.length == 0)
      return;
    
    var form_element = $( '#user-form' );
    var submitbutton_element = $( '#user-submit-button' );
    var cancelbutton_element = $( '#user-cancel-button' );
    
    var avatar_element = $( '#user-avatar' );
    var avatarlink_element = $( '#user-avatar-link' );
    var changeavatar_element = $( '#user-change-avatar' );
    var changebackground_element = $( '#user-change-background' );
    
    var nicknameinput_element = $( '#user-nickname-input' );
    var firstnameinput_element = $( '#user-firstname-input' );
    var lastnameinput_element = $( '#user-lastname-input' );
    var sexinput_element = $( '#user-sex-input' );
    var birthdayinput_element = $( '#user-birthday-input' );
    var timezoneinput_element = $( '#user-timezone-input' );
    var regioninput_element = $( '#user-region-input' );
    var tagsinput_element = $( '#user-tags-input' );
    var briefinput_element = $( '#user-brief-input' );
    var introinput_element = $( '#user-intro-input' );
    
    $( '#user-change-avatar-text' ).html( strings('Change Avatar') );
    $( '#user-change-background-text' ).html( strings('Change Background') );
    
    $( '#user-metadata-region-name' ).html( strings('Region') );
    $( '#user-metadata-birthday-name' ).html( strings('Birthday') );
    $( '#user-metadata-timezone-name' ).html( strings('Timezone') );
    $( '#user-metadata-tags-name' ).html( strings('Tags') );
    
    $( '#user-nickname-text' ).html( strings('Nick Name') );
    $( '#user-firstname-text' ).html( strings('First Name') );
    $( '#user-lastname-text' ).html( strings('Last Name') );
    $( '#user-sex-text' ).html( strings('Sex') );
    $( '#user-birthday-text' ).html( strings('Birthday') );
    $( '#user-timezone-text' ).html( strings('Timezone') );
    $( '#user-region-text' ).html( strings('Region') );
    $( '#user-tags-text' ).html( strings('Tags') );
    $( '#user-brief-text' ).html( strings('Brief') );
    $( '#user-intro-text' ).html( strings('Introduction') );
    
    $( '#user-submit-button' ).html( strings('Save') );
    $( '#user-cancel-button' ).html( strings('Cancel') );
    
    avatarlink_element
      .attr( 'onclick', 'javascript:listuser.click_avatar();return false;' )
      .attr( 'href', '' );
    
    changeavatar_element
      .attr( 'onClick', 'javascript:listuser.changeavatar();return false;' );
    
    changebackground_element
      .attr( 'onClick', 'javascript:listuser.changebackground();return false;' );
    
    nicknameinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-nickname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-nickname-lock\', true);' )
      .attr( 'value', '' );
    
    firstnameinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-firstname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-firstname-lock\', true);' )
      .attr( 'value', '' );
    
    lastnameinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-lastname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-lastname-lock\', true);' )
      .attr( 'value', '' );
    
    sexinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-sex-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-sex-lock\', true);' )
      .attr( 'onFocus', 'javascript:listuser.on_sex_focus(this);' )
      .attr( 'onBlur', 'javascript:listuser.on_sex_blur(this);' )
      .attr( 'value', '' );
    
    birthdayinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-birthday-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-birthday-lock\', true);' )
      .attr( 'value', '' ).datepicker();
    
    timezoneinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-timezone-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-timezone-lock\', true);' )
      .attr( 'onFocus', 'javascript:listuser.on_timezone_focus(this);' )
      .attr( 'onBlur', 'javascript:listuser.on_timezone_blur(this);' )
      .attr( 'value', '' );
    
    regioninput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-region-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-region-lock\', true);' )
      .attr( 'value', '' );
    
    tagsinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-tags-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-tags-lock\', true);' )
      .attr( 'value', '' );
    
    briefinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-brief-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-brief-lock\', true);' )
      .attr( 'value', '' );
    
    introinput_element
      .attr( 'onChange', 'javascript:listuser.lockchanged(\'#user-intro-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listuser.lockchanged(\'#user-intro-lock\', true);' )
      .attr( 'value', '' );
    
    this.init_lockelement( '#user-nickname-lock' );
    this.init_lockelement( '#user-firstname-lock' );
    this.init_lockelement( '#user-lastname-lock' );
    this.init_lockelement( '#user-sex-lock' );
    this.init_lockelement( '#user-birthday-lock' );
    this.init_lockelement( '#user-timezone-lock' );
    this.init_lockelement( '#user-region-lock' );
    this.init_lockelement( '#user-tags-lock' );
    this.init_lockelement( '#user-brief-lock' );
    this.init_lockelement( '#user-intro-lock' );
    
    listuser.empty_avatar = 'css/' + app.theme + '/images/posters/friend.png';
    listuser.empty_background = 'css/' + app.theme + '/images/background.png';
    
    var params = '&action=update&username=' + encodeURIComponent(username);
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/userinfo?token=' + app.token + params + '&wt=json',
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
              sammy.refresh();
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
    //  .attr( 'onClick', 'javascript:listuser.save_submit();return false;' );
    
    cancelbutton_element
      .attr( 'onClick', 'javascript:listuser.save_cancel();return false;' );
    
  },
  init_values: function( username, user, group, profile, avatarSection, backgroundSection )
  {
    $( '#user-metadata' ).addClass( 'hide' );
    $( '#user-form' ).removeClass( 'hide' );
    $( '#user-change-avatar' ).removeClass( 'hide' );
    $( '#user-change-background' ).removeClass( 'hide' );
    
    if (username == null || profile == null) return;
    
    var nickname = this.get_value(profile, 'nickname');
    var firstname = this.get_value(profile, 'firstname');
    var lastname = this.get_value(profile, 'lastname');
    var sex = this.get_value(profile, 'sex');
    var birthday = this.get_value(profile, 'birthday');
    var timezone = this.get_value(profile, 'timezone');
    var region = this.get_value(profile, 'region');
    var tags = this.get_value(profile, 'tags');
    var brief = this.get_value(profile, 'brief');
    var intro = this.get_value(profile, 'intro');
    var avatar = this.get_value(profile, 'avatar');
    var background = this.get_value(profile, 'background');
    
    var nicknameinput_element = $( '#user-nickname-input' );
    var firstnameinput_element = $( '#user-firstname-input' );
    var lastnameinput_element = $( '#user-lastname-input' );
    var sexinput_element = $( '#user-sex-input' );
    var birthdayinput_element = $( '#user-birthday-input' );
    var timezoneinput_element = $( '#user-timezone-input' );
    var regioninput_element = $( '#user-region-input' );
    var tagsinput_element = $( '#user-tags-input' );
    var briefinput_element = $( '#user-brief-input' );
    var introinput_element = $( '#user-intro-input' );
    
    nicknameinput_element.attr( 'value', nickname );
    firstnameinput_element.attr( 'value', firstname );
    lastnameinput_element.attr( 'value', lastname );
    sexinput_element.attr( 'value', sex );
    birthdayinput_element.attr( 'value', birthday );
    timezoneinput_element.attr( 'value', timezone );
    regioninput_element.attr( 'value', region );
    //tagsinput_element.attr( 'value', tags );
    briefinput_element.attr( 'value', brief );
    introinput_element.attr( 'value', intro );
    
    user_tags.init_value( tags );
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    if (avatarSection && avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
      
      listuser.slidephotos = [];
      listuser.slidephotos.push( avatarSection );
    }
    
    $( '#user-avatar' ).attr( 'src', thumbsrc);
    
    if (background != null && background.length > 0) {
      var id = background;
      var extension = 'jpg';
      
      var src = app.base_path + '/image/' + id + '.' + extension + '?token=' + app.token;
      $( '#background-image' )
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
    
    var cb = this.showcb;
    if (cb) cb.call(this, username, user, group, profile);
  },
  init_details: function( username, user, group, profile, avatarSection, backgroundSection )
  {
    $( '#user-metadata' ).removeClass( 'hide' );
    $( '#user-form' ).addClass( 'hide' );
    $( '#user-change-avatar' ).addClass( 'hide' );
    $( '#user-change-background' ).addClass( 'hide' );
    
    $( '#user-metadata-region-name' ).html( strings('Region') );
    $( '#user-metadata-birthday-name' ).html( strings('Birthday') );
    $( '#user-metadata-timezone-name' ).html( strings('Timezone') );
    $( '#user-metadata-tags-name' ).html( strings('Tags') );
    
    if (username == null || profile == null) return;
    
    var nickname = this.get_value(profile, 'nickname');
    var firstname = this.get_value(profile, 'firstname');
    var lastname = this.get_value(profile, 'lastname');
    var sex = this.get_value(profile, 'sex');
    var birthday = this.get_value(profile, 'birthday');
    var timezone = this.get_value(profile, 'timezone');
    var region = this.get_value(profile, 'region');
    var tags = this.get_value(profile, 'tags');
    var brief = this.get_value(profile, 'brief');
    var intro = this.get_value(profile, 'intro');
    var avatar = this.get_value(profile, 'avatar');
    var background = this.get_value(profile, 'background');
    
    if (nickname == null) nickname = '';
    if (firstname == null) firstname = '';
    if (lastname == null) lastname = '';
    
    var mtime = profile['mtime'];
    var date = mtime > 0 ? new Date(mtime) : new Date();
    
    var year = '' + (1900+date.getYear());
    var title = nickname;
    var title2 = '';
    var subtitle = brief;
    var summary = intro;
    
    var name = ''; //nickname;
    if (name == null || name.length == 0) {
      name = firstname;
      if (lastname != null && lastname.length > 0) {
        if (name.length > 0) name += ' ';
        name += lastname;
      }
    }
    if (title == null || title.length == 0) {
      if (username == null || username.length == 0) 
        username = globalApp.get_username();
      title = username;
      if (title == null || title.length == 0) 
        title = strings( '[Untitled]' );
    }
    
    if (group) {
      var memberCount = group['mcount'];
      if (memberCount == null || memberCount < 0)
        memberCount = 0;
      
      name = (name.trim() + ' ' + memberCount).trim();
    }
    
    $( '#user-metadata-year' ).html( year );
    $( '#user-metadata-title' ).html( title );
    $( '#user-metadata-title2' ).html( title2 );
    $( '#user-metadata-name' ).html( name );
    $( '#user-metadata-subtitle' ).html( subtitle );
    $( '#user-metadata-region' ).html( region );
    $( '#user-metadata-birthday' ).html( birthday );
    $( '#user-metadata-timezone' ).html( timezone );
    $( '#user-metadata-tags' ).html( tags );
    $( '#user-metadata-summary' ).html( summary );
    
    if (sex == 'male') {
      $( '#user-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon male"></i>' );
    } else if (sex == 'female') {
      $( '#user-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon female"></i>' );
    }
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    
    if (avatarSection && avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
      
      listuser.slidephotos = [];
      listuser.slidephotos.push( avatarSection );
    }
    
    $( '#user-avatar' ).attr( 'src', thumbsrc);
    
    if (background != null && background.length > 0) {
      var id = background;
      var extension = 'jpg';
      
      var src = app.base_path + '/image/' + id + '.' + extension + '?token=' + app.token;
      $( '#background-image' )
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
    
    var cb = this.showcb;
    if (cb) cb.call(this, username, user, group, profile);
  },
  showgroup: function()
  {
    listuser.showcb = this.group_showcb;
    listuser.showlist0( this.username, false );
  },
  showgroupedit: function()
  {
    listuser.showcb = this.group_showcb;
    listuser.showlist0( this.username, true );
  },
  group_showcb: function( username, user, group, profile )
  {
    var infoclickto = 'javascript:groupinfo.showdetails(\'' + username + '\');return false;';
    var memberlinkto = '#/~members/' + encodeURIComponent(username);
    var librarylinkto = '#/~browse/' + encodeURIComponent(globalApp.get_browsekey(username));
    
    var nickname = null;
    if (group) nickname = group['title'];
    navbar.init_grouptitle( username, nickname, infoclickto );
    
    $( '#action-edit-item' ).addClass( 'hide' );
    $( '#action-publish-item' ).addClass( 'hide' );
    $( '#action-send-item' ).addClass( 'hide' );
    $( '#action-chat-item' ).addClass( 'hide' );
    $( '#action-member-item' ).addClass( 'hide' );
    $( '#action-library-item' ).addClass( 'hide' );
    $( '#action-info-item' ).removeClass( 'hide' );
    
    if (group) {
      var role = group['role'];
      if (role == 'owner' || role == 'manager') {
        $( '#action-edit-item' ).removeClass( 'hide' );
      }
      if (role != null && role.length > 0) {
        $( '#action-publish-item' ).removeClass( 'hide' );
        $( '#action-chat-item' ).removeClass( 'hide' );
        $( '#action-member-item' ).removeClass( 'hide' );
        $( '#action-library-item' ).removeClass( 'hide' );
      }
    }
    
    $( '#user-lastname-group' ).addClass( 'hide' );
    $( '#user-sex-row' ).addClass( 'hide' );
    
    $( '#action-member-link' )
      .attr( 'href', memberlinkto );
    
    $( '#action-library-link' )
      .attr( 'href', librarylinkto );
    
    $( '#action-info-link' )
      .attr( 'onClick', infoclickto );
    
    $( '#user-metadata-nameicon' )
      .attr( 'class', 'unwatched-icon' )
      .attr( 'style', 'background-color: #000000;' )
      .html( '<i class="glyphicon parents"></i>' );
  },
  user_showcb: function( username, user, group, profile )
  {
    var infoclickto = 'javascript:userinfo.showdetails(\'' + username + '\');return false;';
    
    var nickname = null;
    if (user) nickname = user['title'];
    navbar.init_usertitle( username, nickname, infoclickto );
    
    $( '#action-edit-item' ).addClass( 'hide' );
    $( '#action-publish-item' ).addClass( 'hide' );
    $( '#action-send-item' ).removeClass( 'hide' );
    $( '#action-chat-item' ).addClass( 'hide' );
    $( '#action-member-item' ).addClass( 'hide' );
    $( '#action-library-item' ).addClass( 'hide' );
    $( '#action-info-item' ).removeClass( 'hide' );
    
    $( '#action-info-link' )
      .attr( 'onClick', infoclickto );
  },
  edit: function()
  {
    this.showgroupedit();
  },
  view: function()
  {
    this.showlist0( this.username, false );
  },
  save_submit: function()
  {
    var form_element = $( '#user-form' );
    
    form_element.submit();
  },
  save_cancel: function()
  {
    this.view();
  },
  init_lockelement: function( idname )
  {
    if (idname == null) return;
    var lockelement = $( idname );
    if (lockelement && idname) {
      this.lockchanged( idname, false );
      lockelement
        .attr( 'onClick', 'javascript:listuser.changelock(\'' + idname + '\');' );
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
    
    var scrollelems = $( '#user-scroll' );
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
        '<li id="selectlist-item-' + index + '" onMouseOver="javascript:listuser.on_select_focus(' + index + ');" onMouseOut="javascript:listuser.on_select_out(' + index + ');" onMouseDown="javascript:listuser.on_select_click(' + index + ');" onClick="javascript:return false;" class="select2-results-dept-0 select2-result select2-result-selectable ' + highlight + '">' + "\n" +
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
  }
};

var user_headbar = {
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

// #/~user
sammy.get
(
  // /^#\/(~user)$/,
  new RegExp( '(~user)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(8);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    user_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/user.html',
      function( template )
      {
        body_element
          .html( template );

        listuser.username = id_param;
        listuser.showlist();

        statusbar.show();
      }
    );
  }
);

// #/~group
sammy.get
(
  // /^#\/(~group)$/,
  new RegExp( '(~group)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(9);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    user_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/user.html',
      function( template )
      {
        body_element
          .html( template );

        listuser.username = id_param;
        listuser.showgroup();

        statusbar.show();
      }
    );
  }
);
