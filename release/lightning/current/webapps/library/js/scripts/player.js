
var musicvolume = {
  media: null,
  volume_draging: false,
  volume_div: null,
  volume_bar: null,
  volume_thumb: null,
  volume_icon: null,
  volume_left: 0,
  volume_posx: 0,
  volume_px: 0,
  volume_muted: false,
  volume: 0,
  
  init: function( media, volumePx )
  {
    this.media = media;
    this.volume_draging = false;
    this.volume_div = $( '#musicplayer-volume' );
    this.volume_bar = $( '#musicplayer-volume-bar' );
    this.volume_thumb = $( '#musicplayer-volume-thumb' );
    this.volume_icon = $( '#musicplayer-volume-icon' );
    this.volume_left = 0;
    this.volume_posx = 0;
    this.volume_px = 0;
    this.volume_muted = false;
    this.volume = 0;
    
    $( '#musicplayer-volume-track' )
      .attr( 'onmousedown', 'javascript:musicvolume.onmousedown(this,event);' );
    
    $( '#musicplayer-volume-icon' )
      .attr( 'onclick', 'javascript:musicvolume.set_muted();return false;' );
    
    this.set_volume( volumePx, this.volume_bar, this.volume_thumb, this.volume_icon );
  },
  onmousedown: function( element, event )
  {
    if (event == null) event = window.event; // ie
    this.volume_posx = event.clientX;
    this.volume_left = getElementLeft( element );
    this.volume_draging = true;
    
    document.onmouseup = this.onmouseup;
    document.onmousemove = this.onmousemove;
    
    if (this.volume_div)
      this.volume_div.addClass( 'player-active-slider' );
  },
  onmouseup: function( event )
  {
    if (event == null) event = window.event; // ie
    musicvolume.volume_posx = event.clientX;
    musicvolume.volume_draging = false;
    musicvolume.update_volume();
    
    document.onmouseup = null;
    document.onmousemove = null;
    
    if (musicvolume.volume_div)
      musicvolume.volume_div.removeClass( 'player-active-slider' );
  },
  onmousemove: function( event )
  {
    if (event == null) event = window.event; // ie
    musicvolume.volume_posx = event.clientX;
    if (musicvolume.volume_draging == false) return;
    musicvolume.update_volume();
  },
  onmouseout: function( event )
  {
    if (event == null) event = window.event; // ie
    //musicvolume.volume_draging = false;
  },
  update_volume: function()
  {
    var volumePx = musicvolume.volume_posx - musicvolume.volume_left;
    musicvolume.set_volume( volumePx, 
      musicvolume.volume_bar, musicvolume.volume_thumb, 
      musicvolume.volume_icon );
  },
  set_volume: function( volumePx, volume_bar, volume_thumb, volume_icon )
  {
    if (volumePx == null || volumePx > 100) volumePx = 100;
    if (volumePx < 0) volumePx = 0;
    
    var volume = volumePx / 100;
    
    var changed = false;
    var media = this.media;
    if (media) {
      media.volume = volume;
      musicvolume.volume = volume;
      musicvolume.volume_muted = false;
        
      if (volume < 0.05) {
        media.muted = true;
        this.volume_muted = true;
        volumePx = 0;
      } else 
        media.muted = false;
        
      changed = true;
    }
    if (changed == false) return;
    
    this.volume_px = volumePx;
    this.set_volumeicon( volume, volume_icon );
    
    var volumeThumb = volumePx - 3;
    if (volumeThumb < 0) volumeThumb = 0;
    
    if (volume_bar) 
      volume_bar.attr( 'style', 'width: ' + volumePx + 'px;' );
    
    if (volume_thumb)
      volume_thumb.attr( 'style', 'left: ' + volumeThumb + 'px;' );
  },
  set_volumeicon: function( volume, volume_icon )
  {
    if (volume_icon) {
      if (volume >= 0.5) {
        volume_icon
          .removeClass( 'volume-low' )
          .removeClass( 'volume-off' );
      } else if (volume >= 0.05) {
        volume_icon
          .addClass( 'volume-low' )
          .removeClass( 'volume-off' );
      } else {
        volume_icon
          .removeClass( 'volume-low' )
          .addClass( 'volume-off' );
      }
    }
  },
  set_muted: function()
  {
    var changed = false;
    var media = this.media;
    if (media) {
      var volume_bar = this.volume_bar;
      var volume_thumb = this.volume_thumb;
      var volume_icon = this.volume_icon;
    
      var volumePx = this.volume_px;
      var muted = media.muted;
    
      if (muted == true) {
        media.muted = false;
        media.volume = this.volume;
        this.volume_muted = false;
        this.set_volumeicon( 100, volume_icon );
      
      } else {
        this.volume = media.volume;
        media.muted = true;
        this.volume_muted = true;
        this.set_volumeicon( 0, volume_icon );
      }
    
      changed = true;
    }
  }
};

var musicprogress = {
  callbacks: [],
  callback_event: null,
  callback_eventname: null,
  section: null,
  media: null,
  action_pause: null,
  progress_draging: false,
  progress_time: null,
  progress_duration: null,
  progress_bar: null,
  progress_thumb: null,
  progress_width: 0,
  progress_left: 0,
  progress_posx: 0,
  duration: 0,

  add_callback: function( cb )
  {
    if (cb == null) return;
    
    var eventname = musicprogress.callback_eventname;
    var event = musicprogress.callback_event;
    var section = musicprogress.section;
    
    if (eventname != null && eventname.length > 0)
      cb.call( this, eventname, event, section );
    
    for (var idx=0; idx < this.callbacks.length; idx++) {
      var callback = this.callbacks[idx];
      if (callback == cb) return;
    }
    
    this.callbacks.push( cb );
  },
  remove_callback: function( cb )
  {
    if (cb == null) return;
    
    var index = -1;
    
    for (var idx=0; idx < this.callbacks.length; idx++) {
      var callback = this.callbacks[idx];
      if (callback == cb) {
        index = idx; break;
      }
    }
    
    if (index >= 0 && index < this.callbacks.length)
      this.callbacks.splice( index, 1 );
  },
  init: function( section, media, duration )
  {
    var mediaold = this.media;
    if (mediaold) {
      mediaold.pause();
      mediaold.removeEventListener('playing', musicprogress.onplaying, true);
      mediaold.removeEventListener('pause', musicprogress.onpause, true);
      mediaold.removeEventListener('ended', musicprogress.onended, true);
      mediaold.removeEventListener('timeupdate', musicprogress.ontimeupdate, true);
    }
    
    this.section = section;
    this.media = media;
    this.action_pause = $( '#musicplayer-pause-icon' );
    this.progress_draging = false;
    this.progress_time = $( '#musicplayer-currenttime' );
    this.progress_duration = $( '#musicplayer-duration' );
    this.progress_bar = $( '#musicplayer-progress-bar' );
    this.progress_thumb = $( '#musicplayer-progress-thumb' );
    this.progress_width = 0;
    this.progress_left = 0;
    this.progress_posx = 0;
    this.duration = duration;
    
    if (media) {
      media.addEventListener('playing', musicprogress.onplaying, true);
      media.addEventListener('pause', musicprogress.onpause, true);
      media.addEventListener('ended', musicprogress.onended, true);
      media.addEventListener('timeupdate', musicprogress.ontimeupdate, true);
    }
    
    $( '#musicplayer-progress-track' )
      .attr( 'onmousedown', 'javascript:musicprogress.onmousedown(this,event);' );
  },
  onplaying: function( event )
  {
    var eventname = 'playing';
    musicprogress.callback_eventname = eventname;
    musicprogress.callback_event = event;
    
    if (musicprogress.action_pause) 
      musicprogress.action_pause.attr( 'class', 'glyphicon pause' );
    
    var callbacks = musicprogress.callbacks;
    var section = musicprogress.section;
    if (callbacks) {
      for (var key in callbacks) {
        var cb = callbacks[key];
        if (cb) cb.call( this, eventname, event, section );
      }
    }
  },
  onpause: function( event )
  {
    var eventname = 'pause';
    musicprogress.callback_eventname = eventname;
    musicprogress.callback_event = event;
    
    if (musicprogress.action_pause) 
      musicprogress.action_pause.attr( 'class', 'glyphicon play' );
    
    var callbacks = musicprogress.callbacks;
    var section = musicprogress.section;
    if (callbacks) {
      for (var key in callbacks) {
        var cb = callbacks[key];
        if (cb) cb.call( this, eventname, event, section );
      }
    }
  },
  onended: function( event )
  {
    var eventname = 'ended';
    musicprogress.callback_eventname = eventname;
    musicprogress.callback_event = event;
    
    if (musicprogress.action_pause) 
      musicprogress.action_pause.attr( 'class', 'glyphicon play' );
    
    if (musicplayer.playnext() == false)
      musicplayer.hide_player();
    
    var callbacks = musicprogress.callbacks;
    var section = musicprogress.section;
    if (callbacks) {
      for (var key in callbacks) {
        var cb = callbacks[key];
        if (cb) cb.call( this, eventname, event, section );
      }
    }
  },
  ontimeupdate: function( event )
  {
    var media = musicprogress.media;
    if (media == null) return;
    
    var currentTime = media.currentTime;
    var totalTime = media.duration;
    var progress = 0;
  
    if (currentTime == null || currentTime <= 0) 
      currentTime = 0;
    if (totalTime == null || totalTime == Infinity || totalTime <= 0) 
      totalTime = musicprogress.duration;
    if (totalTime > 0) 
      progress = currentTime / totalTime;
  
    if (totalTime > 65356367383 || media.ended == true) {
      totalTime = musicprogress.duration;
      currentTime = totalTime;
      progress = 1;
    }
  
    if (musicprogress.volume_draging == true)
      return;
    
    musicprogress.set_progress(progress, currentTime, totalTime);
  },
  onmousedown: function( element, event )
  {
    if (event == null) event = window.event; // ie
    this.progress_posx = event.clientX;
    this.progress_width = element.clientWidth;
    this.progress_left = getElementLeft( element );
    this.progress_draging = true;
    
    document.onmouseup = this.onmouseup;
    document.onmousemove = this.onmousemove;
    
    var media = musicprogress.media;
    if (media) media.pause();
  },
  onmouseup: function( event )
  {
    if (event == null) event = window.event; // ie
    musicprogress.progress_posx = event.clientX;
    musicprogress.progress_draging = false;
    musicprogress.update_progress();
    
    document.onmouseup = null;
    document.onmousemove = null;
    
    var media = musicprogress.media;
    if (media) media.play();
  },
  onmousemove: function( event )
  {
    if (event == null) event = window.event; // ie
    musicprogress.progress_posx = event.clientX;
    if (musicprogress.progress_draging == false) return;
    musicprogress.update_progress();
  },
  update_progress: function()
  {
    var progressPx = musicprogress.progress_posx - musicprogress.progress_left;
    var progressWidth = musicprogress.progress_width;
    
    if (progressPx == null || progressPx < 0) progressPx = 0;
    if (progressWidth <= 0) return;
    
    var progress = progressPx / progressWidth;
    if (progress < 0) progress = 0;
    if (progress > 1) progress = 1;
    
    var duration = musicprogress.duration;
    if (duration <= 0) duration = 300;
    
    var currenttime = duration * progress;
    musicprogress.set_progress( progress, currenttime, duration );
    
    var media = musicprogress.media;
    if (media && currenttime >= 0) {
      //var seekstart = media.seekable.start();
      //var seekend = media.seekable.end();
      media.currentTime = currenttime;
      //var played = media.played.end();
    }
  },
  set_progress: function( progress, currenttime, duration )
  {
    if (progress < 0) progress = 0;
    if (progress > 1) progress = 1;
    
    var percent = progress * 100;
    if (progress >= 1) percent = 100 - 0.02;
    var percentThumb = percent + 0.02;
    var percentBar = percent - 0.02;
  
    if (duration <= 0) duration = 300;
    if (currenttime < 0) currenttime = 0;
    if (currenttime > duration) currenttime = duration;
    
    musicprogress.duration = duration;
  
    var currentStr = musicplayer.readableSeconds( currenttime );
    var durationStr = musicplayer.readableSeconds( duration );
    
    if (musicprogress.progress_time) 
      musicprogress.progress_time.html( currentStr );
    
    if (musicprogress.progress_duration) 
      musicprogress.progress_duration.html( durationStr );
    
    if (musicprogress.progress_bar) 
      musicprogress.progress_bar.attr( 'style', 'width: ' + percentBar + '%;' );
    
    if (musicprogress.progress_thumb) 
      musicprogress.progress_thumb.attr( 'style', 'left: ' + percentThumb + '%;' );
  }
};

var musicplayer = {
  sections: [],
  section_idx: 0,
  section: null,
  
  reset: function()
  {
    this.stop();
    this.hide_player();
    this.sections = [];
    this.section_idx = 0;
    this.section = null;
  },
  is_empty: function()
  {
    var sections = this.sections;
    if (sections != null && sections.length > 0)
      return false;
    return true;
  },
  addid: function( id ) 
  {
    if (id == null || id.length == 0) return;
    
    if (this.sections == null)
      this.sections = [];
    
    for (var key=0; key < this.sections.length; key++) {
      var sec = this.sections[key];
      if (sec && id == sec['id']) {
        return;
      }
    }
    
    var params = '&action=details&id=' + encodeURIComponent(id);

    $.ajax
    (
      {
        url : app.base_path + '/sectioninfo?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          musicplayer.add( response );
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
  removeid: function( id )
  {
    if (id == null || id.length == 0) return;
    
    if (this.sections == null)
      this.sections = [];
    
    for (var key=0; key < this.sections.length; key++) {
      var sec = this.sections[key];
      if (sec && id == sec['id']) {
        var section = this.section;
        if (section && id == section['id']) 
          this.stop();
        
        this.sections.splice(key,1);
        if (this.section_idx > key)
          this.section_idx = this.section_idx-1;
        
        return;
      }
    }
  },
  showdetailsid: function( id )
  {
    if (id == null || id.length == 0) return;
    
    if (this.sections == null)
      this.sections = [];
    
    for (var key=0; key < this.sections.length; key++) {
      var sec = this.sections[key];
      if (sec && id == sec['id']) {
        fileinfo.showdetails( sec );
        return;
      }
    }
  },
  playid: function( id )
  {
    if (id == null || id.length == 0) return;
    
    if (this.sections == null)
      this.sections = [];
    
    for (var key=0; key < this.sections.length; key++) {
      var sec = this.sections[key];
      if (sec && id == sec['id']) {
        this.play( sec );
        return;
      }
    }
    
    var params = '&action=details&id=' + encodeURIComponent(id);

    $.ajax
    (
      {
        url : app.base_path + '/sectioninfo?token=' + app.token + params + '&wt=json',
        dataType : 'json',
        context : null,
        beforeSend : function( xhr, settings )
        {
          show_loading();
        },
        success : function( response, text_status, xhr )
        {
          musicplayer.play( response );
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
  add: function( section )
  {
    if (section == null) return;
    
    var sectionid = section['id'];
    if (sectionid == null || sectionid.length == 0)
      return;
    
    if (this.sections == null)
      this.sections = [];
    
    for (var key=0; key < this.sections.length; key++) {
      var sec = this.sections[key];
      if (sec && sectionid == sec['id']) {
        section['media'] = sec['media'];
        
        this.sections[key] = section;
        return;
      }
    }
    
    this.sections.push( section );
    this.update_player();
  },
  play: function( section )
  {
    if (section == null) return;
    
    var sectionid = section['id'];
    if (sectionid == null || sectionid.length == 0)
      return;
    
    if (this.sections == null)
      this.sections = [];
    
    for (var key=0; key < this.sections.length; key++) {
      var sec = this.sections[key];
      if (sec && sectionid == sec['id']) {
        section['media'] = sec['media'];
        
        this.sections[key] = section;
        this.section = section;
        this.section_idx = key;
        
        this.stop();
        this.init_player();
        this.init_navicon();
        return;
      }
    }
    
    this.sections.push( section );
    this.section = section;
    this.section_idx = this.sections.length -1;
    
    this.stop();
    this.init_player();
    this.init_navicon();
  },
  stop: function()
  {
    var section = this.section;
    if (section) {
      var media = section['media'];
      if (media) { 
        media.pause();
        media.src = ''; // must close
      }
      section['media'] = null;
    }
    
    this.hide_player();
  },
  pause: function()
  {
    var section = this.section;
    if (section) {
      var media = section['media'];
      if (media) {
        if (media.ended) {
          media.pause();
          media.startTime = 0;
          media.currentTime = 0;
          media.play();
        } else {
          if (media.paused) media.play();
          else media.pause();
        }
      }
    }
  },
  playprev: function()
  {
    var sections = this.sections;
    var section_idx = this.section_idx;
    
    if (sections && sections.length > 0) {
      var idx = section_idx - 1;
      if (idx >= 0 && idx < sections.length) {
        this.play( sections[idx] );
        return true;
      }
    }
    
    return false;
  },
  playnext: function()
  {
    var sections = this.sections;
    var section_idx = this.section_idx;
    
    if (sections && sections.length > 0) {
      var idx = section_idx + 1;
      if (idx >= 0 && idx < sections.length) {
        this.play( sections[idx] );
        return true;
      }
    }
    
    return false;
  },
  toggle_player: function()
  {
    var javen_element = $( '#javen' );
    if (javen_element.hasClass( 'show-music-player' )) {
      this.show_player();
    } else { 
      this.hide_player();
    }
  },
  show_player: function()
  {
    this.init_player();
    this.init_navicon();
  },
  hide_player: function()
  {
    var javen_element = $( '#javen' );
    var player_element = $( '#music-player' );
    
    javen_element.removeClass( 'show-music-player' );
    player_element.html( '' );
  },
  update_player: function()
  {
    var javen_element = $( '#javen' );
    if (javen_element.hasClass( 'show-music-player' )) {
      var section_idx = this.section_idx;
      var sections = this.sections;
      
      var prevDisabled = true;
      var nextDisabled = true;
    
      if (sections && sections.length > 0) {
        if (section_idx > 0 && section_idx <= sections.length -1) 
          prevDisabled = false;
        if (section_idx >= 0 && section_idx < sections.length -1) 
          nextDisabled = false;
      }
      
      var prev_element = $( '#musicplayer-prev' );
      var next_element = $( '#musicplayer-next' );
      
      if (prev_element) { 
        if (prevDisabled)
          prev_element.addClass( 'disabled' );
        else
          prev_element.removeClass( 'disabled' );
      }
      
      if (next_element) { 
        if (nextDisabled)
          next_element.addClass( 'disabled' );
        else
          next_element.removeClass( 'disabled' );
      }
    }
  },
  click_poster: function()
  {
    var section = this.section;
    if (section) {
      fileinfo.showdetails( section );
      return;
    }
  },
  init_navicon: function()
  {
    var musiclist_element = $( '#musiclist-link' );
    if (musiclist_element) {
      musiclist_element
        //.attr( 'onclick', 'javascript:musicplayer.toggle_player();return false;' )
        .attr( 'href', '#/~playlist' )
        .attr( 'title', strings( 'Music' ) );
      if (this.is_empty()) {
        musiclist_element.addClass( 'hide' );
      } else {
        musiclist_element.removeClass( 'hide' );
      }
    }
  },
  init_player: function()
  {
    var section = this.section;
    var sections = this.sections;
    var section_idx = this.section_idx;
    if (section == null) {
      this.hide_player();
      return;
    }
    
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
    
    var javen_element = $( '#javen' );
    var player_element = $( '#music-player' );
    
    //if (javen_element.hasClass( 'show-music-player' ))
    //  return;
    
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
    
    if (poster != null && poster.length > 0) {
      var imgid = poster;
      var imgext = 'jpg';
      
      postersrc = app.base_path + '/image/' + imgid + '_256t.' + imgext + '?token=' + app.token;
    }
    
    var prevDisabled = 'disabled';
    var nextDisabled = 'disabled';
    
    if (sections && sections.length > 0) {
      if (section_idx > 0 && section_idx <= sections.length -1) 
        prevDisabled = '';
      if (section_idx >= 0 && section_idx < sections.length -1) 
        nextDisabled = '';
    }
    
    var media = section['media']; // how to restart?
    if (media == null || media.ended == true) { // || media.paused == true) {
      media = new Audio(audiosrc);
      section['media'] = media;
    }
    
    var volumePx = musicplayer.get_volumepx( media );
    var volumeThumb = volumePx - 3;
    
    var posterClick = 'javascript:musicplayer.click_poster();return false;';
    var stopClick = 'javascript:musicplayer.stop();return false;';
    var pauseClick = 'javascript:musicplayer.pause();return false;';
    var prevClick = 'javascript:musicplayer.playprev();return false;';
    var nextClick = 'javascript:musicplayer.playnext();return false;';
    
    var item = '<div class="player-controls">' + "\n" +
               '	<a class="now-playing-album" href="" onClick="' + posterClick + '"><img class="poster now-playing-poster placeholder" src="' + postersrc + '"></a>' + "\n" +
               '	<div class="now-playing">' + "\n" +
               '		<a class="now-playing-title">' + singer.esc() + '</a>' + "\n" +
               '		<span class="now-playing-subtitle">' + title.esc() + '</span>' + "\n" +
               '	</div>' + "\n" +
               '	<div class="player-controls-left">' + "\n" +
               '		<button id="musicplayer-prev" class="player-previous-btn player-btn ' + prevDisabled + '" onclick="' + prevClick + '"><i class="glyphicon rewind"></i></button>' + "\n" +
               '		<button id="musicplayer-play" class="player-pause-btn player-play-pause-btn player-btn" onclick="' + pauseClick + '"><i id="musicplayer-pause-icon" class="glyphicon pause"></i></button>' + "\n" +
               '		<button id="musicplayer-stop" class="player-stop-btn player-btn" onclick="' + stopClick + '"><i class="glyphicon stop"></i></button>' + "\n" +
               '		<button id="musicplayer-next" class="player-next-btn player-btn ' + nextDisabled + '" onclick="' + nextClick + '"><i class="glyphicon forward"></i></button>' + "\n" +
               '	</div>' + "\n" +
               '	<div class="player-controls-right">' + "\n" +
               '		<div id="musicplayer-volume" class="player-volume">' + "\n" +
               '		<div id="musicplayer-volume-track" class="player-slider-track" style="margin-top: 10px;">' + "\n" +
               '			<div id="musicplayer-volume-thumb" class="player-slider-thumb" style="left: ' + volumeThumb + 'px;"></div>' + "\n" +
               '			<div id="musicplayer-volume-bar" class="player-slider-bar" style="width: ' + volumePx + 'px;"></div>' + "\n" +
               '		</div>' + "\n" +
               '		<button id="musicplayer-volume-icon" class="player-btn player-slider-btn glyphicon"></button>' + "\n" +
               '		</div>' + "\n" +
               '		<div class="player-time" style="padding-top: 2px;">' + "\n" +
               '			<span id="musicplayer-currenttime" class="player-current-time">0:00</span> / <span id="musicplayer-duration" class="player-duration">0:00</span>' + "\n" +
               '		</div>' + "\n" +
               '	</div>' + "\n" +
               '	<div class="player-controls-center-wrapper">' + "\n" +
               '		<div class="player-controls-center">' + "\n" +
               '		<div id="musicplayer-progress-track" class="player-slider-track player-seek-bar" style="margin-top: 10px;">' + "\n" +
               '			<div id="musicplayer-progress-thumb" class="player-slider-thumb player-progress-thumb" style="left: 0.0%;"></div>' + "\n" +
               '			<div id="musicplayer-progress-bar" class="player-slider-bar player-progress-bar" style="width: 0.0%;"></div>' + "\n" +
               '			<div class="player-buffer-bar" style="left: 0px; width: 100%;"></div>' + "\n" +
               '		</div>' + "\n" +
               '		</div>' + "\n" +
               '	</div>' + "\n" +
               '</div>';
    
    javen_element.addClass( 'show-music-player' );
    player_element.html( item );
    
    musicprogress.init( section, media, durationLen );
    musicvolume.init( media, volumePx );
    
    if (media) media.play();
  },
  get_volumepx: function( media)
  {
    var volume = 0;
    if (media) {
      if (musicvolume.volume > 0)
        media.volume = musicvolume.volume;
      volume = media.volume;
    }
    
    if (volume == null || volume == Infinity || volume < 0)
      volume = 0;
    
    var px = volume * 100;
    px = parseInt( px || 0, 10 );
    
    return px;
  },
  readableSeconds: function( seconds )
  {
    if (seconds == null || seconds < 0) 
      seconds = 0;
    
    seconds = parseInt( seconds || 0, 10 );
    var minutes = Math.floor( seconds / 60 );
    var hours = Math.floor( minutes / 60 );

    var text = [];
    if (hours > 0)
    {
      text.push( hours + ':' );
      seconds -= hours * 60 * 60;
      minutes -= hours * 60;
    }

    if (minutes >= 0)
    {
      text.push( minutes + ':' );
      seconds -= minutes * 60;
    }

    if (seconds >= 0)
    {
      var chr = '';
      if (seconds < 10) chr = '0';
      text.push( chr + seconds );
    }

    return text.join( ' ' );
  },
  init_dialog: function( dialog_element ) 
  {
  }
};