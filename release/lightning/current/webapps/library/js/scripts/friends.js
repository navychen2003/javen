
var friend_dialogs = { 
  delete_confirm_dialog: null,
  
  init_message: function( dialog_element, template ) 
  {
    friend_dialogs.delete_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Friend' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to remove friend \"{0}\"?' );
        if (msg == null) msg = "";
        
        msg = msg.format( friendform.username );
        
        $( '#message-text' )
          .html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:friendform.delete_submit();return false;' )
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

var listfriend = {
  showlist: function()
  {
    friendform.successcb = null;
    navbar.init_metitle( null, '#/~profile' );
    
    var params = '&action=list';
    
    $.ajax
    (
      {
        url : app.user_path + '/friend?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var invites = response['invites'];
          var friends = response['friends'];
          
          listfriend.init_content( invites, friends );
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
  init_content: function( invites, friends )
  {
    if (invites == null) invites = {};
    if (friends == null) friends = {};
  
    var inviteCount = 0;
    var inviteContent = [];
    var requestContent = [];
  
    for (var key in invites) {
      var invite = invites[key];
      if (invite == null) continue;
    
      var name = invite['name'];
      var title = invite['title'];
      var type = invite['type'];
      var utype = invite['utype'];
      var flag = invite['flag'];
      var avatar = invite['avatar'];
      var message = invite['message'];
      var time = invite['time'];
    
      if (name == null) name = '';
      if (title == null || title.length == 0) title = name;
    
      var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
      var thumbClick = 'javascript:userinfo.showdetails(\'' + name + '\');return false;';
      var acceptClick = 'javascript:friendform.accept_invite(\'' + name + '\');return false;';
      var rejectClick = 'javascript:friendform.reject_invite(\'' + name + '\');return false;';
      var cancelClick = 'javascript:friendform.cancel_invite(\'' + name + '\');return false;';
    
      if (avatar != null && avatar.length > 0) {
        var id = avatar;
        var extension = 'jpg';
      
        var src = app.base_path + '/image/' + id + '_64t.' + extension + '?token=' + app.token;
        thumbsrc = src;
      }
      
      if (type == 'in') {
        if (message == null || message.length == 0)
          message = strings( 'wants to be your friend' );
        else
          message = strings( message );
        
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions">' + "\n" +
                   '        <button type="button" class="accept-btn btn btn-success btn-icon" rel="tooltip" title="' + strings('Accept') + '" onClick="' + acceptClick + '"><i class="glyphicon circle-ok"></i></button>' + "\n" +
                   '        <button type="button" class="reject-btn btn btn-danger btn-icon" rel="tooltip" title="' + strings('Reject') + '" onClick="' + rejectClick + '"><i class="glyphicon ban"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3>' + title.esc() + '</h3>' + "\n" +
                   '    <h4>' + message.esc() + '</h4>' + "\n" +
                   '</li>';
      
        inviteContent.push( item );
        inviteCount ++;
        
      } else {
        if (message == null || message.length == 0)
          message = strings( 'has not accepted yet' );
        else
          message = strings( message );
        
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions">' + "\n" +
                   '        <button type="button" class="cancel-btn btn btn btn-icon" title="' + strings('Cancel') + '" onClick="' + cancelClick + '"><i class="glyphicon circle-remove"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3>' + title.esc() + '</h3>' + "\n" +
                   '    <h4>' + message.esc() + '</h4>' + "\n" +
                   '</li>';
      
        requestContent.push( item );
        inviteCount ++;
      }
    }
    
    var friendCount = 0;
    var friendContent = [];
    
    for (var gkey in friends) {
      var group = friends[gkey];
      if (group == null) continue;
      
      for (var fkey in group) {
        var friend = group[fkey];
        if (friend == null) continue;
        
        var name = friend['name'];
        var flag = friend['flag'];
        var title = friend['title'];
        var status = friend['status'];
        var avatar = friend['avatar'];
        
        if (name == null) name = '';
        if (status == null) status = '';
        if (title == null || title.length == 0) title = name;
        
        var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
        var thumbClick = 'javascript:userinfo.showdetails(\'' + name + '\');return false;';
        var deleteClick = 'javascript:friendform.delete_friend(\'' + name + '\');return false;';
        
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
        
        friendContent.push( item );
        friendCount ++;
      }
    }
    
    var inviteTitle = strings( 'Invites' ) + ' <span class="well-header-count">' + inviteCount + '</span>';
    var friendTitle = strings( 'Friends' ) + ' <span class="well-header-count">' + friendCount + '</span>';
    
    $( '#friend-invite-title' ).html( inviteTitle );
    $( '#friend-list-title' ).html( friendTitle );
    
    $( '#friend-invitelist' ).html( inviteContent.join( "\n" ) );
    $( '#friend-requestlist' ).html( requestContent.join( "\n" ) );
    $( '#friend-list' ).html( friendContent.join( "\n" ) );
    
    if (inviteContent.length == 0) {
      $( '#received-requests-container' ).addClass( 'hide' );
      $( '#sent-requests-container' ).removeClass( 'span6' );
    }
    if (requestContent.length == 0) {
      $( '#received-requests-container' ).removeClass( 'span6' );
      $( '#sent-requests-container' ).addClass( 'hide' );
    }
    
    if (inviteCount > 0)
      $( '#requests-container' ).removeClass( 'hide' );
    else
      $( '#requests-container' ).addClass( 'hide' );
    
    if (friendContent.length == 0 && inviteCount == 0) {
      $( '#friend-empty' )
        .html( strings( 'No friends :(' ) )
        .removeClass( 'hide' );
    } else {
      $( '#friend-empty' )
        .addClass( 'hide' );
    }
  }
};

var friendform = {
  username: null,
  successcb: null,
  
  submit: function()
  {
    var username = $( '#friend-add-username' ).attr( 'value' ).trim();
    if (username == null || username.length == 0) {
      this.showerror( 'Username or Email is empty' );
      return;
    }
    
    this.invite_friend(username);
  },
  invite_friend: function( username )
  {
    if (username == null || username.length == 0)
      return;
    
    var params = '&action=invite&username=' + encodeURIComponent(username);
    
    $.ajax
    (
      {
        url : app.user_path + '/friend?token=' + app.token + params + '&wt=json',
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
            var cb = friendform.successcb;
            if (cb) cb.call(friendform);
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
  action_submit: function( username, action )
  {
    if (username == null || action == null) return;
    
    var params = '&action=' + encodeURIComponent(action) + 
                 '&username=' + encodeURIComponent(username);
    
    $.ajax
    (
      {
        url : app.user_path + '/friend?token=' + app.token + params + '&wt=json',
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
            var cb = friendform.successcb;
            if (cb) cb.call(friendform);
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
  accept_invite: function( username )
  {
    this.action_submit( username, 'accept' );
  },
  reject_invite: function( username )
  {
    this.action_submit( username, 'reject' );
  },
  cancel_invite: function( username )
  {
    this.action_submit( username, 'cancel' );
  },
  delete_friend: function( username )
  {
    if (username == null|| username.length == 0)
      return;
    
    this.username = username;
    dialog.show( friend_dialogs.delete_confirm_dialog );
  },
  delete_submit: function()
  {
    dialog.hide();
    
    var username = this.username;
    if (username == null|| username.length == 0)
      return;
    
    this.action_submit( username, 'delete' );
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var friend_headbar = {
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

// #/~friends
sammy.get
(
  /^#\/(~friends)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    friend_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/friends.html',
      function( template )
      {
        body_element
          .html( template );

        $( '#friend-add-submit-text' ).html( strings( 'Send Invite' ) );
        
        $( '#friend-list-friends' )
          .attr( 'href', '#/~friends' )
          .html( strings( 'Friends' ) );
        
        $( '#friend-list-groups' )
          .attr( 'href', '#/~groups' )
          .html( strings( 'Groups' ) );
        
        $( '#friend-list-find' )
          .attr( 'href', '#/~find' )
          .html( strings( 'Find' ) );
        
        $( '#friend-add-username' )
          .attr( 'placeholder', strings( 'username or email' ) );

        $( '#friend-add-submit' )
          .attr( 'onClick', 'javascript:friendform.submit();return false;' )
          .attr( 'title', strings( 'Add Friend' ) );
        
        listfriend.showlist();

        statusbar.show();
      }
    );
  }
);