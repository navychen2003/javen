
var userlockform = {
  lock_confirm_dialog: null,
  user: null,
  
  init_message: function( dialog_element, template ) 
  {
    userlockform.lock_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        var user = userlockform.user;
        if (user == null) user = {};
        
        var key = user['key'];
        var flag = user['flag'];
        var name = user['name'];
        var title = user['title'];
        var type = user['type'];
        
        var userFlag = strings('Enabled');
        var actionTitle = strings('Lock');
        var iconClass = 'glyphicon lock';
        
        var actionokHtml = strings('Lock');
        var actionokClick = 'javascript:userlockform.readonly_submit();return false;';
        var actionokClass = 'btn-danger';
        
        var actionokHtml2 = strings('Disable');
        var actionokClick2 = 'javascript:userlockform.disable_submit();return false;';
        var actionokClass2 = 'btn-danger';
        
        if (flag == 'disabled' || flag == 'readonly') {
          actionTitle = strings('Unlock');
          iconClass = 'glyphicon unlock';
          
          if (flag == 'disabled') {
            userFlag = strings('Disabled');
            
            actionokHtml = strings('Lock');
            actionokClick = 'javascript:userlockform.readonly_submit();return false;';
            actionokClass = 'btn-danger';
            
            actionokHtml2 = strings('Enable');
            actionokClick2 = 'javascript:userlockform.enable_submit();return false;';
            actionokClass2 = 'btn-success';
            
          } else {
            userFlag = strings('Readonly');
            
            actionokHtml = strings('Disable');
            actionokClick = 'javascript:userlockform.disable_submit();return false;';
            actionokClass = 'btn-danger';
            
            actionokHtml2 = strings('Enable');
            actionokClick2 = 'javascript:userlockform.enable_submit();return false;';
            actionokClass2 = 'btn-success';
          }
        }
        
        if (title == null) title = '';
        if (name == null) name = '';
        if (name != null && name.length > 0) {
          if (title != null && title.length > 0)
            title = title + '(' + name + ')';
          else
            title = name;
        }
        
        if (type == 'group') title = strings('Group') + ': ' + title;
        else title = strings('User') + ': ' + title;
        
        $( '#message-title' ).html( strings( actionTitle ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', iconClass );
        
        var msg = strings( '{0} status is {1}, please select operation.' );
        if (msg == null) msg = "";
        msg = msg.format( title, userFlag );
        
        $( '#message-text' ).html( msg.esc() );
        
        $( '#message-ok2' )
          .attr( 'onclick', actionokClick2 )
          .addClass( actionokClass2 ).removeClass( 'hide' )
          .html( actionokHtml2 );
        
        $( '#message-ok' )
          .attr( 'onclick', actionokClick )
          .addClass( actionokClass ).removeClass( 'hide' )
          .html( actionokHtml );
        
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
  },
  build_action: function( username, flag )
  {
    if (username == null || username.length == 0)
      return null;
    
    var isadmin = globalApp.is_admin();
    var me = globalApp.get_username();
    if (me == username || isadmin !== true)
      return null;
    
    var actionTitle = strings('Lock');
    var iconClass = 'glyphicon lock';
    
    if (flag == 'disabled' || flag == 'readonly') {
      actionTitle = strings('Unlock');
      iconClass = 'glyphicon unlock';
    }
    
    var updateClick = 'javascript:userlockform.updateflag_click(\'' + name + '\');return false;';
    var item = '        <button type="button" class="update-btn btn btn-icon" rel="tooltip" title="' + actionTitle + '" onClick="' + updateClick + '"><i class="' + iconClass + '"></i></button>' + "\n";
    
    return item;
  },
  updateflag: function( user )
  {
    if (user == null) return;
    userlockform.user = user;
    dialog.show( userlockform.lock_confirm_dialog );
  },
  updateflag_click: function( username ) 
  {
    if (username == null || username.length == 0)
      return;
  },
  updateflag_submit: function( username, flag, successcb )
  {
    if (username == null || flag == null) return;
    
    var action = 'updateflag';
    var params = '&action=' + encodeURIComponent(action) + 
                 '&username=' + encodeURIComponent(username) +
                 '&flag=' + encodeURIComponent(flag);
    
    $.ajax
    (
      {
        url : app.user_path + '/login?token=' + app.token + params + '&wt=json',
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
            var cb = successcb;
            if (cb) cb.call(userlockform, response['user']);
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
  show_resultmessage: function( user )
  {
    var olduser = userlockform.user;
    if (olduser == null) olduser = {};
    
    var oldkey = olduser['key'];
    var oldflag = olduser['flag'];
    var oldname = olduser['name'];
    var oldtype = olduser['type'];
    
    if (user == null) user = {};
    userlockform.user = user;
    
    var key = user['key'];
    var flag = user['flag'];
    var name = user['name'];
    var type = user['type'];
    var title = user['title'];
    
    if (key == null || key != oldkey || name != oldname || type != oldtype) {
      var message = strings( 'Response user is wrong' );
      userlockform.showerror( message );
      return;
    }
    
    var userFlag = strings('Enabled');
    if (flag == 'disabled') {
      userFlag = strings('Disabled');
    } else if (flag == 'readonly') {
      userFlag = strings('Readonly');
    }
    
    if (title == null) title = '';
    if (name == null) name = '';
    if (name != null && name.length > 0) {
      if (title != null && title.length > 0)
        title = title + '(' + name + ')';
      else
        title = name;
    }
    
    if (type == 'group') title = strings('Group') + ': ' + title;
    else title = strings('User') + ': ' + title;
    
    if (flag == oldflag) {
      var message = strings( '{0} status is {1}, not changed.' );
      if (message == null) message = '';
      message = message.format( title, userFlag );
      userlockform.showerror( message );
      return;
    }
    
    var olduserFlag = strings('Enabled');
    if (oldflag == 'disabled') {
      olduserFlag = strings('Disabled');
    } else if (oldflag == 'readonly') {
      olduserFlag = strings('Readonly');
    }
    
    var message = strings( '{0} status changed from {1} to {2}.' );
    if (message == null) message = '';
    message = message.format( title, olduserFlag, userFlag );
    userlockform.showerror( message );
    
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  },
  action_submit: function( newflag )
  {
    dialog.hide();
    
    var user = userlockform.user;
    if (user == null) user = {};
        
    var key = user['key'];
    var flag = user['flag'];
    var name = user['name'];
    
    if (name == null || name.length == 0)
      return;
    
    if (newflag == null || newflag.length == 0 || newflag == flag)
      return;
    
    userlockform.updateflag_submit( name, newflag, 
      userlockform.show_resultmessage);
  },
  enable_submit: function()
  {
    userlockform.action_submit( 'enabled' );
  },
  disable_submit: function()
  {
    userlockform.action_submit( 'disabled' );
  },
  readonly_submit: function()
  {
    userlockform.action_submit( 'readonly' );
  }
};

var userinfo = {
  userdetails_dialog: null,
  userdetails_showcb: null,
  details_profile: null,
  details_user: null,
  details_group: null,
  username: null,
  
  showdetails0: function( username, showcb )
  {
    userinfo.username = null;
    if (username == null) return;
    userinfo.username = username;
    userinfo.userdetails_showcb = showcb;
    
    var params = '&action=info&username=' + encodeURIComponent(username);
    
    $.ajax
    (
      {
        url : app.user_path + '/userinfo?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : $( '#userinfo-text' ),
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var profile = response['profile'];
          var user = response['user'];
          var group = response['group'];
          
          if (group) {
            if (userinfo.userdetails_showcb == userinfo.showcb)
              userinfo.userdetails_showcb = groupinfo.showcb;
            else if (userinfo.userdetails_showcb == userinfo.showcbonly)
              userinfo.userdetails_showcb = groupinfo.showcbonly;
          } else {
            if (userinfo.userdetails_showcb == groupinfo.showcb)
              userinfo.userdetails_showcb = userinfo.showcb;
            else if (userinfo.userdetails_showcb == groupinfo.showcbonly)
              userinfo.userdetails_showcb = userinfo.showcbonly;
          }
          
          userinfo.details_profile = profile;
          userinfo.details_user = user;
          userinfo.details_group = group;
          dialog.show( userinfo.userdetails_dialog );
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
    var profile = userinfo.details_profile;
    var user = userinfo.details_user;
    var group = userinfo.details_group;
    if (profile == null) return;
    
    var avatar = userinfo.get_value(profile, 'avatar');
    
    if (avatar != null && avatar.length > 0) {
      fileinfo.showdetailsid( avatar );
    }
  },
  init_details: function()
  {
    $( '#userinfo-metadata-region-name' ).html( strings('Region') );
    $( '#userinfo-metadata-birthday-name' ).html( strings('Birthday') );
    $( '#userinfo-metadata-timezone-name' ).html( strings('Timezone') );
    $( '#userinfo-metadata-tags-name' ).html( strings('Tags') );
    
    var profile = userinfo.details_profile;
    var user = userinfo.details_user;
    var group = userinfo.details_group;
    if (profile == null) return;
    
    var nickname = userinfo.get_value(profile, 'nickname');
    var firstname = userinfo.get_value(profile, 'firstname');
    var lastname = userinfo.get_value(profile, 'lastname');
    var sex = userinfo.get_value(profile, 'sex');
    var birthday = userinfo.get_value(profile, 'birthday');
    var timezone = userinfo.get_value(profile, 'timezone');
    var region = userinfo.get_value(profile, 'region');
    var tags = userinfo.get_value(profile, 'tags');
    var brief = userinfo.get_value(profile, 'brief');
    var intro = userinfo.get_value(profile, 'intro');
    var avatar = userinfo.get_value(profile, 'avatar');
    var background = userinfo.get_value(profile, 'background');
    
    if (nickname == null) nickname = '';
    if (firstname == null) firstname = '';
    if (lastname == null) lastname = '';
    
    var mtime = profile['mtime'];
    var date = mtime > 0 ? new Date(mtime) : new Date();
    
    var year = '';// + (1900+date.getYear());
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
      var username = userinfo.username;
      if (username == null || username.length == 0) 
        username = globalApp.get_username();
      title = username;
      if (title == null || title.length == 0) 
        title = strings( '[Untitled]' );
    }
    
    var flag = null;
    if (group) {
      flag = group['flag'];
      
      var memberCount = group['mcount'];
      if (memberCount == null || memberCount < 0)
        memberCount = 0;
      
      name = (name.trim() + ' ' + memberCount).trim();
      
    } else if (user) {
      flag = user['flag'];
    }
    
    if (flag == 'disabled' || flag == 'readonly') {
      $( '#userinfo-lock' ).html( strings( 'Unlock' ) );
    }
    
    $( '#userinfo-rating' ).addClass( 'hide' );
    
    $( '#userinfo-metadata-year' ).html( year );
    $( '#userinfo-metadata-title' ).html( title );
    $( '#userinfo-metadata-title2' ).html( title2 );
    $( '#userinfo-metadata-name' ).html( name );
    $( '#userinfo-metadata-subtitle' ).html( subtitle );
    $( '#userinfo-metadata-region' ).html( region );
    $( '#userinfo-metadata-birthday' ).html( birthday );
    $( '#userinfo-metadata-timezone' ).html( timezone );
    $( '#userinfo-metadata-tags' ).html( tags );
    $( '#userinfo-metadata-summary' ).html( summary );
    
    if (region != null && region.length > 0) $( '#userinfo-metadata-region-item' ).removeClass( 'hide' );
    if (birthday != null && birthday.length > 0) $( '#userinfo-metadata-birthday-item' ).removeClass( 'hide' );
    if (timezone != null && timezone.length > 0) $( '#userinfo-metadata-timezone-item' ).removeClass( 'hide' );
    if (tags != null && tags.tags > 0) $( '#userinfo-metadata-tags-item' ).removeClass( 'hide' );
    
    if (sex == 'male') {
      $( '#userinfo-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon male"></i>' );
    } else if (sex == 'female') {
      $( '#userinfo-metadata-nameicon' )
        .attr( 'class', 'unwatched-icon' )
        .attr( 'style', 'background-color: #000000;' )
        .html( '<i class="glyphicon female"></i>' );
    }
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    if (avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_256t.' + extension + '?token=' + app.token;
    }
    
    $( '#userinfo-avatar' ).attr( 'src', thumbsrc);
    
    $( '#userinfo-avatar-link' )
      .attr( 'onclick', 'javascript:userinfo.click_avatar();return false;' )
      .attr( 'href', '' );
    
    if (background != null && background.length > 0) {
      var id = background;
      var extension = 'jpg';
      
      var src = app.base_path + '/image/' + id + '.' + extension + '?token=' + app.token;
      $( '#userinfo-background-image' )
        .attr( 'style', 'background-image: url(\'' + src + '\');' );
    }
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/userinfo.html',
      function( template )
      {
        userinfo.userdetails_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#userinfo-title' ).html( strings( 'User Information' ) );
            $( '#userinfo-lock' ).html( strings( 'Lock' ) );
            $( '#userinfo-send' ).html( strings( 'Conversation' ) );
            $( '#userinfo-invite' ).html( strings( 'Send Invite' ) );
            $( '#userinfo-ok' ).html( strings( 'Open' ) );
            $( '#userinfo-no' ).html( strings( 'Close' ) );
            
            $( '#userinfo-details-show' ).html( strings( 'Show More Actions' ) );
            $( '#userinfo-details-hide' ).html( strings( 'Hide More Actions' ) );
            
            userinfo.init_details();
            
            $( '#userinfo-details-show' )
              .attr( 'onclick', 'javascript:userinfo.showactions();return false;' )
              .attr( 'href', '' );
            
            $( '#userinfo-details-hide' )
              .attr( 'onclick', 'javascript:userinfo.hideactions();return false;' )
              .attr( 'href', '' );
            
            $( '#userinfo-lock' )
              .attr( 'onclick', 'javascript:userinfo.updateflag();return false;' );
            
            $( '#userinfo-send' )
              .attr( 'onclick', 'javascript:userinfo.send();return false;' );
            
            $( '#userinfo-invite' )
              .attr( 'onclick', 'javascript:userinfo.invite();return false;' );
            
            $( '#userinfo-ok' )
              .attr( 'onclick', 'javascript:userinfo.open();return false;' );
            
            $( '#userinfo-no' )
              .attr( 'onclick', 'javascript:userinfo.close();return false;' );
            
            $( '#userinfo-close' )
              .attr( 'onclick', 'javascript:userinfo.close();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            var cb = userinfo.userdetails_showcb;
            if (cb) cb.call(userinfo);
          },
          hidecb: function()
          {
          },
          shown: false
        };
      }
    );
  },
  close: function()
  {
    dialog.hide();
  },
  showme: function()
  {
    userinfo.showdetails('');
  },
  showdetails: function( username )
  {
    userinfo.showdetails0( username, userinfo.showcb);
  },
  showonly: function( username )
  {
    userinfo.showdetails0( username, userinfo.showcbonly);
  },
  init_okaction: function()
  {
    var context = system.context;
    var username = userinfo.username;
    var me = globalApp.get_username();
    
    if (context.path == '#/~profile') {
      if (username == null || username.length == 0 || username == me) 
        $( '#userinfo-ok' ).addClass( 'hide' );
    } else if (username != null && username.length > 0) {
      var linkto = '#/~user/' + encodeURIComponent(username);
      if (context.path == linkto) 
        $( '#userinfo-ok' ).addClass( 'hide' );
    }
  },
  showactions: function()
  {
    $( '#userinfo-details-show' ).addClass( 'hide' );
    $( '#userinfo-details-hide' ).removeClass( 'hide' );
    
    $( '#userinfo-send' ).removeClass( 'hide' );
    $( '#userinfo-invite' ).removeClass( 'hide' );
    $( '#userinfo-ok' ).removeClass( 'hide' );
    $( '#userinfo-close' ).removeClass( 'hide' );
    
    userinfo.init_okaction();
    
    var username = userinfo.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0) 
      username = me;
    
    if (username == me) {
      $( '#userinfo-invite' ).addClass( 'hide' );
    }
    
    var user = userinfo.details_user;
    if (user) {
      var invite = user['invite'];
      if (invite == 'friend')
        $( '#userinfo-invite' ).addClass( 'hide' );
    }
    
    var isadmin = globalApp.is_admin();
    if (isadmin == true && username != me) {
      $( '#userinfo-lock' ).removeClass( 'hide' );
    }
  },
  hideactions: function()
  {
    $( '#userinfo-details-show' ).removeClass( 'hide' );
    $( '#userinfo-details-hide' ).addClass( 'hide' );
    
    $( '#userinfo-lock' ).addClass( 'hide' );
    $( '#userinfo-send' ).addClass( 'hide' );
    $( '#userinfo-invite' ).addClass( 'hide' );
    $( '#userinfo-ok' ).removeClass( 'hide' );
    $( '#userinfo-close' ).removeClass( 'hide' );
    
    userinfo.init_okaction();
  },
  showcb: function()
  {
    userinfo.hideactions();
    
    $( '#userinfo-send' ).html( strings( 'Chat' ) );
  },
  showcbonly: function()
  {
    userinfo.showcb();
    
    $( '#userinfo-details-show' ).addClass( 'hide' );
    $( '#userinfo-details-hide' ).addClass( 'hide' );
    
    $( '#userinfo-lock' ).addClass( 'hide' );
    $( '#userinfo-send' ).addClass( 'hide' );
    $( '#userinfo-invite' ).addClass( 'hide' );
    $( '#userinfo-ok' ).addClass( 'hide' );
    $( '#userinfo-close' ).removeClass( 'hide' );
  },
  open: function()
  {
    var username = userinfo.username;
    var me = globalApp.get_username();
    var context = system.context;
    
    if (username == null || username.length == 0 || username == me) {
      context.redirect( '#/~profile' );
      return;
    }
    
    context.redirect( '#/~user/' + encodeURIComponent(username) );
  },
  updateflag: function()
  {
    var username = userinfo.username;
    var me = globalApp.get_username();
    
    if (username != me) {
      var user = userinfo.details_user;
      var group = userinfo.details_group;
      
      if (userlockform && user) 
        userlockform.updateflag( user );
    }
  },
  send: function()
  {
    var username = userinfo.username;
    if (username == null || username.length == 0)
      username = globalApp.get_username();
    
    if (username != null && username.length > 0) {
      userinfo.close();
      //compose.show( 'mail', username );
      
      var context = system.context;
      context.redirect( '#/~chat/' + encodeURIComponent(username) );
    }
  },
  invite: function()
  {
    if (friendform) {
      userinfo.close();
      friendform.invite_friend( userinfo.username );
    }
  }
};

var groupinfo = {
  showdetails: function( groupname )
  {
    userinfo.showdetails0( groupname, groupinfo.showcb);
  },
  showonly: function( groupname )
  {
    userinfo.showdetails0( groupname, groupinfo.showcbonly);
  },
  init_okaction: function()
  {
    var context = system.context;
    var username = userinfo.username;
    
    if (username != null && username.length > 0) {
      var linkto = '#/~group/' + encodeURIComponent(username);
      if (context.path == linkto) 
        $( '#userinfo-ok' ).addClass( 'hide' );
    }
  },
  showactions: function()
  {
    $( '#userinfo-details-show' ).addClass( 'hide' );
    $( '#userinfo-details-hide' ).removeClass( 'hide' );
    
    $( '#userinfo-send' ).addClass( 'hide' );
    $( '#userinfo-invite' ).removeClass( 'hide' );
    $( '#userinfo-ok' ).removeClass( 'hide' );
    $( '#userinfo-close' ).removeClass( 'hide' );
    
    groupinfo.init_okaction();
    
    var group = userinfo.details_group;
    if (group) { 
      var role = group['role'];
      if (role != null && role.length > 0) {
        $( '#userinfo-send' ).removeClass( 'hide' );
        $( '#userinfo-invite' ).addClass( 'hide' );
      }
    }
    
    var username = userinfo.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0) 
      username = me;
    
    var isadmin = globalApp.is_admin();
    if (isadmin == true && username != me) {
      $( '#userinfo-lock' ).removeClass( 'hide' );
    }
  },
  hideactions: function()
  {
    $( '#userinfo-details-show' ).removeClass( 'hide' );
    $( '#userinfo-details-hide' ).addClass( 'hide' );
    
    $( '#userinfo-lock' ).addClass( 'hide' );
    $( '#userinfo-send' ).addClass( 'hide' );
    $( '#userinfo-invite' ).addClass( 'hide' );
    $( '#userinfo-ok' ).removeClass( 'hide' );
    $( '#userinfo-close' ).removeClass( 'hide' );
    
    groupinfo.init_okaction();
  },
  showcb: function()
  {
    $( '#userinfo-send' ).html( strings( 'Conversation' ) );
    $( '#userinfo-title' ).html( strings( 'Group Information' ) );
    $( '#userinfo-invite' ).html( strings( 'Join Group' ) );
    $( '#userinfo-icon' ).attr( 'class', 'glyphicon parents' );
    
    $( '#userinfo-metadata-nameicon' )
      .attr( 'class', 'unwatched-icon' )
      .attr( 'style', 'background-color: #000000;' )
      .html( '<i class="glyphicon parents"></i>' );
    
    $( '#userinfo-details-show' )
      .attr( 'onclick', 'javascript:groupinfo.showactions();return false;' )
      .attr( 'href', '' );
    
    $( '#userinfo-details-hide' )
      .attr( 'onclick', 'javascript:groupinfo.hideactions();return false;' )
      .attr( 'href', '' );
    
    $( '#userinfo-lock' )
      .attr( 'onclick', 'javascript:groupinfo.updateflag();return false;' );
    
    $( '#userinfo-send' )
      .attr( 'onclick', 'javascript:groupinfo.send();return false;' );
    
    $( '#userinfo-invite' )
      .attr( 'onclick', 'javascript:groupinfo.invite();return false;' );
    
    $( '#userinfo-ok' )
      .attr( 'onclick', 'javascript:groupinfo.open();return false;' );
    
    groupinfo.hideactions();
  },
  showcbonly: function()
  {
    groupinfo.showcb();
    
    $( '#userinfo-details-show' ).addClass( 'hide' );
    $( '#userinfo-details-hide' ).addClass( 'hide' );
    
    $( '#userinfo-lock' ).addClass( 'hide' );
    $( '#userinfo-send' ).addClass( 'hide' );
    $( '#userinfo-invite' ).addClass( 'hide' );
    $( '#userinfo-ok' ).addClass( 'hide' );
    $( '#userinfo-close' ).removeClass( 'hide' );
  },
  open: function()
  {
    var groupname = userinfo.username;
    var context = system.context;
    
    if (groupname == null || groupname.length == 0) {
      //context.redirect( '#/~profile' );
      return;
    }
    
    context.redirect( '#/~group/' + encodeURIComponent(groupname) );
  },
  updateflag: function()
  {
    var groupname = userinfo.username;
    var me = globalApp.get_username();
    
    if (groupname != me) {
      var user = userinfo.details_user;
      var group = userinfo.details_group;
      
      if (userlockform && group) 
        userlockform.updateflag( group );
    }
  },
  send: function()
  {
    var groupname = userinfo.username;
    if (groupname == null) return;
    var context = system.context;
    context.redirect( '#/~conversation/' + encodeURIComponent(groupname) );
  },
  invite: function()
  {
    var groupname = userinfo.username;
    var me = globalApp.get_username();
    
    if (memberform) {
      userinfo.close();
      memberform.join_group( groupname, me );
    }
  }
};
