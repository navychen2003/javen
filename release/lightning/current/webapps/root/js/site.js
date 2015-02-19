
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

  $(document).ready(function() {
    $("a[data-expand-clickable]").each(function(i, el) {
      var $el = $(el);
      $el.parent().on("click", function(evt) {
        if (evt.target != el) {
          evt.preventDefault();
          $el[0].click();
        }
      });
    });
    $("div[data-video-autofit]").fitVids({customSelector: "video"});

    var $navbar = $(".so-affix");
    if ($navbar.length) {
      var top = $navbar.parents(".container").offset().top;
      var bot = 0;
      $navbar.parents(".container").nextAll().each(function(){
        bot += $(this).outerHeight();
      });
      bot = Math.floor(bot);
      $navbar.affix({
        offset: {
          top: function() {
            return top;
          },
          bottom: function() {
            return bot;
          }
        }
      });
    }
    $('a[data-smoothscroll]').click(function() {
      var target = $(this.hash);
      target = target.length ? target : $('[name=' + this.hash.slice(1) +']');
      if (target.length) {
        $('html,body').stop().animate({
          scrollTop: target.offset().top
        }, 1000);
        return false;
      }
    });
    $('.ifscript').show();
  });
