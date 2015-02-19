
var listfind = {
  findparams: null,
  
  showlist: function()
  {
    navbar.init_metitle( null, '#/~profile' );
    
    var params = '&action=listall';
    this.showlist0( params );
  },
  showlist0: function( params )
  {
    friendform.successcb = findform.successcb;
    groupform.successcb = findform.successcb;
    memberform.successcb = memberform.successcb;
    
    if (params == null) params = '&action=listall';
    this.findparams = params;
    
    $.ajax
    (
      {
        url : app.user_path + '/find?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var users = response['users'];
          var groups = response['groups'];
          
          listfind.init_content( users, groups );
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
  init_content: function( users, groups )
  {
    if (users == null) users = {};
    if (groups == null) groups = {};
  
    var username = globalApp.get_username();
  
    var userCount = 0;
    var userContent = [];
    
    for (var ukey in users) {
      var user = users[ukey];
      if (user == null) continue;
      
      var name = user['name'];
      var title = user['title'];
      var status = user['status'];
      var avatar = user['avatar'];
      var invite = user['invite'];
      var message = user['message'];
        
      if (name == null) name = '';
      if (status == null) status = '';
      if (title == null || title.length == 0) title = name;
        
      var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
      var thumbClick = 'javascript:userinfo.showdetails(\'' + name + '\');return false;';
      var inviteClick = 'javascript:friendform.invite_friend(\'' + name + '\');return false;';
      var deleteClick = 'javascript:friendform.delete_friend(\'' + name + '\');return false;';
      var acceptClick = 'javascript:friendform.accept_invite(\'' + name + '\');return false;';
      var rejectClick = 'javascript:friendform.reject_invite(\'' + name + '\');return false;';
      var cancelClick = 'javascript:friendform.cancel_invite(\'' + name + '\');return false;';
        
      if (avatar != null && avatar.length > 0) {
        var id = avatar;
        var extension = 'jpg';
      
        var src = app.base_path + '/image/' + id + '_64t.' + extension + '?token=' + app.token;
        thumbsrc = src;
      }
      
      if (invite == 'friend') {
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions">' + "\n" +
                   '        <button type="button" class="delete-btn btn btn-danger btn-icon" title="' + strings('Remove') + '" onClick="' + deleteClick + '"><i class="glyphicon ban"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3>' + title.esc() + '</h3>' + "\n" +
                   '    <h4>' + status.esc() + '</h4>' + "\n" +
                   '</li>';
        
        userContent.push( item );
        userCount ++;
      
      } else if (invite == 'in') {
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
                   '    <h3>' + name.esc() + '</h3>' + "\n" +
                   '    <h4>' + message.esc() + '</h4>' + "\n" +
                   '</li>';
        
        userContent.push( item );
        userCount ++;
        
      } else if (invite == 'out') {
        if (message == null || message.length == 0)
          message = strings( 'has not accepted yet' );
        else
          message = strings( message );
        
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions">' + "\n" +
                   '        <button type="button" class="cancel-btn btn btn btn-icon" title="' + strings('Cancel') + '" onClick="' + cancelClick + '"><i class="glyphicon circle-remove"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3>' + name.esc() + '</h3>' + "\n" +
                   '    <h4>' + message.esc() + '</h4>' + "\n" +
                   '</li>';
        
        userContent.push( item );
        userCount ++;
        
      } else {
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions">' + "\n" +
                   '        <button type="button" class="add-btn btn btn-icon" title="' + strings('Send Invite') + '" onClick="' + inviteClick + '"><i class="glyphicon user-add"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3>' + title.esc() + '</h3>' + "\n" +
                   '    <h4>' + status.esc() + '</h4>' + "\n" +
                   '</li>';
        
        userContent.push( item );
        userCount ++;
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
      var joinClick = 'javascript:memberform.join_group(\'' + name + '\',\'' + username + '\');return false;';
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
      
      if (role == null || role.length == 0) {
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions">' + "\n" +
                   '        <button type="button" class="add-btn btn btn-icon" title="' + strings('Join Group') + '" onClick="' + joinClick + '"><i class="glyphicon user-add"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3>' + title.esc() + '</h3>' + "\n" +
                   '    <h4>' + status.esc() + '</h4>' + "\n" +
                   '</li>';
        
        groupContent.push( item );
        groupCount ++;
        
      } else {
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
    }
    
    var userTitle = strings( 'Users' ) + ' <span class="well-header-count">' + userCount + '</span>';
    var groupTitle = strings( 'Groups' ) + ' <span class="well-header-count">' + groupCount + '</span>';
    
    $( '#find-userlist-title' ).html( userTitle );
    $( '#find-grouplist-title' ).html( groupTitle );
    
    $( '#find-userlist' ).html( userContent.join( "\n" ) );
    $( '#find-grouplist' ).html( groupContent.join( "\n" ) );
    
    $( '#groups-container' ).removeClass( 'hide' );
    $( '#users-container' ).removeClass( 'hide' );
    
    if (groupContent.length == 0) {
      $( '#groups-container' ).addClass( 'hide' );
      
    } else if (userContent.length == 0) {
      $( '#users-container' ).addClass( 'hide' );
    }
    
    if (groupContent.length == 0 && userContent.length == 0) {
      $( '#find-empty' )
        .html( strings( 'No users :(' ) )
        .removeClass( 'hide' );
    } else {
      $( '#find-empty' )
        .addClass( 'hide' );
    }
  }
};

var findform = {
  search_submit: function()
  {
    var searchname = $( '#find-search-name' ).attr( 'value' ).trim();
    if (searchname == null) searchname = '';
    
    var params = '&action=search&name=' + encodeURIComponent(searchname);
    listfind.showlist0( params );
  },
  successcb: function()
  {
    var params = listfind.findparams;
    listfind.showlist0( params );
  }
};

var find_headbar = {
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

// #/~find
sammy.get
(
  /^#\/(~find)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    group_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/find.html',
      function( template )
      {
        body_element
          .html( template );

        $( '#find-search-submit-text' ).html( strings( 'Find' ) );
        
        $( '#find-list-friends' )
          .attr( 'href', '#/~friends' )
          .html( strings( 'Friends' ) );
        
        $( '#find-list-groups' )
          .attr( 'href', '#/~groups' )
          .html( strings( 'Groups' ) );
        
        $( '#find-list-find' )
          .attr( 'href', '#/~find' )
          .html( strings( 'Find' ) );
        
        $( '#find-search-name' )
          .attr( 'placeholder', strings( 'username or groupname' ) );

        $( '#find-search-submit' )
          .attr( 'onClick', 'javascript:findform.search_submit();return false;' )
          .attr( 'title', strings( 'Find' ) );
        
        listfind.showlist();

        statusbar.show();
      }
    );
  }
);