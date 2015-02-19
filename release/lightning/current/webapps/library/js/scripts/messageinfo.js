
var messageinfo_dialogs = { 
  delete_confirm_dialog: null,
  trash_confirm_dialog: null,
  
  init_message: function( dialog_element, template ) 
  {
    messageinfo_dialogs.delete_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Message' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to remove this message?' );
        if (msg == null) msg = "";
        
        $( '#message-text' ).html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:messageform.delete_submit();return false;' )
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
    
    messageinfo_dialogs.trash_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Message' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon circle-question-mark' );
        
        var msg = strings( 'Are you sure you want to move this message to trash folder?' );
        if (msg == null) msg = "";
        
        var optionchecked = '';
        if (messageform.trash_prompt == false) optionchecked = 'checked';
        var optiontitle = strings( 'Don\'t prompt' );
        var optionclickto = 'javascript:return messageform.trash_noprompt(this);';
        var optionhtml = '<label class="checkbox" for="input_noprompt" style="font-size: 14px;"><input id="input_noprompt" type="checkbox" class="input-dark" name="noprompt" value="true" onclick="' + optionclickto + '" ' + optionchecked + '>&nbsp;' + optiontitle.esc() + '</label>';
        
        $( '#message-text' ).html( msg.esc() );
        $( '#message-options' ).html( optionhtml );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:messageform.trash_submit();return false;' );
        
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

var messageform = {
  username: null,
  messageid: null,
  type: null,
  folder: null,
  folderto: null,
  successcb: null,
  trash_prompt: true,
  
  action_submit: function( username, type, folder, messageid, action, flag, folderto )
  {
    if (type == null || messageid == null || action == null) return;
    if (username == null) username = '';
    if (folder == null) folder = '';
    if (flag == null) flag = '';
    if (folderto == null) folderto = '';
    
    this.username = username;
    this.messageid = messageid;
    this.type = type;
    this.folder = folder;
    this.folderto = folderto;
    
    var params = '&action=' + encodeURIComponent(action) + 
        '&username=' + encodeURIComponent(username) +
        '&type=' + encodeURIComponent(type) +
        '&folder=' + encodeURIComponent(folder) +
        '&messageid=' + encodeURIComponent(messageid) +
        '&flag=' + encodeURIComponent(flag) +
        '&folderto=' + encodeURIComponent(folderto);
    
    $.ajax
    (
      {
        url : app.user_path + '/message?token=' + app.token + params + '&wt=json',
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
            var cb = messageform.successcb;
            messageform.successcb = null;
            if (cb) cb.call(messageform);
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
  setflag_message: function( username, type, folder, messageid, flag, successcb )
  {
    if (type == null || messageid == null || messageid.length == 0)
      return;
    
    this.username = username;
    this.messageid = messageid;
    this.type = type;
    this.folder = folder;
    this.successcb = successcb;
    
    this.action_submit( username, type, folder, messageid, 'setflag', flag );
  },
  move_message: function( username, type, folder, messageid, folderto, successcb )
  {
    if (type == null || messageid == null || folderto == null)
      return;
    
    this.username = username;
    this.messageid = messageid;
    this.type = type;
    this.folder = folder;
    this.folderto = folderto;
    this.successcb = successcb;
    
    this.action_submit( username, type, folder, messageid, 'move', null, folderto );
  },
  trash_message: function( username, type, folder, messageid, successcb )
  {
    if (type == null || messageid == null|| messageid.length == 0)
      return;
    
    this.username = username;
    this.messageid = messageid;
    this.type = type;
    this.folder = folder;
    this.successcb = successcb;
    
    if (messageform.trash_prompt == true)
      dialog.show( messageinfo_dialogs.trash_confirm_dialog );
    else
      this.trash_submit();
  },
  trash_noprompt: function( elem )
  {
    if (elem && elem.checked) {
      messageform.trash_prompt = false;
      return true;
    } else {
      messageform.trash_prompt = true;
      return true;
    }
  },
  trash_submit: function()
  {
    dialog.hide();
    
    var username = this.username;
    var messageid = this.messageid;
    var type = this.type;
    var folder = this.folder;
    
    if (type == null || messageid == null|| messageid.length == 0)
      return;
    
    this.action_submit( username, type, folder, messageid, 'trash' );
  },
  delete_message: function( username, type, folder, messageid, successcb )
  {
    if (type == null || messageid == null || messageid.length == 0)
      return;
    
    this.username = username;
    this.messageid = messageid;
    this.type = type;
    this.folder = folder;
    this.successcb = successcb;
    
    dialog.show( messageinfo_dialogs.delete_confirm_dialog );
  },
  delete_submit: function()
  {
    dialog.hide();
    
    var username = this.username;
    var messageid = this.messageid;
    var type = this.type;
    var folder = this.folder;
    
    if (type == null || messageid == null || messageid.length == 0)
      return;
    
    this.action_submit( username, type, folder, messageid, 'delete' );
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var messageinfo = {
  messageinfo_dialog: null,
  message_type: null,
  message_folder: null,
  message_folderfrom: null,
  message_id: null,
  message_from: null,
  message_to: null,
  message_cc: null,
  message_replyto: null,
  message_subject: null,
  message_body: null,
  message_attachments: null,
  message_time: null,
  message_status: null,
  message_user: null,
  message_userfrom: null,
  message_tousers: null,
  message_streamid: null,
  dialog_shown: false,
  showcb: null,
  
  showmessage: function( username, type, folder, id, showcb )
  {
    if (username == null || type == null || folder == null || id == null)
      return;
    
    if (this.dialog_shown == true)
      this.close();
    
    this.message_type = type;
    this.message_folder = folder;
    this.message_folderfrom = null;
    this.message_id = id;
    this.message_from = null;
    this.message_to = null;
    this.message_cc = null;
    this.message_replyto = null;
    this.message_subject = null;
    this.message_body = null;
    this.message_attachments = null;
    this.message_user = null;
    this.message_userfrom = null;
    this.message_tousers = null;
    this.message_status = null;
    this.message_streamid = null;
    this.message_time = 0;
    this.showcb = showcb;
    
    var params = '&action=get' 
        + '&username=' + encodeURIComponent(username) 
        + '&type=' + encodeURIComponent(type) 
        + '&folder=' + encodeURIComponent(folder) 
        + '&messageid=' + encodeURIComponent(id);
    
    $.ajax
    (
      {
        url : app.user_path + '/message?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var message = response['message'];
          if (message) {
            messageinfo.message_from = message['from'];
            messageinfo.message_to = message['to'];
            messageinfo.message_cc = message['cc'];
            messageinfo.message_replyto = message['replyto'];
            messageinfo.message_subject = message['subject'];
            messageinfo.message_body = message['body'];
            messageinfo.message_attachments = message['attachments'];
            messageinfo.message_id = message['id'];
            messageinfo.message_streamid = message['streamid'];
            messageinfo.message_folder = message['folder'];
            messageinfo.message_folderfrom = message['folderfrom'];
            messageinfo.message_type = message['type'];
            messageinfo.message_user = message['user'];
            messageinfo.message_userfrom = message['userfrom'];
            messageinfo.message_tousers = message['tousers'];
            messageinfo.message_status = message['status'];
            messageinfo.message_time = message['mtime'];
            dialog.show( messageinfo.messageinfo_dialog );
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
  delete: function()
  {
    var id = messageinfo.message_id;
    var folder = messageinfo.message_folder;
    var type = messageinfo.message_type;
    
    var user = this.message_user;
    if (user == null) user = {};
    var username = user['name'];
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (folder == 'Trash' || folder == 'System') {
      messageform.delete_message( username, type, folder, id, 
        function() {
          dialog.hide();
          if (listmessage.refresh() == false) 
            sammy.refresh();
        });
    } else {
      messageform.trash_message( username, type, folder, id, 
        function() {
          dialog.hide();
          if (listmessage.refresh() == false) 
            sammy.refresh();
        });
    }
  },
  restore: function()
  {
    var id = messageinfo.message_id;
    var folder = messageinfo.message_folder;
    var folderfrom = messageinfo.message_folderfrom;
    var type = messageinfo.message_type;
    
    var user = this.message_user;
    if (user == null) user = {};
    var username = user['name'];
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (folder == 'Trash' && folderfrom != null && folderfrom.length > 0 && folderfrom != folder) {
      messageform.move_message( username, type, folder, id, folderfrom, 
        function() {
          dialog.hide();
          if (listmessage.refresh() == false) 
            sammy.refresh();
        });
    }
  },
  showstream: function()
  {
    var streamid = messageinfo.message_streamid;
    if (streamid == null || streamid.length == 0)
      return;
    
    var context = system.context;
    context.redirect( '#/~messagestream/' + encodeURIComponent(streamid) );
  },
  showchat: function( username )
  {
    if (username == null || username.length == 0)
      return;
    
    var context = system.context;
    context.redirect( '#/~chat/' + encodeURIComponent(username) );
  },
  showconversation: function( groupname )
  {
    if (groupname == null || groupname.length == 0)
      return;
    
    var context = system.context;
    context.redirect( '#/~conversation/' + encodeURIComponent(groupname) );
  },
  reply: function()
  {
    var from = messageinfo.message_from;
    var to = messageinfo.message_to;
    var cc = messageinfo.message_cc;
    var replyto = messageinfo.message_replyto;
    var subject = messageinfo.message_subject;
    var body = messageinfo.message_body;
    var id = messageinfo.message_id;
    var streamid = messageinfo.message_streamid;
    var mtime = messageinfo.message_time;
    
    var user = this.message_user;
    if (user == null) user = {};
    var username = user['name'];
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (from != null && from.length > 0) {
      if (subject == null) subject = '';
      var composeto = from;
      if (replyto != null && replyto.length > 0)
        composeto = replyto;
      
      var composesubject = 'Re: ' + subject;
      var replyid = id;
      if (username != me) replyid = '';
      
      dialog.hide();
      compose.success_cb = listmessage.refresh;
      compose.show( 'mail', composeto, composesubject, null, replyid, streamid );
    }
  },
  replyall: function()
  {
    var from = messageinfo.message_from;
    var to = messageinfo.message_to;
    var cc = messageinfo.message_cc;
    var replyto = messageinfo.message_replyto;
    var subject = messageinfo.message_subject;
    var body = messageinfo.message_body;
    var id = messageinfo.message_id;
    var streamid = messageinfo.message_streamid;
    var mtime = messageinfo.message_time;
    
    var user = this.message_user;
    if (user == null) user = {};
    var username = user['name'];
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (from != null && from.length > 0) {
      if (subject == null) subject = '';
      var composeto = from;
      if (replyto != null && replyto.length > 0)
        composeto = composeto + ', ' + replyto;
      if (to != null && to.length > 0)
        composeto = composeto + ', ' + to;
      if (cc != null && cc.length > 0)
        composeto = composeto + ', ' + cc;
      
      var composesubject = 'Re: ' + subject;
      var replyid = id;
      if (username != me) replyid = '';
      
      dialog.hide();
      compose.success_cb = listmessage.refresh;
      compose.show( 'mail', composeto, composesubject, null, replyid, streamid );
    }
  },
  forward: function()
  {
    var from = messageinfo.message_from;
    var to = messageinfo.message_to;
    var cc = messageinfo.message_cc;
    var replyto = messageinfo.message_replyto;
    var subject = messageinfo.message_subject;
    var body = messageinfo.message_body;
    var attachments = this.message_attachments;
    var id = messageinfo.message_id;
    var streamid = messageinfo.message_streamid;
    var mtime = messageinfo.message_time;
    
    var user = this.message_user;
    if (user == null) user = {};
    var username = user['name'];
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (from != null && from.length > 0) {
      if (subject == null) subject = '';
      if (body == null) body = '';
      
      var fwbody = body.replaceAll('\n','\n> ');
      var msgtime = format_time(mtime);
      var composebody = '' + from + ' wrote at ' + msgtime + ':\r\n' 
        + '> ' + fwbody;
      
      var composesubject = 'Fw: ' + subject;
      var replyid = null; //id;
      if (username != me) replyid = '';
      
      dialog.hide();
      compose.success_cb = listmessage.refresh;
      compose.show( 'mail', '', composesubject, composebody, replyid, streamid, attachments );
    }
  },
  close: function()
  {
    messageinfo.dialog_shown = false;
    dialog.hide();
  },
  buildAddressItem: function( addrs, users )
  {
    var content = []
    
    for (var key in users) {
      var user = users[key];
      if (user == null) continue;
      
      var username = user['name'];
      var type = user['type'];
      var avatar = user['avatar'];
      var nickname = user['nickname'];
      var firstname = user['firstname'];
      var lastname = user['lastname'];
      var title = user['title'];
      
      if (username == null || username.length == 0) continue;
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
      
      var chatbutton = '';
      if (type == 'group') {
        var chattitle = strings( 'Conversation in {0}' ).format(name);
        var chatclick = 'javascript:messageinfo.showconversation(\'' + username + '\');';
        chatbutton = '<button class="player-btn " onclick="' + chatclick + '" title="' + chattitle.esc() + '" style="width: 20px;height: 20px;font-size: 18px;"><i class="glyphicon conversation"></i></button>';
      } else {
        var chattitle = strings( 'Chat with {0}' ).format(name);
        var chatclick = 'javascript:messageinfo.showchat(\'' + username + '\');';
        chatbutton = '<button class="player-btn " onclick="' + chatclick + '" title="' + chattitle.esc() + '" style="width: 20px;height: 20px;font-size: 18px;"><i class="glyphicon chat"></i></button>';
      }
      
      var item = '<a href="" onClick="javascript:userinfo.showdetails(\'' + username + '\');return false;">' + name + '</a>' + chatbutton;
      content.push( item );
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
  get_iconclass: function( type, folder, status, streamid )
  {
    var iconclass = 'message-plus';
    
    if (type == 'notice' || type == 'notification') {
      iconclass = 'bullhorn';
      //if (folder == 'System')
      //  iconclass = 'cogwheels';
    } else if (type == 'chat' || type == 'conversation') {
      iconclass = 'chat';
    } else if (streamid != null && streamid.length > 0) {
      iconclass = 'chat';
    } else {
      if (status == 'new')
        iconclass = 'message-new';
      else if (status == 'deleted')
        iconclass = 'message-ban';
      else if (status == 'draft')
        iconclass = 'message-new';
      else if (status == 'starred')
        iconclass = 'message-flag';
      else if (status == 'queued')
        iconclass = 'message-new';
      else if (status == 'sending')
        iconclass = 'message-new';
      else if (status == 'sent')
        iconclass = 'message-minus';
      else if (status == 'failed')
        iconclass = 'message-ban';
      else if (status == 'replied')
        iconclass = 'message-plus';
      else if (status == 'read')
        iconclass = 'message-plus';
    }
    
    return 'glyphicon ' + iconclass;
  },
  init_message: function()
  {
    var type = this.message_type;
    var folder = this.message_folder;
    var status = this.message_status;
    var from = this.message_from;
    var to = this.message_to;
    var subject = this.message_subject;
    var body = this.message_body;
    var attachments = this.message_attachments;
    var mtime = this.message_time;
    var streamid = this.message_streamid;
    var tousers = this.message_tousers;
    
    var user = this.message_userfrom;
    if (user == null) user = {};
    var avatar = user['avatar'];
    var nickname = user['nickname'];
    var firstname = user['firstname'];
    var lastname = user['lastname'];
    var usertitle = user['title'];
    
    if (from == null) from = '';
    if (subject == null) subject = '';
    if (body == null) body = '';
    if (avatar == null) avatar = '';
    if (nickname == null) nickname = '';
    if (firstname == null) firstname = '';
    if (lastname == null) lastname = '';
    if (usertitle == null) usertitle = '';
    if (streamid == null) streamid = '';
    if (mtime == null || mtime < 0) mtime = 0;
    
    var username = from;
    var name = nickname;
    var msgtime = format_time(mtime);
    
    if (name == null || name.length == 0)
      name = usertitle;
    if (name == null || name.length == 0) {
      if (firstname != null && firstname.length > 0) {
        name = firstname;
        if (lastname != null && lastname.length > 0)
          name += ' ' + lastname; 
      }
    }
    if (name == null || name.length == 0)
      name = username;
    
    if (subject == null || subject.length == 0)
      subject = strings( '[No Subject]' );
    
    var iconclass = messageinfo.get_iconclass(type, folder, status);
    if (iconclass == null || iconclass.length == 0)
      iconclass = 'glyphicon message-plus';
    
    if (type == 'notice' || type == 'notification') {
      $( '#messageinfo-icon' ).attr( 'class', iconclass );
      $( '#messageinfo-title' ).html( strings( 'Notification' ) );
    } else {
      $( '#messageinfo-icon' ).attr( 'class', iconclass );
    }
    
    var tolist = '<span class="metadata-label">' + strings( 'To' ) + '</span>' 
        + this.buildAddressItem(to, tousers);
    
    var timelist = '<span class="metadata-label">' + strings( 'Time' ) + '</span>' 
        + '<a>' + msgtime + '</a>';
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    if (avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_128t.' + extension + '?token=' + app.token;
    }
    
    var streamtitle = strings( 'Subject Conversation' );
    var streamclick = 'javascript:messageinfo.showstream(\'' + streamid + '\');';
    var streambutton = '<button class="player-btn " onclick="' + streamclick + '" title="' + streamtitle.esc() + '"><i class="glyphicon chat"></i></button>';
    if (streamid == null || streamid.length == 0)
      streambutton = '';
    if (type == 'notice' || type == 'notification')
      streambutton = '';
    
    var subjecthtml = subject.esc() + ' ' + streambutton;
    var bodyhtml = body;
    
    $( '#messageinfo-from-avatar' )
      .attr( 'style', 'width: 96px;height: 96px;' )
      .attr( 'src', thumbsrc );
    
    $( '#messageinfo-from-link' )
      .attr( 'onClick', 'javascript:userinfo.showdetails(\'' + username + '\');return false;' )
      .attr( 'href', '' );
    
    $( '#messageinfo-from-name' ).html( name.esc() );
    $( '#messageinfo-to-list' ).html( tolist );
    $( '#messageinfo-time-list' ).html( timelist );
    $( '#messageinfo-subject' ).html( subjecthtml );
    $( '#messageinfo-body' ).html( bodyhtml );
    
    this.init_attachments( attachments );
  },
  init_attachments: function( attachments )
  {
    if (attachments == null) attachments = {};
    
    var content = [];
    var attcount = 0;
    
    if (attachments) {
      for (var key in attachments) {
        var section = attachments[key];
        if (section == null) continue;
      
        var sec_id = section['id'];
        if (sec_id != null && sec_id.length > 0)
          attcount ++;
      }
    }
    
    if (attachments) {
      for (var key in attachments) {
        var section = attachments[key];
        if (section == null) continue;
      
        var item = this.buildAttachment( section, attcount );
        if (item != null) 
          content.push( item );
      }
    }
    
    $( '#messageinfo-attachment-list' ).html( content.join("\n") );
    
    if (content.length > 0) {
      $( '#messageinfo-attachments' ).removeClass( 'hide' );
    }
    
    if (content.length > 3) {
      $( '#messageinfo-container' ).attr( 'style', 'width: 710px;' );
    }
  },
  click_attachment: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    var attachments = this.message_attachments;
    if (attachments == null) return;
    
    for (var akey in attachments) {
      var att = attachments[akey];
      if (att == null) continue;
      
      var att_id = att['id'];
      if (att_id == id) {
        fileinfo.showdetails( att );
        return;
      }
    }
  },
  buildAttachment: function( section, attcount )
  {
    if (section == null) return null;
    
    var sec_id = section['id'];
    var sec_name = section['name'];
    var contentType = section['type'];
    var extension = section['extname'];
    var poster = section['poster'];
    var path = section['path'];
    var isfolder = section['isfolder'];
    //var ctime = section['ctime'];
    var mtime = section['mtime'];
    //var indextime = section['itime'];
    var width = section['width'];
    var height = section['height'];
    var timelen = section['timelen'];
    var length = section['length'];
    var subcount = section['subcount'];
    var sublength = section['sublen'];
    
    if (sec_id == null || sec_id.length == 0)
      return null;
    
    if (extension == null && path != null) { 
      var pos = path.lastIndexOf('.');
      if (pos >= 0) 
        extension = path.substring(pos+1);
    }

    if (sec_name == null) sec_name = '';
    if (contentType == null) contentType = '';
    if (extension == null || extension.length == 0) extension = 'dat';
    if (isfolder == null) isfolder = false;
    
    var titleText = fileinfo.getdisplayname( sec_name, contentType );
    var lengthText = readableBytes2(length);
    
    var thumbsrc = null;
    var openlink = null;
    
    var buttonicon = 'glyphicon file';
    var imgstyle = 'style="width: 128px;"';
    
    if (attcount < 4)
      imgstyle = 'style="width: 150px;"';
    
    if (contentType.indexOf('image/') == 0) {
      thumbsrc = app.base_path + '/image/' + sec_id + '_192t.' + extension + '?token=' + app.token;
      openlink = app.base_path + '/image/' + sec_id + '_0.' + extension + '?token=' + app.token;
      
      if (attcount == 1) {
        thumbsrc = app.base_path + '/image/' + sec_id + '_256.' + extension + '?token=' + app.token;
        imgstyle = 'style="width: 100%;"';
      }
      
      lengthText = '' + width + ' x ' + height;
      buttonicon = 'glyphicon picture';
      
    } else if (contentType.indexOf('audio/') == 0) { 
      thumbsrc = 'css/' + app.theme + '/images/posters/music.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      
      buttonicon = 'glyphicon music';
      
    } else if (contentType.indexOf('video/') == 0) { 
      thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      
      buttonicon = 'glyphicon film';
      
    } else if (contentType.indexOf('text/') == 0) { 
      thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
      
      buttonicon = 'glyphicon notes';
      
    } else {
      thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
      openlink = app.base_path + '/file/' + sec_id + '.' + extension + '?token=' + app.token;
    }
    
    if (isfolder) {
      openlink = '#/~browse/' + key;
      thumbsrc = 'css/' + app.theme + '/images/posters/folder.png';
      
      buttonicon = 'glyphicon file';
    }

    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + imgid + '_192t.' + imgext + '?token=' + app.token;
    }
    
    if (thumbsrc == null || thumbsrc.length == 0)
      return null;
    
    if (titleText == null) titleText = '';
    if (lengthText != null && lengthText.length > 0) 
      titleText = titleText + ' (' + lengthText + ')';
    
    var clickto = 'javascript:messageinfo.click_attachment(\'' + sec_id + '\');return false;';
    
    var buttonhide = '';
    if (buttonicon == null || buttonicon.length == 0)
      buttonhide = 'hide';
    
    var item = 
        '<span>' + "\n" +
        '  <a class="artwork-option" data-rating-key="" data-focus="keyboard" style="margin-left: 0px;margin-bottom: 5px;" href="">' + "\n" +
        '    <img class="poster poster-poster" src="' + thumbsrc.esc() + '" onClick="' + clickto + '" title="' + titleText.esc() + '" ' + imgstyle + '/>' + "\n" +
        '  </a>' + "\n" +
        '  <button type="button" class="hover-menu-btn poster-info-btn ' + buttonhide + '" style="margin-left: -40px;margin-top: 5px;padding: 1px 8px 3px;"><i class="' + buttonicon + '"></i></button>' + "\n" +
        '</span>';
    
    return item;
  },
  showactions: function()
  {
    var type = this.message_type;
    var folder = this.message_folder;
    var status = this.message_status;
    
    $( '#messageinfo-details-show' ).addClass( 'hide' );
    $( '#messageinfo-details-hide' ).removeClass( 'hide' );
    
    if (type == 'notice' || type == 'notification') {
      $( '#messageinfo-details-show' ).addClass( 'hide' );
      $( '#messageinfo-details-hide' ).addClass( 'hide' );
      $( '#messageinfo-reply' ).addClass( 'hide' );
      $( '#messageinfo-replyall' ).addClass( 'hide' );
      $( '#messageinfo-forward' ).addClass( 'hide' );
      $( '#messageinfo-delete' ).removeClass( 'hide' );
    } else {
      $( '#messageinfo-reply' ).removeClass( 'hide' );
      $( '#messageinfo-replyall' ).removeClass( 'hide' );
      $( '#messageinfo-forward' ).removeClass( 'hide' );
      $( '#messageinfo-delete' ).removeClass( 'hide' );
    }
  },
  hideactions: function()
  {
    var type = this.message_type;
    var folder = this.message_folder;
    var folderfrom = this.message_folderfrom;
    var status = this.message_status;
    
    $( '#messageinfo-details-show' ).removeClass( 'hide' );
    $( '#messageinfo-details-hide' ).addClass( 'hide' );
    
    if (type == 'notice' || type == 'notification') {
      $( '#messageinfo-details-show' ).addClass( 'hide' );
      $( '#messageinfo-details-hide' ).addClass( 'hide' );
      $( '#messageinfo-reply' ).addClass( 'hide' );
      $( '#messageinfo-replyall' ).addClass( 'hide' );
      $( '#messageinfo-forward' ).addClass( 'hide' );
      $( '#messageinfo-delete' ).removeClass( 'hide' );
    } else {
      $( '#messageinfo-reply' ).removeClass( 'hide' );
      $( '#messageinfo-replyall' ).addClass( 'hide' );
      $( '#messageinfo-forward' ).addClass( 'hide' );
      $( '#messageinfo-delete' ).removeClass( 'hide' );
    }
    
    if (folder == 'Trash') {
      $( '#messageinfo-details-show' ).addClass( 'hide' );
      $( '#messageinfo-details-hide' ).addClass( 'hide' );
      
      $( '#messageinfo-reply' ).removeClass( 'hide' );
      $( '#messageinfo-replyall' ).addClass( 'hide' );
      $( '#messageinfo-forward' ).addClass( 'hide' );
      $( '#messageinfo-delete' ).removeClass( 'hide' );
      
      $( '#messageinfo-reply' )
        .attr( 'onclick', 'javascript:messageinfo.restore();return false;' )
        .attr( 'title', strings( 'Restore to' ) + ' ' + strings( folderfrom ) )
        .addClass( 'btn-success' )
        .html( strings( 'Restore' ) );
    }
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/messageinfo.html',
      function( template )
      {
        messageinfo.messageinfo_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#messageinfo-title' ).html( strings( 'Message' ) );
            $( '#messageinfo-reply' ).html( strings( 'Reply' ) );
            $( '#messageinfo-replyall' ).html( strings( 'Reply All' ) );
            $( '#messageinfo-forward' ).html( strings( 'Forward' ) );
            $( '#messageinfo-delete' ).html( strings( 'Delete' ) );
            $( '#messageinfo-no' ).html( strings( 'Close' ) );
            
            $( '#messageinfo-details-show' ).html( strings( 'Show More Actions' ) );
            $( '#messageinfo-details-hide' ).html( strings( 'Hide More Actions' ) );
            
            messageinfo.init_message();
            
            $( '#messageinfo-details-show' )
              .attr( 'onclick', 'javascript:messageinfo.showactions();return false;' )
              .attr( 'href', '' );
            
            $( '#messageinfo-details-hide' )
              .attr( 'onclick', 'javascript:messageinfo.hideactions();return false;' )
              .attr( 'href', '' );
            
            $( '#messageinfo-reply' )
              .attr( 'onclick', 'javascript:messageinfo.reply();return false;' );
            
            $( '#messageinfo-replyall' )
              .attr( 'onclick', 'javascript:messageinfo.replyall();return false;' );
            
            $( '#messageinfo-forward' )
              .attr( 'onclick', 'javascript:messageinfo.forward();return false;' );
            
            $( '#messageinfo-delete' )
              .attr( 'onclick', 'javascript:messageinfo.delete();return false;' );
            
            $( '#messageinfo-no' )
              .attr( 'onclick', 'javascript:messageinfo.close();return false;' );
            
            $( '#messageinfo-close' )
              .attr( 'onclick', 'javascript:messageinfo.close();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            $( '#messageinfo-next' )
              .attr( 'title', strings( 'Next Message' ) );
            
            $( '#messageinfo-prev' )
              .attr( 'title', strings( 'Previous Message' ) );
            
            messageinfo.hideactions();
            messageinfo.dialog_shown = true;
            
            var cb = messageinfo.showcb;
            if (cb) cb.call(messageinfo);
          },
          hidecb: function()
          {
            messageinfo.dialog_shown = false;
          },
          shown: false
        };
      }
    );
  }
};
