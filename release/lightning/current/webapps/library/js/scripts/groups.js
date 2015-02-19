
var listgroup = {
  showlist: function()
  {
    groupform.successcb = null;
    navbar.init_metitle( null, '#/~profile' );
    
    var params = '&action=list';
    
    $.ajax
    (
      {
        url : app.user_path + '/group?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var invites = response['invites'];
          var groups = response['groups'];
          
          listgroup.init_content( invites, groups );
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
  init_content: function( invites, groups )
  {
    if (invites == null) invites = {};
    if (groups == null) groups = {};
  
    var username = globalApp.get_username();
  
    var inviteCount = 0;
    var inviteContent = [];
    var requestContent = [];
  
    for (var key in invites) {
      var invite = invites[key];
      if (invite == null) continue;
    
      var name = invite['name'];
      var title = invite['title'];
      var type = invite['type'];
      var avatar = invite['avatar'];
      var message = invite['message'];
      var time = invite['time'];
    
      if (name == null) name = '';
      if (title == null || title.length == 0) title = name;
    
      var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
      var thumbClick = 'javascript:groupinfo.showdetails(\'' + name + '\');return false;';
      var acceptClick = 'javascript:memberform.accept_invite(\'' + name + '\',\'' + username + '\');return false;';
      var rejectClick = 'javascript:memberform.reject_invite(\'' + name + '\',\'' + username + '\');return false;';
      var cancelClick = 'javascript:memberform.cancel_invite(\'' + name + '\',\'' + username + '\');return false;';
    
      if (avatar != null && avatar.length > 0) {
        var id = avatar;
        var extension = 'jpg';
      
        var src = app.base_path + '/image/' + id + '_64t.' + extension + '?token=' + app.token;
        thumbsrc = src;
      }
    
      if (type == 'in') {
        if (message == null || message.length == 0)
          message = strings( 'invite to join group' );
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
    
    var groupCount = 0;
    var groupContent = [];
    
    for (var gkey in groups) {
      var group = groups[gkey];
      if (group == null) continue;
      
      var name = group['name'];
      var title = group['title'];
      var status = group['status'];
      var avatar = group['avatar'];
      var role = group['role'];
      
      if (name == null) name = '';
      if (status == null) status = '';
      if (title == null || title.length == 0) title = name;
      
      var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
      var thumbClick = 'javascript:groupinfo.showdetails(\'' + name + '\');return false;';
      var leaveClick = 'javascript:memberform.leave_group(\'' + name + '\',\'' + username + '\');return false;';
        
      if (avatar != null && avatar.length > 0) {
        var id = avatar;
        var extension = 'jpg';
      
        var src = app.base_path + '/image/' + id + '_64t.' + extension + '?token=' + app.token;
        thumbsrc = src;
      }
      
      var actionHide = '';
      if (role == 'owner') {
        status = strings( 'You are owner' );
        actionHide = 'hide';
      } else if (role == 'manager') {
        status = strings( 'You are manager' );
      } else if (role == 'member') {
        status = strings( 'You are member' );
      }
      
      var item = '<li class="well">' + "\n" +
                 '    <div class="actions ' + actionHide + '">' + "\n" +
                 '        <button type="button" class="delete-btn btn btn-danger btn-icon" title="' + strings('Leave Group') + '" onClick="' + leaveClick + '"><i class="glyphicon ban"></i></button>' + "\n" +
                 '    </div>' + "\n" +
                 '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                 '    <h3>' + title.esc() + '</h3>' + "\n" +
                 '    <h4>' + status.esc() + '</h4>' + "\n" +
                 '</li>';
        
      groupContent.push( item );
      groupCount ++;
    }
    
    var inviteTitle = strings( 'Invites' ) + ' <span class="well-header-count">' + inviteCount + '</span>';
    var groupTitle = strings( 'Groups' ) + ' <span class="well-header-count">' + groupCount + '</span>';
    
    $( '#group-invite-title' ).html( inviteTitle );
    $( '#group-list-title' ).html( groupTitle );
    
    $( '#group-invitelist' ).html( inviteContent.join( "\n" ) );
    $( '#group-requestlist' ).html( requestContent.join( "\n" ) );
    $( '#group-list' ).html( groupContent.join( "\n" ) );
    
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
    
    if (groupContent.length == 0 && inviteCount == 0) {
      $( '#group-empty' )
        .html( strings( 'No groups :(' ) )
        .removeClass( 'hide' );
    } else {
      $( '#group-empty' )
        .addClass( 'hide' );
    }
  }
};

var groupform = {
  groupname: null,
  successcb: null,
  
  newgroup: function()
  {
    dialog.show( group_dialogs.addgroup_dialog );
  },
  newgroup_html: function()
  {
    var name_title = strings( 'Group Name' );
    var name_required = strings( 'A group name is required.' );
    
    var nick_title = strings( 'Group Nickname' );
    var nick_required = strings( 'A group nickname is required.' );
    
    var category_title = strings( 'Group Category' );
    var public_title = strings( 'Public Group' );
    var private_title = strings( 'Private Group' );
    
    var categoryhide = 'hide';
    if (globalApp.is_admin()) categoryhide = '';
    
    return '<div class="add-section-details" id="newgroup-details">' + "\n" +
		   '  <div class="row-fluid">' + "\n" +
		   '	<div class="name-group control-group" id="newgroup-name-group">' + "\n" +
		   '	  <span class="help-inline hide" id="newgroup-required-name">' + name_required + '</span>' + "\n" +
		   '	  <label id="newgroup-name-label">' + name_title.esc() + '</label>' + "\n" +
		   '	  <input type="text" name="name" class="span12" id="newgroup-name-input" value="">' + "\n" +
		   '    </div>' + "\n" +
		   '  </div>' + "\n" +
		   '  <div class="row-fluid">' + "\n" +
		   '	<div class="name-group control-group" id="newgroup-nickname-group">' + "\n" +
		   '	  <span class="help-inline hide" id="newgroup-required-nickname">' + nick_required + '</span>' + "\n" +
		   '	  <label id="newgroup-nickname-label">' + nick_title.esc() + '</label>' + "\n" +
		   '	  <input type="text" name="nickname" class="span12" id="newgroup-nickname-input" value="">' + "\n" +
		   '    </div>' + "\n" +
		   '  </div>' + "\n" +
		   '  <div class="row-fluid ' + categoryhide + '">' + "\n" +
           '    <div class="control-group">' + "\n" +
           '      <label id="newgroup-category-label">' + category_title.esc() + '</label>' + "\n" +
           '        <select name="category" class="span6" id="newgroup-category-input">' + "\n" +
           '          <option value="public">' + public_title.esc() + '</option>' + "\n" +
           '          <option value="private" selected>' + private_title.esc() + '</option>' + "\n" +
           '        </select>' + "\n" +
           '        <p class="folder-help hide"></p>' + "\n" +
           '    </div>' + "\n" +
           '  </div>' + "\n" +
		   '</div>';
  },
  newgroup_submit: function()
  {
    var input_error = false;
    
    var input_name = $( '#newgroup-name-input' )
      .attr( 'value' ).trim();
    
    var input_nickname = $( '#newgroup-nickname-input' )
      .attr( 'value' ).trim();
    
    var input_category = $( '#newgroup-category-input' )
      .attr( 'value' ).trim();
    
    if (input_name == null || input_name.length == 0)
    {
      $( '#newgroup-required-name' )
        .removeClass( 'hide' );
      
      $( '#newgroup-name-group' )
        .addClass( 'error' );
      
      input_error = true;
    }
    else
    {
      $( '#newgroup-required-name' )
        .addClass( 'hide' );
      
      $( '#newgroup-name-group' )
        .removeClass( 'error' );
    }
    
    if (input_category == null || input_category.length == 0)
      input_category = 'private';
    
    if (input_error) return;
    //dialog.hide();

    var groupname = input_name;
    var nickname = input_nickname;
    var category = input_category;
    
    if (globalApp.is_admin() == false)
      category = 'private';
    
    var params = '&action=register&groupname=' + encodeURIComponent(groupname) 
               + '&nickname=' + encodeURIComponent(nickname) 
               + '&category=' + encodeURIComponent(category);
    
    $.ajax
    (
      {
        url : app.user_path + '/group?token=' + app.token + params + '&wt=json',
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
            dialog.hide();
            
            var cb = groupform.successcb;
            if (cb) cb.call(friendform);
            else sammy.refresh();
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
  addgroup: function()
  {
    var groupname = $( '#group-add-groupname' ).attr( 'value' ).trim();
    if (groupname == null || groupname.length == 0) {
      this.showerror( 'Groupname or Email is empty' );
      return;
    }
    
    var username = globalApp.get_username();
    
    memberform.join_group( groupname, username );
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var group_dialogs = { 
  addgroup_dialog: null,
  
  init: function( dialog_element ) 
  {
    $.get
    (
      'tpl/addgroup.html',
      function( template )
      {
        group_dialogs.addgroup_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          {
            $( '#newgroup-title' ).html( strings( 'New Group' ) );
            $( '#newgroup-ok' ).html( strings( 'Ok' ) );
            $( '#newgroup-no' ).html( strings( 'Cancel' ) );
            
            $( '#newgroup-container' )
              .attr( 'class', 'edit-section-modal modal fade in' );
            
            $( '#newgroup-icon' )
              .attr( 'class', 'glyphicon user-add' );
            
            var html = groupform.newgroup_html();;
            if (html == null) html = "";
            
            $( '#newgroup-text' )
              .html( html );
            
            $( '#newgroup-ok' )
              .attr( 'onclick', 'javascript:groupform.newgroup_submit();return false;' );
            
            $( '#newgroup-no' )
              .attr( 'onclick', 'javascript:dialog.hide();return false;' );
            
            $( '#newgroup-close' )
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

var group_headbar = {
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

// #/~groups
sammy.get
(
  /^#\/(~groups)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    group_headbar.init( header_element );
    group_dialogs.init( dialog_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/groups.html',
      function( template )
      {
        body_element
          .html( template );

        $( '#group-add-submit-text' ).html( strings( 'Join Group' ) );
        $( '#group-new-submit-text' ).html( strings( 'New Group' ) );
        
        $( '#group-list-friends' )
          .attr( 'href', '#/~friends' )
          .html( strings( 'Friends' ) );
        
        $( '#group-list-groups' )
          .attr( 'href', '#/~groups' )
          .html( strings( 'Groups' ) );
        
        $( '#group-list-find' )
          .attr( 'href', '#/~find' )
          .html( strings( 'Find' ) );
        
        $( '#group-add-groupname' )
          .attr( 'placeholder', strings( 'groupname or email' ) );

        $( '#group-add-submit' )
          .attr( 'onClick', 'javascript:groupform.addgroup();return false;' )
          .attr( 'title', strings( 'Join Group' ) );
        
        $( '#group-new-submit' )
          .attr( 'onClick', 'javascript:groupform.newgroup();return false;' )
          .attr( 'title', strings( 'New Group' ) );
        
        listgroup.showlist();

        statusbar.show();
      }
    );
  }
);