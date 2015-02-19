
var listsetting = {
  category_name: null,
  group_name: null,
  confirm_dialog: null,
  
  showlist: function( categoryName, groupName )
  {
    globalApp.check_theme();
    navbar.init_name( strings( 'Setting' ), null, '#/~settings' );
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    
    var stitle_element = $( '#setting-title' );
    var source_element = $( '#setting-source' );
    var version_element = $( '#setting-version' );
    
    var categories_element = $( '#settings-list' );
    var groups_element = $( '#groups-list' );
    var settings_element = $( '#active-group' );
    
    var form_element = $( '#setting-form' );
    var category_element = $( '#input_category' );
    var group_element = $( '#input_group' );
    var actions_element = $( '#setting-actions' );
    var save_element = $( '#save-button' );
    var cancel_element = $( '#cancel-button' );
    
    var params = '';
    if (categoryName != null) {
      params = '&category=' + categoryName;
      
      if (groupName != null) 
        params = params + '&group=' + groupName;
    }
    
    listsetting.category_name = categoryName;
    listsetting.group_name = groupName;
    
    form_element
      .ajaxForm
      (
        {
          url : app.user_path + '/setting?action=update&token=' + app.token + '&wt=json',
          dataType : 'json',
          beforeSubmit : function( array, form, options )
          {
            show_loading();
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
              globalApp.update( 'all', function() {
                  sammy.refresh();
                });
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
    
    $.ajax
    (
      {
        url : app.user_path + '/setting?action=' + params + '&token=' + app.token + '&wt=json',
        dataType : 'json',
        context : $( '#settings-list', body_element ),
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          var version = response['version'];
          var category = response['category'];
          var group = response['group'];
          
          var categories = response['categories'];
          var categoryContent = [];
          
          var groups = response['groups'];
          var groupContent = [];
          
          var settings = response['settings'];
          var settingContent = [];
          
          version_element
            .html( strings( 'Version {0}' ).format(version) );
          
          category_element
            .attr( 'value', category.esc() );
          
          group_element
            .attr( 'value', group.esc() );
          
          for (var key in categories) { 
            var data = categories[key];
            
            var name = data['name'];
            var title = data['title'];
            
            var active = '';
            if (name == category) active = 'active';
            
            var onclick = "javascript:listsetting.showlist('" + name + "',null);return false;";
            
            title = strings( title );
            
            var item = '<li><a class="settings-filter ' + active + '" onclick="' + onclick + '" href="">' + title.esc() + '</a></li>';
	        
	        categoryContent.push( item );
          }
          
          for (var key in groups) { 
            var data = groups[key];
            
            var name = data['name'];
            var title = data['title'];
            
            var active = '';
            if (name == group) active = 'active';
            
            var onclick = "javascript:listsetting.showlist('" + category + "','" + name + "');return false;";
            
            if (title == null || title.length == 0)
              title = name;
            title = strings( title );
            
            var item = '<li class="' + active + '"><a onclick="' + onclick + '" href="">' + title.esc() + '</a></li>';
	        
	        groupContent.push( item );
          }
          
          for (var key in settings) { 
            var data = settings[key];
            
            var type = data['type'];
            var name = data['name'];
            var title = data['title'];
            var desc = data['desc'];
            var value = data['value'];
            
            title = strings( title );
            desc = strings( desc );
            
            if (type == 'text') 
            {
              var item = '			<div class="control-group">' + "\n" +
                         '            <label class="control-label" for="input_' + name + '">' + title.esc() + '</label>' + "\n" +
                         '            <div class="controls">' + "\n" +
                         '              <input id="input_' + name + '" type="text" class="input-dark span4" name="' + name + '" value="' + value.esc() + '">' + "\n" +
                         '              <p class="help-block">' + desc + '</p>' + "\n" +
                         '            </div>' + "\n" +
                         '          </div>';
	        
	          settingContent.push( item );
	        }
	        else if (type == 'checkbox')
	        {
	          var checked = data['checked'];
	          
	          var checkattr = '';
	          if (checked) checkattr = 'checked';
	          
              var item = '			<div class="control-group">' + "\n" +
                         '            <label class="checkbox" for="input_' + name + '">' + "\n" +
                         '              <input id="input_' + name + '" type="checkbox" class="input-dark" name="' + name + '" value="' + value.esc() + '" ' + checkattr + '>' + "\n" +
                         '              &nbsp;' + title.esc() + "\n" +
                         '            </label>' + "\n" +
                         '            <p class="help-block">' + desc + '</p>' + "\n" +
                         '          </div>';
	        
	          settingContent.push( item );
	        }
	        else if (type == 'select')
	        {
	          var options = data['options'];
	          var optionContent = [];
	          
	          for (var opkey in options) { 
                var option = options[opkey];
                
                var op_value = option['value'];
                var op_title = option['title'];
                
                if (op_value == null) op_value = '';
                if (op_title == null) op_title = '';
                
                if (name != 'language')
                  op_title = strings( op_title );
                
                var op_selected = '';
                if (value == op_value)
                  op_selected = 'selected';
                
                var op_item = '                <option value="' + op_value.esc() + '" ' + op_selected + '>' + op_title.esc() + '</option>';
                optionContent.push( op_item );
              }
	          
	          var optionsItem = optionContent.join( "\n" );
	          
              var item = '			<div class="control-group">' + "\n" +
                         '            <label class="control-label" for="input_' + name + '">' + title.esc() + '</label>' + "\n" +
                         '            <div class="controls">' + "\n" +
                         '              <select id="input_' + name + '" name="' + name + '" onChange="javascript:listsetting.onselectchange(this);">' + "\n" +
                         optionsItem +
                         '              </select>' + "\n" +
                         '              <p class="help-block">' + desc + '</p>' + "\n" +
                         '            </div>' + "\n" +
                         '          </div>';
	        
	          settingContent.push( item );
	        }
          }
          
          categories_element
            .html( categoryContent.join( "\n" ) );
          
          groups_element
            .html( groupContent.join( "\n" ) );
            
          settings_element
            .html( settingContent.join( "\n" ) );
          
          if (settingContent.length > 0)
            actions_element.removeClass( 'hide' );
          else
            actions_element.addClass( 'hide' );
          
          if (groupContent.length == 0) {
            $( '#setting-empty' )
              .html( strings( 'No settings :(' ) )
              .removeClass( 'hide' );
          } else {
            $( '#setting-empty' )
              .addClass( 'hide' );
          }
        },
        error : function( xhr, text_status, error_thrown)
        {
          request_error( xhr, text_status, error_thrown );
        },
        complete : function( xhr, text_status )
        {
          var page_title = strings( 'My Library' );
          var setting_title = strings( 'My Library' );
        
          if (system.friendlyName != null)
            page_title = page_title + ' (' + system.friendlyName + ')';
        
          title_element
            .html( page_title.esc() );
        
          stitle_element
            .html( strings( 'Setting' ).esc() );
        
          save_element
            .html( strings( 'Save' ) );
        
          cancel_element
            .html( strings( 'Cancel' ) );
          
          hide_loading();
        }
      }
    );
  },
  onselectchange: function( element )
  {
    if (element == null) return;
    var selected = element.options[element.selectedIndex].value;
    var name = element.name;
    if (name && selected) {
      if (name == 'theme') 
        globalApp.update_theme( selected );
    }
  },
  init_message: function( dialog_element, template ) 
  {
    listsetting.confirm_dialog =
    {
      element: dialog_element, 
      html: template,
      showcb: function()
      { 
        $( '#message-title' ).html( strings( 'Warning' ) );
        $( '#message-ok' ).html( strings( 'Ok' ) );
        $( '#message-no' ).html( strings( 'Cancel' ) );
        
        $( '#message-icon' ).attr( 'class', 'glyphicon circle-question-mark' );
        
        var msg = strings( 'Are you sure ?' );
        if (msg == null) msg = "";
        
        $( '#message-text' ).html( msg.esc() );
        
        $( '#message-ok' )
          .attr( 'onclick', 'javascript:;' );
        
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

var setting_headbar = {
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

// #/~settings
sammy.get
(
  /^#\/(~settings)$/,
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

    setting_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/setting.html',
      function( template )
      {
        body_element
          .html( template );

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
          $( '#announcement-subnav' ).removeClass( 'hide' );
          $( '#cluster-subnav' ).removeClass( 'hide' );
        } else {
          $( '#publish-subnav' ).addClass( 'hide' );
          $( '#announcement-subnav' ).addClass( 'hide' );
          $( '#cluster-subnav' ).addClass( 'hide' );
        }

        listsetting.showlist(listsetting.category_name, listsetting.group_name);

        statusbar.show();
      }
    );
  }
);