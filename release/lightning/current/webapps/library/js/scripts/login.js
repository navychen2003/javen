
var logoutform = {
  logout: function() 
  {
    if (uploader) uploader.reset();
    if (musicplayer) musicplayer.reset();
    
    var params = '&action=logout';
    
    $.ajax
    (
      {
        url : app.user_path + '/login?token=' + app.token + params + '&wt=json',
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
            globalApp.clear_user();
            
            var context = system.context;
            context.redirect( '#/~login' );
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
  relogin: function( token, redirectTo )
  {
    var context = system.context;
    if (token == null || token.length == 0) {
      context.redirect( '#/~login' );
      return;
    }
    
    var remember_me = false;
    var params = '&action=check';
    
    var lang = localizations.lang; 
    var apptype = 'web';
    var appname = navigator.appName;
    var appversion = navigator.appVersion;
    var applang = navigator.language ? navigator.language : navigator.userLanguage;
    
    if (lang == null) lang = '';
    if (appname == null) appname = 'unknown';
    if (appversion == null) appversion = 'unknown';
    if (applang == null) applang = '';
    
    params += '&lang=' + encodeURIComponent(lang);
    params += '&apptype=' + encodeURIComponent(apptype);
    params += '&appname=' + encodeURIComponent(appname);
    params += '&appversion=' + encodeURIComponent(appversion);
    params += '&applang=' + encodeURIComponent(applang);
    
    $.ajax
    (
      {
        url : app.user_path + '/login?token=' + token + params + '&wt=json',
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
            var rsp_content = response;
            var rsp_user = response['user'];
            
            if (rsp_user) {
              var rembme = rsp_user['rembme'];
              if (rembme == true) remember_me = true;
            }
            
            system.set_expiredays( remember_me ? 7 : 0 );
            globalApp.clear_user();
            globalApp.update_content( rsp_content );
            
            if (redirectTo != null && redirectTo.length > 0)
              context.redirect( '#/~' + redirectTo );
            else
              context.redirect( '#/~dashboard' );
            
            //sammy.refresh();
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
  }
};

var loginform = {
  context: null,
  
  init_form: function( context ) 
  {
    this.context = context;
    
    var titlehtml = strings( 'Sign in to your account' ) + ' ' + strings( 'or' ) + ' '
      + '<a href="#/~register">' + strings( 'Sign Up' ) + '</a>';
    
    $( '#login-title' ).html( titlehtml );
    $( '#login-slogan' ).html( strings( 'Your private cloud, store locally, access anywhere.' ) );
    $( '#login-rememberme' ).html( strings( 'Remember Me' ) );
    $( '#login-submit-button' ).html( strings( 'Sign In' ) );
    
    $( '#login-username-input' ).attr( 'placeholder', strings( 'username or email' ) );
    $( '#login-password-input' ).attr( 'placeholder', strings( 'password' ) );

    var page_title = strings( 'My Library' );

    if (system.friendlyName != null)
      page_title = page_title + ' (' + system.friendlyName + ')';

    $( '#content-title' )
      .html( page_title.esc() );

    var loginname = globalApp.get_loginname();
    if (loginname && loginname.length > 0)
      $( '#login-username-input' ).attr( 'value', loginname );
    
    var form_element = $( '#login-form' );
    var remember_me = false;
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/login?action=login&wt=json',
          dataType : 'json',
          beforeSubmit : function( array, form, options )
          {
            var input_error = false;
            var input_password = null;
            var arrayidx_password = -1;
            var arrayidx_secret = -1;
            
            for (var i=0; i < array.length; i++) { 
              var input = array[i];
              var name = input['name'];
              var value = input['value'];
              
              if (name == 'username') { 
                if (value == null || value.length == 0) { 
                  $( '#login-username-input' )
                    .attr( 'placeholder', strings( 'username or email cannot be empty' ) );
                  input_error = true;
                }
              } else if (name == 'password') { 
                if (value == null || value.length == 0) { 
                  $( '#login-password-input' )
                    .attr( 'placeholder', strings( 'password cannot be empty' ) );
                  input_error = true;
                }
                input_password = value;
                arrayidx_password = i;
              } else if (name == 'secret') { 
                arrayidx_secret = i;
              } else if (name == 'rememberMe') {
                if (value == '1') remember_me = true;
              }
            }
            
            if (!input_error) {
              show_loading();
              
              var secret = encodeSecret(input_password);
              if (arrayidx_secret >= 0) array[arrayidx_secret]['value'] = secret;
              if (arrayidx_password >= 0) array[arrayidx_password]['value'] = '';
              
              $( '#login-secret-input' ).attr( 'value', secret );
              $( '#login-password-input' ).attr( 'value', '' );
            }
            
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
              var rsp_content = response;
              var rsp_user = response['user'];
            
              if (rsp_user) {
                var rembme = rsp_user['rembme'];
                if (rembme == true) remember_me = true;
              }
              
              system.set_expiredays( remember_me ? 7 : 0 );
              globalApp.update_content( rsp_content );
              
              var token = app.token;
              globalApp.clear_user();
              
              context.redirect( system.hostAddress + '/library/#/~signin/' + token );
              //context.redirect( '#/~dashboard' );
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
  }
};

var registerform = {
  context: null,
  
  init_form: function( context ) 
  {
    this.context = context;
    
    var titlehtml = strings( 'Sign up your account' ) + ' ' + strings( 'or' ) + ' '
      + '<a href="#/~login">' + strings( 'Sign In' ) + '</a>';
    
    $( '#register-title' ).html( titlehtml );
    $( '#register-rememberme' ).html( strings( 'Remember Me' ) );
    $( '#register-email-text' ).html( strings( 'Email' ) );
    $( '#register-username-text' ).html( strings( 'User Name' ) );
    $( '#register-password-text' ).html( strings( 'Password' ) );
    $( '#register-password-text2' ).html( strings( 'Password' ) );
    $( '#register-submit-button' ).html( strings( 'Sign Up' ) );
    
    $( '#register-email-input' ).attr( 'placeholder', strings( 'email address' ) );
    $( '#register-username-input' ).attr( 'placeholder', strings( 'username' ) );
    $( '#register-password-input' ).attr( 'placeholder', strings( 'password' ) );
    $( '#register-password-input2' ).attr( 'placeholder', strings( 'password again' ) );

    $( '#register-required-email' ).html( strings( 'email cannot be empty' ) );
    $( '#register-required-username' ).html( strings( 'username cannot be empty' ) );
    $( '#register-required-password' ).html( strings( 'password cannot be empty' ) );
    $( '#register-required-password2' ).html( strings( 'password must equals to the first password' ) );

    $( '#register-email-input' )
      .attr( 'onBlur', 'javascript:registerform.on_email_blur(this);' );

    var page_title = strings( 'My Library' );

    if (system.friendlyName != null)
      page_title = page_title + ' (' + system.friendlyName + ')';

    $( '#content-title' )
      .html( page_title.esc() );

    //var loginname = globalApp.get_loginname();
    //if (loginname && loginname.length > 0)
    //  $( '#register-username-input' ).attr( 'value', loginname );
    
    var form_element = $( '#register-form' );
    var remember_me = false;
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/login?action=registerlogin&wt=json',
          dataType : 'json',
          beforeSubmit : function( array, form, options )
          {
            var input_error = false;
            var input_password = null;
            var input_password2 = null;
            var arrayidx_password = -1;
            var arrayidx_password2 = -1;
            var arrayidx_secret = -1;
            
            for (var i=0; i < array.length; i++) { 
              var input = array[i];
              var name = input['name'];
              var value = input['value'];
              
              if (name == 'email') {
                if (value == null || value.length == 0) { 
                  $( '#register-email-input' )
                    .attr( 'placeholder', strings( 'email cannot be empty' ) );
                  $( '#register-required-email' ).removeClass( 'hide' );
                  $( '#register-email-group' ).addClass( 'error' );
                  input_error = true;
                }
              } else if (name == 'username') { 
                if (value == null || value.length == 0) { 
                  $( '#register-username-input' )
                    .attr( 'placeholder', strings( 'username cannot be empty' ) );
                  $( '#register-required-username' ).removeClass( 'hide' );
                  $( '#register-username-group' ).addClass( 'error' );
                  input_error = true;
                }
              } else if (name == 'password') { 
                if (value == null || value.length == 0) { 
                  $( '#register-password-input' )
                    .attr( 'placeholder', strings( 'password cannot be empty' ) );
                  $( '#register-required-password' ).removeClass( 'hide' );
                  $( '#register-password-group' ).addClass( 'error' );
                  input_error = true;
                }
                input_password = value;
                arrayidx_password = i;
              } else if (name == 'password2') {
                input_password2 = value;
                arrayidx_password2 = i;
              } else if (name == 'secret') {
                arrayidx_secret = i;
              } else if (name == 'rememberMe') {
                if (value == '1') remember_me = true;
              }
            }
            
            if (input_password != null && input_password.length > 0 && input_password != input_password2) {
              $( '#register-password-input2' )
                .attr( 'placeholder', strings( 'password must equals to the first password' ) );
              $( '#register-required-password2' ).removeClass( 'hide' );
              $( '#register-password-group2' ).addClass( 'error' );
              input_error = true;
            }
            
            if (!input_error) {
              show_loading();
              
              var secret = encodeSecret(input_password);
              if (arrayidx_secret >= 0) array[arrayidx_secret]['value'] = secret;
              if (arrayidx_password >= 0) array[arrayidx_password]['value'] = '';
              if (arrayidx_password2 >= 0) array[arrayidx_password2]['value'] = '';
              
              $( '#login-secret-input' ).attr( 'value', secret );
              $( '#login-password-input' ).attr( 'value', '' );
              $( '#login-password-input2' ).attr( 'value', '' );
            }
            
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
              var rsp_content = response;
              var rsp_user = response['user'];
            
              if (rsp_user) {
                var rembme = rsp_user['rembme'];
                if (rembme == true) remember_me = true;
              }
              
              system.set_expiredays( remember_me ? 7 : 0 );
              globalApp.update_content( rsp_content );
              
              var token = app.token;
              globalApp.clear_user();
              
              context.redirect( system.hostAddress + '/library/#/~signin/' + token );
              //context.redirect( '#/~dashboard' );
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
  on_email_blur: function( elem )
  {
    if (elem == null) return;
    var value = elem.value;
    if (value != null && value.length > 0) {
      var pos = value.indexOf('@');
      if (pos >= 0) value = value.substring(0, pos);
      if (value != null && value.length > 0) {
        var username_input = $( '#register-username-input' );
        if (username_input) {
          var val = username_input.val();
          if (val == null || val.length == 0)
            username_input.attr( 'value', value.esc() );
        }
      }
    }
  }
};

var login_langs = {
  lang: null,

  init_langs: function()
  {
    var langs = globalApp.languages;
    if (langs) { 
      this.init_navlangs( langs, null );
      return;
    }
    
    globalApp.update( 'all', function() {
        login_langs.init_navlangs( null, null );
      });
  },
  init_navlangs: function( langs, lang )
  {
    if (langs == null) 
      langs = globalApp.languages;
    if (lang == null) 
      lang = localizations.lang; 
    
    var langtitle = null;
    var langimg = null;
    
    if (langs != null) { 
      var content = [];
      
      for (var key in langs) {
        var title = langs[key];
        if (title == null) continue;
        
        if (lang == key) { 
          langtitle = title;
          langimg = 'img/flag/' + key + '.png';
        }
        
        var item = '<li><a class="lang-btn" data-focus="keyboard" href="" onClick="javascript:login_langs.update_lang(\'' + key + '\');return false;"><span>' + title.esc() + '</span></a></li>';
        content.push( item );
      }
      
      $( '#lang-dropdown-list' )
        .html( content.join("\n") );
    }
    
    if (langtitle == null) {
      langtitle = 'English';
      langimg = 'img/flag/en.png';
    }
    
    $( '#lang-link' )
      .attr( 'title', langtitle.esc() )
      .attr( 'onclick', 'javascript:opener.toggle(\'#lang-dropdown\');return false;' )
      .attr( 'href', '' )
      .addClass( 'active' );
    
    $( '#lang-title' )
      .attr( 'title', langtitle.esc() )
      .attr( 'src', langimg.esc() );
  },
  init_appinfo: function()
  {
    var lang = localizations.lang; 
    var apptype = 'web';
    var appname = navigator.appName;
    var appversion = navigator.appVersion;
    var applang = navigator.language ? navigator.language : navigator.userLanguage;
    
    if (lang == null) lang = '';
    if (appname == null) appname = 'unknown';
    if (appversion == null) appversion = 'unknown';
    if (applang == null) applang = '';
    
    $( '#login-lang' ).attr( 'value', lang.esc() );
    $( '#login-apptype' ).attr( 'value', apptype.esc() );
    $( '#login-appname' ).attr( 'value', appname.esc() );
    $( '#login-appversion' ).attr( 'value', appversion.esc() );
    $( '#login-applang' ).attr( 'value', applang.esc() );
  },
  update_lang: function( lang )
  {
    if (lang == null || lang.length == 0)
      return;
    
    globalApp.update_lang( lang, function() {
        sammy.refresh();
      });
  },
  check_lang: function( cb )
  {
    var lang0 = this.lang;
    var lang1 = localizations.lang;
    var lang2 = globalApp.get_language();
    
    if (lang0 != lang1) {
      if (cb) cb.call(this);
      else sammy.refresh();
    }
  }
};

var login_headbar = {
  signup_title: null,
  signup_linkto: null,
  
  init: function( header_element ) 
  { 
    headbar = this;
    $.get
    (
      'tpl/navbar2.html',
      function( template )
      {
        header_element
          .html( template );
        
        var backlink_element = $( '#back-link' );
        var homelink_element = $( '#home-link' );
        
        var signuplink_element = $( '#signup-link' );
        var langlink_element = $( '#lang-link' );
        var langtitle_element = $( '#lang-title' );
        
        backlink_element
          .attr( 'title', strings( 'Back' ) );
        
        homelink_element
          .attr( 'title', strings( 'Home' ) );
        
        signuplink_element
          .attr( 'href', login_headbar.signup_linkto )
          .html( strings( login_headbar.signup_title ) );
        
        login_langs.init_langs();
      }
    );
  }
}

// #/~login
sammy.get
(
  /^#\/(~login)$/,
  function( context )
  {
    var hostlocation = system.get_hostlocation();
    if (hostlocation != null && hostlocation.length > 0) {
      var redir = hostlocation + '#/~login';
      context.redirect( redir );
    }
    
    init_page2(context);
    login_headbar.lang = localizations.lang; 
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    login_headbar.signup_title = 'Sign Up';
    login_headbar.signup_linkto = '#/~register';
    
    login_headbar.init( header_element );
    message_dialogs.init( dialog_element );
    
    uploader.reset();
    musicplayer.reset();

    $.get
    (
      'tpl/login.html',
      function( template )
      {
        body_element
          .html( template );

        login_langs.init_appinfo();
        loginform.init_form(context);
        
        statusbar.show();
        
        login_langs.check_lang( function() {
            loginform.init_form(context);
          });
      }
    );
  }
);

// #/~register
sammy.get
(
  /^#\/(~register)$/,
  function( context )
  {
    var hostlocation = system.get_hostlocation();
    if (hostlocation != null && hostlocation.length > 0) {
      var redir = hostlocation + '#/~register';
      context.redirect( redir );
    }
    
    init_page2(context);
    login_langs.lang = localizations.lang; 
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    login_headbar.signup_title = 'Sign In';
    login_headbar.signup_linkto = '#/~login';
    
    login_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    uploader.reset();
    musicplayer.reset();

    $.get
    (
      'tpl/register.html',
      function( template )
      {
        body_element
          .html( template );

        login_langs.init_appinfo();
        registerform.init_form(context);
        
        statusbar.show();
        
        login_langs.check_lang( function() {
            registerform.init_form(context);
          });
      }
    );
  }
);

// #/~signin
sammy.get
(
  // /^#\/(~signin)$/,
  new RegExp( '(~signin)\\/' ),
  function( context )
  {
    init_page(context, false);
    login_langs.lang = localizations.lang; 
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    login_headbar.signup_title = 'Sign Up';
    login_headbar.signup_linkto = '#/~register';
    
    login_headbar.init( header_element );
    message_dialogs.init( dialog_element );
    
    var path_param = this.path.slice(10);
    var id_param = path_param;
    var redir_param = null;
    
    if (path_param != null) { 
      var pos = path_param.indexOf('/');
      if (pos > 0) {
        id_param = path_param.substring(0, pos);
        redir_param = path_param.substring(pos+1);
      }
    }

    logoutform.relogin( id_param, redir_param );
  }
);
