
var publishinfo_dialogs = { 
  delete_confirm_dialog: null,
  trash_confirm_dialog: null,
  
  init_message: function( dialog_element, template ) 
  {
    publishinfo_dialogs.delete_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Item' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon warning-sign' );
        
        var msg = strings( 'Are you sure you want to remove this item?' );
        if (msg == null) msg = "";
        
        $( '#message-text' ).html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:publishform.delete_submit();return false;' )
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
    
    publishinfo_dialogs.trash_confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Delete Item' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon circle-question-mark' );
        
        var msg = strings( 'Are you sure you want to move this item to trash?' );
        if (msg == null) msg = "";
        
        var optionchecked = '';
        if (publishform.trash_prompt == false) optionchecked = 'checked';
        var optiontitle = strings( 'Don\'t prompt' );
        var optionclickto = 'javascript:return publishform.trash_noprompt(this);';
        var optionhtml = '<label class="checkbox" for="input_noprompt" style="font-size: 14px;"><input id="input_noprompt" type="checkbox" class="input-dark" name="noprompt" value="true" onclick="' + optionclickto + '" ' + optionchecked + '>&nbsp;' + optiontitle.esc() + '</label>';
        
        $( '#message-text' ).html( msg.esc() );
        $( '#message-options' ).html( optionhtml );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:publishform.trash_submit();return false;' );
        
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

var publishform = {
  username: null,
  publishid: null,
  type: null,
  channel: null,
  channelto: null,
  successcb: null,
  trash_prompt: true,
  
  action_submit: function( username, type, channel, publishid, action, flag, channelto )
  {
    if (type == null || publishid == null || action == null) return;
    if (username == null) username = '';
    if (channel == null) channel = '';
    if (flag == null) flag = '';
    if (channelto == null) channelto = '';
    
    this.username = username;
    this.publishid = publishid;
    this.type = type;
    this.channel = channel;
    this.channelto = channelto;
    
    var params = '&action=' + encodeURIComponent(action) + 
        '&username=' + encodeURIComponent(username) +
        '&type=' + encodeURIComponent(type) +
        '&channel=' + encodeURIComponent(channel) +
        '&publishid=' + encodeURIComponent(publishid) +
        '&flag=' + encodeURIComponent(flag) +
        '&channelto=' + encodeURIComponent(channelto);
    
    $.ajax
    (
      {
        url : app.user_path + '/publish?token=' + app.token + params + '&wt=json',
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
            var cb = publishform.successcb;
            publishform.successcb = null;
            if (cb) cb.call(publishform);
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
  setflag_item: function( username, type, channel, publishid, flag, successcb )
  {
    if (type == null || publishid == null || publishid.length == 0)
      return;
    
    this.username = username;
    this.publishid = publishid;
    this.type = type;
    this.channel = channel;
    this.successcb = successcb;
    
    this.action_submit( username, type, channel, publishid, 'setflag', flag );
  },
  move_item: function( username, type, channel, publishid, channelto, successcb )
  {
    if (type == null || publishid == null || channelto == null)
      return;
    
    this.username = username;
    this.publishid = publishid;
    this.type = type;
    this.channel = channel;
    this.channelto = channelto;
    this.successcb = successcb;
    
    this.action_submit( username, type, channel, publishid, 'move', null, channelto );
  },
  trash_item: function( username, type, channel, publishid, successcb )
  {
    if (type == null || publishid == null|| publishid.length == 0)
      return;
    
    this.username = username;
    this.publishid = publishid;
    this.type = type;
    this.channel = channel;
    this.successcb = successcb;
    
    if (publishform.trash_prompt == true)
      dialog.show( publishinfo_dialogs.trash_confirm_dialog );
    else
      this.trash_submit();
  },
  trash_noprompt: function( elem )
  {
    if (elem && elem.checked) {
      publishform.trash_prompt = false;
      return true;
    } else {
      publishform.trash_prompt = true;
      return true;
    }
  },
  trash_submit: function()
  {
    dialog.hide();
    
    var username = this.username;
    var publishid = this.publishid;
    var type = this.type;
    var channel = this.channel;
    
    if (type == null || publishid == null|| publishid.length == 0)
      return;
    
    this.action_submit( username, type, channel, publishid, 'trash' );
  },
  delete_item: function( username, type, channel, publishid, successcb )
  {
    if (type == null || publishid == null || publishid.length == 0)
      return;
    
    this.username = username;
    this.publishid = publishid;
    this.type = type;
    this.channel = channel;
    this.successcb = successcb;
    
    dialog.show( publishinfo_dialogs.delete_confirm_dialog );
  },
  delete_submit: function()
  {
    dialog.hide();
    
    var username = this.username;
    var publishid = this.publishid;
    var type = this.type;
    var channel = this.channel;
    
    if (type == null || publishid == null || publishid.length == 0)
      return;
    
    this.action_submit( username, type, channel, publishid, 'delete' );
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var publishinfo = {
  publishinfo_dialog: null,
  publish_username: null,
  publish_type: null,
  publish_channel: null,
  publish_channelfrom: null,
  publish_id: null,
  publish_subject: null,
  publish_body: null,
  publish_attachments: null,
  publish_time: null,
  publish_status: null,
  publish_owner: null,
  publish_userowner: null,
  publish_streamid: null,
  dialog_shown: false,
  showcb: null,
  
  showinfo: function( username, type, channel, id, showcb )
  {
    if (username == null || type == null || channel == null || id == null)
      return;
    
    if (this.dialog_shown == true)
      this.close();
    
    this.publish_username = username;
    this.publish_type = type;
    this.publish_channel = channel;
    this.publish_channelfrom = null;
    this.publish_id = id;
    this.publish_subject = null;
    this.publish_body = null;
    this.publish_attachments = null;
    this.publish_owner = null;
    this.publish_userowner = null;
    this.publish_status = null;
    this.publish_streamid = null;
    this.publish_time = 0;
    this.showcb = showcb;
    
    var params = '&action=get' 
        + '&username=' + encodeURIComponent(username) 
        + '&type=' + encodeURIComponent(type) 
        + '&channel=' + encodeURIComponent(channel) 
        + '&publishid=' + encodeURIComponent(id);
    
    $.ajax
    (
      {
        url : app.user_path + '/publish?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var publication = response['publication'];
          if (publication) {
            publishinfo.publish_username = publication['username'];
            publishinfo.publish_subject = publication['subject'];
            publishinfo.publish_body = publication['body'];
            publishinfo.publish_attachments = publication['attachments'];
            publishinfo.publish_id = publication['id'];
            publishinfo.publish_streamid = publication['streamid'];
            publishinfo.publish_channel = publication['channel'];
            publishinfo.publish_channelfrom = publication['channelfrom'];
            publishinfo.publish_type = publication['stype'];
            publishinfo.publish_owner = publication['owner'];
            publishinfo.publish_userowner = publication['userowner'];
            publishinfo.publish_status = publication['status'];
            publishinfo.publish_time = publication['ptime'];
            dialog.show( publishinfo.publishinfo_dialog );
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
    var id = publishinfo.publish_id;
    var channel = publishinfo.publish_channel;
    var type = publishinfo.publish_type;
    
    var username = publishinfo.publish_username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (channel == 'Trash') {
      publishform.delete_item( username, type, channel, id, 
        function() {
          dialog.hide();
          if (listpublication.refresh() == false) 
            sammy.refresh();
        });
    } else {
      publishform.trash_item( username, type, channel, id, 
        function() {
          dialog.hide();
          if (listpublication.refresh() == false) 
            sammy.refresh();
        });
    }
  },
  restore: function()
  {
    var id = publishinfo.publish_id;
    var channel = publishinfo.publish_channel;
    var channelfrom = publishinfo.publish_channelfrom;
    var type = publishinfo.publish_type;
    
    var username = publishinfo.publish_username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (channel == 'Trash' && channelfrom != null && channelfrom.length > 0 && channelfrom != channel) {
      publishform.move_item( username, type, channel, id, channelfrom, 
        function() {
          dialog.hide();
          if (listpublication.refresh() == false) 
            sammy.refresh();
        });
    }
  },
  showstream: function()
  {
    var stype = publishinfo.publish_type;
    var streamid = publishinfo.publish_streamid;
    if (streamid == null || streamid.length == 0)
      return;
    
    var username = publishinfo.publish_username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    var name = null;
    if (stype == 'post') {
      name = 'posts';
    } else if (stype == 'subscription') {
      name = 'subscriptions';
    } else if (stype == 'comment') {
      name = 'comments';
    }
    
    if (name != null && name.length > 0) {
      var context = system.context;
      var location = '#/~' + name + '/' + encodeURIComponent(username) + '/' + encodeURIComponent(streamid);
      context.redirect( location );
    }
  },
  reply: function()
  {
    var type = publishinfo.publish_type;
    var channel = publishinfo.publish_channel;
    var subject = publishinfo.publish_subject;
    var body = publishinfo.publish_body;
    var id = publishinfo.publish_id;
    var streamid = publishinfo.publish_streamid;
    var ptime = publishinfo.publish_time;
    var owner = publishinfo.publish_owner;
    
    var username = publishinfo.publish_username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (owner != null && owner.length > 0) {
      if (subject == null) subject = '';
      var resubject = 'Re: ' + subject;
      var replyid = id;
      //if (username != me) replyid = '';
      
      dialog.hide();
      publish.success_cb = listpublication.refresh;
      publish.show( username, type, channel, resubject, null, replyid, streamid );
    }
  },
  close: function()
  {
    publishinfo.dialog_shown = false;
    dialog.hide();
  },
  get_iconclass: function( type, channel, status, streamid )
  {
    var iconclass = 'globe';
    
    if (type == 'post') {
      iconclass = 'globe';
    } else if (type == 'subscription') {
      iconclass = 'rss';
    } else if (type == 'comment') {
      iconclass = 'comments';
    } else {
      iconclass = 'globe';
    }
    
    return 'glyphicon ' + iconclass;
  },
  init_item: function()
  {
    var type = this.publish_type;
    var channel = this.publish_channel;
    var status = this.publish_status;
    var subject = this.publish_subject;
    var body = this.publish_body;
    var attachments = this.publish_attachments;
    var ptime = this.publish_time;
    var streamid = this.publish_streamid;
    var owner = this.publish_owner;
    
    var user = this.publish_userowner;
    if (user == null) user = {};
    var avatar = user['avatar'];
    var nickname = user['nickname'];
    var firstname = user['firstname'];
    var lastname = user['lastname'];
    var usertitle = user['title'];
    
    if (subject == null) subject = '';
    if (body == null) body = '';
    if (avatar == null) avatar = '';
    if (nickname == null) nickname = '';
    if (firstname == null) firstname = '';
    if (lastname == null) lastname = '';
    if (usertitle == null) usertitle = '';
    if (streamid == null) streamid = '';
    if (ptime == null || ptime < 0) ptime = 0;
    
    var username = owner;
    var name = nickname;
    var msgtime = format_time(ptime);
    
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
    
    var iconclass = publishinfo.get_iconclass(type, channel, status);
    if (iconclass == null || iconclass.length == 0)
      iconclass = 'glyphicon message-plus';
    
    $( '#publishinfo-icon' ).attr( 'class', iconclass );
    
    var timelist = '<span class="metadata-label">' + strings( 'Time' ) + '</span>' 
        + '<a>' + msgtime + '</a>';
    
    var thumbsrc = 'css/' + app.theme + '/images/posters/friend.png';
    if (avatar != null && avatar.length > 0) {
      var id = avatar;
      var extension = 'jpg';
      
      thumbsrc = app.base_path + '/image/' + id + '_128t.' + extension + '?token=' + app.token;
    }
    
    var streamtitle = strings( 'Stream Group' );
    var streamclick = 'javascript:publishinfo.showstream(\'' + streamid + '\');';
    var streambutton = '<button class="player-btn " onclick="' + streamclick + '" title="' + streamtitle.esc() + '"><i class="glyphicon chat"></i></button>';
    if (streamid == null || streamid.length == 0)
      streambutton = '';
    
    var subjecthtml = subject.esc() + ' ' + streambutton;
    var bodyhtml = body;
    
    $( '#publishinfo-owner-avatar' )
      .attr( 'style', 'width: 96px;height: 96px;' )
      .attr( 'src', thumbsrc );
    
    $( '#publishinfo-owner-link' )
      .attr( 'onClick', 'javascript:userinfo.showdetails(\'' + username + '\');return false;' )
      .attr( 'href', '' );
    
    $( '#publishinfo-owner-name' ).html( name.esc() );
    $( '#publishinfo-time-list' ).html( timelist );
    $( '#publishinfo-subject' ).html( subjecthtml );
    $( '#publishinfo-body' ).html( bodyhtml );
    
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
    
    $( '#publishinfo-attachment-list' ).html( content.join("\n") );
    
    if (content.length > 0) {
      $( '#publishinfo-attachments' ).removeClass( 'hide' );
    }
    
    if (content.length > 3) {
      $( '#publishinfo-container' ).attr( 'style', 'width: 710px;' );
    }
  },
  click_attachment: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    var attachments = this.publish_attachments;
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
    
    var clickto = 'javascript:publishinfo.click_attachment(\'' + sec_id + '\');return false;';
    
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
    var type = this.publish_type;
    var channel = this.publish_channel;
    var status = this.publish_status;
    
    $( '#publishinfo-details-show' ).addClass( 'hide' );
    $( '#publishinfo-details-hide' ).removeClass( 'hide' );
    
    if (type == 'notice' || type == 'notification') {
      $( '#publishinfo-details-show' ).addClass( 'hide' );
      $( '#publishinfo-details-hide' ).addClass( 'hide' );
      $( '#publishinfo-reply' ).addClass( 'hide' );
      $( '#publishinfo-delete' ).removeClass( 'hide' );
    } else {
      $( '#publishinfo-reply' ).removeClass( 'hide' );
      $( '#publishinfo-delete' ).removeClass( 'hide' );
    }
  },
  hideactions: function()
  {
    var type = this.publish_type;
    var channel = this.publish_channel;
    var channelfrom = this.publish_channelfrom;
    var status = this.publish_status;
    
    $( '#publishinfo-details-show' ).removeClass( 'hide' );
    $( '#publishinfo-details-hide' ).addClass( 'hide' );
    
    if (type == 'notice' || type == 'notification') {
      $( '#publishinfo-details-show' ).addClass( 'hide' );
      $( '#publishinfo-details-hide' ).addClass( 'hide' );
      $( '#publishinfo-reply' ).addClass( 'hide' );
      $( '#publishinfo-delete' ).removeClass( 'hide' );
    } else {
      $( '#publishinfo-reply' ).removeClass( 'hide' );
      $( '#publishinfo-delete' ).removeClass( 'hide' );
    }
    
    if (channel == 'Trash') {
      $( '#publishinfo-details-show' ).addClass( 'hide' );
      $( '#publishinfo-details-hide' ).addClass( 'hide' );
      
      $( '#publishinfo-reply' ).removeClass( 'hide' );
      $( '#publishinfo-delete' ).removeClass( 'hide' );
      
      $( '#publishinfo-reply' )
        .attr( 'onclick', 'javascript:publishinfo.restore();return false;' )
        .attr( 'title', strings( 'Restore to' ) + ' ' + strings( channelfrom ) )
        .addClass( 'btn-success' )
        .html( strings( 'Restore' ) );
    }
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/publishinfo.html',
      function( template )
      {
        publishinfo.publishinfo_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#publishinfo-title' ).html( strings( 'Post' ) );
            $( '#publishinfo-reply' ).html( strings( 'Reply' ) );
            $( '#publishinfo-delete' ).html( strings( 'Delete' ) );
            $( '#publishinfo-no' ).html( strings( 'Close' ) );
            
            $( '#publishinfo-details-show' ).html( strings( 'Show More Actions' ) );
            $( '#publishinfo-details-hide' ).html( strings( 'Hide More Actions' ) );
            
            var type = publishinfo.publish_type;
            if (type == 'post') {
              $( '#publishinfo-title' ).html( strings( 'Post' ) );
            } else if (type == 'subscription') {
              $( '#publishinfo-title' ).html( strings( 'Subscription' ) );
            } else if (type == 'comment') {
              $( '#publishinfo-title' ).html( strings( 'Comment' ) );
            }
            publishinfo.init_item();
            
            $( '#publishinfo-details-show' )
              .attr( 'onclick', 'javascript:publishinfo.showactions();return false;' )
              .attr( 'href', '' );
            
            $( '#publishinfo-details-hide' )
              .attr( 'onclick', 'javascript:publishinfo.hideactions();return false;' )
              .attr( 'href', '' );
            
            $( '#publishinfo-reply' )
              .attr( 'onclick', 'javascript:publishinfo.reply();return false;' );
            
            $( '#publishinfo-delete' )
              .attr( 'onclick', 'javascript:publishinfo.delete();return false;' );
            
            $( '#publishinfo-no' )
              .attr( 'onclick', 'javascript:publishinfo.close();return false;' );
            
            $( '#publishinfo-close' )
              .attr( 'onclick', 'javascript:publishinfo.close();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            $( '#publishinfo-next' )
              .attr( 'title', strings( 'Next Item' ) );
            
            $( '#publishinfo-prev' )
              .attr( 'title', strings( 'Previous Item' ) );
            
            publishinfo.hideactions();
            publishinfo.dialog_shown = true;
            
            var cb = publishinfo.showcb;
            if (cb) cb.call(publishinfo);
          },
          hidecb: function()
          {
            publishinfo.dialog_shown = false;
          },
          shown: false
        };
      }
    );
  }
};
