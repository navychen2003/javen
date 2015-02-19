
var profile_tags = {
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
    $( '#profile-tags-autogen' )
      .attr( 'onClick', 'javascript:profile_tags.onfocus();' );
    
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
      
      var clickto = 'javascript:profile_tags.remove_value(' + key + ');return false;';
      var item = '<li class="select2-search-choice"><div>' + value.esc() + '</div><a onclick="' + clickto + '" class="select2-search-choice-close" tabindex="-1"></a></li>';
      
      valueContent.push( item );
    }
    
    var keydown = 'javascript:return profile_tags.input_keydown(this,event);';
    var keyup = 'javascript:profile_tags.input_keyup(this,event);';
    var focus = 'javascript:profile_tags.input_focus(this,event);';
    var blur = 'javascript:profile_tags.input_blur(this,event);';
    
    var item = '<li class="select2-search-field"><input id="profile-tags-selectinput" type="text" autocomplete="off" class="select2-input" style="width: 10px;" onKeyDown="' + keydown + '" onKeyUp="' + keyup + '" onFocus="' + focus + '" onBlur="' + blur + '"></li>';
    
    valueContent.push( item );
    
    $( '#profile-tags-list' ).html( valueContent.join( '\n' ) );
    $( '#profile-tags-input' ).attr( 'value', valueStr.esc() );
    
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
    $( '#profile-tags-autogen' )
      .addClass( 'select2-container-active' );
    $( '#profile-tags-selectinput' )
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
    
    $( '#profile-tags-selectinput' ).attr( 'style', 'width: ' + width + 'px;' );
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
    
    $( '#profile-tags-autogen' )
      .removeClass( 'select2-container-active' );
    $( '#profile-tags-selectinput' )
      .removeClass( 'select2-focused' );
  }
};

var listprofile = { 
  slidephotos: [],
  selectlist: null,
  selectelement: null,
  lockelement: null,
  empty_avatar: null,
  empty_background: null,
  
  view: function()
  {
    this.showlist(false);
  },
  edit: function()
  {
    this.showlist(true);
  },
  showpublication: function()
  {
    var context = system.context;
    context.redirect( '#/~posts/me' );
  },
  showmessage: function()
  {
    var context = system.context;
    context.redirect( '#/~messages' );
  },
  showfriend: function()
  {
    var context = system.context;
    context.redirect( '#/~friends' );
  },
  showcontact: function()
  {
    var context = system.context;
    context.redirect( '#/~contacts' );
  },
  showlibrary: function()
  {
    var context = system.context;
    var username = globalApp.get_username();
    context.redirect( '#/~browse/' + encodeURIComponent(globalApp.get_browsekey(username)) );
  },
  showinfo: function()
  {
    userinfo.showme();
  },
  click_avatar: function()
  {
    photoslide.show( listprofile.slidephotos, 0, false );
  },
  changeavatar: function()
  {
    var emptysrc = this.empty_avatar;
    
    artwork.showselect('Public Files', function( section ) {
          if (section) {
            var id = section['id'];
            if (id == null || id.length <= 0) id = 'null';
            listprofile.save_artwork(id, null);
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
            listprofile.save_artwork(null, id);
          }
        }, emptysrc);
  },
  save_artwork: function(avatar_id, background_id)
  {
    if (avatar_id == null && background_id == null)
      return;
    
    var avatar = avatar_id;
    var background = background_id;
    
    if (avatar == null) avatar = '';
    if (background == null) background = '';
    
    var params = '&action=update&avatar=' + encodeURIComponent(avatar) 
      + '&background=' + encodeURIComponent(background);
    
    $.ajax
    (
      {
        url : app.user_path + '/profile?token=' + app.token + params + '&wt=json',
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
            globalApp.update( 'all', function() {
                sammy.refresh();
              });
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
  showlist: function( editmode )
  {
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var backlink_element = $( '#back-link' );
    
    var page_title = strings( 'My Library' );
    if (system.friendlyName != null)
      page_title = page_title + ' (' + system.friendlyName + ')';
    
    title_element.html( page_title.esc() );
    
    var metadata_element = $( '#profile-metadata' );
    var form_element = $( '#profile-form' );
    var submitbutton_element = $( '#profile-submit-button' );
    var cancelbutton_element = $( '#profile-cancel-button' );
    
    var editlink_element = $( '#action-edit-link' );
    var publishlink_element = $( '#action-publish-link' );
    var messagelink_element = $( '#action-message-link' );
    var friendlink_element = $( '#action-friend-link' );
    var contactlink_element = $( '#action-contact-link' );
    var librarylink_element = $( '#action-library-link' );
    var infolink_element = $( '#action-info-link' );
    
    var avatar_element = $( '#profile-avatar' );
    var avatarlink_element = $( '#profile-avatar-link' );
    var changeavatar_element = $( '#profile-change-avatar' );
    var changebackground_element = $( '#profile-change-background' );
    
    var emailinput_element = $( '#profile-email-input' );
    var nicknameinput_element = $( '#profile-nickname-input' );
    var firstnameinput_element = $( '#profile-firstname-input' );
    var lastnameinput_element = $( '#profile-lastname-input' );
    var sexinput_element = $( '#profile-sex-input' );
    var birthdayinput_element = $( '#profile-birthday-input' );
    var timezoneinput_element = $( '#profile-timezone-input' );
    var regioninput_element = $( '#profile-region-input' );
    var tagsinput_element = $( '#profile-tags-input' );
    var briefinput_element = $( '#profile-brief-input' );
    var introinput_element = $( '#profile-intro-input' );
    
    if (editlink_element) {
      editlink_element
        .attr( 'title', strings('Edit') )
        .attr( 'onClick', 'javascript:listprofile.edit();return false;' )
        .attr( 'href', '' );
    }
    if (publishlink_element) {
      publishlink_element
        .attr( 'title', strings('Publications') )
        .attr( 'onClick', 'javascript:listprofile.showpublication();return false;' )
        .attr( 'href', '' );
    }
    if (messagelink_element) {
      messagelink_element
        .attr( 'title', strings('Messages') )
        .attr( 'onClick', 'javascript:listprofile.showmessage();return false;' )
        .attr( 'href', '' );
    }
    if (friendlink_element) {
      friendlink_element
        .attr( 'title', strings('Friends') )
        .attr( 'onClick', 'javascript:listprofile.showfriend();return false;' )
        .attr( 'href', '' );
    }
    if (contactlink_element) {
      contactlink_element
        .attr( 'title', strings('Contact') )
        .attr( 'onClick', 'javascript:listprofile.showcontact();return false;' )
        .attr( 'href', '' );
    }
    if (librarylink_element) {
      librarylink_element
        .attr( 'title', strings('Library') )
        .attr( 'onClick', 'javascript:listprofile.showlibrary();return false;' )
        .attr( 'href', '' );
    }
    if (infolink_element) {
      infolink_element
        .attr( 'title', strings('Information') )
        .attr( 'onClick', 'javascript:listprofile.showinfo();return false;' )
        .attr( 'href', '' );
    }
    
    $( '#profile-change-avatar-text' ).html( strings('Change Avatar') );
    $( '#profile-change-background-text' ).html( strings('Change Background') );
    
    $( '#profile-metadata-region-name' ).html( strings('Region') );
    $( '#profile-metadata-birthday-name' ).html( strings('Birthday') );
    $( '#profile-metadata-timezone-name' ).html( strings('Timezone') );
    $( '#profile-metadata-tags-name' ).html( strings('Tags') );
    
    $( '#profile-email-text' ).html( strings('Email') );
    $( '#profile-nickname-text' ).html( strings('Nick Name') );
    $( '#profile-firstname-text' ).html( strings('First Name') );
    $( '#profile-lastname-text' ).html( strings('Last Name') );
    $( '#profile-sex-text' ).html( strings('Sex') );
    $( '#profile-birthday-text' ).html( strings('Birthday') );
    $( '#profile-timezone-text' ).html( strings('Timezone') );
    $( '#profile-region-text' ).html( strings('Region') );
    $( '#profile-tags-text' ).html( strings('Tags') );
    $( '#profile-brief-text' ).html( strings('Brief') );
    $( '#profile-intro-text' ).html( strings('Introduction') );
    
    $( '#profile-submit-button' ).html( strings('Save') );
    $( '#profile-cancel-button' ).html( strings('Cancel') );
    
    navbar.init_metitle( 'javascript:userinfo.showme();' );
    
    avatarlink_element
      .attr( 'onclick', 'javascript:listprofile.click_avatar();return false;' )
      .attr( 'href', '' );
    
    changeavatar_element
      .attr( 'onClick', 'javascript:listprofile.changeavatar();return false;' );
    
    changebackground_element
      .attr( 'onClick', 'javascript:listprofile.changebackground();return false;' );
    
    nicknameinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-nickname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-nickname-lock\', true);' )
      .attr( 'value', '' );
    
    emailinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-email-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-email-lock\', true);' )
      .attr( 'value', '' );
    
    firstnameinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-firstname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-firstname-lock\', true);' )
      .attr( 'value', '' );
    
    lastnameinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-lastname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-lastname-lock\', true);' )
      .attr( 'value', '' );
    
    sexinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-sex-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-sex-lock\', true);' )
      .attr( 'onFocus', 'javascript:listprofile.on_sex_focus(this);' )
      .attr( 'onBlur', 'javascript:listprofile.on_sex_blur(this);' )
      .attr( 'value', '' );
    
    birthdayinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-birthday-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-birthday-lock\', true);' )
      .attr( 'value', '' ).datepicker();
    
    timezoneinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-timezone-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-timezone-lock\', true);' )
      .attr( 'onFocus', 'javascript:listprofile.on_timezone_focus(this);' )
      .attr( 'onBlur', 'javascript:listprofile.on_timezone_blur(this);' )
      .attr( 'value', '' );
    
    regioninput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-region-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-region-lock\', true);' )
      .attr( 'value', '' );
    
    tagsinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-tags-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-tags-lock\', true);' )
      .attr( 'value', '' );
    
    briefinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-brief-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-brief-lock\', true);' )
      .attr( 'value', '' );
    
    introinput_element
      .attr( 'onChange', 'javascript:listprofile.lockchanged(\'#profile-intro-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:listprofile.lockchanged(\'#profile-intro-lock\', true);' )
      .attr( 'value', '' );
    
    this.init_lockelement( '#profile-email-lock' );
    this.init_lockelement( '#profile-nickname-lock' );
    this.init_lockelement( '#profile-firstname-lock' );
    this.init_lockelement( '#profile-lastname-lock' );
    this.init_lockelement( '#profile-sex-lock' );
    this.init_lockelement( '#profile-birthday-lock' );
    this.init_lockelement( '#profile-timezone-lock' );
    this.init_lockelement( '#profile-region-lock' );
    this.init_lockelement( '#profile-tags-lock' );
    this.init_lockelement( '#profile-brief-lock' );
    this.init_lockelement( '#profile-intro-lock' );
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/profile?action=update&token=' + app.token + '&wt=json',
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
              globalApp.update( 'all', function() {
                  sammy.refresh();
                });
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
    //  .attr( 'onClick', 'javascript:listprofile.save_submit();' );
    
    cancelbutton_element
      .attr( 'onClick', 'javascript:listprofile.save_cancel();' );
    
    this.slidephotos = [];
    var params = '&action=info';
    
    $.ajax
    (
      {
        url : app.user_path + '/profile?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var profile = response['profile'];
          var avatarSection = response['avatar'];
          var backgroundSection = response['background'];
          if (editmode == true)
            listprofile.init_values( profile, avatarSection, backgroundSection );
          else
            listprofile.init_metadata( profile, avatarSection, backgroundSection );
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
  init_metadata: function( profile, avatarSection, backgroundSection )
  {
    var metadata_element = $( '#profile-metadata' );
    var form_element = $( '#profile-form' );
    var changeavatar_element = $( '#profile-change-avatar' );
    var changebackground_element = $( '#profile-change-background' );
    
    metadata_element.removeClass( 'hide' );
    form_element.addClass( 'hide' );
    //changeavatar_element.addClass( 'hide' );
    //changebackground_element.addClass( 'hide' );
    
    if (profile == null) return;
    
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
    
    var year = ''; // + (1900+date.getYear());
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
      title = globalApp.get_username();
      if (title == null || title.length == 0) 
        title = '[Untitled]';
    }
    
    $( '#profile-metadata-year' ).html( year );
    $( '#profile-metadata-title' ).html( title );
    $( '#profile-metadata-title2' ).html( title2 );
    $( '#profile-metadata-name' ).html( name );
    $( '#profile-metadata-subtitle' ).html( subtitle );
    $( '#profile-metadata-region' ).html( region );
    $( '#profile-metadata-birthday' ).html( birthday );
    $( '#profile-metadata-timezone' ).html( timezone );
    $( '#profile-metadata-tags' ).html( tags );
    $( '#profile-metadata-summary' ).html( summary );
    
    if (sex == 'male') {
      $( '#profile-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon male"></i>' );
    } else if (sex == 'female') {
      $( '#profile-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon female"></i>' );
    }
    
    var avatar_element = $( '#profile-avatar' );
    var background_element = $( '#background-image' );
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    
    listprofile.empty_avatar = thumbsrc;
    listprofile.empty_background = 'css/' + app.theme + '/images/background.png';
    
    if (avatarSection && avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
      
      listprofile.slidephotos = [];
      listprofile.slidephotos.push( avatarSection );
    }
    
    avatar_element.attr( 'src', thumbsrc );
    
    if (background != null && background.length > 0) {
      var id = background;
      var extension = 'jpg';
      
      var src = app.base_path + '/image/' + id + '.' + extension + '?token=' + app.token;
      background_element
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
  },
  init_values: function( profile, avatarSection, backgroundSection )
  {
    var metadata_element = $( '#profile-metadata' );
    var form_element = $( '#profile-form' );
    var changeavatar_element = $( '#profile-change-avatar' );
    var changebackground_element = $( '#profile-change-background' );
    
    metadata_element.addClass( 'hide' );
    form_element.removeClass( 'hide' );
    changeavatar_element.removeClass( 'hide' );
    changebackground_element.removeClass( 'hide' );
    
    if (profile == null) return;
    
    var email = this.get_value(profile, 'email');
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
    
    var emailinput_element = $( '#profile-email-input' );
    var nicknameinput_element = $( '#profile-nickname-input' );
    var firstnameinput_element = $( '#profile-firstname-input' );
    var lastnameinput_element = $( '#profile-lastname-input' );
    var sexinput_element = $( '#profile-sex-input' );
    var birthdayinput_element = $( '#profile-birthday-input' );
    var timezoneinput_element = $( '#profile-timezone-input' );
    var regioninput_element = $( '#profile-region-input' );
    var tagsinput_element = $( '#profile-tags-input' );
    var briefinput_element = $( '#profile-brief-input' );
    var introinput_element = $( '#profile-intro-input' );
    var avatar_element = $( '#profile-avatar' );
    var background_element = $( '#background-image' );
    
    emailinput_element.attr( 'value', email );
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
    
    profile_tags.init_value( tags );
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    if (avatarSection && avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
      
      listprofile.slidephotos = [];
      listprofile.slidephotos.push( avatarSection );
    }
    
    avatar_element.attr( 'src', thumbsrc );
    
    if (background != null && background.length > 0) {
      var id = background;
      var extension = 'jpg';
      
      var src = app.base_path + '/image/' + id + '.' + extension + '?token=' + app.token;
      background_element
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
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
  save_submit: function()
  {
    var form_element = $( '#profile-form' );
    
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
        .attr( 'onClick', 'javascript:listprofile.changelock(\'' + idname + '\');' );
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
    
    var scrollelems = $( '#profile-scroll' );
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
        '<li id="selectlist-item-' + index + '" onMouseOver="javascript:listprofile.on_select_focus(' + index + ');" onMouseOut="javascript:listprofile.on_select_out(' + index + ');" onMouseDown="javascript:listprofile.on_select_click(' + index + ');" onClick="javascript:return false;" class="select2-results-dept-0 select2-result select2-result-selectable ' + highlight + '">' + "\n" +
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
  on_sex_focus: function( elem )
  {
    var values = {};
    var selectedval = '';
    values['unisex'] = strings( 'Unisex' );
    values['male'] = strings( 'Male' );
    values['female'] = strings( 'Female' );
    
    this.selectelement = $( '#profile-sex-input' );
    this.lockelement = $( '#profile-sex-lock' );
    this.showselects( elem, values, selectedval );
  },
  on_sex_blur: function( elem )
  {
    var selectlist_element = $( '#selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( '' );
  },
  on_timezone_focus: function( elem )
  {
    var values = {};
    var selectedval = '';
    values['(GMT-10:00) Hawaii'] = strings( '(GMT-10:00) Hawaii' );
    values['(GMT-09:00) Alaska'] = strings( '(GMT-09:00) Alaska' );
    values['(GMT-08:00) Pacific Time (US & Canada)'] = strings( '(GMT-08:00) Pacific Time (US & Canada)' );
    values['(GMT-07:00) Arizona'] = strings( '(GMT-07:00) Arizona' );
    values['(GMT-07:00) Mountain Time (US & Canada)'] = strings( '(GMT-07:00) Mountain Time (US & Canada)' );
    values['(GMT-06:00) Central Time (US & Canada)'] = strings( '(GMT-06:00) Central Time (US & Canada)' );
    values['(GMT-05:00) Eastern Time (US & Canada)'] = strings( '(GMT-05:00) Eastern Time (US & Canada)' );
    values['(GMT-05:00) Indiana (East)'] = strings( '(GMT-05:00) Indiana (East)' );
    values['(GMT-11:00) American Samoa'] = strings( '(GMT-11:00) American Samoa' );
    values['(GMT-11:00) International Date Line West'] = strings( '(GMT-11:00) International Date Line West' );
    values['(GMT-11:00) Midway Island'] = strings( '(GMT-11:00) Midway Island' );
    values['(GMT-08:00) Tijuana'] = strings( '(GMT-08:00) Tijuana' );
    values['(GMT-07:00) Chihuahua'] = strings( '(GMT-07:00) Chihuahua' );
    values['(GMT-07:00) Mazatlan'] = strings( '(GMT-07:00) Mazatlan' );
    values['(GMT-06:00) Central America'] = strings( '(GMT-06:00) Central America' );
    values['(GMT-06:00) Guadalajara'] = strings( '(GMT-06:00) Guadalajara' );
    values['(GMT-06:00) Mexico City'] = strings( '(GMT-06:00) Mexico City' );
    values['(GMT-06:00) Monterrey'] = strings( '(GMT-06:00) Monterrey' );
    values['(GMT-06:00) Saskatchewan'] = strings( '(GMT-06:00) Saskatchewan' );
    values['(GMT-05:00) Bogota'] = strings( '(GMT-05:00) Bogota' );
    values['(GMT-05:00) Lima'] = strings( '(GMT-05:00) Lima' );
    values['(GMT-05:00) Quito'] = strings( '(GMT-05:00) Quito' );
    values['(GMT-04:30) Caracas'] = strings( '(GMT-04:30) Caracas' );
    values['(GMT-04:00) Atlantic Time (Canada)'] = strings( '(GMT-04:00) Atlantic Time (Canada)' );
    values['(GMT-04:00) Georgetown'] = strings( '(GMT-04:00) Georgetown' );
    values['(GMT-04:00) La Paz'] = strings( '(GMT-04:00) La Paz' );
    values['(GMT-04:00) Santiago'] = strings( '(GMT-04:00) Santiago' );
    values['(GMT-03:30) Newfoundland'] = strings( '(GMT-03:30) Newfoundland' );
    values['(GMT-03:00) Brasilia'] = strings( '(GMT-03:00) Brasilia' );
    values['(GMT-03:00) Buenos Aires'] = strings( '(GMT-03:00) Buenos Aires' );
    values['(GMT-03:00) Greenland'] = strings( '(GMT-03:00) Greenland' );
    values['(GMT-02:00) Mid-Atlantic'] = strings( '(GMT-02:00) Mid-Atlantic' );
    values['(GMT-01:00) Azores'] = strings( '(GMT-01:00) Azores' );
    values['(GMT-01:00) Cape Verde Is.'] = strings( '(GMT-01:00) Cape Verde Is.' );
    values['(GMT+00:00) Casablanca'] = strings( '(GMT+00:00) Casablanca' );
    values['(GMT+00:00) Dublin'] = strings( '(GMT+00:00) Dublin' );
    values['(GMT+00:00) Edinburgh'] = strings( '(GMT+00:00) Edinburgh' );
    values['(GMT+00:00) Lisbon'] = strings( '(GMT+00:00) Lisbon' );
    values['(GMT+00:00) London'] = strings( '(GMT+00:00) London' );
    values['(GMT+00:00) Monrovia'] = strings( '(GMT+00:00) Monrovia' );
    values['(GMT+00:00) UTC'] = strings( '(GMT+00:00) UTC' );
    values['(GMT+01:00) Amsterdam'] = strings( '(GMT+01:00) Amsterdam' );
    values['(GMT+01:00) Belgrade'] = strings( '(GMT+01:00) Belgrade' );
    values['(GMT+01:00) Berlin'] = strings( '(GMT+01:00) Berlin' );
    values['(GMT+01:00) Bern'] = strings( '(GMT+01:00) Bern' );
    values['(GMT+01:00) Bratislava'] = strings( '(GMT+01:00) Bratislava' );
    values['(GMT+01:00) Brussels'] = strings( '(GMT+01:00) Brussels' );
    values['(GMT+01:00) Budapest'] = strings( '(GMT+01:00) Budapest' );
    values['(GMT+01:00) Copenhagen'] = strings( '(GMT+01:00) Copenhagen' );
    values['(GMT+01:00) Ljubljana'] = strings( '(GMT+01:00) Ljubljana' );
    values['(GMT+01:00) Madrid'] = strings( '(GMT+01:00) Madrid' );
    values['(GMT+01:00) Paris'] = strings( '(GMT+01:00) Paris' );
    values['(GMT+01:00) Prague'] = strings( '(GMT+01:00) Prague' );
    values['(GMT+01:00) Rome'] = strings( '(GMT+01:00) Rome' );
    values['(GMT+01:00) Sarajevo'] = strings( '(GMT+01:00) Sarajevo' );
    values['(GMT+01:00) Skopje'] = strings( '(GMT+01:00) Skopje' );
    values['(GMT+01:00) Stockholm'] = strings( '(GMT+01:00) Stockholm' );
    values['(GMT+01:00) Vienna'] = strings( '(GMT+01:00) Vienna' );
    values['(GMT+01:00) Warsaw'] = strings( '(GMT+01:00) Warsaw' );
    values['(GMT+01:00) West Central Africa'] = strings( '(GMT+01:00) West Central Africa' );
    values['(GMT+01:00) Zagreb'] = strings( '(GMT+01:00) Zagreb' );
    values['(GMT+02:00) Athens'] = strings( '(GMT+02:00) Athens' );
    values['(GMT+02:00) Bucharest'] = strings( '(GMT+02:00) Bucharest' );
    values['(GMT+02:00) Cairo'] = strings( '(GMT+02:00) Cairo' );
    values['(GMT+02:00) Harare'] = strings( '(GMT+02:00) Harare' );
    values['(GMT+02:00) Helsinki'] = strings( '(GMT+02:00) Helsinki' );
    values['(GMT+02:00) Istanbul'] = strings( '(GMT+02:00) Istanbul' );
    values['(GMT+02:00) Jerusalem'] = strings( '(GMT+02:00) Jerusalem' );
    values['(GMT+02:00) Kyiv'] = strings( '(GMT+02:00) Kyiv' );
    values['(GMT+02:00) Pretoria'] = strings( '(GMT+02:00) Pretoria' );
    values['(GMT+02:00) Riga'] = strings( '(GMT+02:00) Riga' );
    values['(GMT+02:00) Sofia'] = strings( '(GMT+02:00) Sofia' );
    values['(GMT+02:00) Tallinn'] = strings( '(GMT+02:00) Tallinn' );
    values['(GMT+02:00) Vilnius'] = strings( '(GMT+02:00) Vilnius' );
    values['(GMT+03:00) Baghdad'] = strings( '(GMT+03:00) Baghdad' );
    values['(GMT+03:00) Kuwait'] = strings( '(GMT+03:00) Kuwait' );
    values['(GMT+03:00) Minsk'] = strings( '(GMT+03:00) Minsk' );
    values['(GMT+03:00) Nairobi'] = strings( '(GMT+03:00) Nairobi' );
    values['(GMT+03:00) Riyadh'] = strings( '(GMT+03:00) Riyadh' );
    values['(GMT+03:30) Tehran'] = strings( '(GMT+03:30) Tehran' );
    values['(GMT+04:00) Abu Dhabi'] = strings( '(GMT+04:00) Abu Dhabi' );
    values['(GMT+04:00) Baku'] = strings( '(GMT+04:00) Baku' );
    values['(GMT+04:00) Moscow'] = strings( '(GMT+04:00) Moscow' );
    values['(GMT+04:00) Muscat'] = strings( '(GMT+04:00) Muscat' );
    values['(GMT+04:00) St. Petersburg'] = strings( '(GMT+04:00) St. Petersburg' );
    values['(GMT+04:00) Tbilisi'] = strings( '(GMT+04:00) Tbilisi' );
    values['(GMT+04:00) Volgograd'] = strings( '(GMT+04:00) Volgograd' );
    values['(GMT+04:00) Yerevan'] = strings( '(GMT+04:00) Yerevan' );
    values['(GMT+04:30) Kabul'] = strings( '(GMT+04:30) Kabul' );
    values['(GMT+05:00) Islamabad'] = strings( '(GMT+05:00) Islamabad' );
    values['(GMT+05:00) Karachi'] = strings( '(GMT+05:00) Karachi' );
    values['(GMT+05:00) Tashkent'] = strings( '(GMT+05:00) Tashkent' );
    values['(GMT+05:30) Chennai'] = strings( '(GMT+05:30) Chennai' );
    values['(GMT+05:30) Kolkata'] = strings( '(GMT+05:30) Kolkata' );
    values['(GMT+05:30) Mumbai'] = strings( '(GMT+05:30) Mumbai' );
    values['(GMT+05:30) New Delhi'] = strings( '(GMT+05:30) New Delhi' );
    values['(GMT+05:30) Sri Jayawardenepura'] = strings( '(GMT+05:30) Sri Jayawardenepura' );
    values['(GMT+05:45) Kathmandu'] = strings( '(GMT+05:45) Kathmandu' );
    values['(GMT+06:00) Almaty'] = strings( '(GMT+06:00) Almaty' );
    values['(GMT+06:00) Astana'] = strings( '(GMT+06:00) Astana' );
    values['(GMT+06:00) Dhaka'] = strings( '(GMT+06:00) Dhaka' );
    values['(GMT+06:00) Ekaterinburg'] = strings( '(GMT+06:00) Ekaterinburg' );
    values['(GMT+06:30) Rangoon'] = strings( '(GMT+06:30) Rangoon' );
    values['(GMT+07:00) Bangkok'] = strings( '(GMT+07:00) Bangkok' );
    values['(GMT+07:00) Hanoi'] = strings( '(GMT+07:00) Hanoi' );
    values['(GMT+07:00) Jakarta'] = strings( '(GMT+07:00) Jakarta' );
    values['(GMT+07:00) Novosibirsk'] = strings( '(GMT+07:00) Novosibirsk' );
    values['(GMT+08:00) Beijing'] = strings( '(GMT+08:00) Beijing' );
    values['(GMT+08:00) Chongqing'] = strings( '(GMT+08:00) Chongqing' );
    values['(GMT+08:00) Hong Kong'] = strings( '(GMT+08:00) Hong Kong' );
    values['(GMT+08:00) Krasnoyarsk'] = strings( '(GMT+08:00) Krasnoyarsk' );
    values['(GMT+08:00) Kuala Lumpur'] = strings( '(GMT+08:00) Kuala Lumpur' );
    values['(GMT+08:00) Perth'] = strings( '(GMT+08:00) Perth' );
    values['(GMT+08:00) Singapore'] = strings( '(GMT+08:00) Singapore' );
    values['(GMT+08:00) Taipei'] = strings( '(GMT+08:00) Taipei' );
    values['(GMT+08:00) Ulaan Bataar'] = strings( '(GMT+08:00) Ulaan Bataar' );
    values['(GMT+08:00) Urumqi'] = strings( '(GMT+08:00) Urumqi' );
    values['(GMT+09:00) Irkutsk'] = strings( '(GMT+09:00) Irkutsk' );
    values['(GMT+09:00) Osaka'] = strings( '(GMT+09:00) Osaka' );
    values['(GMT+09:00) Sapporo'] = strings( '(GMT+09:00) Sapporo' );
    values['(GMT+09:00) Seoul'] = strings( '(GMT+09:00) Seoul' );
    values['(GMT+09:00) Tokyo'] = strings( '(GMT+09:00) Tokyo' );
    values['(GMT+09:30) Adelaide'] = strings( '(GMT+09:30) Adelaide' );
    values['(GMT+09:30) Darwin'] = strings( '(GMT+09:30) Darwin' );
    values['(GMT+10:00) Brisbane'] = strings( '(GMT+10:00) Brisbane' );
    values['(GMT+10:00) Canberra'] = strings( '(GMT+10:00) Canberra' );
    values['(GMT+10:00) Guam'] = strings( '(GMT+10:00) Guam' );
    values['(GMT+10:00) Hobart'] = strings( '(GMT+10:00) Hobart' );
    values['(GMT+10:00) Melbourne'] = strings( '(GMT+10:00) Melbourne' );
    values['(GMT+10:00) Port Moresby'] = strings( '(GMT+10:00) Port Moresby' );
    values['(GMT+10:00) Sydney'] = strings( '(GMT+10:00) Sydney' );
    values['(GMT+10:00) Yakutsk'] = strings( '(GMT+10:00) Yakutsk' );
    values['(GMT+11:00) New Caledonia'] = strings( '(GMT+11:00) New Caledonia' );
    values['(GMT+11:00) Vladivostok'] = strings( '(GMT+11:00) Vladivostok' );
    values['(GMT+12:00) Auckland'] = strings( '(GMT+12:00) Auckland' );
    values['(GMT+12:00) Fiji'] = strings( '(GMT+12:00) Fiji' );
    values['(GMT+12:00) Kamchatka'] = strings( '(GMT+12:00) Kamchatka' );
    values['(GMT+12:00) Magadan'] = strings( '(GMT+12:00) Magadan' );
    values['(GMT+12:00) Marshall Is.'] = strings( '(GMT+12:00) Marshall Is.' );
    values['(GMT+12:00) Solomon Is.'] = strings( '(GMT+12:00) Solomon Is.' );
    values['(GMT+12:00) Wellington'] = strings( '(GMT+12:00) Wellington' );
    values['(GMT+13:00) Nuku\'alofa'] = strings( '(GMT+13:00) Nuku\'alofa' );
    values['(GMT+13:00) Samoa'] = strings( '(GMT+13:00) Samoa' );
    values['(GMT+13:00) Tokelau Is.'] = strings( '(GMT+13:00) Tokelau Is.' );
    
    this.selectelement = $( '#profile-timezone-input' );
    this.lockelement = $( '#profile-timezone-lock' );
    this.showselects( elem, values, selectedval );
  },
  on_timezone_blur: function( elem )
  {
    var selectlist_element = $( '#selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( '' );
  }
};

var profile_headbar = {
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

// #/~profile
sammy.get
(
  /^#\/(~profile)$/,
  //new RegExp( '(~profile)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    profile_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/profile.html',
      function( template )
      {
        body_element
          .html( template );

        //listprofile.id_param = id_param;
        listprofile.showlist(false);

        statusbar.show();
      }
    );
  }
);
