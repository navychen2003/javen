
var contact_tags = {
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
    $( '#contact-tags-autogen' )
      .attr( 'onClick', 'javascript:contact_tags.onfocus();return false;' );
    
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
      
      var clickto = 'javascript:contact_tags.remove_value(' + key + ');return false;';
      var item = '<li class="select2-search-choice"><div>' + value.esc() + '</div><a onclick="' + clickto + '" class="select2-search-choice-close" tabindex="-1"></a></li>';
      
      valueContent.push( item );
    }
    
    var keydown = 'javascript:return contact_tags.input_keydown(this,event);';
    var keyup = 'javascript:contact_tags.input_keyup(this,event);';
    var focus = 'javascript:contact_tags.input_focus(this,event);';
    var blur = 'javascript:contact_tags.input_blur(this,event);';
    
    var item = '<li class="select2-search-field"><input id="contact-tags-selectinput" type="text" autocomplete="off" class="select2-input" style="width: 10px;" onKeyDown="' + keydown + '" onKeyUp="' + keyup + '" onFocus="' + focus + '" onBlur="' + blur + '"></li>';
    
    valueContent.push( item );
    
    $( '#contact-tags-list' ).html( valueContent.join( '\n' ) );
    $( '#contact-tags-input' ).attr( 'value', valueStr.esc() );
    
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
    $( '#contact-tags-autogen' )
      .addClass( 'select2-container-active' );
    $( '#contact-tags-selectinput' )
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
    
    $( '#contact-tags-selectinput' ).attr( 'style', 'width: ' + width + 'px;' );
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
    
    $( '#contact-tags-autogen' )
      .removeClass( 'select2-container-active' );
    $( '#contact-tags-selectinput' )
      .removeClass( 'select2-focused' );
  }
};

var contact_dialogs = { 
  delete_confirm_dialog: null,
  
  init_message: function( dialog_element, template ) 
  {
    contact_dialogs.delete_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Contact' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to remove this contact?' );
        if (msg == null) msg = "";
        
        $( '#message-text' )
          .html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:contactform.delete_submit();return false;' )
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

var contactform = {
  contactkey: null,
  successcb: null,
  
  add_contact: function()
  {
    var context = system.context;
    context.redirect( '#/~contact/new' );
  },
  action_submit: function( contactkey, action )
  {
    if (contactkey == null || action == null) return;
    
    var params = '&action=' + encodeURIComponent(action) + 
                 '&key=' + encodeURIComponent(contactkey);
    
    $.ajax
    (
      {
        url : app.user_path + '/contact?token=' + app.token + params + '&wt=json',
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
            var cb = contactform.successcb;
            if (cb) cb.call(contactform);
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
  delete_contact: function( contactkey )
  {
    if (contactkey == null|| contactkey.length == 0)
      return;
    
    this.contactkey = contactkey;
    dialog.show( contact_dialogs.delete_confirm_dialog );
  },
  delete_submit: function()
  {
    dialog.hide();
    
    var contactkey = this.contactkey;
    if (contactkey== null|| contactkey.length == 0)
      return;
    
    this.action_submit( contactkey, 'delete' );
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var listcontact = {
  showlist: function()
  {
    contactform.successcb = null;
    navbar.init_metitle( null, '#/~profile' );
    
    var params = '&action=list';
    
    $.ajax
    (
      {
        url : app.user_path + '/contact?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var contacts = response['contacts'];
          listcontact.init_content( contacts );
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
  init_content: function( contacts )
  {
    if (contacts == null) contacts = {};
    
    var contactCount = 0;
    var contactContent = [];
    
    for (var gkey in contacts) {
      var group = contacts[gkey];
      if (group == null) continue;
      
      for (var fkey in group) {
        var contact = group[fkey];
        if (contact == null) continue;
        
        var key = contact['key'];
        var nickname = contact['nickname'];
        var firstname = contact['firstname'];
        var lastname = contact['lastname'];
        var sex = contact['sex'];
        var region = contact['region'];
        var title = contact['title'];
        var brief = contact['brief'];
        var avatar = contact['avatar'];
        var status = contact['status'];
        
        if (key == null) key = '';
        if (nickname == null) nickname = '';
        if (firstname == null) firstname = '';
        if (lastname == null) lastname = '';
        if (sex == null) sex = '';
        if (region == null) region = '';
        if (title == null) title = '';
        if (brief == null) brief = '';
        if (status == null) status = '';
        
        var name = nickname;
        if (name == null || name.length == 0) {
          name = firstname;
          if (lastname != null && lastname.length > 0) {
            if (name.length > 0) name += ' ';
            name += lastname;
          }
        }
        if (title == null || title.length == 0) 
          title = name;
        if (status == null || status.length == 0)
          status = brief;
        
        var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
        var thumbClick = 'javascript:listcontact.showcontact(\'' + key + '\');return false;';
        var deleteClick = 'javascript:listcontact.deletecontact(\'' + key + '\');return false;';
        
        if (avatar != null && avatar.length > 0) {
          var id = avatar;
          var extension = 'jpg';
      
          var src = app.base_path + '/image/' + id + '_64t.' + extension + '?token=' + app.token;
          thumbsrc = src;
        }
        
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions">' + "\n" +
                   '        <button type="button" class="delete-btn btn btn-danger btn-icon" title="' + strings('Remove') + '" onClick="' + deleteClick + '"><i class="glyphicon ban"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3>' + title.esc() + '</h3>' + "\n" +
                   '    <h4>' + status.esc() + '</h4>' + "\n" +
                   '</li>';
        
        contactContent.push( item );
        contactCount ++;
      }
    }
    
    var contactTitle = strings( 'Contacts' ) + ' <span class="well-header-count">' + contactCount + '</span>';
    
    $( '#contact-list-title' ).html( contactTitle );
    $( '#contact-list' ).html( contactContent.join( "\n" ) );
    
    if (contactContent.length == 0) {
      $( '#contact-empty' )
        .html( strings( 'No contacts :(' ) )
        .removeClass( 'hide' );
    } else {
      $( '#contact-empty' )
        .addClass( 'hide' );
    }
  },
  showcontact: function( key )
  {
    if (key == null || key.length == 0)
      return;
    
    var context = system.context;
    context.redirect( '#/~contact/' + encodeURIComponent(key) );
  },
  deletecontact: function( key )
  {
    if (key == null || key.length == 0)
      return;
    
    contactform.delete_contact( key );
  }
};

var contactdetails = {
  contactkey: null,
  showcb: null,
  selectlist: null,
  selectelement: null,
  lockelement: null,
  empty_avatar: null,
  empty_background: null,
  slidephotos: [],
  
  edit: function( contactkey )
  {
    contactdetails.show0( contactkey, true );
  },
  show: function( contactkey )
  {
    contactdetails.show0( contactkey, false );
  },
  show0: function( contactkey, editmode )
  {
    if (contactkey == null || contactkey.length == 0)
      contactkey = 'new';
    
    contactform.successcb = null;
    navbar.init_name( strings( 'Contacts' ), null, '#/~contacts' );
    
    var editlink_element = $( '#action-edit-link' );
    
    if (editlink_element) {
      editlink_element
        .attr( 'title', strings('Edit') )
        .attr( 'onClick', 'javascript:contactdetails.edit(\'' + contactkey + '\');return false;' )
        .attr( 'href', '' );
    }
    
    this.contactkey = contactkey;
    this.slidephotos = [];
    this.init_form( contactkey );
    
    if (contactkey == 'new') {
      this.init_values( contactkey, {} );
      return;
    }
    
    var params = '&action=info&key=' + encodeURIComponent(contactkey);
    
    $.ajax
    (
      {
        url : app.user_path + '/contact?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var key = response['key'];
          var profile = response['contact'];
          var avatarSection = response['avatar'];
          var backgroundSection = response['background'];
          if (editmode == true)
            contactdetails.init_values( key, profile, avatarSection, backgroundSection );
          else
            contactdetails.init_details( key, profile, avatarSection, backgroundSection );
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
    var contactkey = this.contactkey;
    if (contactkey == null || contactkey.length == 0)
      return;
    
    photoslide.show( contactdetails.slidephotos, 0, false );
  },
  changeavatar: function()
  {
    var contactkey = this.contactkey;
    if (contactkey == null || contactkey.length == 0)
      return;
    
    var emptysrc = this.empty_avatar;
    
    artwork.showselect0( contactkey, 'Public Files', 
      function( section ) {
        if (section) {
          var id = section['id'];
          if (id == null || id.length <= 0) id = 'null';
          contactdetails.save_artwork(id, null);
        }
      }, emptysrc);
  },
  changebackground: function()
  {
    var contactkey = this.contactkey;
    if (contactkey == null || contactkey.length == 0)
      return;
    
    var emptysrc = this.empty_background;
    
    artwork.showselect0( contactkey, 'Public Files', 
      function( section ) {
        if (section) {
          var id = section['id'];
          if (id == null || id.length <= 0) id = 'null';
          contactdetails.save_artwork(null, id);
        }
      }, emptysrc);
  },
  save_artwork: function(avatar_id, background_id)
  {
    if (avatar_id == null && background_id == null)
      return;
    
    var contactkey = this.contactkey;
    if (contactkey == null || contactkey.length == 0)
      return;
    
    var avatar = avatar_id;
    var background = background_id;
    
    if (avatar == null) avatar = '';
    if (background == null) background = '';
    
    var params = '&action=update&key=' + encodeURIComponent(contactkey) 
               + '&avatar=' + encodeURIComponent(avatar) 
               + '&background=' + encodeURIComponent(background);
    
    $.ajax
    (
      {
        url : app.user_path + '/contact?token=' + app.token + params + '&wt=json',
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
              if (contactkey == 'new') {
                var context = system.context;
                context.redirect( '#/~contact/' + encodeURIComponent(key) );
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
  init_form: function( contactkey )
  {
    if (contactkey == null || contactkey.length == 0)
      return;
    
    var form_element = $( '#contact-form' );
    var submitbutton_element = $( '#contact-submit-button' );
    var cancelbutton_element = $( '#contact-cancel-button' );
    
    var avatar_element = $( '#contact-avatar' );
    var avatarlink_element = $( '#contact-avatar-link' );
    var changeavatar_element = $( '#contact-change-avatar' );
    var changebackground_element = $( '#contact-change-background' );
    
    var nicknameinput_element = $( '#contact-nickname-input' );
    var firstnameinput_element = $( '#contact-firstname-input' );
    var lastnameinput_element = $( '#contact-lastname-input' );
    var sexinput_element = $( '#contact-sex-input' );
    var birthdayinput_element = $( '#contact-birthday-input' );
    var titleinput_element = $( '#contact-title-input' );
    var regioninput_element = $( '#contact-region-input' );
    var tagsinput_element = $( '#contact-tags-input' );
    var briefinput_element = $( '#contact-brief-input' );
    var introinput_element = $( '#contact-intro-input' );
    
    $( '#contact-change-avatar-text' ).html( strings('Change Avatar') );
    $( '#contact-change-background-text' ).html( strings('Change Background') );
    
    $( '#contact-metadata-region-name' ).html( strings('Region') );
    $( '#contact-metadata-birthday-name' ).html( strings('Birthday') );
    $( '#contact-metadata-title-name' ).html( strings('Title') );
    $( '#contact-metadata-tags-name' ).html( strings('Tags') );
    
    $( '#contact-nickname-text' ).html( strings('Nick Name') );
    $( '#contact-firstname-text' ).html( strings('First Name') );
    $( '#contact-lastname-text' ).html( strings('Last Name') );
    $( '#contact-sex-text' ).html( strings('Sex') );
    $( '#contact-birthday-text' ).html( strings('Birthday') );
    $( '#contact-title-text' ).html( strings('Title') );
    $( '#contact-region-text' ).html( strings('Region') );
    $( '#contact-tags-text' ).html( strings('Tags') );
    $( '#contact-brief-text' ).html( strings('Brief') );
    $( '#contact-intro-text' ).html( strings('Introduction') );
    
    $( '#contact-submit-button' ).html( strings('Save') );
    $( '#contact-cancel-button' ).html( strings('Cancel') );
    
    avatarlink_element
      .attr( 'onclick', 'javascript:contactdetails.click_avatar();return false;' )
      .attr( 'href', '' );
    
    changeavatar_element
      .attr( 'onClick', 'javascript:contactdetails.changeavatar();return false;' );
    
    changebackground_element
      .attr( 'onClick', 'javascript:contactdetails.changebackground();return false;' );
    
    nicknameinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-nickname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-nickname-lock\', true);' )
      .attr( 'value', '' );
    
    firstnameinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-firstname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-firstname-lock\', true);' )
      .attr( 'value', '' );
    
    lastnameinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-lastname-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-lastname-lock\', true);' )
      .attr( 'value', '' );
    
    sexinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-sex-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-sex-lock\', true);' )
      .attr( 'onFocus', 'javascript:contactdetails.on_sex_focus(this);' )
      .attr( 'onBlur', 'javascript:contactdetails.on_sex_blur(this);' )
      .attr( 'value', '' );
    
    birthdayinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-birthday-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-birthday-lock\', true);' )
      .attr( 'value', '' ).datepicker();
    
    titleinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-title-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-title-lock\', true);' )
      .attr( 'value', '' );
    
    regioninput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-region-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-region-lock\', true);' )
      .attr( 'value', '' );
    
    tagsinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-tags-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-tags-lock\', true);' )
      .attr( 'value', '' );
    
    briefinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-brief-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-brief-lock\', true);' )
      .attr( 'value', '' );
    
    introinput_element
      .attr( 'onChange', 'javascript:contactdetails.lockchanged(\'#contact-intro-lock\', true);' )
      .attr( 'onKeyDown', 'javascript:contactdetails.lockchanged(\'#contact-intro-lock\', true);' )
      .attr( 'value', '' );
    
    this.init_lockelement( '#contact-nickname-lock' );
    this.init_lockelement( '#contact-firstname-lock' );
    this.init_lockelement( '#contact-lastname-lock' );
    this.init_lockelement( '#contact-sex-lock' );
    this.init_lockelement( '#contact-birthday-lock' );
    this.init_lockelement( '#contact-title-lock' );
    this.init_lockelement( '#contact-region-lock' );
    this.init_lockelement( '#contact-tags-lock' );
    this.init_lockelement( '#contact-brief-lock' );
    this.init_lockelement( '#contact-intro-lock' );
    
    contactdetails.empty_avatar = 'css/' + app.theme + '/images/posters/friend.png';
    contactdetails.empty_background = 'css/' + app.theme + '/images/background.png';
    
    var params = '&action=update&key=' + encodeURIComponent(contactkey);
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/contact?token=' + app.token + params + '&wt=json',
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
                if (contactkey == 'new') {
                  var context = system.context;
                  context.redirect( '#/~contact/' + encodeURIComponent(key) );
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
    //  .attr( 'onClick', 'javascript:contactdetails.save_submit();return false;' );
    
    cancelbutton_element
      .attr( 'onClick', 'javascript:contactdetails.save_cancel();return false;' );
    
  },
  init_values: function( contactkey, profile, avatarSection, backgroundSection )
  {
    $( '#contact-metadata' ).addClass( 'hide' );
    $( '#contact-form' ).removeClass( 'hide' );
    $( '#contact-change-avatar' ).removeClass( 'hide' );
    $( '#contact-change-background' ).removeClass( 'hide' );
    
    if (contactkey == null || profile == null) return;
    
    var nickname = this.get_value(profile, 'nickname');
    var firstname = this.get_value(profile, 'firstname');
    var lastname = this.get_value(profile, 'lastname');
    var sex = this.get_value(profile, 'sex');
    var birthday = this.get_value(profile, 'birthday');
    var title = this.get_value(profile, 'title');
    var region = this.get_value(profile, 'region');
    var tags = this.get_value(profile, 'tags');
    var brief = this.get_value(profile, 'brief');
    var intro = this.get_value(profile, 'intro');
    var avatar = this.get_value(profile, 'avatar');
    var background = this.get_value(profile, 'background');
    
    $( '#contact-nickname-input' ).attr( 'value', nickname );
    $( '#contact-firstname-input' ).attr( 'value', firstname );
    $( '#contact-lastname-input' ).attr( 'value', lastname );
    $( '#contact-sex-input' ).attr( 'value', sex );
    $( '#contact-birthday-input' ).attr( 'value', birthday );
    $( '#contact-title-input' ).attr( 'value', title );
    $( '#contact-region-input' ).attr( 'value', region );
    //$( '#contact-tags-input' ).attr( 'value', tags );
    $( '#contact-brief-input' ).attr( 'value', brief );
    $( '#contact-intro-input' ).attr( 'value', intro );
    
    contact_tags.init_value( tags );
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    if (avatarSection && avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
      
      contactdetails.slidephotos = [];
      contactdetails.slidephotos.push( avatarSection );
    }
    
    $( '#contact-avatar' ).attr( 'src', thumbsrc);
    
    if (background != null && background.length > 0) {
      var id = background;
      var extension = 'jpg';
      
      var src = app.base_path + '/image/' + id + '.' + extension + '?token=' + app.token;
      $( '#background-image' )
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
    
    var cb = contactdetails.showcb;
    if (cb) cb.call(this, contactkey, profile);
  },
  init_details: function( contactkey, profile, avatarSection, backgroundSection )
  {
    $( '#contact-metadata' ).removeClass( 'hide' );
    $( '#contact-form' ).addClass( 'hide' );
    $( '#contact-change-avatar' ).addClass( 'hide' );
    $( '#contact-change-background' ).addClass( 'hide' );
    
    $( '#contact-metadata-region-name' ).html( strings('Region') );
    $( '#contact-metadata-birthday-name' ).html( strings('Birthday') );
    $( '#contact-metadata-cast-name' ).html( strings('Title') );
    $( '#contact-metadata-tags-name' ).html( strings('Tags') );
    
    if (contactkey == null || profile == null) return;
    
    var nickname = this.get_value(profile, 'nickname');
    var firstname = this.get_value(profile, 'firstname');
    var lastname = this.get_value(profile, 'lastname');
    var sex = this.get_value(profile, 'sex');
    var birthday = this.get_value(profile, 'birthday');
    var title = this.get_value(profile, 'title');
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
    var title2 = '';
    var subtitle = brief;
    var summary = intro;
    
    var realname = firstname;
    if (lastname != null && lastname.length > 0) {
      if (realname.length > 0) realname += ' ';
      realname += lastname;
    }
    
    var name = title; 
    if (name == null || name.length == 0) name = nickname;
    if (name == null || name.length == 0) name = realname;
    if (name == null || name.length == 0) {
      name = title;
      if (name == null || name.length == 0) 
        name = strings( '[Untitled]' );
    }
    
    $( '#contact-metadata-year' ).html( year );
    $( '#contact-metadata-title' ).html( name );
    $( '#contact-metadata-title2' ).html( title2 );
    $( '#contact-metadata-name' ).html( realname );
    $( '#contact-metadata-subtitle' ).html( subtitle );
    $( '#contact-metadata-region' ).html( region );
    $( '#contact-metadata-birthday' ).html( birthday );
    $( '#contact-metadata-cast' ).html( title );
    $( '#contact-metadata-tags' ).html( tags );
    $( '#contact-metadata-summary' ).html( summary );
    
    if (sex == 'male') {
      $( '#contact-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon male"></i>' );
    } else if (sex == 'female') {
      $( '#contact-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon female"></i>' );
    }
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    
    if (avatarSection && avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
      
      contactdetails.slidephotos = [];
      contactdetails.slidephotos.push( avatarSection );
    }
    
    $( '#contact-avatar' ).attr( 'src', thumbsrc);
    
    if (background != null && background.length > 0) {
      var id = background;
      var extension = 'jpg';
      
      var src = app.base_path + '/image/' + id + '.' + extension + '?token=' + app.token;
      $( '#background-image' )
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
    
    var cb = contactdetails.showcb;
    if (cb) cb.call(this, contactkey, profile);
  },
  init_lockelement: function( idname )
  {
    if (idname == null) return;
    var lockelement = $( idname );
    if (lockelement && idname) {
      this.lockchanged( idname, false );
      lockelement
        .attr( 'onClick', 'javascript:contactdetails.changelock(\'' + idname + '\');return false;' );
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
    
    var scrollelems = $( '#contact-scroll' );
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
        '<li id="selectlist-item-' + index + '" onMouseOver="javascript:contactdetails.on_select_focus(' + index + ');" onMouseOut="javascript:contactdetails.on_select_out(' + index + ');" onMouseDown="javascript:contactdetails.on_select_click(' + index + ');" onClick="javascript:return false;" class="select2-results-dept-0 select2-result select2-result-selectable ' + highlight + '">' + "\n" +
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
    
    this.selectelement = $( '#contact-sex-input' );
    this.lockelement = $( '#contact-sex-lock' );
    this.showselects( elem, values, selectedval );
  },
  on_sex_blur: function( elem )
  {
    var selectlist_element = $( '#selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( '' );
  },
  save_submit: function()
  {
    var form_element = $( '#contact-form' );
    
    form_element.submit();
  },
  save_cancel: function()
  {
    contactdetails.show( contactdetails.contactkey );
  }
};

var contact_headbar = {
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

// #/~contacts
sammy.get
(
  /^#\/(~contacts)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    contact_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/contacts.html',
      function( template )
      {
        body_element
          .html( template );

        $( '#contact-add-submit-text' ).html( strings( 'Add Contact' ) );
        
        $( '#contact-list-contacts' )
          .attr( 'href', '#/~contacts' )
          .html( strings( 'Contacts' ) );
        
        $( '#contact-add-submit' )
          .attr( 'onClick', 'javascript:contactform.add_contact();return false;' )
          .attr( 'title', strings( 'Add Contact' ) );

        listcontact.showlist();

        statusbar.show();
      }
    );
  }
);

// #/~contact
sammy.get
(
  // /^#\/(~contact)$/,
  new RegExp( '(~contact)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(11);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }

    contact_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/contact.html',
      function( template )
      {
        body_element
          .html( template );

        contactdetails.show( id_param );

        statusbar.show();
      }
    );
  }
);
