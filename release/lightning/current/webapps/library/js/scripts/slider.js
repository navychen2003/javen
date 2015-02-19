
var photoslide = {
  clientWidth: 0,
  clientHeight: 0,
  slide_photos: [],
  slide_next: 0,
  islistsection: false,
  photo: null,
  timer: null,
  
  show: function( photos, idx, islist )
  {
    var popover_element = $( '#content-popover' );
    if (photos == null) photos = [];
    if (photos == null || photos.length == 0)
      return;
    
    if (idx == null || idx < 0) idx = 0;
    if (islist == null) islist = false;
    
    photoslide.init0( popover_element, photos, islist, 
      function() {
        popover.show( idx );
      });
  },
  init: function( popover_element, islist ) 
  {
    this.init0( popover_element, [], islist, null );
  },
  init0: function( popover_element, photos, islist, callback ) 
  {
    this.slide_photos = photos;
    this.islistsection = islist;
    
    $.get
    (
      'tpl/photoslide.html',
      function( template )
      {
        var popover_html = template;
        var popover_photos = photoslide.slide_photos;
        
        popover.init
        ( 
          popover_element, 
          popover_html, 
          popover_photos, 
          function( photos, photoidx ) 
          {
            $( '#dismiss-button' )
              .attr( 'onclick', 'javascript:popover.hide();return false;' );
            
            photoslide.stop();
            photoslide.showphoto( photoidx );
            
            //window.onresize = adjustImageSize;
            photoslide.adjustImageSize();
          },
          function() 
          {
            photoslide.stop();
            window.onresize = null;
          }
        );
        
        if (callback) callback.call(photoslide);
      }
    );
    
    window.addEventListener('resize', function() {
        var width = document.body.clientWidth;
        var height = document.body.clientHeight;
        
        if (photoslide.clientWidth != width || photoslide.clientHeight != height) {
          photoslide.clientWidth = width;
          photoslide.clientHeight = height;
          
          photoslide.adjustImageSize();
          
          if (listsection && photoslide.islistsection && listsection.is_shown()) {
            var sectionscroll_element = $( '#section-scroll' );
            if (sectionscroll_element)
              listsection.scroll_more( sectionscroll_element );
          }
        }
      });
  },
  adjustImageSize: function()
  {
    var image_element = $( '#currentphoto-image' );
    var slide_element = $( '#slidephoto-image' );
  
    var maxWidth = parseInt(document.body.clientWidth) - 200;
    var maxHeight = parseInt(document.body.clientHeight) - 150;
  
    var imageStyle = 'max-width: ' + maxWidth + 'px; max-height: ' + maxHeight + 'px;';
  
    if (image_element)
      image_element.attr( 'style', imageStyle.esc() );
  
    if (slide_element)
      slide_element.attr( 'style', imageStyle.esc() );
  },
  startTimer: function( timeout )
  {
    this.stopTimer();
    
    this.timer = $.timer(timeout, function() {
        photoslide.shownext();
      });
  },
  stopTimer: function()
  {
    if (this.timer)
      this.timer.stop();
  },
  start: function() 
  {
    //this.slide_next = photoidx;
    this.startTimer( 5000 );
    
    var slideplay_element = $( '#slideplay-button' );
    var slidepause_element = $( '#slidepause-button' );
    
    slideplay_element
      .addClass( 'hide' );
    
    slidepause_element
      .removeClass( 'hide' )
      .attr( 'onclick', 'javascript:photoslide.stop();return false;');
  },
  stop: function()
  {
    this.stopTimer();
    
    var slideplay_element = $( '#slideplay-button' );
    var slidepause_element = $( '#slidepause-button' );
    
    slideplay_element
      .removeClass( 'hide' );
    
    slidepause_element
      .addClass( 'hide' );
  },
  shownext: function() 
  {
    if (this.slide_next >= 0) { 
      this.showphoto( this.slide_next );
      return;
    }
    this.stop();
  },
  clickphoto : function( photoidx )
  {
    this.stop();
    this.showphoto( photoidx );
  },
  showphoto : function( photoidx )
  {
    this.photo = null;
    
    if (this.slide_photos == null || this.slide_photos.length == 0)
      return;
    
    var prevbutton_element = $( '#prev-button' );
    var nextbutton_element = $( '#next-button' );
    var slideplay_element = $( '#slideplay-button' );
    var slidepause_element = $( '#slidepause-button' );
    var photoedit_element = $( '#photoedit-button' );
    var photoshare_element = $( '#photoshare-button' );
    var photoinfo_element = $( '#photoinfo-button' );
    
    var currentimage_element = $( '#currentphoto-image' );
    var currentname_element = $( '#currentphoto-name' );
    var currentindex_element = $( '#currentphoto-index' );
    
    if (!currentimage_element) { 
      this.stop();
      return;
    }
    
    var fetchfrom = 0;
    
    if (listsection && photoslide.islistsection && listsection.is_shown()) {
      fetchfrom = listsection.getmorefrom();
      if (fetchfrom > 0 && photoidx >= this.slide_photos.length) {
        listsection.showmorecb( fetchfrom, function() { 
            photoslide.showphoto( photoidx );
          });
        return;
      }
    }
    
    var currentphoto = null;
    if (photoidx >= 0 && photoidx < this.slide_photos.length)
      currentphoto = this.slide_photos[ photoidx ];
    
    this.photo = currentphoto;
    
    if (currentphoto) {
      var photoid = currentphoto['id'];
      var photoname = currentphoto['name'];
      var contentType = currentphoto['type'];
      var extension = currentphoto['extname'];
      var owner = currentphoto['owner'];
      
      var maxWidth = parseInt(document.body.clientWidth);
      var maxHeight = parseInt(document.body.clientHeight);
      
      var reqsize = '';
      if (maxWidth > 4000 || maxHeight > 2000)
        reqsize = '_4096';
      else if (maxWidth > 1600 || maxHeight > 1000)
        reqsize = '_2048';
      
      var imagesrc = app.base_path + '/image/' + photoid + reqsize + '.' + extension + '?token=' + app.token;
      var indextxt = '' + (photoidx + 1) + ' / ' + this.slide_photos.length;
      
      currentimage_element.attr( 'src', imagesrc.esc() );
      currentname_element.html( photoname.esc() );
      currentindex_element.html( indextxt.esc() );
    
      slideplay_element
        .attr( 'onclick', 'javascript:photoslide.start();return false;' );
      
      prevbutton_element
        .attr( 'onclick', 'javascript:photoslide.clickphoto(' + (photoidx-1) + ');return false;' )
        .removeClass( 'hide' );
    
      nextbutton_element
        .attr( 'onclick', 'javascript:photoslide.clickphoto(' + (photoidx+1) + ');return false;' )
        .removeClass( 'hide' );
      
      photoedit_element
        .attr( 'onclick', 'javascript:photoslide.editdetails();return false;' );
      
      photoshare_element
        .attr( 'onclick', 'javascript:photoslide.showshare();return false;' );
      
      photoinfo_element
        .attr( 'onclick', 'javascript:photoslide.showdetails();return false;' );
      
      var username = globalApp.get_username();
      if (owner != username)
        photoedit_element.addClass( 'hide' );
      
      this.slide_next = photoidx + 1;
      
    } else {
      photoedit_element.addClass( 'hide' );
      photoshare_element.addClass( 'hide' );
      photoinfo_element.addClass( 'hide' );
    }
    
    if (listsection && photoslide.islistsection && listsection.is_shown()) 
      fetchfrom = listsection.getmorefrom();
    else
      fetchfrom = 0;
    
    if (photoidx <= 0) 
      prevbutton_element.addClass( 'hide' );
      
    if (photoidx >= this.slide_photos.length - 1 && fetchfrom <= 0) {
      nextbutton_element.addClass( 'hide' );
      this.slide_next = -1;
    }
    
    if (this.slide_photos.length <= 1)
      slideplay_element.addClass( 'hide' );
  },
  showshare: function()
  {
    var currentphoto = this.photo;
    if (currentphoto == null)
      return;
    
    var items = [];
    items.push( currentphoto );
    
    compose.share( items );
  },
  showdetails: function()
  {
    var currentphoto = this.photo;
    if (currentphoto == null)
      return;
    
    fileinfo.showdetails( currentphoto );
  },
  editdetails: function()
  {
    var currentphoto = this.photo;
    if (currentphoto == null)
      return;
    
    var photoid = currentphoto['id'];
    if (photoid == null || photoid.length == 0)
      return;
    
    var context = system.context;
    var openlink = '#/~edit/' + photoid;
    context.redirect( openlink );
  }
};

var photoviewer = {
  clientWidth: 0,
  clientHeight: 0,
  
  init: function( popover_element, popover_photos ) 
  { 
    $.get
    (
      'tpl/photoslide.html',
      function( template )
      {
        var popover_html = template;
        
        popover.init
        ( 
          popover_element, 
          popover_html, 
          popover_photos, 
          function( photos, photoidx ) 
          {
            $( '#dismiss-button' )
              .attr( 'onclick', 'javascript:popover.hide();return false;' );
            
            //window.onresize = adjustImageSize;
            photoviewer.adjustImageSize();
          },
          function() 
          {
            window.onresize = null;
          }
        );
      }
    );
    
    window.addEventListener('resize', function() {
        var width = document.body.clientWidth;
        var height = document.body.clientHeight;
        
        if (photoviewer.clientWidth != width || photoviewer.clientHeight != height) {
          photoviewer.clientWidth = width;
          photoviewer.clientHeight = height;
          
          photoviewer.adjustImageSize();
        }
      });
  },
  adjustImageSize: function()
  {
    var image_element = $( '#currentphoto-image' );
    var slide_element = $( '#slidephoto-image' );
  
    var maxWidth = parseInt(document.body.clientWidth) - 200;
    var maxHeight = parseInt(document.body.clientHeight) - 150;
  
    var imageStyle = 'max-width: ' + maxWidth + 'px; max-height: ' + maxHeight + 'px;';
  
    if (image_element)
      image_element.attr( 'style', imageStyle.esc() );
  
    if (slide_element)
      slide_element.attr( 'style', imageStyle.esc() );
  }
};
