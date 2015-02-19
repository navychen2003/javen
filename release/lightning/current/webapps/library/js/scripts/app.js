Number.prototype.esc = function()
{
  return new String( this ).esc();
}

String.prototype.esc = function()
{
  return this.replace( /</g, '&lt;' ).replace( />/g, '&gt;' );
}

String.prototype.trim = function()
{
  return this.replace(/(^\s*)|(\s*$)/g,""); 
}

String.prototype.replaceAll = function(s1,s2) 
{
  return this.replace(new RegExp(s1,"gm"),s2);
}

String.prototype.format = function() 
{ 
    var args = arguments; 
    return this.replace(/\{(\d+)\}/g, 
        function(m,i){ 
            return args[i]; 
        }); 
} 

String.format = function() { 
    if( arguments.length == 0 ) 
        return null; 

    var str = arguments[0]; 
    for (var i=1; i < arguments.length; i++) { 
        var re = new RegExp('\\{' + (i-1) + '\\}','gm'); 
        str = str.replace(re, arguments[i]); 
    }
    
    return str; 
}

function parseDate(iso8601) 
{
  if (typeof(iso8601) == 'number') { 
    return new Date(iso8601);
  }
  
  if (typeof(iso8601) == 'string') {
    var s = $.trim(iso8601);
    s = s.replace(/\.\d\d\d+/,""); // remove milliseconds
    s = s.replace(/-/g,"/");
    s = s.replace(/(\d)T(\d)/,"$1 $2").replace(/(\d)Z/,"$1 UTC");
    s = s.replace(/([\+\-]\d\d)\:?(\d\d)/," $1$2"); // -04:00 -> -0400
    return new Date(s);
  }
  
  return new Date(0);
}

function trim_line( text, linelength )
{
  if (text == null) text = '';
  if (linelength == null || linelength <= 0)
    linelength = 50;
  
  if (typeof(text) == 'string') {
    text = text.replaceAll( '\r', '' );
    text = text.replaceAll( '\n', '' );
    text = text.replaceAll( '\t', '' );
    text = text.trim();
    
    if (text.length > linelength)
      text = text.substring(0, linelength) + '...';
  }
  
  return text;
}

function is_empty_object( obj ) 
{
  if (obj) {
    for ( var name in obj ) {
      return false;
    }
  }
  return true;
}

function getElementLeft( element )
{
  var actualLeft = element.offsetLeft;
  var current = element.offsetParent;

  while (current !== null) {
    actualLeft += current.offsetLeft;
    current = current.offsetParent;
  }

  return actualLeft;
}

function format_json( json_str )
{
  if (JSON.stringify && JSON.parse) {
    json_str = JSON.stringify( JSON.parse( json_str ), undefined, 2 );
  }

  return json_str;
};

function duration_to_seconds( str )
{
  var seconds = 0;
  var arr = new String( str || '' ).split( '.' );
  var parts = arr[0].split( ':' ).reverse();
  var parts_count = parts.length;

  for (var i = 0; i < parts_count; i++) {
    seconds += ( parseInt( parts[i], 10 ) || 0 ) * Math.pow( 60, i );
  }

  // treat more or equal than .5 as additional second
  if (arr[1] && 5 <= parseInt( arr[1][0], 10 )) {
    seconds++;
  }

  return seconds;
};

function readableSeconds( seconds )
{
  seconds = parseInt( seconds || 0, 10 );
  var minutes = Math.floor( seconds / 60 );
  var hours = Math.floor( minutes / 60 );

  var text = [];
  if (hours !== 0)
  {
    text.push( hours + 'h' );
    seconds -= hours * 60 * 60;
    minutes -= hours * 60;
  }

  if (minutes !== 0)
  {
    text.push( minutes + 'm' );
    seconds -= minutes * 60;
  }

  if (seconds !== 0)
  {
    text.push( ( '0' + seconds ).substr( -2 ) + 's' );
  }

  return text.join( ' ' );
};

function readableBytes( bytes )
{
  var bb = bytes;
  var kb = Math.floor( bb / 1024 );
  var mb = Math.floor( kb / 1024 );
  var gb = Math.floor( mb / 1024 );

  var text = [];
  if (gb > 0)
  {
    text.push( gb + ' GB' );
    bb -= gb * 1024 * 1024 * 1024;
    kb -= gb * 1024 * 1024;
    mb -= gb * 1024;
  }

  if (mb > 0)
  {
    text.push( mb + ' MB' );
    bb -= mb * 1024 * 1024;
    kb -= mb * 1024;
  }

  if (kb > 0)
  {
    text.push( kb + ' KB' );
    bb -= kb * 1024;
  }

  if (text.length == 0)
  {
    text.push( bb + ' ' + strings( 'Bytes' ) );
  }

  text.push( '(' + bytes + ' ' + strings( 'Bytes' ) + ')' );

  return text.join( ' ' );
};

function readableSeconds2( seconds )
{
  seconds = parseInt( seconds || 0, 10 );
  var minutes = Math.floor( seconds / 60 );
  var hours = Math.floor( minutes / 60 );

  var text = [];
  if (hours !== 0)
  {
    text.push( hours + 'h' );
    seconds -= hours * 60 * 60;
    minutes -= hours * 60;
  }

  if (minutes !== 0)
  {
    text.push( minutes + 'm' );
    seconds -= minutes * 60;
  }

  if (seconds !== 0 || text.length == 0)
  {
    text.push( seconds + 's' );
  }

  return text.join( ' ' );
};

function readableBytes2( bytes )
{
  var bb = bytes;
  var kb = Math.floor( bb / 1024 );
  var mb = Math.floor( kb / 1024 );
  var gb = Math.floor( mb / 1024 );

  var text = [];
  if (gb > 0)
  {
    var num = bytes / (1024 * 1024 * 1024);
    if (num > 0) {
      num = Math.round(num*100)/100;
      return num + ' GB';
    }
    
    text.push( gb + ' GB' );
    bb -= gb * 1024 * 1024 * 1024;
    kb -= gb * 1024 * 1024;
    mb -= gb * 1024;
  }

  if (mb > 0)
  {
    text.push( mb + ' MB' );
    bb -= mb * 1024 * 1024;
    kb -= mb * 1024;
  }

  if (kb > 0)
  {
    text.push( kb + ' KB' );
    bb -= kb * 1024;
  }

  if (text.length == 0)
  {
    text.push( bb + ' ' + strings( 'Bytes' ) );
  }

  return text[0]; //text.join( ' ' );
};

var SECRET_KEYS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

function encodeSecret( input ) 
{
  if (input == null) input = "";
  
  var output = "";
  var chr1, chr2, chr3 = "";
  var enc1, enc2, enc3, enc4 = "";
  var i = 0;

  do {
     chr1 = input.charCodeAt(i++);
     chr2 = input.charCodeAt(i++);
     chr3 = input.charCodeAt(i++);

     enc1 = chr1 >> 2;
     enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
     enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
     enc4 = chr3 & 63;

     if (isNaN(chr2)) {
        enc3 = enc4 = 64;
     } else if (isNaN(chr3)) {
        enc4 = 64;
     }

     output = output + 
        SECRET_KEYS.charAt(enc1) + 
        SECRET_KEYS.charAt(enc2) + 
        SECRET_KEYS.charAt(enc3) + 
        SECRET_KEYS.charAt(enc4);
     
     chr1 = chr2 = chr3 = "";
     enc1 = enc2 = enc3 = enc4 = "";
  } while (i < input.length);

  return output;
};

var sammy = $.sammy
(
  function()
  {
    this.bind
    (
      'run',
      function( event, config )
      {
        if (config.start_url.length === 0)
        {
          location.href = '#/~dashboard';
          return false;
        }
      }
    );

    this.bind
    (
      'error',
      function( message, original_error )
      {
        alert( original_error.message );
      }
    );
        
    // activate_core
    this.before
    (
      {},
      function( context )
      {
      }
    );
  }
);

var statusbar = {
  display: false,
  error: null,
  id: null,
  type: null,
  message: null,
  clickto: null,
  linkto: null,
  time: null,
  timer: null,
  
  init: function( works )
  {
    if (works && works.length > 0)
    {
      var work = works[0];
      var id = work['id'];
      if (id)
      {
        this.display = true;
        this.id = work['id'];
        this.type = work['type'];
        this.message = work['message'];
        this.time = work['start'];
        this.clickto = 'javascript:return false;';
        this.linkto = '';
        
        return;
      }
    }
    
    var invite = globalApp.invite_next;
    if (invite) {
      var key = invite['key'];
      var name = invite['name'];
      var title = invite['title'];
      var type = invite['type'];
      var message = invite['message'];
      var time = invite['time'];
      
      if (name == null) name = '';
      if (title == null || title.length == 0) title = name;
      
      if (type == 'in') {
        if (message == null || message.length == 0)
          message = strings( 'wants to be your friend' );
        else
          message = strings( message );
        
        message = title + ' ' + message;
        
        this.id = key;
        this.type = type;
        this.message = message;
        this.time = time;
        this.clickto = '';
        this.linkto = '#/~friends';
        this.display = true;
        
        return;
      }
    }
    
    var message = globalApp.message_next;
    if (message) {
      var id = message['id'];
      var type = message['type'];
      var folder = message['folder'];
      var from = message['from'];
      var to = message['to'];
      var cc = message['cc'];
      var text = message['subject'];
      var ctype = message['ctype'];
      var status = message['status'];
      var flag = message['flag'];
      var ctime = message['ctime'];
      var utime = message['utime'];
      var mtime = message['mtime'];
      var attcount = message['attcount'];
      var fromtitle = message['fromtitle'];
      
      if (id == null) id = '';
      if (type == null) type = '';
      if (folder == null) folder = '';
      if (from == null) from = '';
      if (fromtitle == null || fromtitle.length == 0) fromtitle = from;
      if (attcount == null) attcount = 0;
      
      if (status == 'new') {
        if (text == null || text.length == 0)
          text = strings( '[No Subject]' );
      
        text = text.replaceAll( '\r', '' );
        text = text.replaceAll( '\n', '' );
        text = text.replaceAll( '\t', '' );
        text = text.replaceAll( 'Fw:', '' );
        text = text.replaceAll( 'Re:', '' );
        
        text = text.trim();
      
        var msg = strings( 'Received message from {0}: {1}' );
        msg = msg.format( fromtitle, text );
        
        var username = globalApp.get_username();
        if (username == null) username = '';
        
        var clickto = 'javascript:messageinfo.showmessage(\'' + username.esc() + '\',\'' + type.esc() + '\',\'' + folder.esc() + '\',\'' + id.esc() + '\');return false;';
        
        this.id = id;
        this.type = type;
        this.message = msg;
        this.time = mtime;
        this.clickto = clickto;
        this.linkto = ''; //'#/~messages';
        this.display = true;
        
        return;
      }
    }
    
    this.display = false;
  },
  show: function() 
  {
    var statusbar_element = $( '#statusbar' );
    var statusbarmessage_element = $( '#statusbar-message' );
    var statusbaricon_element = $( '#statusbar-icon' );
    var statusbarlink_element = $( '#statusbar-link' );
    
    if (statusbar_element && statusbarmessage_element)
    {
      if (this.display)
      {
        var message = '';
        var linkto = '';
        var clickto = '';
        
        if (this.id) {
          message = this.message;
          linkto = this.linkto;
          clickto = this.clickto;
        }
        
        if (message == null) message = '';
        if (linkto == null) linkto = '';
        if (clickto == null) clickto = '';
      
        statusbar_element
          .removeClass( 'hide' );
      
        statusbarmessage_element
          .html( message.esc() );
        
        statusbarlink_element
          .attr( 'onclick', clickto )
          .attr( 'href', linkto );
      }
      else
      {
        statusbar_element
          .addClass( 'hide' );
      }
    }
    
    var error = this.error;
    this.error = null;
    if (error) show_error( error );
  },
  startTimer: function()
  {
    if (this.timer) return;
    //this.stopTimer();
    
    this.timer = $.timer(10000, function() {
        if (globalApp.update_error) return;
        
        var uptime = globalApp.update_time;
        var now = new Date();
        if (uptime && now.getTime() - uptime.getTime() < 10000)
          return;
        
        globalApp.update( 'get', function() {
            statusbar.show();
            if (listdashboard) listdashboard.showannouncementnext();
          } );
      });
  },
  stopTimer: function()
  {
    var timer = this.timer;
    this.timer = null;
    if (timer) timer.stop();
    
    statusbar.show();
  }
};

var localization = null;
var localizations = {
  lang: null,
  init: function( lang, cb )
  {
    if (lang == null || lang.length == 0) return;
    if (lang == this.lang && localization) {
      if (cb) cb.call(localizations);
      return;
    }
    $.get
    (
      'localizations/' + lang + '.json',
      function( data )
      {
        if (typeof data === "string")
          localization = JSON.parse( data );
        else
          localization = data;
        
        $.datepicker.setDefaults($.datepicker.regional[lang]);
        
        localizations.lang = lang;
        if (cb) cb.call(localizations);
      },
      function( response, status, xhr )
      {
      },
      'text'
    );
  }
}

function strings( str ) 
{
  if (localization) 
  {
    var text = localization[ str ];
    if (text) 
      return text;
  }
  
  return str;
};

var system = {
  friendlyName: null,
  hostMode: null,
  hostKey: null,
  hostDomain: null,
  hostAddress: null,
  hosts: null,
  scheme: null,
  context: null,
  backPath: null,
  backPaths: [],
  expiresDays: 7,
  
  set_expiredays: function( days )
  {
    if (days != null && days >= 0)
      this.expiresDays = days;
  },
  save_cookie: function( name, value )
  {
    if (name == null || name.length == 0)
      return;
    
    var days = this.expiresDays;
    if (days != null && days > 0 && value != null) {
      $.cookie( 'lightning.' + name, null);
      $.cookie( 'lightning.' + name, value, {expires: days, path: '/'} );
      return;
    }
    
    this.set_cookie( name, value );
  },
  set_cookie: function( name, value )
  {
    if (name && name.length > 0)
      $.cookie( 'lightning.' + name, value, {path: '/'} );
  },
  get_cookie: function( name )
  {
    if (name && name.length > 0)
      return $.cookie( 'lightning.' + name );
    
    return null;
  },
  remove_cookie: function( name )
  {
    this.set_cookie( name, null );
  },
  get_hostlocation: function()
  {
    var address = this.get_attachhost_address();
    if (address != null && address.length > 0) 
      return address + '/library/';
    return '';
  },
  get_attachhost_address: function()
  {
    var scheme = this.scheme;
    var attachHosts = this.hosts;
    var attachHostKey = globalApp.get_attachhostkey();
    if (attachHosts == null || attachHostKey == null) 
      return null;
    
    for (var key in attachHosts) {
      var attachHost = attachHosts[key];
      if (attachHost == null) continue;
      
      var domain = attachHost['domain'];
      var hostkey = attachHost['key'];
      var hostname = attachHost['hostname'];
      var hostaddr = attachHost['hostaddr'];
      var httpport = attachHost['httpport'];
      var httpsport = attachHost['httpsport'];
      
      if (hostkey != attachHostKey)
        continue;
      
      var hostAddress = null;
      if (hostaddr != null && hostaddr.length > 0) {
        if (httpport == null) httpport = 80;
        if (httpsport == null) httpsport = 443;
      
        var host = hostaddr;
        if (domain != null && domain.length > 0)
          host = domain;
      
        if (scheme == 'https') {
          hostAddress = 'https://' + host;
          if (httpsport != null && httpsport != 443)
            hostAddress += ':' + httpsport;
        } else {
          hostAddress = 'http://' + host;
          if (httpport != null && httpport != 80)
            hostAddress += ':' + httpport;
        }
      
        return hostAddress;
      }
    }
    
    return null;
  },
  update_hosts: function( content )
  {
    if (content == null) return;
    
    this.hosts = content;
  },
  update_system: function( content )
  {
    if (content == null) return;
    
    var notice = content['notice'];
    var domain = content['domain'];
    var hostkey = content['hostkey'];
    var hostname = content['hostname'];
    var hostaddr = content['hostaddr'];
    var httpport = content['httpport'];
    var httpsport = content['httpsport'];
    var mode = content['mode']
    var scheme = content['scheme'];
    
    var hostAddress = '';
    if (hostaddr != null && hostaddr.length > 0) {
      if (httpport == null) httpport = 80;
      if (httpsport == null) httpsport = 443;
      
      var host = hostaddr;
      if (domain != null && domain.length > 0)
        host = domain;
      
      if (scheme == 'https') {
        hostAddress = 'https://' + host;
        if (httpsport != null && httpsport != 443)
          hostAddress += ':' + httpsport;
      } else {
        hostAddress = 'http://' + host;
        if (httpport != null && httpport != 80)
          hostAddress += ':' + httpport;
      }
      
      this.hostAddress = hostAddress;
      this.scheme = scheme;
    }
    
    if (hostname != null && hostname.length > 0)
      this.friendlyName = hostname;
    
    if (hostkey != null && hostkey.length > 0)
      this.hostKey = hostkey;
    
    if (domain != null && domain.length > 0)
      this.hostDomain = domain;
    
    if (mode != null && mode.length > 0)
      this.hostMode = mode;
    
    if (notice != null && notice.length > 0) {
      var error = {};
      error['code'] = 0;
      error['msg'] = strings( notice );
      error['trace'] = '';
      statusbar.error = error;
    }
  }
};

var globalApp = {
  announcements: {},
  libraries: {},
  storages: {},
  stores: {},
  user: null,
  language: null,
  languages: null,
  theme: null,
  themes: null,
  update_time: null,
  update_error: false,
  user_flag: null,
  used_space: 0,
  usable_space: 0,
  total_space: 0,
  invite_count: 0,
  invite_next: null,
  message_count: 0,
  message_next: null,
  
  init: function()
  {
    this.restore_user();
    
    var lang = this.get_language();
    var theme = this.get_theme();
    
    if (lang == null) lang = '';
    if (theme == null) theme = '';
    
    var params = 'action=all&lang=' + encodeURIComponent(lang) 
      + '&theme=' + encodeURIComponent(theme);
    
    params = params + '&maxinvites=1&maxmessages=1&maxworks=1';
    
    $.ajax
    (
      {
        url : user_path + '/heartbeat?' + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
          reset_page();
          show_loading();
        },
        success : function( response )
        {
          globalApp.update_content(response);
          sammy.run( location.hash );
          statusbar.startTimer();
        },
        error : function( xhr, text_status, error_thrown )
        {
          globalApp.update_error = true;
          statusbar.stopTimer();
          
          request_error( xhr, text_status, error_thrown );
        },
        complete : function()
        {
          hide_loading();
        }
      }
    );
  },
  update: function( action, callback )
  {
    var lang = this.get_language();
    if (lang == null) lang = '';
    if (action == null) action = '';
    
    var params = 'action=' + encodeURIComponent(action) + '&lang=' + encodeURIComponent(lang);
    var token = app.token;
    
    if (token != null && token.length > 0) { 
      params = params + '&token=' + encodeURIComponent(token);
    } else { 
      return; //unauthorized
    }
    
    params = params + '&maxinvites=1&maxmessages=1&maxworks=1';
    
    $.ajax
    (
      {
        url : user_path + '/heartbeat?' + params + '&wt=json',
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {
        },
        success : function( response )
        {
          globalApp.update_content(response);
          if (callback) callback.call( this );
        },
        error : function( xhr, text_status, error_thrown )
        {
          var error = build_error( xhr, text_status, error_thrown );
          if (error != null) statusbar.error = error;
          globalApp.update_error = true;
          statusbar.stopTimer();
        },
        complete : function()
        {
        }
      }
    );
  },
  update_content: function( content )
  {
    if (content == null) return;
    
    this.update_time = new Date();
    this.update_error = false;
    
    var system2 = content['system'];
    var setting = content['setting'];
    var langs = content['langs'];
    var themes = content['themes'];
    var user = content['user'];
    var works = content['works'];
    var hosts = content['hosts'];
    
    if (langs) this.languages = langs;
    if (themes) this.themes = themes;
    
    this.update_system( system2 );
    this.update_hosts( hosts );
    this.update_user( user );
    this.update_setting( setting );
    
    statusbar.init(works);
  },
  update_system: function( content )
  {
    system.update_system( content );
  },
  update_hosts: function( content )
  {
    system.update_hosts( content );
  },
  update_user: function( user ) 
  {
    if (user == null) return; 
    
    var hostkey = system.hostKey;
    var userkey = user['key'];
    var username = user['name'];
    var usertoken = user['token'];
    var usertype = user['type'];
    var userflag = user['flag'];
    var usedspace = user['used'];
    var capacity = user['capacity'];
    var usablespace = user['usable'];
    var invites = user['invites'];
    var messages = user['messages'];
    var attachHostKey = user['attachhostkey'];
    
    if (hostkey == null) hostkey = '';
    if (userkey == null) userkey = '';
    if (username == null) username = '';
    if (usertoken == null) usertoken = '';
    if (usertype == null) usertype = '';
    if (usedspace == null) usedspace = 0;
    if (capacity == null) capacity = 0;
    if (usablespace == null) usablespace = 0;
    
    if (attachHostKey == null) attachHostKey = '';
    
    if (invites == null) invites = {};
    if (messages == null) messages = {};
    
    var invitecount = invites['count'];
    var invitelist = invites['invites'];
    var messagecount = messages['count'];
    var messagelist = messages['messages'];
    
    var invitenext = null;
    var messagenext = null;
    
    if (invitelist != null) {
      for (var key in invitelist) {
        var item = invitelist[key];
        if (item != null) {
          invitenext = item; break;
        }
      }
    }
    
    if (messagelist != null) {
      for (var key in messagelist) {
        var item = messagelist[key];
        if (item != null) {
          messagenext = item; break;
        }
      }
    }
    
    if (invitecount == null) invitecount = 0;
    if (messagecount == null) messagecount = 0;
    
    var oldflag = this.user_flag;
    this.user_flag = userflag;
    this.used_space = usedspace;
    this.usable_space = usablespace;
    this.total_space = capacity;
    this.invite_count = invitecount;
    this.invite_next = invitenext;
    this.message_count = messagecount;
    this.message_next = messagenext;
    
    if (hostkey != null && hostkey.length > 0 && 
        userkey != null && userkey.length > 0 && 
        usertoken != null && usertoken.length > 0 && 
        username != null && username.length > 0) 
    {
      this.user = user;
      app.token = hostkey + userkey + usertoken;
      
      system.set_cookie( 'host.key', hostkey );
      system.set_cookie( 'user.key', userkey );
      system.set_cookie( 'user.name', username );
      system.set_cookie( 'user.token', usertoken );
      system.set_cookie( 'user.type', usertype );
      system.set_cookie( 'user.attachhostkey', attachHostKey );
      
      system.save_cookie( 'login.name', username );
    }
    
    this.check_userflag( userflag, oldflag );
  },
  restore_user: function()
  {
    this.user = null;
    app.token = '';
    
    var hostkey = system.get_cookie( 'host.key' );
    var userkey = system.get_cookie( 'user.key' );
    var username = system.get_cookie( 'user.name' );
    var usertoken = system.get_cookie( 'user.token' );
    var usertype = system.get_cookie( 'user.type' );
    var attachHostKey = system.get_cookie( 'user.attachhostkey' );
    
    if (hostkey != null && hostkey.length > 0 && 
        userkey != null && userkey.length > 0 && 
        usertoken != null && usertoken.length > 0 && 
        username != null && username.length > 0) 
    { 
      var user = {};
      user['key'] = userkey;
      user['name'] = username;
      user['token'] = usertoken;
      user['type'] = usertype;
      user['attachhostkey'] = attachHostKey;
      
      this.user = user;
      app.token = hostkey + userkey + usertoken;
      system.hostKey = hostkey;
    }
  },
  clear_user: function()
  {
    this.user = null;
    this.user_flag = null;
    this.invite_count = 0;
    this.message_count = 0;
    
    app.token = '';
      
    system.remove_cookie( 'user.key' );
    system.remove_cookie( 'user.name' );
    system.remove_cookie( 'user.token' );
    system.remove_cookie( 'user.type' );
    system.remove_cookie( 'user.attachhostkey' );
  },
  update_setting: function( setting )
  {
    if (setting == null) return;
    
    var lang = setting['lang'];
    var theme = setting['theme'];
    
    if (lang != null && lang.length > 0) 
      this.language = lang;
    
    if (theme != null && theme.length > 0) 
      this.theme = theme;
    
    this.check_lang();
    this.check_theme();
  },
  update_lang: function( lang, cb )
  {
    if (lang == null || lang.length == 0) 
      return;
    
    localizations.init( lang, function() {
        system.save_cookie( 'login.lang', lang );
        if (cb) cb.call(globalApp);
      });
  },
  check_lang: function( cb )
  {
    var lang = this.language;
    if (lang != null && lang.length > 0 && lang != localizations.lang) {
      this.update_lang( lang, cb );
      return;
    }
  },
  update_theme: function( theme )
  {
    if (theme == null || theme.length == 0)
      return;
    
    app.theme = theme;
    system.save_cookie( 'login.theme', theme );
    
    var main_element = $( '#theme-main-css' );
    var ui_element = $( '#theme-ui-css' );
    
    if (main_element)
      main_element.attr( 'href', 'css/' + theme + '/javen.css' );
    
    if (ui_element)
      ui_element.attr( 'href', 'css/ui-' + theme + '/jquery-ui.css' );
  },
  check_theme: function()
  {
    var theme = this.theme;
    if (theme != null && theme.length > 0 && theme != app.theme)
      this.update_theme( theme );
  },
  restore_theme: function()
  {
    var theme = system.get_cookie( 'login.theme' );
    if (theme != null && theme.length > 0)
      this.update_theme( theme );
  },
  is_admin: function()
  {
    var user = this.user;
    if (user) {
      var type = user['type'];
      if (type == 'administrator' || type == 'manager')
        return true;
    }
    return false;
  },
  go_storage_library: function( libid )
  {
    if (libid == null || libid.length == 0)
      return false;
    
    var storages = this.storages;
    if (storages == null) return null;
    
    for (var key in storages) { 
      var storage = storages[key];
      if (storage == null) continue;
      
      var storage_host = storage['host'];
      if (storage_host == null) continue;
      
      var storage_user = storage['user'];
      if (storage_user == null) continue;
      
      var storage_libs = storage['libraries'];
      if (storage_libs == null) continue;
      
      for (var libkey in storage_libs) {
        var library = storage_libs[libkey];
        if (library == null) continue;
        
        var id = library['id'];
        if (id == libid) {
          var hostkey = storage_host['key'];
          var hostdomain = storage_host['domain'];
          var hostaddr = storage_host['hostaddr'];
          var httpport = storage_host['httpport'];
          var httpsport = storage_host['httpsport'];
          var userkey = storage_user['key'];
          var usertoken = this.user['token'];
          
          if (hostdomain != null && hostdomain.length > 0)
            hostaddr = hostdomain;
          
          if (hostaddr != null && hostaddr.length > 0) {
            var scheme = system.scheme;
            var hostAddress = null;
            if (scheme == 'https') {
              hostAddress = 'https://' + hostaddr;
              if (httpsport != null && httpsport != 443)
                hostAddress += ':' + httpsport;
            } else {
              hostAddress = 'http://' + hostaddr;
              if (httpport != null && httpport != 80)
                hostAddress += ':' + httpport;
            }
            
            var authtoken = hostkey + userkey + usertoken;
            var redirto = hostAddress + '/library/#/~signin/' + authtoken + '/browse/' + id;
            
            var context = system.context;
            context.redirect(redirto);
            
            return true;
          }
        }
      }
    }
    
    return false;
  },
  get_remainingtitle: function()
  {
    var remaining = this.usable_space;
    if (remaining == null || remaining < 0) remaining = 0;
    
    var text = strings( 'Remaining {0} Space' );
    return text.format( readableBytes2(remaining) );
  },
  get_usertitle: function()
  {
    var username = this.get_username();
    var nickname = this.get_nickname();
    if (username == null) username = '';
    
    var title = strings('Me') + ': ' + username;
    if (nickname != null && nickname.length > 0)
      title = title + '(' + nickname + ')';
    
    return title;
  },
  get_usersimple: function()
  {
    var username = this.get_username();
    var nickname = this.get_nickname();
    
    if (username == null) username = '';
    if (nickname != null && nickname.length > 0)
      return nickname;
    
    return username;
  },
  get_nickname: function()
  {
    var user = this.user;
    if (user) return user['nick'];
    return null;
  },
  get_username: function()
  {
    var user = this.user;
    if (user) return user['name'];
    return null;
  },
  get_attachhostkey: function()
  {
    var user = this.user;
    if (user) return user['attachhostkey'];
    return system.get_cookie( 'user.attachhostkey' );
  },
  get_browsekey: function( username )
  {
    if (username == null || username.length == 0)
      username = this.get_username();
    
    if (username != null && username.indexOf('@') < 0)
      username += '@';
    
    return username;
  },
  get_loginname: function()
  {
    var name = this.get_username();
    if (name == null || name.length == 0)
      name = system.get_cookie( 'login.name' );
    return name;
  },
  get_language: function()
  {
    var lang = localizations.lang;
    if (lang == null || lang.length == 0)
      lang = system.get_cookie( 'login.lang' );
    if (lang == null || lang.length == 0)
      lang = this.language;
    if (lang == null || lang.length == 0)
      lang = navigator.language ? navigator.language : navigator.userLanguage;
    return lang;
  },
  get_theme: function()
  {
    var theme = app.theme;
    if (theme == null || theme.length == 0)
      theme = system.get_cookie( 'login.theme' );
    if (theme == null || theme.length == 0)
      theme = this.theme;
    return theme;
  },
  check_userflag: function( flag, oldflag )
  {
    if (flag == null || flag.length == 0) 
      return;
    
    if (flag == oldflag) return;
    
    var error_msg = null;
    
    if (flag == 'readonly') {
      error_msg = strings( 'Your account are locked as readonly' );
    } else if (flag == 'disabled') {
      error_msg = strings( 'Your account are disabled' );
    } else if (flag == 'enabled') {
      if (oldflag != null && oldflag.length > 0)
        error_msg = strings( 'Your account are enabled' );
    }
    
    if (error_msg != null) {
      var error = {};
      error['code'] = -1;
      error['msg'] = error_msg;
      error['trace'] = '';
      statusbar.error = error;
    }
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var lightning_app = function( app_config )
{
  that = this,
  
  config = app_config,
  base_path = app_config.root_path,
  index_path = app_config.index_path,
  user_path = app_config.user_path,
  theme = '';
  token = '',
  
  this.config = config;
  this.base_path = config.root_path;
  this.index_path = config.index_path;
  this.user_path = config.user_path;
  this.theme = '';
  this.token = '';

  this.run = function()
  {
    globalApp.init();
  };
};

function reset_page( context )
{
  $( '#content-title' ).html( '' );
  $( '#content-popover' ).html( '' );
  $( '#content-popover-top' ).html( '' );
  $( '#content-dialog' ).html( '' );
  
  $( '#content-body' )
    .html( '<div class="javen-icon-preloader javen-icon-myjaven"></div>' );
  
  $( 'body' )
    .attr( 'onclick', 'javascript:on_page_click();');
  
}

function init_page_title( context )
{
  var page_title = strings( 'My Library' );
  
  if (system.friendlyName != null)
    page_title = page_title + ' (' + system.friendlyName + ')';

  $( '#content-title' )
    .html( page_title.esc() );
  
  return true;
};

function init_page( context, redirectLogin )
{
  photoslide.stop();
  reset_page( context);
  
  if (system.context) {
    var path = system.context.path;
    var backpath = system.context['back-path'];
    if (path != null && path.length > 0 && backpath != true) {
      if (path == '#/~dashboard') {
        system.backPaths = [];
        
      } else {
        if (system.backPaths == null)
          system.backPaths = [];
      
        var lastpath = '';
        if (system.backPaths.length > 0)
          lastpath = system.backPaths[system.backPaths.length -1];
      
        if (lastpath != path)
          system.backPaths.push( path );
      }
    }
  }
  
  system.context = context;
  globalApp.update_error = false;
  
  if (system.context) {
    if (system.context.path == system.backPath)
      system.context['back-path'] = true;
  }
  
  system.backPath = null;
  searchform.library_id = null;
  
  if (globalApp.user == null) { 
    globalApp.restore_user();
    
    if (globalApp.user == null) {
      globalApp.clear_user();
      statusbar.stopTimer();
      
      if (redirectLogin == null || redirectLogin == true)
        context.redirect( '#/~login' );
      
      return false;
    } else {
      globalApp.update( 'refresh' );
    }
  }
  
  if (statusbar.timer == null)
    statusbar.startTimer();
  
  globalApp.check_theme();
  
  return init_page_title( context );
};

function init_page2( context )
{
  photoslide.stop();
  reset_page( context);
  
  if (context) system.context = context;
  globalApp.update_error = false;
  
  return init_page_title( context );
}

function back_page()
{
    var context = system.context;
    var linkto = null;
    
    if (system.backPaths && system.backPaths.length > 0) {
      linkto = system.backPaths.pop();
    }
    
    if (linkto != null && linkto.length > 0) {
      system.backPath = linkto;
      context.redirect( linkto );
      return;
    }
    
    var hostlocation = system.get_hostlocation();
    if (hostlocation == null) hostlocation = '';
    
    context.redirect( hostlocation + '#/~dashboard' );
}

function show_loading()
{
  $( '#content-loading' ).removeClass( 'hide' );
};

function hide_loading()
{
  $( '#content-loading' ).addClass( 'hide' );
};

function build_error( xhr, text_status, error_thrown )
{
  if (xhr) {
    var statusCode = xhr.status;
    var statusText = text_status;
    var statusTrace = error_thrown;
    
    var text = xhr.responseText;
    if (text && text.length > 0) 
    {
      var firstChr = text.charAt(0);
      //var lastChr = text.charAt(text.length-1);
      
      if (firstChr == '{')
      {
        var response = JSON.parse( text );
        var error = response['error'];
        if (error)
        {
          statusCode = error['code'];
          statusText = error['msg'];
          statusTrace = error['trace'];
        }
      }
    }
    
    if (statusText == 'error')
    {
      if (statusCode == 0) 
      {
        statusCode = -1;
        statusText = 'Connect to server error';
      }
      else if (statusTrace != null && statusTrace.length > 0)
      {
        statusText = '' + statusCode + ' ' + statusTrace;
        statusTrace = '';
      } 
    }
    
    var error = {};
    error['code'] = statusCode;
    error['msg'] = statusText;
    error['trace'] = statusTrace;
    
    return error
  }
  
  return null;
}

function show_error( error )
{
  if (error != null && messager != null) 
  {
    messager.error_code = error['code'];
    messager.error_msg = error['msg'];
    messager.error_trace = error['trace'];
    
    if (system.context && system.context.path) {
      var path = system.context.path;
      if (path.indexOf('~login') >= 0 || path.indexOf('~register') >= 0 || 
          path.indexOf('~signin') >= 0) {
        var msg = messager.error_msg;
        if (msg != null && msg.length > 0) {
          var txt = msg.toLowerCase();
          if (txt.indexOf('unauthorized') >= 0) {
            globalApp.clear_user();
            return;
          }
        }
      }
    }
    
    dialog.show( messager.message_dialog );
  }
}

function request_error( xhr, text_status, error_thrown )
{
  var error = build_error( xhr, text_status, error_thrown );
  show_error( error );
}

var opener = {
  event_src: null,
  open_key: null,
  
  toggle: function( key )
  {
    var element = $( key );
    if (element)
    {
      if (element.hasClass( 'open' )) 
        this.hide( key );
      else
        this.show( key);
    }
  },
  show: function( key )
  {
    if (this.open_key && this.open_key != key)
      this.hide( this.open_key );
    
    var element = $( key );
    if (element)
    {
      element.addClass( 'open' );
      
      this.event_src = event.srcElement;
      this.open_key = key;
    }
  },
  hide: function( key )
  {
    var element = $( key );
    if (element)
    {
      element.removeClass( 'open' );
    }
    
    this.event_src = null;
    this.open_key = null;
  },
  hideAll: function()
  {
    if (this.event_src == event.srcElement)
      return;
    
    if (this.open_key)
      this.hide( this.open_key );
  }
};

var dialog_data = [];

var dialog = {
  unauthorized: false,
  
  show: function( data )
  {
    if (data) {
      this.hide_alert();
      this.hide( data );
      
      var dialogid = 'dialog_' + dialog_data.length;
      var html = '<div id="' + dialogid + '">' + data.html + '</div>';
      
      data.element.append( html );
        
      //$( '#dismiss-button' )
      //  .attr( 'onclick', 'javascript:dialog.hide();return false;' );
      
      if (data.showcb)
        data.showcb.call( this );
      
      data.dialogid = dialogid;
      data.shown = true;
      
      dialog_data.push( data );
    }
  },
  hide: function( hidedata )
  {
    var context = system.context;
    var unauthorized = this.unauthorized;
    this.unauthorized = false;
    
    var data = null;
    
    if (hidedata != null) {
      var idx = 0;
      for (idx=0; i < dialog_data.length; i++) {
        var d = dialog_data[idx];
        if (d == hidedata && d != null) {
          data = d;
          dialog_data.splice(idx, 1);
          break;
        }
      }
    } else {
      data = dialog_data.pop();
    }
    
    if (data) {
      if (unauthorized && context) { 
        context.redirect( '#/~login' );
        return;
      }
      
      var dialogid = 'dialog_' + dialog_data.length;
      var dialog_element = $( '#' + dialogid );
      
      dialog_element.remove();
      //data.element.html( '' );
      
      if (data.hidecb)
        data.hidecb.call( this );
      
      data.shown = false;
    }
  },
  hide_alert: function()
  {
    if (dialog_data != null && dialog_data.length > 0) {
      var data = dialog_data[dialog_data.length -1];
      if (data && data.alertdialog == true)
        dialog.hide();
    }
  }
};

var popover_data = {
  element: null,
  html: null,
  data: null,
  showcb: null,
  hidecb: null
};

var popover = {
  shown: false,
  isshown: function()
  {
    return this.shown;
  },
  init: function( em, txt, data, showcb, hidecb )
  {
    this.hide();
    
    popover_data.element = em;
    popover_data.html = txt;
    popover_data.data = data;
    popover_data.showcb = showcb;
    popover_data.hidecb = hidecb;
  },
  show: function( id )
  {
    if (popover_data.element && popover_data.html)
    {
      //dialog.hide();
      
      popover_data.element
        .html( popover_data.html );
      
      //$( '#dismiss-button' )
      //  .attr( 'onclick', 'javascript:popover.hide();return false;' );
      
      if (popover_data.showcb)
        popover_data.showcb.call( this, popover_data.data, id );
      
      this.shown = true;
    }
  },
  hide: function()
  {
    if (popover_data.element)
    {
      popover_data.element
        .html( '' );
      
      if (popover_data.hidecb)
        popover_data.hidecb.call( this );
      
      this.shown = false;
    }
  }
};

function on_page_exit()
{
  if (globalApp.user)
    return strings( 'Are you sure to exit?' );
}

function on_page_click()
{
  opener.hideAll();
}

function on_mouse_down()
{
  if (event.button == 0)
  {
    // left
  }
  else if (event.button == 2)
  {
    // right
  }
  else if (event.button == 1)
  {
    // middle
  }
};

//window.onbeforeunload = on_page_exit;
//document.onmousedown = on_mouse_down;

//$(window).bind('beforeunload', on_page_exit);

var sectioncb;
var headbar;
var messager;

var app = new lightning_app( app_config );
