
var playlist = {
  refresh: function()
  {
    this.showlist();
  },
  shuffle: function()
  {
  },
  showlist: function()
  {
    navbar.init_name( strings( 'Playlist' ), 'javascript:return false;', null );
    musicplayer.init_navicon();
    
    var shufflelink_element = $( '#action-shuffle-link' );
    
    if (shufflelink_element) {
      shufflelink_element
        .attr( 'title', strings('Shuffle') )
        .attr( 'onClick', 'javascript:playlist.shuffle();return false;' )
        .attr( 'href', '' );
    }
    
    var sections = musicplayer.sections;
    if (sections == null) sections = [];
    
    var sectionContent = [];
    for (var key=0; key < sections.length; key++) {
      var section = sections[key];
      if (section == null) continue;
      
      var item = this.buildItem( section, key );
      if (item == null) continue;
      
      sectionContent.push( item );
    }
    
    $( '#albums-tracklist' ).html( sectionContent.join("\n") );
  },
  buildItem: function( section, idx )
  {
    if (section == null) return null;
    
    var section_id = section['id'];
    var section_name = section['name'];
    var section_type = section['type'];
    var section_perms = section['perms'];
    var section_ops = section['ops'];
    var extname = section['extname'];
    var isfolder = section['isfolder'];
    var timeLen = section['timelen'];
    var query = section['query'];
    var poster = section['poster'];
    var background = section['background'];
  
    var root_id = section['root_id'];
    var parent_id = section['parent_id'];
    var parent_name = section['parent_name'];
    var parent_type = section['parent_type'];
    
    var library_id = section['library_id'];
    var library_name = section['library_name'];
    var library_type = section['library_type'];
    var library_hostname = section['hostname'];
  
    var extension = extname;
    var meta_author = '';
    var meta_genre = '';
    var meta_album = '';
    var meta_year = '';
    var meta_title = '';
    var meta_subtitle = '';
    var meta_summary = '';
    
    var section_details = section['details'];
    if (section_details) {
      var metadata = section_details['metadata'];
      if (metadata) {
        meta_author = metadata['author'];
        meta_genre = metadata['genre'];
        meta_album = metadata['album'];
        meta_year = metadata['year'];
        meta_title = metadata['title'];
        meta_subtitle = metadata['subtitle'];
        meta_summary = metadata['summary'];
      }
    }
    
    if (meta_author == null) meta_author = '';
    if (meta_genre == null) meta_genre = '';
    if (meta_album == null) meta_album = '';
    if (meta_year == null) meta_year = '';
    if (meta_title == null) meta_title = '';
    if (meta_subtitle == null) meta_subtitle = '';
    if (meta_summary == null) meta_summary = '';
  
    if (section_type == null) section_type = '';
    if (section_name == null) section_name = '';
    if (section_id == null) section_id = '';
    if (extname == null) extname = '';
    if (extension == null || extension.length == 0) extension = 'dat';
    if (isfolder == null) isfolder = false;
    
    var postersrc = 'css/' + app.theme + '/images/posters/music.png';
    var audiosrc = app.base_path + '/file/' + section_id + '.' + extension + '?token=' + app.token;
    var singer = '';
    var title = section_name;
    
    if (meta_title != null && meta_title.length > 0)
      title = meta_title;
    if (meta_author != null && meta_author.length > 0)
      singer = meta_author;
    
    var durationLen = 0;
    if (timeLen != null && timeLen > 0)
      durationLen = timeLen / 1000;
    
    if (durationLen <= 0) durationLen = 300;
    var durationStr = musicplayer.readableSeconds( durationLen );
    if (durationStr == null) durationStr = '';
    
    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      postersrc = app.base_path + '/image/' + imgid + '_256t.' + imgext + '?token=' + app.token;
    }
    
    var playClick = 'javascript:musicplayer.playid(\'' + section_id + '\');return false;';
    var removeClick = 'javascript:musicplayer.removeid(\'' + section_id + '\');playlist.refresh();return false;';
    var infoClick = 'javascript:musicplayer.showdetailsid(\'' + section_id + '\');return false;';
    var editClick = 'javascript:return false;';
    
    var item = 
       '	<li>' + "\n" +
       '		<div class="album-art">' + "\n" +
       '			<a class="album-link" href="" onclick="' + infoClick + '">' + "\n" +
       '				<img class="poster media-tile-list-poster placeholder" src="' + postersrc + '">' + "\n" +
       '			</a>' + "\n" +
       '			<div class="album-title hide"></div>' + "\n" +
       '			<div class="album-year hide"></div>' + "\n" +
       '		</div>' + "\n" +
       '		<ul class="list media-list media-compact-list track-list rounded-list">' + "\n" +
       '			<li>' + "\n" +
       '				<a data-focus="keyboard">' + "\n" +
       '				<span class="number track-number">' + (idx+1) + '</span>' + "\n" +
       '				<span class="title track-title">' + title.esc() + '</span>' + "\n" +
       '				<span class="subtitle track-duration media-details-right">' + durationStr.esc() + '</span>' + "\n" +
       '				<button class="play-btn hover-btn" tabindex="-1" onclick="' + playClick + '"><i class="glyphicon play"></i></button>' + "\n" +
       '				<button class="remove-btn hover-btn" tabindex="-1" onclick="' + removeClick + '"><i class="glyphicon remove-2"></i></button>' + "\n" +
       '				<button class="edit-btn hover-btn hide" tabindex="-1" onclick="' + editClick + '"><i class="glyphicon pencil"></i></button>' + "\n" +
       '				<button class="info-btn hover-btn hide" tabindex="-1" onclick="' + infoClick + '"><i class="glyphicon circle-info"></i></button>' + "\n" +
       '				</a>' + "\n" +
       '			</li>' + "\n" +
       '		</ul>' + "\n" +
       '	</li>';
	
	return item;
  }
};

var playlist_headbar = {
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
        $( '#musiclist-link' ).addClass( 'active' );
        
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

// #/~playlist
sammy.get
(
  /^#\/(~playlist)$/,
  function( context )
  {
    if (init_page(context) == false) return;
    
    var title_element = $( '#content-title' );
    var header_element = $( '#content-header' );
    var body_element = $( '#content-body' );
    var dialog_element = $( '#content-dialog' );
    
    playlist_headbar.init( header_element );
    message_dialogs.init( dialog_element );

    $.get
    (
      'tpl/playlist.html',
      function( template )
      {
        body_element
          .html( template );

        playlist.showlist();

        statusbar.show();
      }
    );
  }
);