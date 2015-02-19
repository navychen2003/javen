
function format_time( time )
{
  if (time != null)
    return parseDate(time).format('yyyy-mm-dd HH:MM:ss');
  else
    return '' + time;
}

function format_message( error_code, error_msg, error_trace ) 
{
  var msg = error_msg;
  if (msg == null) msg = '';

  if (message_strings) 
  {
    msg = message_strings.format_message( error_code, error_msg, error_trace );
  }
  else
  {
    msg = strings( msg );
  }
  
  return msg
}

function format_trace( error_trace ) 
{
  var trace = error_trace;
  if (trace == null) trace = '';
  trace = trace.replaceAll('<','&It;');
  trace = trace.replaceAll('>','&gt');
  trace = trace.replaceAll(' ','&nbsp;');
  trace = trace.replaceAll('\t','&nbsp;&nbsp;');
  trace = trace.replaceAll('\r\n','<br>');
  trace = trace.replaceAll('\n','<br>');
  return trace;
}

function format_message_html( error_code, error_msg, error_trace ) 
{
  var msg = format_message(error_code, error_msg, error_trace);
  if (msg == null) msg = '';
  
  var trace = format_trace(error_trace);
  if (trace == null) trace = '';
  
  var html = '<div class="row-fluid">' + "\n" +
             '  <div class="control-group">' + "\n" +
             '      <label>' + msg.esc() + '</label>' + "\n" +
             '  </div>' + "\n" +
             '</div>' + "\n" +
             '<div class="row-fluid">' + "\n" +
             '</div>' + "\n" +
             '<div class="row-fluid advanced-options hide" id="message-details">' + "\n" +
             '  <div class="control-group">' + "\n" +
             '      <span style="font-size: 12px;">' + trace + '</span>' + "\n" +
             '  </div>' + "\n" +
             '</div>';
  
  return html;
}

var message_dialogs = {
  message_dialog: null,
  error_msg: null,
  error_code: null,
  error_trace: null,
  
  init: function( dialog_element ) 
  { 
    messager = this;
    this.init_dialogs( dialog_element );
    
    $.get
    (
      'tpl/message.html',
      function( template )
      {
        messager.message_dialog =
        {
          element: dialog_element, 
          html: template,
          showcb: function()
          { 
            $( '#message-title' ).html( strings( 'Warning' ) );
            $( '#message-details-show' ).html( strings( 'Show Details' ) );
            $( '#message-details-hide' ).html( strings( 'Hide Details' ) );
            $( '#message-ok' ).html( strings( 'Ok' ) );
            $( '#message-no' ).remove();
            
            var msg = format_message_html(messager.error_code, messager.error_msg, messager.error_trace);
            if (msg == null) msg = '';
            
            var trace = messager.error_trace;
            if (trace != null && trace.length > 0) {
              $( '#message-details-show' )
                .removeClass( 'hide' );
            }
            
            $( '#message-text' )
              .html( msg );
            
            $( '#message-details-show' )
              .attr( 'onclick', 'javascript:messager.showdetails();return false;' )
              .attr( 'href', '' );
            
            $( '#message-details-hide' )
              .attr( 'onclick', 'javascript:messager.hidedetails();return false;' )
              .attr( 'href', '' );
            
            $( '#message-ok' )
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
        
        message_dialogs.init_dialog( dialog_element, template );
      }
    );
  },
  showdetails: function()
  {
    var details_element = $( '#message-details' );
    if (details_element) {
      details_element.removeClass( 'hide' );
      $( '#message-details-show' ).addClass( 'hide' );
      $( '#message-details-hide' ).removeClass( 'hide' );
    }
  },
  hidedetails: function()
  {
    var details_element = $( '#message-details' );
    if (details_element) {
      details_element.addClass( 'hide' );
      $( '#message-details-show' ).removeClass( 'hide' );
      $( '#message-details-hide' ).addClass( 'hide' );
    }
  },
  init_dialogs: function( dialog_element )
  {
    artwork.init_dialog( dialog_element );
    selectfolder.init_dialog( dialog_element );
    compose.init_dialog( dialog_element );
    publish.init_dialog( dialog_element );
    publishinfo.init_dialog( dialog_element );
    messageinfo.init_dialog( dialog_element );
    musicplayer.init_dialog( dialog_element );
    userinfo.init_dialog( dialog_element );
    fileinfo.init_dialog( dialog_element );
    uploader.init_dialog( dialog_element );
    
    library_dialogs.init_addlibrary( dialog_element );
    
  },
  init_dialog: function( dialog_element, template ) 
  {
    if (userlockform)
      userlockform.init_message( dialog_element, template );
    
    if (messageinfo_dialogs)
      messageinfo_dialogs.init_message( dialog_element, template );
    
    if (publishinfo_dialogs)
      publishinfo_dialogs.init_message( dialog_element, template );
    
    if (announcement_dialogs)
      announcement_dialogs.init_message( dialog_element, template );
    
    if (friend_dialogs)
      friend_dialogs.init_message( dialog_element, template );
    
    if (contact_dialogs)
      contact_dialogs.init_message( dialog_element, template );
    
    if (library_dialogs)
      library_dialogs.init_message( dialog_element, template );
    
    if (section_dialogs)
      section_dialogs.init_message( dialog_element, template );
    
    if (member_dialogs)
      member_dialogs.init_message( dialog_element, template );
    
    if (listsetting)
      listsetting.init_message( dialog_element, template );
  },
  showerror: function( message )
  {
    messager.error_code = -1;
    messager.error_msg = message;
    messager.error_trace = '';
    dialog.show( messager.message_dialog );
  }
};

var message_strings = {
  format_message: function( error_code, error_msg, error_trace )
  {
    var msg = error_msg;
    if (msg == null) msg = '';

    var txt = msg.toLowerCase();
  
    if (txt.indexOf('unauthorized') >= 0) 
    {
      globalApp.clear_user();
      dialog.unauthorized = true;
    
      if (txt.indexOf('timeout') >= 0)
        msg = strings( 'Session Timeout' );
      else
        msg = strings( 'Unauthorized Access' );
    
    }
    else if (txt.indexOf('access denied') >= 0) 
    {
      if (txt.indexOf('no permission') >= 0)
        msg = strings( 'Access denied, no permission.' );
      else
        msg = strings( 'Access denied' );
    }
    else if (txt.indexOf('not found') >= 0) 
    {
      if (txt.indexOf('user') >= 0)
        msg = strings( 'User not found' );
      else if (txt.indexOf('group') >= 0)
        msg = strings( 'Group not found' );
      else if (txt.indexOf('file') >= 0)
        msg = strings( 'File not found' );
      else if (txt.indexOf('section') >= 0)
        msg = strings( 'File not found' );
      else if (txt.indexOf('item') >= 0)
        msg = strings( 'File not found' );
      else if (txt.indexOf('data') >= 0)
        msg = strings( 'File not found' );
      else if (txt.indexOf('channel') >= 0)
        msg = strings( 'Channel not found' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('already existed') >= 0) 
    {
      if (txt.indexOf('user') >= 0)
        msg = strings( 'User already existed' );
      else if (txt.indexOf('email') >= 0)
        msg = strings( 'Email already existed' );
      else if (txt.indexOf('folder') >= 0)
        msg = strings( 'Folder name already existed' );
      else if (txt.indexOf('name') >= 0)
        msg = strings( 'Name already existed' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('cannot move item') >= 0) 
    {
      if (txt.indexOf('to itself') >= 0)
        msg = strings( 'Cannot move item to itself' );
      else if (txt.indexOf('current folder') >= 0)
        msg = strings( 'Cannot move item to it\'s current folder' );
      else if (txt.indexOf('sub folder') >= 0)
        msg = strings( 'Cannot move item to it\'s sub folder' );
      else
        msg = strings( msg );
    }
    else if (txt.indexOf('is not empty') >= 0) 
    {
      if (txt.indexOf('library') >= 0) {
        if (txt.indexOf('cannot be deleted') >= 0)
          msg = strings( 'Library is not empty and cannot be deleted' );
        else
          msg = strings( 'Library is not empty' );
      } else if (txt.indexOf('selected root') >= 0)
        msg = strings( 'Selected root folder is not empty' );
      else if (txt.indexOf('root') >= 0)
        msg = strings( 'Root folder is not empty' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('cannot save file') >= 0) 
    {
      if (txt.indexOf('upload target') >= 0)
        msg = strings( 'Upload target cannot save file' );
      else if (txt.indexOf('copy to target') >= 0)
        msg = strings( 'Copy to target cannot save file' );
      else if (txt.indexOf('move to target') >= 0)
        msg = strings( 'Move to target cannot save file' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('cannot create folder') >= 0) 
    {
      if (txt.indexOf('new folder target') >= 0)
        msg = strings( 'New folder target cannot create folder' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('cannot be moved') >= 0) 
    {
      if (txt.indexOf('trash item') >= 0)
        msg = strings( 'Trash item cannot be moved' );
      else if (txt.indexOf('selected item') >= 0)
        msg = strings( 'Selected item cannot be moved' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('cannot be copied') >= 0) 
    {
      if (txt.indexOf('selected item') >= 0)
        msg = strings( 'Selected item cannot be copied' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('cannot be deleted') >= 0) 
    {
      if (txt.indexOf('library') >= 0) {
        if (txt.indexOf('is the only one') >= 0)
          msg = strings( 'Library is the only one and cannot be deleted' );
        else if (txt.indexOf('is default') >= 0)
          msg = strings( 'Library is default and cannot be deleted' );
        else if (txt.indexOf('is not empty') >= 0)
          msg = strings( 'Library is not empty and cannot be deleted' );
        else
          msg = strings( 'Library cannot be deleted' );
      } else if (txt.indexOf('root') >= 0) {
        if (txt.indexOf('is not empty') >= 0)
          msg = strings( 'Root is not empty and cannot be deleted' );
        else
          msg = strings( 'Root cannot be deleted' );
      } else if (txt.indexOf('selected item') >= 0)
        msg = strings( 'Selected item cannot be deleted' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('cannot change name') >= 0) 
    {
      if (txt.indexOf('root') >= 0)
        msg = strings( 'Root cannot change name' );
      else
        msg = strings( msg );
    
    }
    else if (txt.indexOf('is not illegal format') >= 0)
    {
      if (txt.indexOf('email') >= 0)
        msg = strings( 'Email is not illegal format' );
      else
        msg = strings( msg );
      
    }
    else if (txt.indexOf('are not manager of group') >= 0) 
    {
      msg = strings( 'You are not manager of this group' );
    }
    else if (txt.indexOf('is root and cannot be removed') >= 0) 
    {
      msg = strings( 'The folder is root and cannot be removed' );
    }
    else if (txt.indexOf('already registered') >= 0) 
    {
      if (txt.indexOf('user') >= 0)
        msg = strings( 'User already registered' );
      else if (txt.indexOf('library') >= 0)
        msg = strings( 'Library already registered' );
      else
        msg = strings( msg );
    }
    else if (txt.indexOf('disabled') >= 0)
    {
      if (txt.indexOf('user') >= 0) {
        if (txt.indexOf('cannot login') >= 0)
          msg = strings( 'User is disabled, cannot login' );
        else
          msg = strings( 'User is disabled, cannot access' );
      } else if (txt.indexOf('data') >= 0) {
        msg = strings( 'Data is disabled, cannot access' ); 
      } else
        msg = strings( msg );
    }
    else if (txt.indexOf('locked') >= 0) 
    {
      if (txt.indexOf('user') >= 0)
        msg = strings( 'User is locked, cannot access' );
      else if (txt.indexOf('library') >= 0)
        msg = strings( 'Library is locked, cannot access' );
      else if (txt.indexOf('root') >= 0)
        msg = strings( 'Data is locked, cannot access' );
      else if (txt.indexOf('folder') >= 0)
        msg = strings( 'Data is locked, cannot access' );
      else if (txt.indexOf('data') >= 0)
        msg = strings( 'Data is locked, cannot access' );
      else
        msg = strings( msg );
    }
    else if (txt.indexOf('wrong category') >= 0) 
    {
      if (txt.indexOf('user') >= 0)
        msg = strings( 'Wrong category for user' );
      else if (txt.indexOf('group') >= 0)
        msg = strings( 'Wrong category for group' );
      else
        msg = strings( msg );
    }
    else if (txt.indexOf('has no enough space') >= 0) 
    {
      if (txt.indexOf('user') >= 0)
        msg = strings( 'User has no enough space' );
      else if (txt.indexOf('group') >= 0)
        msg = strings( 'Group has no enough space' );
      else
        msg = strings( msg );
    }
    else
    {
      msg = strings( msg );
    }
  
    return msg
  }
};
