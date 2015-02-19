
var publish_tags = {
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
    $( '#publish-tags-autogen' )
      .attr( 'onClick', 'javascript:publish_tags.onfocus();' );
    
    var values = this.values;
    if (values == null) values = [];
    
    var valueContent = [];
    var valueStr = '';
    
    var titleText = strings( 'Tags' );
    var titleItem = '<li class="select2-search-field"><label id="publish-tags-text" style="color: #888;padding-left: 5px;padding-right: 5px;margin-top: 5px;">' + titleText + '</label></li>';
    valueContent.push( titleItem );
    
    for (var key in values) {
      var value = values[key];
      if (value == null || value.length == 0) 
        continue;
      
      if (valueStr.length > 0) valueStr += ',';
      valueStr += value;
      
      var clickto = 'javascript:publish_tags.remove_value(' + key + ');return false;';
      var item = '<li class="select2-search-choice"><div>' + value.esc() + '</div><a onclick="' + clickto + '" class="select2-search-choice-close" tabindex="-1"></a></li>';
      
      valueContent.push( item );
    }
    
    var keydown = 'javascript:return publish_tags.input_keydown(this,event);';
    var keyup = 'javascript:publish_tags.input_keyup(this,event);';
    var focus = 'javascript:publish_tags.input_focus(this,event);';
    var blur = 'javascript:publish_tags.input_blur(this,event);';
    
    var item = '<li class="select2-search-field"><input id="publish-tags-selectinput" type="text" autocomplete="off" class="select2-input" style="width: 10px;" onKeyDown="' + keydown + '" onKeyUp="' + keyup + '" onFocus="' + focus + '" onBlur="' + blur + '"></li>';
    
    valueContent.push( item );
    
    $( '#publish-tags-list' ).html( valueContent.join( '\n' ) );
    $( '#publish-tags-input' ).attr( 'value', valueStr.esc() );
    
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
    $( '#publish-tags-autogen' )
      .addClass( 'select2-container-active' );
    $( '#publish-tags-selectinput' )
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
    
    $( '#publish-tags-selectinput' ).attr( 'style', 'width: ' + width + 'px;' );
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
    
    $( '#publish-tags-autogen' )
      .removeClass( 'select2-container-active' );
    $( '#publish-tags-selectinput' )
      .removeClass( 'select2-focused' );
  }
};

var publish = {
  publish_dialog: null,
  publish_username: null,
  publish_tags: null,
  publish_subject: null,
  publish_body: null,
  publish_id: null,
  publish_replyid: null,
  publish_streamid: null,
  publish_type: null,
  publish_channel: null,
  publish_attachments: null,
  publish_attachids: null,
  attachments: null,
  attbuttonicon: null,
  channel_selects: null,
  selectlist: null,
  selectelement: null,
  success_cb: null,
  failed_cb: null,
  submit_success: false,

  show: function( username, type, channel, subject, body, replyid, streamid, attachments, tags )
  {
    this.publish_username = username;
    this.publish_tags = tags;
    this.publish_subject = subject;
    this.publish_body = body;
    this.publish_id = null;
    this.publish_replyid = replyid;
    this.publish_streamid = streamid;
    this.publish_type = type;
    this.publish_channel = channel;
    this.publish_attachments = attachments;
    this.publish_attachids = null;
    this.attachments = null;
    dialog.show( publish.publish_dialog );
  },
  attachfile: function()
  {
    var emptysrc = null;
    var suffix = '';
    var accept = '*';
    
    artwork.showselect('Public Files', function( section ) {
          if (section) {
            publish.add_attachment( section );
          }
        }, emptysrc, suffix, accept);
  },
  init_attachments: function( attachments )
  {
    if (attachments == null) attachments = {};
    
    this.attachments = [];
    
    for (var akey in attachments) {
      var section = attachments[akey];
      if (section == null) continue;
      
      var id = section['id'];
      if (id == null || id.length == 0)
        continue;
      
      this.attachments.push( section );
    }
    
    this.relist_attachments();
    this.publish_attachids = $( '#publish-attachments-input' ).val();
  },
  add_attachment: function( section )
  {
    if (section == null) return;
    var id = section['id'];
    if (id == null || id.length == 0)
      return;
    
    if (this.attachments == null)
      this.attachments = [];
    
    for (var akey in this.attachments) {
      var att = this.attachments[akey];
      if (att == null) continue;
      
      var att_id = att['id'];
      if (att_id == id) return;
    }
    
    this.attachments.push( section );
    this.relist_attachments();
  },
  remove_attachment: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    if (this.attachments == null)
      this.attachments = [];
    
    for (var key=0; key < this.attachments.length; key++) {
      var att = this.attachments[key];
      if (att == null) continue;
      
      var att_id = att['id'];
      if (att_id == id) {
        this.attachments.splice(key,1);
        this.relist_attachments();
        return;
      }
    }
  },
  relist_attachments: function() 
  {
    if (this.attachments == null)
      this.attachments = [];
    
    var content = [];
    var idlist = [];
    
    for (var akey in this.attachments) {
      var att = this.attachments[akey];
      if (att == null) continue;
      
      var att_id = att['id'];
      if (att_id == null || att_id.length == 0)
        continue;
      
      var item = this.buildAttachment( att );
      if (item) { 
        content.push( item );
        idlist.push( att_id );
      }
    }
    
    if (content.length > 0) {
      $( '#publish-attachments-input' ).attr( 'value', idlist.join(",") );
      $( '#publish-attachment-list' ).html( content.join("\n") );
      
      this.relist_body(true);
      $( '#publish-attachments' ).removeClass( 'hide' );
      
    } else {
      $( '#publish-attachments-input' ).attr( 'value', '' );
      $( '#publish-attachment-list' ).html( '' );
      
      this.relist_body(false);
      $( '#publish-attachments' ).addClass( 'hide' );
    }
  },
  click_attachment: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    if (this.attachments == null)
      this.attachments = [];
    
    for (var akey in this.attachments) {
      var att = this.attachments[akey];
      if (att == null) continue;
      
      var att_id = att['id'];
      if (att_id == id) {
        fileinfo.showdetails( att );
        return;
      }
    }
  },
  click_attbutton: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    publish.remove_attachment( id );
  },
  on_mouseover: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    var button_element = $( '#' + id + '-button' );
    var buttonicon_element = $( '#' + id + '-buttonicon' );
    
    publish.attbuttonicon = buttonicon_element.attr( 'class' );
    buttonicon_element.attr( 'class', 'glyphicon remove-2' );
    button_element.attr( 'title', strings( 'Delete' ) );
  },
  on_mouseout: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    var button_element = $( '#' + id + '-button' );
    var buttonicon_element = $( '#' + id + '-buttonicon' );
    
    var buttonicon = publish.attbuttonicon;
    if (buttonicon == null || buttonicon.length == 0)
      buttonicon = 'glyphicon file';
    
    buttonicon_element.attr( 'class', buttonicon );
    button_element.attr( 'title', '' );
  },
  buildAttachment: function( section )
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
    
    var buttonicon = 'glyphicon picture';
    
    if (contentType.indexOf('image/') == 0) {
      thumbsrc = app.base_path + '/image/' + sec_id + '_192t.' + extension + '?token=' + app.token;
      openlink = app.base_path + '/image/' + sec_id + '_0.' + extension + '?token=' + app.token;
      
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
      thumbsrc = 'css/' + app.theme + '/images/posters/channel.png';
      
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
    
    var clickto = 'javascript:publish.click_attachment(\'' + sec_id + '\');return false;';
    var buttonclick = 'javascript:publish.click_attbutton(\'' + sec_id + '\');return false;';
    var mouseOver = 'javascript:publish.on_mouseover(\'' + sec_id + '\');';
    var mouseOut = 'javascript:publish.on_mouseout(\'' + sec_id + '\');';
    var buttonhide = '';
    
    var item = 
        '<span id="' + sec_id + '-span" onMouseOver="' + mouseOver + '" onMouseOut="' + mouseOut + '">' + "\n" +
        '  <a class="artwork-option" data-rating-key="" data-focus="keyboard" style="margin-left: 5px;margin-right: 5px;margin-bottom: 15px;" href="">' + "\n" +
        '    <img class="poster poster-poster" src="' + thumbsrc.esc() + '" onClick="' + clickto + '" title="' + titleText.esc() + '" />' + "\n" +
        '  </a>' + "\n" +
        '  <button id="' + sec_id + '-button" type="button" class="hover-menu-btn poster-info-btn ' + buttonhide + '" style="margin-left: -45px;margin-top: 5px;padding: 1px 8px 3px;" onclick="' + buttonclick + '"><i id="' + sec_id + '-buttonicon" class="' + buttonicon + '"></i></button>' + "\n" +
        '</span>';
    
    return item;
  },
  submit: function()
  {
    $( '#publish-action-input' ).attr( 'value', 'save' );
    $( '#publish-form' ).submit();
  },
  delete: function()
  {
    var id = publish.publish_id;
    var type = publish.publish_type;
    var channel = publish.publish_channel;
    var username = '';
    
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
  escape_bodystr: function( str )
  {
    if (str == null) str = '';
    
    str = str.replaceAll('\n', '');
    str = str.replaceAll('\r', '');
    
    return str;
  },
  relist_body: function( hasAtt )
  {
    if (hasAtt == null)
      hasAtt = !$( '#publish-attachments' ).hasClass( 'hide' );
    
    var body = $( '#publish-body-input' ).val();
    if (body == null) body = '';
    
    var lines = 0;
    var startPos = 0;
    while (true) {
      var pos = body.indexOf('\n', startPos);
      if (pos < 0) break;
      
      lines ++;
      startPos = pos + 1;
    }
    
    var rows = lines + 1;
    if (hasAtt == false) {
      rows = 8;
    } else {
      if (rows < 2) rows = 2;
      if (rows > 8) rows = 8;
    }
    
    $( '#publish-body-input' ).attr( 'rows', ''+rows );
  },
  on_body_keyup: function( elem, event )
  {
    publish.relist_body();
  },
  close: function()
  {
    var tags = $( '#publish-tags-input' ).val();
    var subject = $( '#publish-subject-input' ).val();
    var body = $( '#publish-body-input' ).val();
    var attachids = $( '#publish-attachments-input' ).val();
    
    tags = this.escape_bodystr( tags );
    subject = this.escape_bodystr( subject );
    body = this.escape_bodystr( body );
    attachids = this.escape_bodystr( attachids );
    
    if ((subject == null || subject.length == 0) && 
        (body == null || body.length == 0) && 
        (attachids == null || attachids.length == 0)) {
      dialog.hide();
      return;
    }
    
    var publish_tags = this.escape_bodystr( this.publish_tags );
    var publish_subject = this.escape_bodystr( this.publish_subject );
    var publish_body = this.escape_bodystr( this.publish_body );
    var publish_attachids = this.escape_bodystr( this.publish_attachids );
    
    if (tags == publish_tags && subject == publish_subject && 
        body == publish_body && attachids == publish_attachids) {
      dialog.hide();
      return;
    }
    
    $( '#publish-action-input' ).attr( 'value', 'draft' );
    $( '#publish-form' ).submit();
  },
  submit_complete: function()
  {
    var successcb = this.success_cb;
    var failedcb = this.failed_cb;
    
    this.success_cb = null;
    this.failed_cb = null;
    
    if (this.submit_success == true) {
      dialog.hide();
      if (successcb) successcb.call(publish);
    } else {
      if (failedcb) failedcb.call(publish);
    }
  },
  showedit: function( username, type, id )
  {
    if (type == null || type.length == 0 || id == null || id.length == 0)
      return;
    
    if (username == null) username = '';
    
    this.publish_username = username;
    this.publish_type = type;
    this.publish_channel = null;
    this.publish_id = id;
    this.publish_attachments = null;
    this.publish_attachids = null;
    this.attachments = null;
    
    var params = '&action=get' 
        + '&username=' + encodeURIComponent(username) 
        + '&type=' + encodeURIComponent(type) 
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
            publish.publish_username = publication['username'];
            publish.publish_tags = publication['tags'];
            publish.publish_subject = publication['subject'];
            publish.publish_body = publication['body'];
            publish.publish_id = publication['id'];
            publish.publish_replyid = publication['replyid'];
            publish.publish_streamid = publication['streamid'];
            publish.publish_type = publication['stype'];
            publish.publish_attachments = publication['attachments'];
            
            var channel = publication['channel'];
            var channelFrom = publication['channelfrom'];
            if (channelFrom != null && channelFrom.length > 0) {
              if (channel == 'Draft') channel = channelFrom;
            }
            publish.publish_channel = channel;
            publish.publish_attachids = null;
            
            dialog.show( publish.publish_dialog );
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
  init_form: function()
  {
    var form_element = $( '#publish-form' );
    var params = '';
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/publish?token=' + app.token + params + '&wt=json',
          dataType : 'json',
          beforeSubmit : function( array, form, options )
          {
            var input_error = false;
            if (!input_error) show_loading();
            
            publish.submit_success = false;
            return !input_error;
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
              publish.submit_success = true;
            }
          },
          error : function( xhr, text_status, error_thrown )
          {
            request_error( xhr, text_status, error_thrown );
          },
          complete : function()
          {
            hide_loading();
            publish.submit_complete();
          }
        }
      );
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/publish.html',
      function( template )
      {
        publish.publish_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#publish-title' ).html( strings( 'New Post' ) );
            $( '#publish-submit' ).html( strings( 'Submit' ) );
            $( '#publish-delete' ).html( strings( 'Delete' ) );
            $( '#publish-no' ).html( strings( 'Close' ) );
            
            $( '#publish-subject-input' ).attr( 'placeholder', strings( 'Title' ) );
            $( '#publish-channel-input' ).attr( 'placeholder', strings( 'Channel' ) );
            
            var username = publish.publish_username;
            var tags = publish.publish_tags;
            var subject = publish.publish_subject;
            var body = publish.publish_body;
            var id = publish.publish_id;
            var replyid = publish.publish_replyid;
            var streamid = publish.publish_streamid;
            var type = publish.publish_type;
            var channel = publish.publish_channel;
            var attachments = publish.publish_attachments;
            
            if (username == null) username = '';
            if (tags == null) tags = '';
            if (subject == null) subject = '';
            if (body == null) body = '';
            if (id == null) id = '';
            if (replyid == null) replyid = '';
            if (streamid == null) streamid = '';
            if (type == null) type = '';
            if (channel == null) channel = '';
            
            var publishTitle = strings( 'New Post' );
            if (type == 'post') {
              if (id == null || id.length == 0)
                publishTitle = strings( 'New Post' );
              else
                publishTitle = strings( 'Edit Post' );
            } else if (type == 'subscription') {
              if (id == null || id.length == 0)
                publishTitle = strings( 'New Subscription' );
              else
                publishTitle = strings( 'Edit Subscription' );
            } else if (type == 'comment') {
              if (id == null || id.length == 0)
                publishTitle = strings( 'New Comment' );
              else
                publishTitle = strings( 'Edit Comment' );
            }
            
            $( '#publish-title' ).html( publishTitle );
            $( '#publish-username-input' ).attr( 'value', username );
            $( '#publish-publishid-input' ).attr( 'value', id );
            $( '#publish-replyid-input' ).attr( 'value', replyid );
            $( '#publish-streamid-input' ).attr( 'value', streamid );
            $( '#publish-type-input' ).attr( 'value', type );
            $( '#publish-subject-input' ).attr( 'value', subject );
            
            $( '#publish-body-input' )
              .attr( 'onKeyUp', 'javascript:publish.on_body_keyup(this,event);' )
              .attr( 'value', body );
            
            $( '#publish-channel-input' )
              .attr( 'onFocus', 'javascript:publish.on_channel_focus(this);' )
              .attr( 'onBlur', 'javascript:publish.on_channel_blur(this);' )
              .attr( 'value', channel );
            
            publish_tags.init_value( tags );
            publish.init_form();
            
            var attachclick = 'javascript:publish.attachfile();return false;';
            var attachhtml = '<a class="standard-options" onclick="' + attachclick + '" href=""><i class="glyphicon paperclip"></i> ' + strings( 'Attach File' ) + '</a>';
            $( '#publish-options' ).html( attachhtml );
            
            if (id == null || id.length == 0) {
              $( '#publish-submit' ).removeClass( 'hide' );
              $( '#publish-delete' ).addClass( 'hide' );
            } else {
              $( '#publish-submit' ).removeClass( 'hide' );
              $( '#publish-delete' ).removeClass( 'hide' );
            }
            
            $( '#publish-submit' )
              .attr( 'onclick', 'javascript:publish.submit();return false;' );
            
            $( '#publish-delete' )
              .attr( 'onclick', 'javascript:publish.delete();return false;' );
            
            $( '#publish-no' )
              .attr( 'onclick', 'javascript:publish.close();return false;' );
            
            $( '#publish-close' )
              .attr( 'onclick', 'javascript:publish.close();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            publish.init_attachments( attachments );
          },
          hidecb: function()
          {
          },
          shown: false
        };
      }
    );
  },
  showselects: function( elem, values, selected_value )
  {
    if (elem == null || values == null)
      return;
    
    var offsetLeft = elem.offsetLeft; // + elem.offsetParent.offsetLeft + document.body.clientLeft;
    var offsetTop = elem.offsetTop + 9; //elem.offsetParent.offsetTop + document.body.clientTop + 10;
    var offsetHeight = elem.offsetHeight;
    var offsetWidth = elem.offsetWidth;
    
    var left = offsetLeft;
    var top = offsetTop + offsetHeight;
    var width = offsetWidth;
    
    var content = [];
    var selectlist = [];
    
    for (var key in values) {
      var value = values[key];
      if (value == null) continue;
      
      var index = selectlist.length;
      var name = key;
      var title = strings( name );
      var highlight = '';
      
      if (selected_value == name) {
        highlight = 'select2-highlighted';
        value['selected'] = true;
      }
      
      var item = 
        '<li id="selectlist-item-' + index + '" onMouseOver="javascript:publish.on_select_focus(' + index + ');" onMouseOut="javascript:publish.on_select_out(' + index + ');" onMouseDown="javascript:publish.on_select_click(' + index + ');" class="select2-results-dept-0 select2-result select2-result-selectable ' + highlight + '">' + "\n" +
        '  <div class="select2-result-label"><span class="select2-match"></span>' + title.esc() + '</div>' + "\n" + 
        '</li>';
      
      selectlist.push( name );
      content.push( item );
    }
    
    this.selectlist = selectlist;
    //this.selectelement = elem;
    
    if (content.length == 0)
      return;
    
    var html = 
      '<div class="select2-drop select2-drop-multi select2-drop-active" style="top: ' + top + 'px; left: ' + left + 'px; width: ' + width + 'px; display: block;">' + "\n" +
      '<ul class="select2-results">' + "\n" + content.join( '\n' ) +
      '</ul></div>';
    
    var selectlist_element = $( '#publish-selectlist-drop' );
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
    
    //var lockelement = this.lockelement;
    //if (lockelement && changed) {
    //  lockelement.addClass( 'selected' );
    //}
  },
  on_channel_focus: function( elem )
  {
    var username = publish.publish_username;
    var stype = publish.publish_type;
    var channel = publish.publish_channel;
    var selectedval = channel;
    
    if (username == null) username = '';
    if (stype == null) stype = '';
    
    if (publish.channel_selects == null) publish.channel_selects = {};
    var channel_values = publish.channel_selects[stype];
    if (channel_values) {
      publish.selectelement = $( '#publish-channel-input' );
      publish.showselects( elem, channel_values, selectedval );
      return;
    }
    
    var params = '&action=getchannels' 
        + '&username=' + encodeURIComponent(username) 
        + '&type=' + encodeURIComponent(stype);

    $.ajax
    (
      {
        url : app.user_path + '/publish?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var values = response['channels'];
          var channel_values = {};
          if (values) {
            for (var key in values) {
              var value = values[key];
              if (value) channel_values[value] = value;
            }
          }
          if (publish.channel_selects == null) publish.channel_selects = {};
          publish.channel_selects[stype] = channel_values;
          publish.selectelement = $( '#publish-channel-input' );
          publish.showselects( elem, channel_values, selectedval );
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
  on_channel_blur: function( elem )
  {
    var selectlist_element = $( '#publish-selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( '' );
  }
};
