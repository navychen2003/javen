
var listpublication = {
  username: null,
  streamid: null,
  publications: null,
  publish_type: null,
  publish_channel: null,
  publish_channels: null,
  publish_page: 1,
  
  showposts: function( username, streamid )
  {
    $( '#publications-post-title' ).addClass( 'active' );
    $( '#publications-subscribe-title' ).removeClass( 'active' );
    $( '#publications-comment-title' ).removeClass( 'active' );
    $( '#publications-featured-title' ).removeClass( 'active' );
    $( '#publications-app-title' ).removeClass( 'active' );
    
    this.showlist(username, 'post', '', 1, streamid);
  },
  showsubscriptions: function( username, streamid )
  {
    $( '#publications-post-title' ).removeClass( 'active' );
    $( '#publications-subscribe-title' ).addClass( 'active' );
    $( '#publications-comment-title' ).removeClass( 'active' );
    $( '#publications-featured-title' ).removeClass( 'active' );
    $( '#publications-app-title' ).removeClass( 'active' );
    
    this.showlist(username, 'subscription', '', 1, streamid);
  },
  showcomments: function( username, streamid )
  {
    $( '#publications-post-title' ).removeClass( 'active' );
    $( '#publications-subscribe-title' ).removeClass( 'active' );
    $( '#publications-comment-title' ).addClass( 'active' );
    $( '#publications-featured-title' ).removeClass( 'active' );
    $( '#publications-app-title' ).removeClass( 'active' );
    
    this.showlist(username, 'comment', '', 1, streamid);
  },
  showfeatured: function( username, streamid )
  {
    $( '#publications-post-title' ).removeClass( 'active' );
    $( '#publications-subscribe-title' ).removeClass( 'active' );
    $( '#publications-comment-title' ).removeClass( 'active' );
    $( '#publications-featured-title' ).addClass( 'active' );
    $( '#publications-app-title' ).removeClass( 'active' );
    
    this.showlist(username, 'featured', '', 1, streamid);
  },
  showapps: function( username, streamid )
  {
    $( '#publications-post-title' ).removeClass( 'active' );
    $( '#publications-subscribe-title' ).removeClass( 'active' );
    $( '#publications-comment-title' ).removeClass( 'active' );
    $( '#publications-featured-title' ).removeClass( 'active' );
    $( '#publications-app-title' ).addClass( 'active' );
    
    this.showlist(username, 'app', '', 1, streamid);
  },
  gocompose: function()
  {
    var username = this.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    var stype = this.publish_type;
    var channel = this.publish_channel;
    var streamid = this.streamid;
    
    if (stype == null) stype = 'post';
    if (channel == null || channel == 'Trash') channel = 'Default';
    
    publish.success_cb = listpublication.refresh;
    publish.show( username, stype, channel, null, null, null, streamid );
  },
  goposts: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('posts') >= 0) {
      listpublication.relist();
      return;
    }
    
    var username = listpublication.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    context.redirect( '#/~posts/' + encodeURIComponent(username) );
  },
  gosubscribes: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('subscriptions') >= 0) {
      listpublication.relist();
      return;
    }
    
    var username = listpublication.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    context.redirect( '#/~subscriptions/' + encodeURIComponent(username) );
  },
  gocomments: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('comments') >= 0) {
      listpublication.relist();
      return;
    }
    
    var username = listpublication.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    context.redirect( '#/~comments/' + encodeURIComponent(username) );
  },
  gofeatured: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('featured') >= 0) {
      listpublication.relist();
      return;
    }
    
    var username = listpublication.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    context.redirect( '#/~featured/' + encodeURIComponent(username) );
  },
  goapps: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('apps') >= 0) {
      listpublication.relist();
      return;
    }
    
    var username = listpublication.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    context.redirect( '#/~apps/' + encodeURIComponent(username) );
  },
  gostreams: function()
  {
    var stype = this.publish_type;
    var streamid = this.streamid;
    if (streamid == null || streamid.length == 0) 
      return;
    
    this.click_stream( stype, streamid );
  },
  showchannel: function( key )
  {
    if (key == null) return;
    
    var username = listpublication.username;
    var type = listpublication.publish_type;
    var channel = listpublication.publish_channel;
    var channels = listpublication.publish_channels;
    
    if (type && channels) { 
      channel = channels[key];
      if (channel)
        listpublication.showlist( username, type, channel, 1);
    }
  },
  showpage: function( page )
  {
    var username = listpublication.username;
    var type = listpublication.publish_type;
    var channel = listpublication.publish_channel;
    var page2 = listpublication.publish_page;
    var streamid = listpublication.streamid;
    
    listpublication.showlist( username, type, channel, page, streamid );
  },
  relist: function()
  {
    var username = listpublication.username;
    var type = listpublication.publish_type;
    var channel = listpublication.publish_channel;
    var page = listpublication.publish_page;
    var streamid = listpublication.streamid;
    
    listpublication.showlist( username, type, channel, page, streamid );
  },
  showlist: function( username, type, channel, page, streamid )
  {
    var composetext_element = $( '#publications-compose-submit-text' );
    var compose_element = $( '#publications-compose-submit' );
    
    var posttitle_element = $( '#publications-post-title' );
    var subscribetitle_element = $( '#publications-subscribe-title' );
    var commenttitle_element = $( '#publications-comment-title' );
    var featuredtitle_element = $( '#publications-featured-title' );
    var apptitle_element = $( '#publications-app-title' );
    
    var postnav_element = $( '#publications-post-nav' );
    var subscribenav_element = $( '#publications-subscribe-nav' );
    var commentnav_element = $( '#publications-comment-nav' );
    var featurednav_element = $( '#publications-featured-nav' );
    var appnav_element = $( '#publications-app-nav' );
    
    var me = globalApp.get_username();
    if (username == null || username.length == 0 || username == me) {
      navbar.init_metitle( null, '#/~profile' );
    }
    
    postnav_element.removeClass( 'hide' );
    subscribenav_element.removeClass( 'hide' );
    commentnav_element.removeClass( 'hide' );
    
    if (username == 'system') {
      navbar.init_name( strings( 'Setting' ), null, '#/~settings' );
      
      featurednav_element.removeClass( 'hide' );
      appnav_element.removeClass( 'hide' );
      
    } else {
      featurednav_element.addClass( 'hide' );
      appnav_element.addClass( 'hide' );
    }
    
    var composeTitle = strings( 'New Post' );
    if (system) {
      var context = system.context;
      var path = context.path;
    
      if (path.indexOf('post') >= 0) {
        composeTitle = strings( 'New Post' );
      } else if (path.indexOf('subscription') >= 0) {
        composeTitle = strings( 'New Subscription' );
      } else if (path.indexOf('comment') >= 0) {
        composeTitle = strings( 'New Comment' );
      }
    }
    
    composetext_element
      .html( composeTitle.esc() );
    
    compose_element
      .attr( 'onClick', 'javascript:listpublication.gocompose();return false;' )
      .attr( 'title', composeTitle.esc() );
    
    posttitle_element
      .attr( 'onClick', 'javascript:listpublication.goposts();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Post' ).esc() );
    
    subscribetitle_element
      .attr( 'onClick', 'javascript:listpublication.gosubscribes();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Subscription' ).esc() );
    
    commenttitle_element
      .attr( 'onClick', 'javascript:listpublication.gocomments();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Comments' ).esc() );
    
    featuredtitle_element
      .attr( 'onClick', 'javascript:listpublication.gofeatured();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Featured' ).esc() );
    
    apptitle_element
      .attr( 'onClick', 'javascript:listpublication.goapps();return false;' )
      .attr( 'href', '' )
      .html( strings( 'Application' ).esc() );
    
    if (page == null || page < 1) page = 1;
    if (type == null) type = '';
    if (channel == null) channel = '';
    if (username == null) username = '';
    if (streamid == null) streamid = '';
    
    this.username = username;
    this.streamid = streamid;
    this.publications = null;
    
    var groupby = '';
    
    if (streamid != null && streamid.length > 0) {
      //conversationtitle_element
      //  .attr( 'onClick', 'javascript:listpublication.gostreams();return false;' )
      //  .html( strings( 'Stream' ).esc() );
      
    } else {
      groupby = 'stream';
    }
    
    var params = '&action=list' 
        + '&username=' + encodeURIComponent(username) 
        + '&streamid=' + encodeURIComponent(streamid) 
        + '&type=' + encodeURIComponent(type) 
        + '&channel=' + encodeURIComponent(channel) 
        + '&groupby=' + encodeURIComponent(groupby) 
        + '&page=' + page;
    
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
          listpublication.init_content( response );
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
  init_title: function( user, streamid )
  {
    if (user) {
      var me = globalApp.get_username();
      
      var username = user['name'];
      var nickname = user['title'];
      var usertype = user['type'];
    
      if (me != username && username != null && username.length > 0) {
        if (username == 'system') {
          navbar.init_name( strings( 'Setting' ), null, '#/~settings' );
          
        } else if (usertype == 'group') {
          var linkto = '#/~group/' + encodeURIComponent(username);
          navbar.init_grouptitle( username, nickname, null, linkto );
        
        } else {
          var linkto = '#/~user/' + encodeURIComponent(username);
          navbar.init_usertitle( username, nickname, null, linkto );
        }
      
        return;
      }
    }
    
    navbar.init_metitle( null, '#/~profile' );
  },
  init_content: function( response )
  {
    if (response == null) response = {};
    
    var channellist_element = $( '#publications-channel-list' );
    var list_element = $( '#publications-list' );
    
    var page_element = $( '#publications-page' );
    var pages_element = $( '#publications-pages' );
    
    var prev_element = $( '#publications-prev' );
    var next_element = $( '#publications-next' );
    
    var the_username = response['username'];
    var the_user = response['user'];
    var the_streamid = response['streamid'];
    var the_type = response['type'];
    var the_channel = response['channel'];
    var the_groupby = response['groupby'];
    var the_page = response['page'];
    var total_page = response['totalpage'];
  
    if (the_page <= 1) {
      prev_element.addClass( 'disabled' );
    
    } else {
      prev_element
        .removeClass( 'disabled' )
        .attr( 'onclick', 'javascript:listpublication.showpage(' + (the_page-1) + ');' );
    }
  
    if (the_page >= total_page) {
      next_element.addClass( 'disabled' );
    
    } else { 
      next_element
        .removeClass( 'disabled' )
        .attr( 'onclick', 'javascript:listpublication.showpage(' + (the_page+1) + ');' );
    }
  
    page_element.html( the_page );
    pages_element.html( total_page );
  
    var channels = response['channels'];
    var channelContent = [];
  
    listpublication.init_title( the_user, the_streamid );
  
    listpublication.username = the_username;
    listpublication.streamid = the_streamid;
    listpublication.publish_type = the_type;
    listpublication.publish_channel = the_channel;
    listpublication.publish_channels = channels;
    listpublication.publish_page = the_page;
    
    var publishIcon = 'globe';
    if (the_type == 'comment') {
      publishIcon = 'comments';
    } else if (the_type == 'subscription') {
      publishIcon = 'rss';
    }
    if (the_channel == 'Trash')
      publishIcon = 'bin';
  
    if (the_user) {
      //listpublication.username = the_user['name'];
    }
    
    if (the_streamid != null && the_streamid.length > 0) {
      channels = {};
    
      var name = strings( 'Stream Group' );
      var clickto = 'javascript:listpublication.gostreams();return false;';
      var hrefto = '';
    
      var active = 'active';
      var item = '<li><a class="settings-filter ' + active + '" onClick="' + clickto + '" href="' + hrefto + '">' + name + '</a></li>';
    
      channelContent.push( item );
    }
    
    $( '#publications-icon' )
      .attr( 'class', 'subnav-icon glyphicon ' + publishIcon );
    
    for (var key in channels) {
      var channelname = channels[key];
    
      var name = strings( channelname );
      var clickto = 'javascript:listpublication.showchannel(' + key + ');return false;';
      var hrefto = '';
      var active = '';
    
      if (channelname == the_channel)
        active = 'active';
    
      var item = '<li><a class="settings-filter ' + active + '" onClick="' + clickto + '" href="' + hrefto + '">' + name + '</a></li>';
    
      channelContent.push( item );
    }
  
    channellist_element
      .html( channelContent.join( "\n" ) );
    
    var publications = response['publications'];
    var publicationContent = [];
    var prevstreamid = null;
  
    listpublication.publications = publications;
  
    for (var key in publications) { 
      var publication = publications[key];
      var idx = publicationContent.length;
      
      var id = publication['id'];
      var sid = publication['streamid'];
      var channel = publication['channel'];
      var stype = publication['stype'];
      var from = publication['from'];
      var text = publication['subject'];
      var ctype = publication['contenttype'];
      var status = publication['status'];
      var flag = publication['flag'];
      var ptime = publication['ptime'];
      var attcount = publication['attcount'];
      var streamcount = publication['streamcount'];
      var owner = publication['owner'];
      var userowner = publication['userowner'];
    
      if (attcount == null) attcount = 0;
      if (streamcount == null) streamcount = 0;
      if (sid == null) sid = '';
    
      var msgauthor = owner;
      var msgtime = format_time(ptime);
    
      var deletetitle = strings( 'Delete' );
      var startitle = strings( 'Mark as important' );
    
      var iconclass = publishinfo.get_iconclass(stype, channel, status, sid);;
      if (iconclass == null || iconclass.length == 0)
        iconclass = 'glyphicon message-plus';
    
      if (msgauthor == null) msgauthor = '';
      msgauthor = listpublication.buildAddressItem( msgauthor, userowner );
    
      if (text == null || text.length == 0)
        text = strings( '[No Title]' );
    
      var starredto = 'javascript:listpublication.starred_item(\'' + stype + '\',\'' + channel + '\',\'' + id + '\');return false;';
      var unstarredto = 'javascript:listpublication.unstarred_item(\'' + stype + '\',\'' + channel + '\',\'' + id + '\');return false;';
      var deleteto = 'javascript:listpublication.delete_item(\'' + stype + '\',\'' + channel + '\',\'' + id + '\');return false;';
      var editto = 'javascript:listpublication.edit_item(\'' + stype + '\',\'' + channel + '\',\'' + id + '\');return false;';
      var clickto = 'javascript:listpublication.click_item(\'' + stype + '\',\'' + channel + '\',\'' + id + '\',' + idx + ');return false;';
      var href = ''; //clickto;
    
      var starclass = 'glyphicon dislikes';
      if (status == 'starred' || flag == 'favorite') {
        starclass = 'glyphicon star';
        starredto = unstarredto;
        startitle = strings( 'Unstar this item' );
      }
    
      var onover = 'javascript:listpublication.onitemover(' + idx + ');';
      var onout = 'javascript:listpublication.onitemout(' + idx + ');';
    
      var editclick = editto;
      var edithide = 'hide';
      var edittitle = strings( 'Edit' );
    
      var streamclick = 'javascript:listpublication.click_stream(\'' + stype + '\',\'' + sid + '\');return false;';
      var streamhide = 'hide';
      var streamtitle = '';
      
      if (streamcount > 1 && (the_streamid == null || the_streamid.length == 0)) {
        streamhide = '';
        streamtitle = strings( 'Show stream group' );
      }
    
      var attachclick = clickto; //'javascript:return false;';
      var attachhide = 'hide';
      var attachtitle = '';
    
      if (attcount > 1) {
        attachhide = '';
        attachtitle = strings( 'This item has {0} attachments' ).format(attcount);
      } else if (attcount > 0) {
        attachhide = '';
        attachtitle = strings( 'This item has an attachment' );
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
      publication['itemstyle'] = itemstyle;
    
      var item = 
         '        <li class="server-event-list-item" id="publications-list-' + idx + '" onmouseover="' + onover + '" onmouseout="' + onout + '" style="' + itemstyle + '">' + "\n" +
         '          <i class="server-event-icon ' + iconclass + '" style="' + iconstyle + '"></i>' + "\n" +
         '          <div class="server-event-details" style="' + starredstyle + '">' + "\n" +
         '            <span class="server-event-action" onclick="' + starredto + '" title="' + startitle + '"><i class="' + starclass + '"></i></span>' + "\n" +
	     '            <span class="server-event-time">' + msgtime.esc() + '</span>' + "\n" +
         '            <span class="server-event-time">' + msgauthor + '</span>' + "\n" +
         '            <a href="' + href + '" onclick="' + clickto + '">' + text + '</a>' + "\n" +
         '            <button id="publications-attach-' + idx + '" class="player-btn ' + attachhide + '" style="width: 20px;height: 20px;font-size: 18px;" onclick="' + attachclick + '" title="' + attachtitle + '"><i class="glyphicon paperclip"></i></button>' + "\n" +
         '            <button id="publications-stream-' + idx + '" class="player-btn ' + streamhide + '" style="width: 20px;height: 20px;font-size: 18px;" onclick="' + streamclick + '" title="' + streamtitle + '"><i class="glyphicon chat"></i></button>' + "\n" +
         '            <button id="publications-edit-' + idx + '" class="player-btn ' + edithide + '" style="width: 20px;height: 20px;font-size: 18px;" onclick="' + editclick + '" title="' + edittitle + '"><i class="glyphicon pencil"></i></button>' + "\n" +
         '            <button id="publications-delete-' + idx + '" class="player-btn hide" style="width: 20px;height: 20px;font-size: 18px;" onclick="' + deleteto + '" title="' + deletetitle + '"><i class="glyphicon remove-2"></i></button>' + "\n" +
         '          </div>' + "\n" +
         '        </li>' + "\n";
    
      publicationContent.push( item );
      prevstreamid = sid;
    }
    
    list_element
      .html( publicationContent.join( "\n" ) );
  
    if (publicationContent.length == 0) {
      var emptytitle = strings( 'No publications :(' );
      if (the_type == 'post')
        emptytitle = strings( 'No posts :(' );
      else if (the_type == 'subscription')
        emptytitle = strings( 'No subscriptions :(' );
      else if (the_type == 'comment')
        emptytitle = strings( 'No comments :(' );
      
      $( '#publications-list' ).addClass( 'hide' );
      $( '#publications-empty' ).html( emptytitle ).removeClass( 'hide' );
    } else {
      $( '#publications-list' ).removeClass( 'hide' );
      $( '#publications-empty' ).addClass( 'hide' );
    }
  },
  onitemover: function( idx )
  {
    if (idx == null) return;
    
    var publications = listpublication.publications;
    if (publications == null || publications.length == 0)
      return;
    
    if (idx >= 0 && idx < publications.length) {
      var publication = publications[idx];
      if (publication) {
        var itemstyle = publication['itemstyle'];
        if (itemstyle == null) itemstyle = '';
        
        var list_element = $( '#publications-list-' + idx );
        var delete_element = $( '#publications-delete-' + idx );
        var edit_element = $( '#publications-edit-' + idx );
    
        if (list_element)
          list_element.attr( 'style', itemstyle + 'background-color: rgba(255,255,255,0.1);' );
    
        if (delete_element)
          delete_element.removeClass( 'hide' );
        
        if (edit_element)
          edit_element.removeClass( 'hide' );
      }
    }
  },
  onitemout: function( idx )
  {
    if (idx == null) return;
    
    var publications = listpublication.publications;
    if (publications == null || publications.length == 0)
      return;
    
    if (idx >= 0 && idx < publications.length) {
      var publication = publications[idx];
      if (publication) {
        var itemstyle = publication['itemstyle'];
        if (itemstyle == null) itemstyle = '';
    
        var list_element = $( '#publications-list-' + idx );
        var delete_element = $( '#publications-delete-' + idx );
        var edit_element = $( '#publications-edit-' + idx );
    
        if (list_element)
          list_element.attr( 'style', itemstyle );
    
        if (delete_element)
          delete_element.addClass( 'hide' );
        
        if (edit_element)
          edit_element.addClass( 'hide' );
      }
    }
  },
  onitemclick: function( idx )
  {
    if (idx == null) return;
    
    var publications = listpublication.publications;
    if (publications == null || publications.length == 0)
      return;
    
    if (idx < 0) {
      messager.showerror( strings( 'You have reached the first item' ) );
      return;
    }
    
    if (idx >= publications.length) {
      messager.showerror( strings( 'You have reached the last item' ) );
      return;
    }
    
    if (idx >= 0 && idx < publications.length) {
      var publication = publications[idx];
      if (publication) {
        var id = publication['id'];
        var sid = publication['streamid'];
        var channel = publication['channel'];
        var stype = publication['stype'];
        var owner = publication['owner'];
        var text = publication['subject'];
        var ctype = publication['contenttype'];
        var status = publication['status'];
        var flag = publication['flag'];
        var ptime = publication['ptime'];
        var attcount = publication['attcount'];
        
        if (id && stype && channel) {
          listpublication.click_item( stype, channel, id, idx );
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
  click_stream: function( stype, streamid )
  {
    if (streamid == null || streamid.length == 0)
      return;
    
    var username = this.username;
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
  edit_item: function( type, channel, id )
  {
    var username = this.username;
    var me = globalApp.get_username();
    if (username == null || username.length == 0)
      username = me;
    
    if (type != null && id != null) {
      publish.success_cb = listpublication.refresh;
      publish.showedit( username, type, id );
    }
  },
  click_item: function( type, channel, id, idx )
  {
    if (channel == 'Draft') {
      listpublication.edit_item( type, channel, id );
      return;
    }
    
    var username = listpublication.username;
    publishinfo.showinfo( username, type, channel, id, 
      function() {
        var publications = listpublication.publications;
        if (publications != null && publications.length > 0 && idx != null && idx >= 0) {
          var previdx = idx - 1;
          var nextidx = idx + 1;
          $( '#publishinfo-prev' )
            .attr( 'onclick', 'javascript:listpublication.onitemclick(' + previdx + ');return false;' )
            .removeClass( 'hide' );
          $( '#publishinfo-next' )
            .attr( 'onclick', 'javascript:listpublication.onitemclick(' + nextidx + ');return false;' )
            .removeClass( 'hide' );
        }
      });
  },
  delete_item: function( type, channel, id )
  {
    var username = listpublication.username;
    
    if (channel == 'Trash') {
      publishform.delete_item( username, type, channel, id, 
        function() {
          listpublication.relist();
        });
    } else {
      publishform.trash_item( username, type, channel, id, 
        function() {
          listpublication.relist();
        });
    }
  },
  starred_item: function( type, channel, id )
  {
    var username = listpublication.username;
    
    publishform.setflag_item( username, type, channel, id, 'favorite', 
      function() {
        listpublication.relist();
      });
  },
  unstarred_item: function( type, channel, id )
  {
    var username = listpublication.username;
    
    publishform.setflag_item( username, type, channel, id, 'null', 
      function() {
        listpublication.relist();
      });
  },
  refresh: function()
  {
    var context = system.context;
    var path = context.path;
    
    if (path.indexOf('posts') >= 0 || 
        path.indexOf('subscriptions') >= 0 || 
        path.indexOf('comments') >= 0) {
      listpublication.relist();
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

// #/~posts
sammy.get
(
  // /^#\/(~posts)$/,
  new RegExp( '(~posts)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(9);
    var id_param = path_param;
    var sub_param = null;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) {
        id_param = path_param.substring(0, pos);
        sub_param = path_param.substring(pos+1);
      }
    }
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/publications.html',
      function( template )
      {
        body_element
          .html( template );

        listpublication.showposts( id_param, sub_param );

        statusbar.show();
      }
    );
  }
);

// #/~subscriptions
sammy.get
(
  // /^#\/(~subscriptions)$/,
  new RegExp( '(~subscriptions)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(17);
    var id_param = path_param;
    var sub_param = null;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) {
        id_param = path_param.substring(0, pos);
        sub_param = path_param.substring(pos+1);
      }
    }
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/publications.html',
      function( template )
      {
        body_element
          .html( template );

        listpublication.showsubscriptions( id_param, sub_param );

        statusbar.show();
      }
    );
  }
);

// #/~comments
sammy.get
(
  // /^#\/(~comments)$/,
  new RegExp( '(~comments)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(12);
    var id_param = path_param;
    var sub_param = null;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) {
        id_param = path_param.substring(0, pos);
        sub_param = path_param.substring(pos+1);
      }
    }
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/publications.html',
      function( template )
      {
        body_element
          .html( template );

        listpublication.showcomments( id_param, sub_param );

        statusbar.show();
      }
    );
  }
);

// #/~featured
sammy.get
(
  // /^#\/(~featured)$/,
  new RegExp( '(~featured)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(12);
    var id_param = path_param;
    var sub_param = null;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) {
        id_param = path_param.substring(0, pos);
        sub_param = path_param.substring(pos+1);
      }
    }
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/publications.html',
      function( template )
      {
        body_element
          .html( template );

        listpublication.showfeatured( id_param, sub_param );

        statusbar.show();
      }
    );
  }
);

// #/~apps
sammy.get
(
  // /^#\/(~apps)$/,
  new RegExp( '(~apps)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var path_param = this.path.slice(8);
    var id_param = path_param;
    var sub_param = null;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) {
        id_param = path_param.substring(0, pos);
        sub_param = path_param.substring(pos+1);
      }
    }
    
    message_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/publications.html',
      function( template )
      {
        body_element
          .html( template );

        listpublication.showapps( id_param, sub_param );

        statusbar.show();
      }
    );
  }
);
