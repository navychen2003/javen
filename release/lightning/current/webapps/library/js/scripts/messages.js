
var listmessage = {
  username: null,
  chatusername: null,
  streamid: null,
  messages: null,
  message_type: null,
  message_folder: null,
  message_folders: null,
  message_page: 1,
  
  showmessages: function( username )
  {
    $( '#messages-message-title' ).addClass( 'active' );
    $( '#messages-conversation-title' ).removeClass( 'active' );
    $( '#messages-notification-title' ).removeClass( 'active' );
    
    this.showlist(username, 'mail', '', 1);
  },
  showconversation: function( username )
  {
    $( '#messages-message-title' ).removeClass( 'active' );
    $( '#messages-conversation-title' ).addClass( 'active' );
    $( '#messages-notification-title' ).removeClass( 'active' );
    
    this.showlist(username, 'chat', '', 1);
  },
  showchat: function( chatusername )
  {
    $( '#messages-message-title' ).removeClass( 'active' );
    $( '#messages-conversation-title' ).addClass( 'active' );
    $( '#messages-notification-title' ).removeClass( 'active' );
    
    this.showlist(globalApp.get_username(), 'mail', '', 1, chatusername);
  },
  showstream: function( streamid )
  {
    $( '#messages-message-title' ).removeClass( 'active' );
    $( '#messages-conversation-title' ).addClass( 'active' );
    $( '#messages-notification-title' ).removeClass( 'active' );
    
    this.showlist(globalApp.get_username(), 'mail', '', 1, null, streamid);
  },
  shownotifications: function( username )
  {
    $( '#messages-message-title' ).removeClass( 'active' );
    $( '#messages-conversation-title' ).removeClass( 'active' );
    $( '#messages-notification-title' ).addClass( 'active' );
    
    this.showlist(username, 'notice', '', 1);
  },
  gocompose: function()
  {
    var username = this.username;
    var me = globalApp.get_username();
    if (username == null || username == me)
      username = '';
    
    var composeto = username;
    var chatusername = this.chatusername;
    if (chatusername != null && chatusername.length > 0)
      composeto = chatusername;
    
    var streamid = this.streamid;
    
    compose.success_cb = listmessage.refresh;
    compose.show( 'mail', composeto, null, null, null, streamid );
  },
  gomessages: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('messages') >= 0) {
      listmessage.relist();
      return;
    }
    
    context.redirect( '#/~messages' );
  },
  goconversations: function()
  {
    var username = this.username;
    if (username == null) return;
    
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('conversation') >= 0) {
      listmessage.relist();
      return;
    }
    
    context.redirect( '#/~conversation/' + encodeURIComponent(username) );
  },
  gochats: function()
  {
    var chatusername = this.chatusername;
    if (chatusername == null) return;
    
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('chat') >= 0) {
      this.relist();
      return;
    }
    
    context.redirect( '#/~chat/' + encodeURIComponent(chatusername) );
  },
  gostreams: function()
  {
    var streamid = this.streamid;
    if (streamid == null) return;
    
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('messagestream') >= 0) {
      this.relist();
      return;
    }
    
    context.redirect( '#/~messagestream/' + encodeURIComponent(streamid) );
  },
  gonotifications: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('notifications') >= 0) {
      this.relist();
      return;
    }
    
    context.redirect( '#/~notifications' );
  },
  showfolder: function( key )
  {
    if (key == null) return;
    
    var username = listmessage.username;
    var type = listmessage.message_type;
    var folder = listmessage.message_folder;
    var folders = listmessage.message_folders;
    
    if (type && folders) { 
      folder = folders[key];
      if (folder)
        listmessage.showlist( username, type, folder, 1);
    }
  },
  showpage: function( page )
  {
    var username = listmessage.username;
    var type = listmessage.message_type;
    var folder = listmessage.message_folder;
    var page2 = listmessage.message_page;
    var chatusername = listmessage.chatusername;
    var streamid = listmessage.streamid;
    
    listmessage.showlist( username, type, folder, page, chatusername, streamid );
  },
  relist: function()
  {
    var username = listmessage.username;
    var type = listmessage.message_type;
    var folder = listmessage.message_folder;
    var page = listmessage.message_page;
    var chatusername = listmessage.chatusername;
    var streamid = listmessage.streamid;
    
    listmessage.showlist( username, type, folder, page, chatusername, streamid );
  },
  init_title: function( user, chatuser, streamid )
  {
    var me = globalApp.get_username();
    
    if (user) {
      var username = user['name'];
      var nickname = user['title'];
      var usertype = user['type'];
    
      if (me != username && username != null && username.length > 0) {
        $( '#messages-message-nav' ).addClass( 'hide' );
        $( '#messages-conversation-nav' ).removeClass( 'hide' );
        $( '#messages-notification-nav' ).addClass( 'hide' );
      
        if (usertype == 'group') {
          var linkto = '#/~group/' + encodeURIComponent(username);
          navbar.init_grouptitle( username, nickname, null, linkto );
        
        } else {
          var linkto = '#/~user/' + encodeURIComponent(username);
          navbar.init_usertitle( username, nickname, null, linkto );
        }
      
        return;
      }
    }
    
    if (chatuser || streamid) {
      $( '#messages-message-nav' ).addClass( 'hide' );
      $( '#messages-conversation-nav' ).removeClass( 'hide' );
      $( '#messages-notification-nav' ).addClass( 'hide' );
        
    } else {
      $( '#messages-message-nav' ).removeClass( 'hide' );
      $( '#messages-conversation-nav' ).addClass( 'hide' );
      $( '#messages-notification-nav' ).removeClass( 'hide' );
    }
      
    navbar.init_metitle( null, '#/~profile' );
  },
  showlist: function( username, type, folder, page, chatusername, streamid )
  {
    var composetext_element = $( '#messages-compose-submit-text' );
    var compose_element = $( '#messages-compose-submit' );
    var messagetitle_element = $( '#messages-message-title' );
    var conversationtitle_element = $( '#messages-conversation-title' );
    var notificationtitle_element = $( '#messages-notification-title' );
    
    var me = globalApp.get_username();
    if (username == null || username.length == 0 || username == me) {
      navbar.init_metitle( null, '#/~profile' );
    }
    
    var composeTitle = strings( 'Compose' );
    if (system) {
      var context = system.context;
      var path = context.path;
    
      if (path.indexOf('conversation') >= 0 ||
          path.indexOf('messagestream') >= 0 ||
          path.indexOf('chat') >= 0) {
        composeTitle = strings( 'Send Message' );
      }
    }
    
    composetext_element
      .html( composeTitle.esc() );
    
    compose_element
      .attr( 'onClick', 'javascript:listmessage.gocompose();return false;' )
      .attr( 'title', composeTitle.esc() );
    
    messagetitle_element
      .attr( 'onClick', 'javascript:listmessage.gomessages();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Mail' ).esc() );
    
    conversationtitle_element
      .attr( 'onClick', 'javascript:listmessage.goconversations();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Conversation' ).esc() );
    
    notificationtitle_element
      .attr( 'onClick', 'javascript:listmessage.gonotifications();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Notification' ).esc() );
    
    if (page == null || page < 1) page = 1;
    if (type == null) type = '';
    if (folder == null) folder = '';
    if (username == null) username = '';
    if (chatusername == null) chatusername = '';
    if (streamid == null) streamid = '';
    
    this.username = username;
    this.chatusername = chatusername;
    this.streamid = streamid;
    this.messages = null;
    
    var groupby = '';
    
    if (chatusername != null && chatusername.length > 0) {
      groupby = 'stream';
      conversationtitle_element
        .attr( 'onClick', 'javascript:listmessage.gochats();return false;' )
        .html( strings( 'Chat' ).esc() );
      
    } else if (streamid != null && streamid.length > 0) {
      conversationtitle_element
        .attr( 'onClick', 'javascript:listmessage.gostreams();return false;' )
        .html( strings( 'Chat' ).esc() );
      
    } else {
      groupby = 'stream';
    }
    
    var params = '&action=list' 
        + '&username=' + encodeURIComponent(username) 
        + '&chatuser=' + encodeURIComponent(chatusername) 
        + '&streamid=' + encodeURIComponent(streamid) 
        + '&type=' + encodeURIComponent(type) 
        + '&folder=' + encodeURIComponent(folder) 
        + '&groupby=' + encodeURIComponent(groupby) 
        + '&page=' + page;
    
    $.ajax
    (
      {
        url : app.user_path + '/message?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          listmessage.init_content( response );
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
  init_content: function( response )
  {
    if (response == null) response = {};
    
    var folderlist_element = $( '#messages-folder-list' );
    var list_element = $( '#messages-list' );
    
    var page_element = $( '#messages-page' );
    var pages_element = $( '#messages-pages' );
    
    var prev_element = $( '#messages-prev' );
    var next_element = $( '#messages-next' );
    
    var the_user = response['user'];
    var the_chatuser = response['chatuser'];
    var the_streamid = response['streamid'];
    var the_type = response['type'];
    var the_folder = response['folder'];
    var the_groupby = response['groupby'];
    var the_page = response['page'];
    var total_page = response['totalpage'];
  
    if (the_page <= 1) {
      prev_element.addClass( 'disabled' );
    
    } else {
      prev_element
        .removeClass( 'disabled' )
        .attr( 'onclick', 'javascript:listmessage.showpage(' + (the_page-1) + ');' );
    }
  
    if (the_page >= total_page) {
      next_element.addClass( 'disabled' );
    
    } else { 
      next_element
        .removeClass( 'disabled' )
        .attr( 'onclick', 'javascript:listmessage.showpage(' + (the_page+1) + ');' );
    }
  
    page_element.html( the_page );
    pages_element.html( total_page );
  
    var folders = response['folders'];
    var folderContent = [];
  
    listmessage.init_title( the_user, the_chatuser, the_streamid );
  
    listmessage.streamid = the_streamid;
    listmessage.message_type = the_type;
    listmessage.message_folder = the_folder;
    listmessage.message_folders = folders;
    listmessage.message_page = the_page;
  
    var messageIcon = 'bullhorn';
    if (the_type == 'message' || the_type == 'mail') {
      if (the_folder == 'Outbox')
        messageIcon = 'inbox-out';
      else if (the_folder == 'Inbox')
        messageIcon = 'inbox-in';
      else if (the_folder == 'Trash')
        messageIcon = 'bin';
      else
        messageIcon = 'inbox';
    } else if (the_type == 'chat' || the_type == 'conversation') {
      messageIcon = 'conversation';
    }
  
    if (the_user) {
      listmessage.username = the_user['name'];
    }
  
    if (the_streamid != null && the_streamid.length > 0) {
      messageIcon = 'chat';
      folders = {};
    
      var name = strings( 'Message Stream' );
      var clickto = 'javascript:listmessage.gostreams();return false;';
      var hrefto = '';
    
      var active = 'active';
      var item = '<li><a class="settings-filter ' + active + '" onClick="' + clickto + '" href="' + hrefto + '">' + name + '</a></li>';
    
      folderContent.push( item );
    
    } else if (the_chatuser) {
      listmessage.chatusername = the_chatuser['name'];
      messageIcon = 'chat';
      folders = {};
    
      var chatuserkey = the_chatuser['key'];
      var chatusername = the_chatuser['name'];
      var chatnickname = the_chatuser['title'];
    
      var name = chatusername;
      if (name == null) name = '';
      if (chatnickname != null && chatnickname.length > 0)
        name = chatnickname;
    
      var clickto = 'javascript:listmessage.gochats();return false;';
      var hrefto = '';
    
      var active = 'active';
      var item = '<li><a class="settings-filter ' + active + '" onClick="' + clickto + '" href="' + hrefto + '">' + name + '</a></li>';
    
      folderContent.push( item );
    }
  
    $( '#messages-icon' )
      .attr( 'class', 'subnav-icon glyphicon ' + messageIcon );
  
    for (var key in folders) {
      var foldername = folders[key];
    
      var name = strings( foldername );
      var clickto = 'javascript:listmessage.showfolder(' + key + ');return false;';
      var hrefto = '';
      var active = '';
    
      if (foldername == the_folder)
        active = 'active';
    
      var item = '<li><a class="settings-filter ' + active + '" onClick="' + clickto + '" href="' + hrefto + '">' + name + '</a></li>';
    
      folderContent.push( item );
    }
  
    folderlist_element
      .html( folderContent.join( "\n" ) );
  
    var messages = response['messages'];
    var messageContent = [];
    var prevstreamid = null;
  
    listmessage.messages = messages;
  
    for (var key in messages) { 
      var message = messages[key];
      var idx = messageContent.length;
    
      var id = message['id'];
      var sid = message['streamid'];
      var mfolder = message['folder'];
      var mtype = message['type'];
      var from = message['from'];
      var text = message['subject'];
      var ctype = message['ctype'];
      var status = message['status'];
      var flag = message['flag'];
      var mtime = message['mtime'];
      var attcount = message['attcount'];
      var streamcount = message['streamcount'];
      var userfrom = message['userfrom'];
    
      if (attcount == null) attcount = 0;
      if (streamcount == null) streamcount = 0;
      if (sid == null) sid = '';
    
      var msgfrom = from;
      var msgtime = format_time(mtime);
    
      var deletetitle = strings( 'Delete' );
      var startitle = strings( 'Mark as important' );
    
      var iconclass = messageinfo.get_iconclass(mtype, mfolder, status, the_streamid);;
      if (iconclass == null || iconclass.length == 0)
        iconclass = 'glyphicon message-plus';
      if (the_chatuser)
        iconclass = 'glyphicon chat';
    
      if (msgfrom == null) msgfrom = '';
      msgfrom = listmessage.buildAddressItem( msgfrom, userfrom );
    
      if (text == null || text.length == 0)
        text = strings( '[No Subject]' );
    
      var starredto = 'javascript:listmessage.starred_message(\'' + mtype + '\',\'' + mfolder + '\',\'' + id + '\');return false;';
      var unstarredto = 'javascript:listmessage.unstarred_message(\'' + mtype + '\',\'' + mfolder + '\',\'' + id + '\');return false;';
      var deleteto = 'javascript:listmessage.delete_message(\'' + mtype + '\',\'' + mfolder + '\',\'' + id + '\');return false;';
      var clickto = 'javascript:listmessage.click_message(\'' + mtype + '\',\'' + mfolder + '\',\'' + id + '\',' + idx + ');return false;';
      var href = ''; //clickto;
    
      var starclass = 'glyphicon dislikes';
      if (status == 'starred' || flag == 'favorite') {
        starclass = 'glyphicon star';
        starredto = unstarredto;
        startitle = strings( 'Unstar this message' );
      }
    
      var onover = 'javascript:listmessage.onmessageover(' + idx + ');';
      var onout = 'javascript:listmessage.onmessageout(' + idx + ');';
    
      var streamclick = 'javascript:listmessage.click_stream(\'' + sid + '\');return false;';
      var streamhide = 'hide';
      var streamtitle = '';
    
      if (streamcount > 1 && (the_streamid == null || the_streamid.length == 0)) {
        streamhide = '';
        streamtitle = strings( 'Subject Conversation' );
      }
    
      if (mtype == 'notice' || mtype == 'notification')
        streamhide = 'hide';
    
      var attachclick = clickto; //'javascript:return false;';
      var attachhide = 'hide';
      var attachtitle = '';
    
      if (attcount > 1) {
        attachhide = '';
        attachtitle = strings( 'This message has {0} attachments' ).format(attcount);
      } else if (attcount > 0) {
        attachhide = '';
        attachtitle = strings( 'This message has an attachment' );
      } else {
        attachhide = 'hide';
      }
    
      var itemstyle = '';
      var iconstyle = '';
      var starredstyle = 'margin-left: 20px;';
    
      if (the_groupby == 'stream' && sid == prevstreamid) {
        //itemstyle = 'margin-left: 20px;';
        iconstyle = 'margin-left: 20px;';
        starredstyle = 'margin-left: 40px;';
        streamhide = 'hide';
      }
    
      text = trim_line( text, 50 );
      message['itemstyle'] = itemstyle;
    
      var item = 
         '        <li class="server-event-list-item" id="messages-list-' + idx + '" onmouseover="' + onover + '" onmouseout="' + onout + '" style="' + itemstyle + '">' + "\n" +
         '          <i class="server-event-icon ' + iconclass + '" style="' + iconstyle + '"></i>' + "\n" +
         '          <div class="server-event-details" style="' + starredstyle + '">' + "\n" +
         '            <span class="server-event-action" onclick="' + starredto + '" title="' + startitle + '"><i class="' + starclass + '"></i></span>' + "\n" +
	     '            <span class="server-event-time">' + msgtime.esc() + '</span>' + "\n" +
         '            <span class="server-event-time">' + msgfrom + '</span>' + "\n" +
         '            <a href="' + href + '" onclick="' + clickto + '">' + text + '</a>' + "\n" +
         '            <button id="messages-attach-' + idx + '" class="player-btn ' + attachhide + '" style="width: 20px;height: 20px;font-size: 18px;" onclick="' + attachclick + '" title="' + attachtitle + '"><i class="glyphicon paperclip"></i></button>' + "\n" +
         '            <button id="messages-stream-' + idx + '" class="player-btn ' + streamhide + '" style="width: 20px;height: 20px;font-size: 18px;" onclick="' + streamclick + '" title="' + streamtitle + '"><i class="glyphicon chat"></i></button>' + "\n" +
         '            <button id="messages-delete-' + idx + '" class="player-btn hide" style="width: 20px;height: 20px;font-size: 18px;" onclick="' + deleteto + '" title="' + deletetitle + '"><i class="glyphicon remove-2"></i></button>' + "\n" +
         '          </div>' + "\n" +
         '        </li>' + "\n";
    
      messageContent.push( item );
      prevstreamid = sid;
    }
  
    list_element
      .html( messageContent.join( "\n" ) );
  
    if (messageContent.length == 0) {
      var emptytitle = strings( 'No messages :(' );
      if (the_type == 'notice' || the_type == 'notification')
        emptytitle = strings( 'No notifications :(' );
    
      $( '#messages-list' ).addClass( 'hide' );
      $( '#messages-empty' ).html( emptytitle ).removeClass( 'hide' );
    } else {
      $( '#messages-list' ).removeClass( 'hide' );
      $( '#messages-empty' ).addClass( 'hide' );
    }
  },
  onmessageover: function( idx )
  {
    if (idx == null) return;
    
    var messages = listmessage.messages;
    if (messages == null || messages.length == 0)
      return;
    
    if (idx >= 0 && idx < messages.length) {
      var message = messages[idx];
      if (message) {
        var itemstyle = message['itemstyle'];
        if (itemstyle == null) itemstyle = '';
        
        var list_element = $( '#messages-list-' + idx );
        var delete_element = $( '#messages-delete-' + idx );
    
        if (list_element)
          list_element.attr( 'style', itemstyle + 'background-color: rgba(255,255,255,0.1);' );
    
        if (delete_element)
          delete_element.removeClass( 'hide' );
      }
    }
  },
  onmessageout: function( idx )
  {
    if (idx == null) return;
    
    var messages = listmessage.messages;
    if (messages == null || messages.length == 0)
      return;
    
    if (idx >= 0 && idx < messages.length) {
      var message = messages[idx];
      if (message) {
        var itemstyle = message['itemstyle'];
        if (itemstyle == null) itemstyle = '';
    
        var list_element = $( '#messages-list-' + idx );
        var delete_element = $( '#messages-delete-' + idx );
    
        if (list_element)
          list_element.attr( 'style', itemstyle );
    
        if (delete_element)
          delete_element.addClass( 'hide' );
      }
    }
  },
  onmessageclick: function( idx )
  {
    if (idx == null) return;
    
    var messages = listmessage.messages;
    if (messages == null || messages.length == 0)
      return;
    
    if (idx < 0) {
      messager.showerror( strings( 'You have reached the first message' ) );
      return;
    }
    
    if (idx >= messages.length) {
      messager.showerror( strings( 'You have reached the last message' ) );
      return;
    }
    
    if (idx >= 0 && idx < messages.length) {
      var message = messages[idx];
      if (message) {
        var id = message['id'];
        var sid = message['streamid'];
        var mfolder = message['folder'];
        var mtype = message['type'];
        var from = message['from'];
        var text = message['subject'];
        var ctype = message['ctype'];
        var status = message['status'];
        var flag = message['flag'];
        var mtime = message['mtime'];
        var attcount = message['attcount'];
        
        if (id && mtype && mfolder) {
          listmessage.click_message( mtype, mfolder, id, idx );
          return;
        }
      }
    }
  },
  buildAddressItem: function( addrs, user )
  {
    var content = [];
    if (user) {
      var username = user['name'];
      var type = user['type'];
      var avatar = user['avatar'];
      var nickname = user['nickname'];
      var firstname = user['firstname'];
      var lastname = user['lastname'];
      var title = user['title'];
      
      if (username == null) username = '';
      if (avatar == null) avatar = '';
      if (nickname == null) nickname = '';
      if (firstname == null) firstname = '';
      if (lastname == null) lastname = '';
      if (title == null) title = '';
      
      var name = nickname;
      if (name == null || name.length == 0)
        name = title;
      if (name == null || name.length == 0) {
        if (firstname != null && firstname.length > 0) {
          name = firstname;
          if (lastname != null && lastname.length > 0)
            name += ' ' + lastname; 
        }
      }
      if (name == null || name.length == 0)
        name = username;
      
      if (username != null && username.length > 0) {
        var item = '<a href="" onClick="javascript:userinfo.showdetails(\'' + username + '\');return false;">' + name.esc() + '</a>';
        content.push( item );
      }
    }
    
    if (content.length == 0) {
      if (addrs == null) addrs = '';
      var values = addrs.split( ',' );
    
      if (values) {
        for (var key in values) {
          var value = values[key];
          if (value != null && value.length > 0) {
            var item = '<a href="" onClick="javascript:userinfo.showdetails(\'' + value + '\');return false;">' + value + '</a>';
            content.push( item );
          }
        }
      }
    }
    
    return content.join( ',' );
  },
  click_stream: function( streamid )
  {
    if (streamid == null || streamid.length == 0)
      return;
    
    var context = system.context;
    context.redirect( '#/~messagestream/' + encodeURIComponent(streamid) );
  },
  click_message: function( type, folder, id, idx )
  {
    if ((type == 'message' || type == 'mail') && folder == 'Draft') {
      compose.success_cb = listmessage.refresh;
      compose.showdraft( type, id );
      return;
    }
    
    var username = listmessage.username;
    messageinfo.showmessage( username, type, folder, id, 
      function() {
        var messages = listmessage.messages;
        if (messages != null && messages.length > 0 && idx != null && idx >= 0) {
          var previdx = idx - 1;
          var nextidx = idx + 1;
          $( '#messageinfo-prev' )
            .attr( 'onclick', 'javascript:listmessage.onmessageclick(' + previdx + ');return false;' )
            .removeClass( 'hide' );
          $( '#messageinfo-next' )
            .attr( 'onclick', 'javascript:listmessage.onmessageclick(' + nextidx + ');return false;' )
            .removeClass( 'hide' );
        }
      });
  },
  delete_message: function( type, folder, id )
  {
    var username = listmessage.username;
    
    if (folder == 'Trash' || folder == 'System') {
      messageform.delete_message( username, type, folder, id, 
        function() {
          listmessage.relist();
        });
    } else {
      messageform.trash_message( username, type, folder, id, 
        function() {
          listmessage.relist();
        });
    }
  },
  starred_message: function( type, folder, id )
  {
    var username = listmessage.username;
    
    messageform.setflag_message( username, type, folder, id, 'favorite', 
      function() {
        listmessage.relist();
      });
  },
  unstarred_message: function( type, folder, id )
  {
    var username = listmessage.username;
    
    messageform.setflag_message( username, type, folder, id, 'null', 
      function() {
        listmessage.relist();
      });
  },
  refresh: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('messages') >= 0 || 
        path.indexOf('notifications') >= 0 || 
        path.indexOf('conversation') >= 0 ||
        path.indexOf('chat') >= 0) {
      listmessage.relist();
      return true;
    }
    
    return false;
  }
};

var message_headbar = {
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

// #/~messages
sammy.get
(
  /^#\/(~messages)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/messages.html',
      function( template )
      {
        body_element
          .html( template );

        listmessage.showmessages();

        statusbar.show();
      }
    );
  }
);

// #/~notifications
sammy.get
(
  /^#\/(~notifications)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/messages.html',
      function( template )
      {
        body_element
          .html( template );

        listmessage.shownotifications();

        statusbar.show();
      }
    );
  }
);

// #/~conversation
sammy.get
(
  // /^#\/(~conversation)$/,
  new RegExp( '(~conversation)\\/' ),
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
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/messages.html',
      function( template )
      {
        body_element
          .html( template );

        listmessage.showconversation( id_param );

        statusbar.show();
      }
    );
  }
);

// #/~chat
sammy.get
(
  // /^#\/(~chat)$/,
  new RegExp( '(~chat)\\/' ),
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
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/messages.html',
      function( template )
      {
        body_element
          .html( template );

        listmessage.showchat( id_param );

        statusbar.show();
      }
    );
  }
);

// #/~messagestream
sammy.get
(
  // /^#\/(~messagestream)$/,
  new RegExp( '(~messagestream)\\/' ),
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
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/messages.html',
      function( template )
      {
        body_element
          .html( template );

        listmessage.showstream( id_param );

        statusbar.show();
      }
    );
  }
);
