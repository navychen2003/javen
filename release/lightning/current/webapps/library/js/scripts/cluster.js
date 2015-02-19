
var clusterform = {
  successcb: null,
  
  add_host: function()
  {
    var address = $( '#cluster-add-address' ).attr( 'value' ).trim();
    if (address == null || address.length == 0) {
      this.showerror( 'Host address is empty' );
      return;
    }
    
    this.invite_host(address);
  },
  invite_host: function( address )
  {
    if (address == null || address.length == 0)
      return;
    
    var params = '&action=invite&address=' + encodeURIComponent(address);
    
    $.ajax
    (
      {
        url : app.user_path + '/cluster?token=' + app.token + params + '&wt=json',
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
            var cb = clusterform.successcb;
            if (cb) cb.call(clusterform);
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
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var listcluster = {
  showlist: function()
  {
    navbar.init_name( strings( 'Setting' ), null, '#/~settings' );
    
    var params = '&action=list';
    
    $.ajax
    (
      {
        url : app.user_path + '/cluster?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var clusters = response['clusters'];
          var clusterid = response['clusterid'];
          var scheme = response['scheme'];
          listcluster.init_content( clusters, clusterid, scheme );
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
  init_content: function( clusters, selfid, scheme )
  {
    if (clusters == null) clusters = {};
    
    var clusterCount = 0;
    var clusterContent = [];
    
    for (var akey in clusters) {
      var cluster = clusters[akey];
      if (cluster != null) {
        var clusterid = cluster['clusterid'];
        var clusterdomain = cluster['clusterdomain'];
        var hostcount = cluster['hostcount'];
        var hosts = cluster['hosts'];
        
        if (clusterid == null) clusterid = '';
        if (clusterdomain == null) clusterdomain = '';
        if (hostcount == null) hostcount = 0;
        
        var firstMode = '';
        var firstAttachUsers = '';
        var firstDomain = '';
        var firstHostKey = '';
        var firstHostAddr = '';
        var firstHostName = '';
        var firstLanAddr = '';
        var firstHttpPort = 0;
        var firstHttpsPort = 0;
        var firstHeartbeat = 0;
        var firstStatus = 0;
		var firstHash = 0;
        var firstSelf = false;
        
        if (hosts) {
          for (var hkey in hosts) {
            var host = hosts[hkey];
            if (host == null) continue;
            
            var hostmode = host['mode'];
            var attachusers = host['attachusers'];
            var hostkey = host['key'];
            var domain = host['domain'];
            var hostaddr = host['hostaddr'];
            var lanaddr = host['lanaddr'];
            var hostname = host['hostname'];
            var httpport = host['httpport'];
            var httpsport = host['httpsport'];
            var heartbeat = host['heartbeat'];
            var status = host['status'];
			var hashcode = host['hashcode'];
            var isself = host['self'];
            
            if (hostmode == null) hostmode = '';
            if (attachusers == null) attachusers = '';
            if (domain == null) domain = '';
            if (hostkey == null) hostkey = '';
            if (hostaddr == null) hostaddr = '';
            if (lanaddr == null) lanaddr = '';
            if (hostname == null) hostname = '';
            if (httpport == null) httpport = 80;
            if (httpsport == null) httpsport = 443;
            if (heartbeat == null) heartbeat = 0;
            if (status == null) status = 0;
			if (hashcode == null) hashcode = 0;
            if (isself == null) isself = false;
            
            firstMode = hostmode.toLowerCase();
            firstAttachUsers = attachusers;
            firstDomain = domain;
            firstHostKey = hostkey;
            firstHostAddr = hostaddr;
            firstLanAddr = lanaddr;
            firstHostName = hostname;
            firstHttpPort = httpport;
            firstHttpsPort = httpsport;
            firstHeartbeat = heartbeat;
            firstStatus = status;
			firstHash = hashcode;
            firstSelf = isself;
            
            break;
          }
        }
        
        var title = clusterid;
        var subtitle = '' + hostcount;
        var hoststatus = '';
        
        var thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
        var thumbClick = 'javascript:listcluster.show_cluster(\'' + clusterid + '\');return false;';
        var joinClick = 'javascript:listcluster.join_cluster(\'' + clusterid + '\');return false;';
        var okClick = 'javascript:return false;';
        var errClick = 'javascript:return false;';
        
        var okhide = 'hide', errhide = 'hide';
        
        if (hostcount > 1) {
          var text = strings( 'There are {0} hosts' );
          
		  title = clusterid;
		  if (clusterdomain != null && clusterdomain.length > 0)
            title = clusterid + '/' + clusterdomain;
		  
          title = strings('Cluster') + ':' + title;
          subtitle = text.format( hostcount );
          
		  if (hostcount > 0) { 
            okhide = '';
            errhide = 'hide';
          } else { 
            okhide = 'hide';
            errhide = '';
          }
		  
        } else {
          var port = firstHttpPort;
          if (scheme == 'https') port = firstHttpsPort;
          
          title = firstDomain + '/' + clusterid + '/' + firstHash + '/' + firstHostKey;
          subtitle = firstHostName + '(' + firstHostAddr + ':' + port + ')';
          
          if (firstMode == 'attach')
            title = strings('Storage Node') + ':' + title;
		  else if (firstMode == 'backup')
		    title = strings('Backup Node') + ':' + title;
          else if (clusterid == selfid)
            title = strings('Host Self') + ':' + title;
          else
            title = strings('Host') + ':' + title;
          
          var statusStr = strings('Unknown');
          if (firstStatus == 1) { 
            statusStr = strings('Ok');
            okhide = '';
            errhide = 'hide';
          } else if (firstStatus != 1 && firstStatus != 0) { 
            statusStr = strings('Error');
            okhide = 'hide';
            errhide = '';
          }
          
          hoststatus = strings('LAN IP') + ':' + firstLanAddr + ' '
            + strings('Heartbeat Time') + ':' + format_time(firstHeartbeat) + ' ' 
            + strings('Status') + ':' + statusStr + ' '
			+ strings('Mode') + ':' + firstMode;
          
          if (firstMode == 'attach')
            hoststatus += ' ' + strings('Users') + ':' + firstAttachUsers;
        }
        
        var actionhide = '';
        //if (clusterid == selfid || firstSelf == true) 
        //  actionhide = 'hide';
        
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions ' + actionhide + '">' + "\n" +
                   '        <button type="button" class="add-btn btn btn-icon hide" title="' + strings('Join Cluster') + '" onClick="' + joinClick + '"><i class="glyphicon circle-plus"></i></button>' + "\n" +
                   '        <button type="button" class="accept-btn btn btn-success btn-icon ' + okhide + '" rel="tooltip" title="' + strings('Ok') + '" onClick="' + okClick + '"><i class="glyphicon circle-ok"></i></button>' + "\n" +
                   '        <button type="button" class="reject-btn btn btn-danger btn-icon ' + errhide + '" rel="tooltip" title="' + strings('Error') + '" onClick="' + errClick + '"><i class="glyphicon ban"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3 title="' + title.esc() + '">' + title.esc() + '</h3>' + "\n" +
                   '    <h4 title="' + hoststatus.esc() + '">' + subtitle.esc() + '</h4>' + "\n" +
                   '</li>';
        
        clusterContent.push( item );
        clusterCount ++;
      }
    }
    
    var clusterTitle = strings( 'Host & Cluster' ) 
      + ' <span class="well-header-count">' + clusterCount + '</span>';
    
    $( '#cluster-list-title' ).html( clusterTitle );
    $( '#cluster-list' ).html( clusterContent.join( "\n" ) );
    
    if (clusterContent.length == 0) {
      $( '#cluster-empty' )
        .html( strings( 'No hosts :(' ) )
        .removeClass( 'hide' );
    } else {
      $( '#cluster-empty' )
        .addClass( 'hide' );
    }
  },
  show_cluster: function( clusterid )
  {
    if (clusterid == null || clusterid.length == 0)
	  return;
	
    var context = system.context;
	context.redirect( '#/~cluster/' + encodeURIComponent(clusterid) );
  },
  join_cluster: function( clusterid )
  {
  },
  showcluster: function( clusterid )
  {
    navbar.init_name( strings( 'Setting' ), null, '#/~settings' );
    
    var params = '&action=cluster&clusterid=' + encodeURIComponent(clusterid);
    
    $.ajax
    (
      {
        url : app.user_path + '/cluster?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          show_loading();
        },
        success : function( response )
        {
          var cluster = response['cluster'];
          var clusterid = response['clusterid'];
          var scheme = response['scheme'];
          listcluster.init_cluster( cluster, clusterid, scheme );
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
  init_cluster: function( cluster, cid, scheme )
  {
    if (cluster == null) cluster = {};
	
	var clusterCount = 0;
    var clusterContent = [];
	
	var clusterid = cluster['clusterid'];
	var clusterdomain = cluster['clusterdomain'];
	var hostcount = cluster['hostcount'];
	var hosts = cluster['hosts'];
	
	if (clusterid == null) clusterid = cid;
	if (clusterdomain == null) clusterdomain = '';
	if (hostcount == null) hostcount = 0;
	if (hosts == null) hosts = {};
	
	var clusterName = clusterid;
	
	if (clusterdomain != null && clusterdomain.length > 0)
		clusterName = clusterid + '/' + clusterdomain;
	
	for (var hkey in hosts) {
		var host = hosts[hkey];
		if (host == null) continue;
		
		var hostmode = host['mode'];
		var attachusers = host['attachusers'];
		var hostkey = host['key'];
		var domain = host['domain'];
		var hostaddr = host['hostaddr'];
		var lanaddr = host['lanaddr'];
		var hostname = host['hostname'];
		var httpport = host['httpport'];
		var httpsport = host['httpsport'];
		var heartbeat = host['heartbeat'];
		var status = host['status'];
		var hashcode = host['hashcode'];
		var isself = host['self'];
		
		if (hostmode == null) hostmode = '';
		if (attachusers == null) attachusers = '';
		if (domain == null) domain = '';
		if (hostkey == null) hostkey = '';
		if (hostaddr == null) hostaddr = '';
		if (lanaddr == null) lanaddr = '';
		if (hostname == null) hostname = '';
		if (httpport == null) httpport = 80;
		if (httpsport == null) httpsport = 443;
		if (heartbeat == null) heartbeat = 0;
		if (status == null) status = 0;
		if (hashcode == null) hashcode = 0;
		if (isself == null) isself = false;
		
		hostmode = hostmode.toLowerCase();
		
		var title = hostname;
        var subtitle = '' + hostcount;
        var hoststatus = '';
        
        var thumbsrc = 'css/' + app.theme + '/images/posters/poster.png';
        var thumbClick = 'javascript:listcluster.show_host(\'' + hostkey + '\');return false;';
        var joinClick = 'javascript:listcluster.join_cluster(\'' + clusterid + '\');return false;';
        var okClick = 'javascript:return false;';
        var errClick = 'javascript:return false;';
        
        var okhide = 'hide', errhide = 'hide';
        
        if (true) {
          var port = httpport;
          if (scheme == 'https') port = httpsport;
          
          title = domain + '/' + clusterid + '/' + hashcode + '/' + hostkey;
          subtitle = hostname + '(' + hostaddr + ':' + port + ')';
		  
          if (hostmode == 'attach')
            title = strings('Storage Node') + ':' + title;
		  else if (hostmode == 'backup')
		    title = strings('Backup Node') + ':' + title;
          else if (isself == true)
            title = strings('Host Self') + ':' + title;
          else
            title = strings('Host') + ':' + title;
          
          var statusStr = strings('Unknown');
          if (status == 1) { 
            statusStr = strings('Ok');
            okhide = '';
            errhide = 'hide';
          } else if (status != 1 && status != 0) { 
            statusStr = strings('Error');
            okhide = 'hide';
            errhide = '';
          }
          
          hoststatus = strings('LAN IP') + ':' + lanaddr + ' '
            + strings('Heartbeat Time') + ':' + format_time(heartbeat) + ' ' 
            + strings('Status') + ':' + statusStr + ' '
			+ strings('Mode') + ':' + hostmode;
          
          if (hostmode == 'attach')
            hoststatus += ' ' + strings('Users') + ':' + attachusers;
        }
        
        var actionhide = '';
        //if (firstSelf == true) actionhide = 'hide';
        
        var item = '<li class="well">' + "\n" +
                   '    <div class="actions ' + actionhide + '">' + "\n" +
                   '        <button type="button" class="add-btn btn btn-icon hide" title="' + strings('Join Cluster') + '" onClick="' + joinClick + '"><i class="glyphicon circle-plus"></i></button>' + "\n" +
                   '        <button type="button" class="accept-btn btn btn-success btn-icon ' + okhide + '" rel="tooltip" title="' + strings('Ok') + '" onClick="' + okClick + '"><i class="glyphicon circle-ok"></i></button>' + "\n" +
                   '        <button type="button" class="reject-btn btn btn-danger btn-icon ' + errhide + '" rel="tooltip" title="' + strings('Error') + '" onClick="' + errClick + '"><i class="glyphicon ban"></i></button>' + "\n" +
                   '    </div>' + "\n" +
                   '    <img class="poster friend-poster placeholder" style="cursor:hand;" onClick="' + thumbClick + '" src="' + thumbsrc + '" />' + "\n" +
                   '    <h3 title="' + title.esc() + '">' + title.esc() + '</h3>' + "\n" +
                   '    <h4 title="' + hoststatus.esc() + '">' + subtitle.esc() + '</h4>' + "\n" +
                   '</li>';
        
        clusterContent.push( item );
        clusterCount ++;
	}
	
	var clusterTitle = strings('Cluster') + ': ' + clusterName 
      + ' <span class="well-header-count">' + clusterCount + '</span>';
    
    $( '#cluster-list-title' ).html( clusterTitle );
    $( '#cluster-list' ).html( clusterContent.join( "\n" ) );
    
    if (clusterContent.length == 0) {
      $( '#cluster-empty' )
        .html( strings( 'No hosts :(' ) )
        .removeClass( 'hide' );
    } else {
      $( '#cluster-empty' )
        .addClass( 'hide' );
    }
  },
  show_host: function( hostkey )
  {
  }
};

var cluster_headbar = {
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
        $( '#settings-link' ).addClass( 'active' );
        
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

// #/~cluster
sammy.get
(
  // /^#\/(~cluster)$/,
  new RegExp( '(~cluster)\\/' ),
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var lang = globalApp.get_language();
    if (lang == null || lang.length == 0)
      lang = 'all';

	var path_param = this.path.slice(11);
    var id_param = path_param;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) id_param = path_param.substring(0, pos);
    }
	  
    cluster_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/clusters.html',
      function( template )
      {
        body_element
          .html( template );

        $( '#cluster-add-submit-text' ).html( strings( 'Add Host' ) );
        $( '#cluster-add-address' ).attr( 'placeholder', strings( 'a host name(address:port)' ) );

        $( '#cluster-add-submit' )
          .attr( 'onClick', 'javascript:clusterform.add_host();return false;' )
          .attr( 'title', strings( 'Add Host' ) );

        $( '#setting-title' )
          .attr( 'href', '#/~settings' )
          .html( strings( 'Setting' ) );

        $( '#cluster-title' )
          .attr( 'href', '#/~clusters' )
          .html( strings( 'Host' ) );

        $( '#announcement-title' )
          .attr( 'href', '#/~announcements/' + encodeURIComponent(lang) )
          .html( strings( 'Announcement' ) );

        $( '#publish-title' )
          .attr( 'href', '#/~featured/' + encodeURIComponent('system') )
          .html( strings( 'Publish' ) );

        if (globalApp.is_admin()) {
          $( '#publish-subnav' ).removeClass( 'hide' );
          $( '#cluster-subnav' ).removeClass( 'hide' );
		  $( '#announcement-subnav' ).removeClass( 'hide' );
        } else {
          $( '#publish-subnav' ).addClass( 'hide' );
          $( '#cluster-subnav' ).addClass( 'hide' );
		  $( '#announcement-subnav' ).addClass( 'hide' );
        }

        listcluster.showcluster( id_param );

        statusbar.show();
      }
    );
  }
);

// #/~clusters
sammy.get
(
  /^#\/(~clusters)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    var lang = globalApp.get_language();
    if (lang == null || lang.length == 0)
      lang = 'all';

    cluster_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/clusters.html',
      function( template )
      {
        body_element
          .html( template );

        $( '#cluster-add-submit-text' ).html( strings( 'Add Host' ) );
        $( '#cluster-add-address' ).attr( 'placeholder', strings( 'a host name(address:port)' ) );

        $( '#cluster-add-submit' )
          .attr( 'onClick', 'javascript:clusterform.add_host();return false;' )
          .attr( 'title', strings( 'Add Host' ) );

        $( '#setting-title' )
          .attr( 'href', '#/~settings' )
          .html( strings( 'Setting' ) );

        $( '#cluster-title' )
          .attr( 'href', '#/~clusters' )
          .html( strings( 'Host' ) );

        $( '#announcement-title' )
          .attr( 'href', '#/~announcements/' + encodeURIComponent(lang) )
          .html( strings( 'Announcement' ) );

        $( '#publish-title' )
          .attr( 'href', '#/~featured/' + encodeURIComponent('system') )
          .html( strings( 'Publish' ) );

        if (globalApp.is_admin()) {
          $( '#publish-subnav' ).removeClass( 'hide' );
          $( '#cluster-subnav' ).removeClass( 'hide' );
          $( '#announcement-subnav' ).removeClass( 'hide' );
          
        } else {
          $( '#publish-subnav' ).addClass( 'hide' );
          $( '#cluster-subnav' ).addClass( 'hide' );
          $( '#announcement-subnav' ).addClass( 'hide' );
        }

        listcluster.showlist();

        statusbar.show();
      }
    );
  }
);
