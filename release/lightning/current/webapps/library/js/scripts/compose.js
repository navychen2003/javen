
var compose_to = {
  values: [],
  keyaction: null,
  selectlist: null,
  selectelement: null,
  inputelement: null,
  friends: null,
  
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
    $( '#compose-to-autogen' )
      .attr( 'onClick', 'javascript:compose_to.onfocus();' );
    
    var values = this.values;
    if (values == null) values = [];
    
    var valueContent = [];
    var valueStr = '';
    
    var toText = strings( 'To' );
    var toItem = '<li class="select2-search-field"><label id="compose-to-text" style="color: #888;padding-left: 5px;padding-right: 5px;margin-top: 5px;">' + toText + '</label></li>';
    valueContent.push( toItem );
    
    for (var key in values) {
      var value = values[key];
      if (value == null || value.length == 0) 
        continue;
      
      if (valueStr.length > 0) valueStr += ',';
      valueStr += value;
      
      var removeto = 'javascript:compose_to.remove_value(' + key + ');return false;';
      var infoto = 'javascript:userinfo.showonly(\'' + value + '\');return false;';
      var item = '<li class="select2-search-choice"><div onclick="' + infoto + '">' + value.esc() + '</div><a onclick="' + removeto + '" class="select2-search-choice-close" tabindex="-1"></a></li>';
      
      valueContent.push( item );
    }
    
    var keydown = 'javascript:return compose_to.input_keydown(this,event);';
    var keyup = 'javascript:compose_to.input_keyup(this,event);';
    var focus = 'javascript:compose_to.input_focus(this,event);';
    var blur = 'javascript:compose_to.input_blur(this,event);';
    
    var inputItem = '<li class="select2-search-field"><input id="compose-to-selectinput" type="text" autocomplete="off" class="select2-input" style="width: 10px;" onKeyDown="' + keydown + '" onKeyUp="' + keyup + '" onFocus="' + focus + '" onBlur="' + blur + '"></li>';
    valueContent.push( inputItem );
    
    $( '#compose-to-list' ).html( valueContent.join( '\n' ) );
    $( '#compose-to-input' ).attr( 'value', valueStr.esc() );
    
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
    $( '#compose-to-autogen' )
      .addClass( 'select2-container-active' );
    $( '#compose-to-selectinput' )
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
    if (code == 16 || code == 8 || code == 37 || code == 39) return true;
    if (code >= 65 && code <= 90) return true;
    if (code >= 48 && code <= 57) return true;
    if (code == 190 || code == 188 || code == 189) return true;
    return false;
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
    
    $( '#compose-to-selectinput' ).attr( 'style', 'width: ' + width + 'px;' );
  },
  input_focus: function( element, event )
  {
    var friends = this.friends;
    if (friends != null) {
      compose_to.init_select( element, friends );
      return;
    }
    
    var params = '&action=select&prefix=';
    
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
          var friends = response['friends'];
          compose_to.init_select( element, friends );
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
  init_select: function( element, friends )
  {
    this.friends = friends;
    if (friends == null) friends = {};
    
    var values = {};
    var selectedval = '';
    
    for (var key in friends) {
      var friend = friends[key];
      if (friend == null) continue;
      
      var name = friend['name'];
      var title = friend['title'];
      var status = friend['status'];
      var avatar = friend['avatar'];
      
      if (name == null) name = '';
      if (status == null) status = '';
      if (title == null || title.length == 0) title = name;
      
      values[name] = title + ' <' + name + '>';
    }
    
    var elem = $( '#compose-to-autogen' )[0];
    
    this.selectelement = element; //$( '#compose-to-selectinput' );
    this.inputelement = $( '#compose-to-selectinput' );
    this.showselects( elem, values, selectedval );
  },
  input_blur: function( element, event )
  {
    var value = element.value;
    if (value != null && value.length > 0) {
      this.add_value( value );
      this.init_values();
    }
    
    $( '#compose-to-autogen' )
      .removeClass( 'select2-container-active' );
    $( '#compose-to-selectinput' )
      .removeClass( 'select2-focused' );
    
    var selectlist_element = $( '#compose-selectlist-drop' );
    if (selectlist_element)
      selectlist_element.html( '' );
  },
  showselects: function( elem, values, selected_value )
  {
    if (elem == null || values == null)
      return;
    
    var offsetLeft = elem.offsetLeft; // + elem.offsetParent.offsetLeft + document.body.clientLeft;
    var offsetTop = elem.offsetTop + 2; // + elem.offsetParent.offsetTop + document.body.clientTop + 14;
    var offsetHeight = elem.offsetHeight;
    var offsetWidth = elem.offsetWidth;
    
    var left = offsetLeft;
    var top = offsetTop + offsetHeight;
    var width = offsetWidth;
    
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
        '<li id="compose-selectlist-item-' + index + '" onMouseOver="javascript:compose_to.on_select_focus(' + index + ');" onMouseOut="javascript:compose_to.on_select_out(' + index + ');" onMouseDown="javascript:compose_to.on_select_click(' + index + ');" onClick="javascript:return false;" class="select2-results-dept-0 select2-result select2-result-selectable ' + highlight + '">' + "\n" +
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
    
    var selectlist_element = $( '#compose-selectlist-drop' );
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
    
    var item_element = $( '#compose-selectlist-item-' + index );
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
    
    var item_element = $( '#compose-selectlist-item-' + index );
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
    
    var inputelement = this.inputelement;
    var changed = false;
    if (inputelement) {
      inputelement.attr( 'value', value.esc() );
      changed = true;
    }
  }
};

var compose = {
  compose_dialog: null,
  compose_to: null,
  compose_subject: null,
  compose_body: null,
  compose_id: null,
  compose_replyid: null,
  compose_streamid: null,
  compose_type: null,
  compose_folder: null,
  compose_attachments: null,
  compose_attachids: null,
  attachments: null,
  attbuttonicon: null,
  success_cb: null,
  failed_cb: null,
  submit_success: false,

  share: function( items, sharefrom, subject, body )
  {
    if (items == null || items.length == 0)
      return;
    
    if (sharefrom == null || sharefrom.length == 0)
      sharefrom = globalApp.get_usersimple();
    
    if (subject == null || subject.length == 0)
      subject = strings( 'Share from {0}' ).format(sharefrom);
    
    if (body == null) body = '';
    
    compose.show('mail', '', subject, body, null, null, items);
  },
  show: function( type, to, subject, body, replyid, streamid, attachments )
  {
    this.compose_to = to;
    this.compose_subject = subject;
    this.compose_body = body;
    this.compose_id = null;
    this.compose_replyid = replyid;
    this.compose_streamid = streamid;
    this.compose_type = type;
    this.compose_attachments = attachments;
    this.compose_attachids = null;
    this.attachments = null;
    dialog.show( compose.compose_dialog );
  },
  attachfile: function()
  {
    var emptysrc = null;
    var suffix = '';
    var accept = '*';
    
    artwork.showselect('Public Files', function( section ) {
          if (section) {
            compose.add_attachment( section );
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
    this.compose_attachids = $( '#compose-attachments-input' ).val();
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
      $( '#compose-attachments-input' ).attr( 'value', idlist.join(",") );
      $( '#compose-attachment-list' ).html( content.join("\n") );
      
      this.relist_body(true);
      $( '#compose-attachments' ).removeClass( 'hide' );
      
    } else {
      $( '#compose-attachments-input' ).attr( 'value', '' );
      $( '#compose-attachment-list' ).html( '' );
      
      this.relist_body(false);
      $( '#compose-attachments' ).addClass( 'hide' );
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
    
    compose.remove_attachment( id );
  },
  on_mouseover: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    var button_element = $( '#' + id + '-button' );
    var buttonicon_element = $( '#' + id + '-buttonicon' );
    
    compose.attbuttonicon = buttonicon_element.attr( 'class' );
    buttonicon_element.attr( 'class', 'glyphicon remove-2' );
    button_element.attr( 'title', strings( 'Delete' ) );
  },
  on_mouseout: function( id )
  {
    if (id == null || id.length == 0)
      return;
    
    var button_element = $( '#' + id + '-button' );
    var buttonicon_element = $( '#' + id + '-buttonicon' );
    
    var buttonicon = compose.attbuttonicon;
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
    
    var clickto = 'javascript:compose.click_attachment(\'' + sec_id + '\');return false;';
    var buttonclick = 'javascript:compose.click_attbutton(\'' + sec_id + '\');return false;';
    var mouseOver = 'javascript:compose.on_mouseover(\'' + sec_id + '\');';
    var mouseOut = 'javascript:compose.on_mouseout(\'' + sec_id + '\');';
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
  send: function()
  {
    $( '#compose-action-input' ).attr( 'value', 'send' );
    $( '#compose-form' ).submit();
  },
  delete: function()
  {
    var id = compose.compose_id;
    var type = compose.compose_type;
    var folder = compose.compose_folder;
    var username = '';
    
    if (folder == 'Trash') {
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
      hasAtt = !$( '#compose-attachments' ).hasClass( 'hide' );
    
    var body = $( '#compose-body-input' ).val();
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
    
    $( '#compose-body-input' ).attr( 'rows', ''+rows );
  },
  on_body_keyup: function( elem, event )
  {
    compose.relist_body();
  },
  close: function()
  {
    var to = $( '#compose-to-input' ).val();
    var subject = $( '#compose-subject-input' ).val();
    var body = $( '#compose-body-input' ).val();
    var attachids = $( '#compose-attachments-input' ).val();
    
    to = this.escape_bodystr( to );
    subject = this.escape_bodystr( subject );
    body = this.escape_bodystr( body );
    attachids = this.escape_bodystr( attachids );
    
    if ((subject == null || subject.length == 0) && 
        (body == null || body.length == 0) && 
        (attachids == null || attachids.length == 0)) {
      dialog.hide();
      return;
    }
    
    var compose_to = this.escape_bodystr( this.compose_to );
    var compose_subject = this.escape_bodystr( this.compose_subject );
    var compose_body = this.escape_bodystr( this.compose_body );
    var compose_attachids = this.escape_bodystr( this.compose_attachids );
    
    if (to == compose_to && subject == compose_subject && 
        body == compose_body && attachids == compose_attachids) {
      dialog.hide();
      return;
    }
    
    $( '#compose-action-input' ).attr( 'value', 'draft' );
    $( '#compose-form' ).submit();
  },
  submit_complete: function()
  {
    var successcb = this.success_cb;
    var failedcb = this.failed_cb;
    
    this.success_cb = null;
    this.failed_cb = null;
    
    if (this.submit_success == true) {
      dialog.hide();
      if (successcb) successcb.call(compose);
    } else {
      if (failedcb) failedcb.call(compose);
    }
  },
  showdraft: function( type, id )
  {
    if (type == null || type.length == 0 || id == null || id.length == 0)
      return;
    
    var folder = 'Draft';
    
    this.compose_type = type;
    this.compose_folder = folder;
    this.compose_id = id;
    this.compose_attachments = null;
    this.compose_attachids = null;
    this.attachments = null;
    
    var params = '&action=get' 
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
            compose.compose_to = message['to'];
            compose.compose_subject = message['subject'];
            compose.compose_body = message['body'];
            compose.compose_id = message['id'];
            compose.compose_replyid = message['replyid'];
            compose.compose_streamid = message['streamid'];
            compose.compose_type = message['type'];
            compose.compose_folder = message['folder']
            compose.compose_attachments = message['attachments'];
            compose.compose_attachids = null;
            dialog.show( compose.compose_dialog );
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
    var form_element = $( '#compose-form' );
    var params = '';
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/message?token=' + app.token + params + '&wt=json',
          dataType : 'json',
          beforeSubmit : function( array, form, options )
          {
            var input_error = false;
            if (!input_error) show_loading();
            
            compose.submit_success = false;
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
              compose.submit_success = true;
            }
          },
          error : function( xhr, text_status, error_thrown )
          {
            request_error( xhr, text_status, error_thrown );
          },
          complete : function()
          {
            hide_loading();
            compose.submit_complete();
          }
        }
      );
  },
  init_dialog: function( dialog_element ) 
  {
    $.get
    (
      'tpl/compose.html',
      function( template )
      {
        compose.compose_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#compose-title' ).html( strings( 'Compose' ) );
            $( '#compose-send' ).html( strings( 'Send' ) );
            $( '#compose-delete' ).html( strings( 'Delete' ) );
            $( '#compose-no' ).html( strings( 'Close' ) );
            
            $( '#compose-subject-input' ).attr( 'placeholder', strings( 'Subject' ) );
            
            var to = compose.compose_to;
            var subject = compose.compose_subject;
            var body = compose.compose_body;
            var id = compose.compose_id;
            var replyid = compose.compose_replyid;
            var streamid = compose.compose_streamid;
            var type = compose.compose_type;
            var folder = compose.compose_folder;
            var attachments = compose.compose_attachments;
            
            if (to == null) to = '';
            if (subject == null) subject = '';
            if (body == null) body = '';
            if (id == null) id = '';
            if (replyid == null) replyid = '';
            if (streamid == null) streamid = '';
            if (type == null) type = 'mail';
            if (folder == null) folder = '';
            
            $( '#compose-draftid-input' ).attr( 'value', id );
            $( '#compose-replyid-input' ).attr( 'value', replyid );
            $( '#compose-streamid-input' ).attr( 'value', streamid );
            $( '#compose-type-input' ).attr( 'value', type );
            $( '#compose-folder-input' ).attr( 'value', folder );
            $( '#compose-subject-input' ).attr( 'value', subject );
            $( '#compose-body-input' ).attr( 'value', body )
              .attr( 'onKeyUp', 'javascript:compose.on_body_keyup(this,event);' );
            
            compose.friends = null;
            compose_to.init_value( to );
            compose.init_form();
            
            var attachclick = 'javascript:compose.attachfile();return false;';
            var attachhtml = '<a class="standard-options" onclick="' + attachclick + '" href=""><i class="glyphicon paperclip"></i> ' + strings( 'Attach File' ) + '</a>';
            $( '#compose-options' ).html( attachhtml );
            
            if (id == null || id.length == 0) {
              $( '#compose-send' ).removeClass( 'hide' );
              $( '#compose-delete' ).addClass( 'hide' );
            } else {
              $( '#compose-send' ).removeClass( 'hide' );
              $( '#compose-delete' ).removeClass( 'hide' );
            }
            
            $( '#compose-send' )
              .attr( 'onclick', 'javascript:compose.send();return false;' );
            
            $( '#compose-delete' )
              .attr( 'onclick', 'javascript:compose.delete();return false;' );
            
            $( '#compose-no' )
              .attr( 'onclick', 'javascript:compose.close();return false;' );
            
            $( '#compose-close' )
              .attr( 'onclick', 'javascript:compose.close();return false;' )
              .attr( 'title', strings( 'Close' ) );
            
            compose.init_attachments( attachments );
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
