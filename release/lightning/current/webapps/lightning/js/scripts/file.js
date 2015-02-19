
// #/:core/schema, #/:core/config
sammy.get
(
  new RegExp( app.core_regex_base + '\\/(schema|config)$' ),
  function( context )
  {
    var core_basepath = this.active_core.attr( 'data-basepath' );
	var filetype = context.params.splat[1]; // either schema or config	
	var filename = this.active_core.attr( filetype );

    $.ajax
    (
      {
        url : core_basepath + '/admin/file?file=" + filename + "&contentType=text/xml;charset=utf-8&token=' + app_token(),
        dataType : 'xml',
        context : $( '#content' ),
        beforeSend : function( xhr, settings )
        {
          this
          .html( '<div class="loader">Loading ...</div>' );
        },
        complete : function( xhr, text_status )
        {
          var code = $(
            '<pre class="syntax language-xml"><code>' +
            xhr.responseText.esc() +
            '</code></pre>'
          );
          this.html( code );

          if( 'success' === text_status )
          {
            hljs.highlightBlock( code.get(0) );
          }
        }
      }
    );
  }
);