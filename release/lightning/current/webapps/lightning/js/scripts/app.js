
var loader = {
    
  show : function( element )
  {
    $( element )
      .addClass( 'loader' );
  },
    
  hide : function( element )
  {
    $( element )
      .removeClass( 'loader' );
  }
    
};

Number.prototype.esc = function()
{
  return new String( this ).esc();
}

String.prototype.esc = function()
{
  return this.replace( /</g, '&lt;' ).replace( />/g, '&gt;' );
}

LightningDate = function( date )
{
  // ["Sat Mar 03 11:00:00 CET 2012", "Sat", "Mar", "03", "11:00:00", "CET", "2012"]
  var parts = date.match( /^(\w+)\s+(\w+)\s+(\d+)\s+(\d+\:\d+\:\d+)\s+(\w+)\s+(\d+)$/ );
    
  // "Sat Mar 03 2012 10:37:33"
  return new Date( parts[1] + ' ' + parts[2] + ' ' + parts[3] + ' ' + parts[6] + ' ' + parts[4] );
}

function app_token()
{
  var hostkey = $.cookie( 'lightning.host.key' );
  var userkey = $.cookie( 'lightning.user.key' );
  var token = $.cookie( 'lightning.user.token' );
  
  if (hostkey == null) hostkey = '';
  if (userkey == null) userkey = '';
  if (token == null) token = '';
  
  return hostkey + userkey + token;
}

function request_error( xhr, text_status, error_thrown )
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
    
    show_global_error
    ( 
      '<div class="message">' + statusText + '</div>'
    );
  }
}

var sammy = $.sammy
(
  function()
  {
    this.bind
    (
      'run',
      function( event, config )
      {
        if( 0 === config.start_url.length )
        {
          location.href = '#/';
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
        app.clear_timeout();

        var menu_wrapper = $( '#menu-wrapper' );

        $( 'li[id].active', menu_wrapper )
          .removeClass( 'active' );
                
        $( 'li.active', menu_wrapper )
          .removeClass( 'active' );

        if( this.params.splat )
        {
          var selector = '~' === this.params.splat[0][0]
                       ? '#' + this.params.splat[0].replace( /^~/, '' ) + '.global'
                       : '#menu-selector #' + this.params.splat[0].replace( /\./g, '__' );

          var active_element = $( selector, menu_wrapper );
                    
          if( 0 === active_element.size() )
          {
            this.app.error( 'There exists no core with name "' + this.params.splat[0] + '"' );
            return false;
          }

          active_element
            .addClass( 'active' );

          if( this.params.splat[1] )
          {
            $( '.' + this.params.splat[1], active_element )
              .addClass( 'active' );
          }

          if( !active_element.hasClass( 'global' ) )
          {
            this.active_core = active_element;
          }
        }
      }
    );
  }
);

var lightning_admin = function( app_config )
{
  that = this,

  menu_element = null,

  is_multicore = null,
  cores_data = null,
  active_core = null,
  environment_basepath = null,
    
  config = app_config,
  params = null,
  dashboard_values = null,
  schema_browser_data = null,

  plugin_data = null,
    
  this.menu_element = $( '#menu-selector' );
  this.config = config;

  this.timeout = null;

  this.core_regex_base = '^#\\/([\\w\\d-\\.]+)';

  show_global_error = function( error )
  {
    var main = $( '#main' );

    $( 'div[id$="-wrapper"]', main )
      .remove();

    main
      .addClass( 'error' )
      .append( error );

    var pre_tags = $( 'pre', main );
    if( 0 !== pre_tags.size() )
    {
      hljs.highlightBlock( pre_tags.get(0) ); 
    }
  };

  sort_cores_data = function sort_cores_data( cores_status )
  {
    // build array of core-names for sorting
    var core_names = [];
    for( var core_name in cores_status )
    {
      core_names.push( core_name );
    }
    core_names.sort();

    var core_count = core_names.length;
    var cores = {};

    for( var i = 0; i < core_count; i++ )
    {
      var core_name = core_names[i];
      cores[core_name] = cores_status[core_name];
    }

    return cores;
  };

  this.set_cores_data = function set_cores_data( cores )
  {
    that.cores_data = sort_cores_data( cores.status );
    
    that.menu_element
      .empty();

    var core_count = 0;
    for( var core_name in that.cores_data )
    {
      core_count++;
      var core_path = config.lightning_path + '/' + core_name;
      var classes = [];

      if( !environment_basepath )
      {
        environment_basepath = core_path;
      }

      if( cores.status[core_name]['isDefaultCore'] )
      {
        classes.push( 'default' );
      }

      var schemaStatus = cores.status[core_name]['schema'];
      var schemaHide = '';
      if (schemaStatus == null || schemaStatus.length == 0 || schemaStatus == 'null')
        schemaHide = 'style="display: none;"';

      var core_tpl = '<li id="' + core_name.replace( /\./g, '__' ) + '" '
                   + '    class="' + classes.join( ' ' ) + '"'
                   + '    data-basepath="' + core_path + '"'
                   + '    schema="' + cores.status[core_name]['schema'] + '"'
                   + '    config="' + cores.status[core_name]['config'] + '"'
                   + '>' + "\n"
                   + '  <p><a href="#/' + core_name + '" title="' + core_name + '">' + core_name + '</a></p>' + "\n"
                   + '  <ul>' + "\n"
                   + '    <li class="ping"><a rel="' + core_path + '/admin/ping"><span>Ping</span></a></li>' + "\n"
                   + '    <li class="query" ' + schemaHide + '><a href="#/' + core_name + '/query"><span>Query</span></a></li>' + "\n"
                   + '    <li class="schema" ' + schemaHide + '><a href="#/' + core_name + '/schema"><span>Schema</span></a></li>' + "\n"
                   + '    <li class="config" ' + schemaHide + '><a href="#/' + core_name + '/config"><span>Config</span></a></li>' + "\n"
                   + '    <li class="replication" ' + schemaHide + '><a href="#/' + core_name + '/replication"><span>Replication</span></a></li>' + "\n"
                   + '    <li class="analysis" ' + schemaHide + '><a href="#/' + core_name + '/analysis"><span>Analysis</span></a></li>' + "\n"
                   + '    <li class="schema-browser" ' + schemaHide + '><a href="#/' + core_name + '/schema-browser"><span>Schema Browser</span></a></li>' + "\n"
                   + '    <li class="plugins"><a href="#/' + core_name + '/plugins"><span>Plugins / Stats</span></a></li>' + "\n"
                   + '    <li class="dataimport" ' + schemaHide + '><a href="#/' + core_name + '/dataimport"><span>Dataimport</span></a></li>' + "\n"
                   + '    </ul>' + "\n"
                   + '</li>';

      that.menu_element
        .append( core_tpl );
    }

    if( cores.initFailures )
    {
      var failures = [];
      for( var core_name in cores.initFailures )
      {
        failures.push
        (
          '<li>' +
            '<strong>' + core_name.esc() + ':</strong>' + "\n" +
            cores.initFailures[core_name].esc() + "\n" +
          '</li>'
        );
      }

      if( 0 !== failures.length )
      {
        var init_failures = $( '#init-failures' );

        init_failures.show();
        $( 'ul', init_failures ).html( failures.join( "\n" ) );
      }
    }

    if( 0 === core_count )
    {
      show_global_error
      ( 
        '<div class="message">There are no Cores running. <br/> Using the Lightning Admin UI currently requires at least one Core.</div>'
      );
    } // else: we have at least one core....
  };

  this.run = function()
  {
    $.ajax
    (
      {
        url : config.lightning_path + config.core_admin_path + '?wt=json&token=' + app_token(),
        dataType : 'json',
        beforeSend : function( arr, form, options )
        {               
          $( '#content' )
            .html( '<div id="index"><div class="loader">Loading ...</div></div>' );
        },
        success : function( response )
        {
          that.set_cores_data( response );

          for( var core_name in response.status )
          {
            var core_path = config.lightning_path + '/' + core_name;
            if( !environment_basepath )
            {
              environment_basepath = core_path;
            }
          }

          var system_url = environment_basepath + '/admin/system?wt=json&token=' + app_token();
          $.ajax
          (
            {
              url : system_url,
              dataType : 'json',
              beforeSend : function( arr, form, options )
              {
              },
              success : function( response )
              {
                that.dashboard_values = response;

                var environment_args = null;
                var cloud_args = null;

                if( response.jvm && response.jvm.jmx && response.jvm.jmx.commandLineArgs )
                {
                  var command_line_args = response.jvm.jmx.commandLineArgs.join( ' | ' );

                  environment_args = command_line_args.match( /-Dlightning.environment=((dev|test|prod)?[\w\d]*)/i );
                  cloud_args = command_line_args.match( /-Dzk/i );
                }

                // title

                $( 'title', document )
                  .append( ' (' + response.core.host + ')' );

                // environment

                var wrapper = $( '#wrapper' );
                var environment_element = $( '#environment' );
                if( environment_args )
                {
                  wrapper
                    .addClass( 'has-environment' );

                  if( environment_args[1] )
                  {
                    environment_element
                      .html( environment_args[1] );
                  }

                  if( environment_args[2] )
                  {
                    environment_element
                      .addClass( environment_args[2] );
                  }
                }
                else
                {
                  wrapper
                    .removeClass( 'has-environment' );
                }

                // cloud

                var cloud_nav_element = $( '#menu #cloud' );
                if( cloud_args )
                {
                  cloud_nav_element
                    .show();
                }

                // sammy

                sammy.run( location.hash );
              },
              error : function()
              {
                show_global_error
                (
                  '<div class="message"><p>Unable to load environment info from <code>' + system_url.esc() + '</code>.</p>' +
                  '<p>This interface requires that you activate the admin request handlers in all Cores by adding the ' +
                  'following configuration to your <code>config.xml</code>:</p></div>' + "\n" +

                  '<div class="code"><pre class="syntax language-xml"><code>' +
                  '<!-- Admin Handlers - This will register all the standard admin RequestHandlers. -->'.esc() + "\n" +
                  '<requestHandler name="/admin/" class="lightning.admin.AdminHandlers" />'.esc() +
                  '</code></pre></div>'
                );
              },
              complete : function()
              {
                loader.hide( this );
              }
            }
          );
        },
        error : function( xhr, text_status, error_thrown )
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function()
        {
        }
      }
    );
  };

  this.convert_duration_to_seconds = function convert_duration_to_seconds( str )
  {
    var seconds = 0;
    var arr = new String( str || '' ).split( '.' );
    var parts = arr[0].split( ':' ).reverse();
    var parts_count = parts.length;

    for( var i = 0; i < parts_count; i++ )
    {
      seconds += ( parseInt( parts[i], 10 ) || 0 ) * Math.pow( 60, i );
    }

    // treat more or equal than .5 as additional second
    if( arr[1] && 5 <= parseInt( arr[1][0], 10 ) )
    {
      seconds++;
    }

    return seconds;
  };

  this.convert_seconds_to_readable_time = function convert_seconds_to_readable_time( seconds )
  {
    seconds = parseInt( seconds || 0, 10 );
    var minutes = Math.floor( seconds / 60 );
    var hours = Math.floor( minutes / 60 );

    var text = [];
    if( 0 !== hours )
    {
      text.push( hours + 'h' );
      seconds -= hours * 60 * 60;
      minutes -= hours * 60;
    }

    if( 0 !== minutes )
    {
      text.push( minutes + 'm' );
      seconds -= minutes * 60;
    }

    if( 0 !== seconds )
    {
      text.push( ( '0' + seconds ).substr( -2 ) + 's' );
    }

    return text.join( ' ' );
  };

  this.clear_timeout = function clear_timeout()
  {
    if( !app.timeout )
    {
      return false;
    }

    console.debug( 'Clearing Timeout #' + this.timeout );
    clearTimeout( this.timeout );
    this.timeout = null;
  };

  this.format_json = function format_json( json_str )
  {
    if( JSON.stringify && JSON.parse )
    {
      json_str = JSON.stringify( JSON.parse( json_str ), undefined, 2 );
    }

    return json_str;
  };

};

var app = new lightning_admin( app_config );
