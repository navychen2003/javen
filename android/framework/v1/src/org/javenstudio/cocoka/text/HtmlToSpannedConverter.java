package org.javenstudio.cocoka.text;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

import org.javenstudio.common.parser.util.XmlUtils;

public class HtmlToSpannedConverter implements Html.Converter {

    private static final float[] HEADER_SIZES = {
        1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
    };

    private Editable mSpannableBuilder;
    private Html.ImageGetter mImageGetter;
    private Html.HtmlHandler mTagHandler;

    public HtmlToSpannedConverter(Html.ImageGetter imageGetter, Html.HtmlHandler tagHandler) {
    	mSpannableBuilder = createSpannableBuilder();
        mImageGetter = imageGetter;
        mTagHandler = tagHandler;
    }

    protected Editable createSpannableBuilder() { 
    	return new SpannableStringBuilder(); 
    }
    
    protected final Editable getSpannableBuilder() { 
    	return mSpannableBuilder;
    }
    
    public final Spanned convert() {
    	final Editable builder = mSpannableBuilder; 
    	final int length = builder.length(); 
    	
        // Fix flags and range for paragraph-type markup.
        Object[] obj = builder.getSpans(0, builder.length(), ParagraphStyle.class);
        
        for (int i = 0; i < obj.length; i++) {
            int start = builder.getSpanStart(obj[i]);
            int end = builder.getSpanEnd(obj[i]);

            // If the last line of the range is blank, back off by one.
            if (end - 2 >= 0) {
                if (builder.charAt(end - 1) == '\n' && builder.charAt(end - 2) == '\n') {
                    end--;
                }
            }

            if (end == start) {
            	builder.removeSpan(obj[i]);
            	
            } else {
            	boolean removeParagraphSpan = false; 
            	
            	if (start != 0 && start != length) {
                    char c = charAt(start - 1);
                    
                    if (c != '\n') 
                    	removeParagraphSpan = true; 
            	}
            	
            	if (end != 0 && end != length()) {
                    char c = charAt(end - 1);

                    if (c != '\n') 
                    	removeParagraphSpan = true; 
            	}
            	
            	if (removeParagraphSpan) 
            		builder.removeSpan(obj[i]); 
            	else 
            		builder.setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH);
            }
        }

        return builder;
    }

    @Override 
    public final void handleStartTag(String tag, String qName, Attributes attributes) {
        if (tag.equalsIgnoreCase("br")) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emite the linebreaks when we handle the close tag.
        } else if (tag.equalsIgnoreCase("p")) {
            handleP(getSpannableBuilder(), true, tag);
        } else if (tag.equalsIgnoreCase("div")) {
            handleP(getSpannableBuilder(), true, tag);
        } else if (tag.equalsIgnoreCase("em")) {
            start(getSpannableBuilder(), new Bold());
        } else if (tag.equalsIgnoreCase("b")) {
            start(getSpannableBuilder(), new Bold());
        } else if (tag.equalsIgnoreCase("strong")) {
            start(getSpannableBuilder(), new Italic());
        } else if (tag.equalsIgnoreCase("cite")) {
            start(getSpannableBuilder(), new Italic());
        } else if (tag.equalsIgnoreCase("dfn")) {
            start(getSpannableBuilder(), new Italic());
        } else if (tag.equalsIgnoreCase("i")) {
            start(getSpannableBuilder(), new Italic());
        } else if (tag.equalsIgnoreCase("big")) {
            start(getSpannableBuilder(), new Big());
        } else if (tag.equalsIgnoreCase("small")) {
            start(getSpannableBuilder(), new Small());
        } else if (tag.equalsIgnoreCase("font")) {
            startFont(getSpannableBuilder(), attributes);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            handleP(getSpannableBuilder(), true, tag);
            start(getSpannableBuilder(), new Blockquote());
        } else if (tag.equalsIgnoreCase("tt")) {
            start(getSpannableBuilder(), new Monospace());
        } else if (tag.equalsIgnoreCase("a")) {
            startA(getSpannableBuilder(), attributes);
        } else if (tag.equalsIgnoreCase("u")) {
            start(getSpannableBuilder(), new Underline());
        } else if (tag.equalsIgnoreCase("sup")) {
            start(getSpannableBuilder(), new Super());
        } else if (tag.equalsIgnoreCase("sub")) {
            start(getSpannableBuilder(), new Sub());
        } else if (tag.equalsIgnoreCase("script")) {
        	startScript(getSpannableBuilder(), tag);
        } else if (tag.equalsIgnoreCase("style")) {
        	startStyle(getSpannableBuilder(), tag);
        } else if (tag.length() == 2 &&
                   Character.toLowerCase(tag.charAt(0)) == 'h' &&
                   tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            handleP(getSpannableBuilder(), true, tag); 
            startHeader(getSpannableBuilder(), tag); 
        } else if (tag.equalsIgnoreCase("img")) {
            startImg(getSpannableBuilder(), attributes, mImageGetter);
        } else if (mTagHandler != null) {
            mTagHandler.handleTag(true, tag, this);
        }
    }

    @Override 
    public final void handleEndTag(String tag, String qName) {
        if (tag.equalsIgnoreCase("br")) {
            handleBr(getSpannableBuilder(), false, tag);
        } else if (tag.equalsIgnoreCase("p")) {
            handleP(getSpannableBuilder(), false, tag);
        } else if (tag.equalsIgnoreCase("div")) {
            handleP(getSpannableBuilder(), false, tag);
        } else if (tag.equalsIgnoreCase("em")) {
            end(getSpannableBuilder(), Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("b")) {
            end(getSpannableBuilder(), Bold.class, new StyleSpan(Typeface.BOLD));
        } else if (tag.equalsIgnoreCase("strong")) {
            end(getSpannableBuilder(), Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("cite")) {
            end(getSpannableBuilder(), Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("dfn")) {
            end(getSpannableBuilder(), Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("i")) {
            end(getSpannableBuilder(), Italic.class, new StyleSpan(Typeface.ITALIC));
        } else if (tag.equalsIgnoreCase("big")) {
            end(getSpannableBuilder(), Big.class, new RelativeSizeSpan(1.25f));
        } else if (tag.equalsIgnoreCase("small")) {
            end(getSpannableBuilder(), Small.class, new RelativeSizeSpan(0.8f));
        } else if (tag.equalsIgnoreCase("font")) {
            endFont(getSpannableBuilder());
        } else if (tag.equalsIgnoreCase("blockquote")) {
            handleP(getSpannableBuilder(), false, tag);
            end(getSpannableBuilder(), Blockquote.class, new QuoteSpan());
        } else if (tag.equalsIgnoreCase("tt")) {
            end(getSpannableBuilder(), Monospace.class, new TypefaceSpan("monospace"));
        } else if (tag.equalsIgnoreCase("a")) {
            endA(getSpannableBuilder());
        } else if (tag.equalsIgnoreCase("u")) {
            end(getSpannableBuilder(), Underline.class, new UnderlineSpan());
        } else if (tag.equalsIgnoreCase("sup")) {
            end(getSpannableBuilder(), Super.class, new SuperscriptSpan());
        } else if (tag.equalsIgnoreCase("sub")) {
            end(getSpannableBuilder(), Sub.class, new SubscriptSpan());
        } else if (tag.equalsIgnoreCase("script")) {
        	endScript(getSpannableBuilder());
        } else if (tag.equalsIgnoreCase("style")) {
        	endStyle(getSpannableBuilder());
        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            handleP(getSpannableBuilder(), false, tag);
            endHeader(getSpannableBuilder());
        } else if (mTagHandler != null) {
            mTagHandler.handleTag(false, tag, this);
        }
    }

    protected void handleP(Editable text, boolean opening, String tag) {
        int len = text.length();

        if (len >= 1 && text.charAt(len - 1) == '\n') {
            if (len >= 2 && text.charAt(len - 2) == '\n') {
                return;
            }

            text.append("\n");
            return;
        }

        if (len != 0) {
            text.append("\n\n");
        }
    }

    protected void handleBr(Editable text, boolean opening, String tag) {
        text.append("\n");
    }

    protected static Object getLast(Spanned text, Class<?> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }

    protected void start(Editable text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
    }

    protected void end(Editable text, Class<?> kind, Object repl) {
        int len = text.length();
        Object obj = getLast(text, kind);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        where = findSpanStart(text, obj, where, len); 
        
        if (where != len && repl != null) {
            text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return;
    }

    protected int findSpanStart(Editable text, Object obj, int where, int len) { 
    	int start = where; 
    	
    	while (start < len) { 
        	char chr = text.charAt(start); 
        	if (XmlUtils.isWhiteSpace(chr)) { 
        		start ++; 
        		continue; 
        	}
        	break; 
        }
    	
    	return start; 
    }
    
    protected void startImg(Editable text, Attributes attributes, Html.ImageGetter img) {
        String src = attributes.getValue("", "src");
        Drawable d = null;

        if (img != null) {
            d = img.getDrawable(src);
        }

        if (d != null) {
	        int len = text.length();
	        text.append("\uFFFC");

	        text.setSpan(new ImageSpan(d, src), len, text.length(),
	                     Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    protected void startFont(Editable text, Attributes attributes) {
        String color = attributes.getValue("", "color");
        String face = attributes.getValue("", "face");

        int len = text.length();
        text.setSpan(new Font(color, face), len, len, Spannable.SPAN_MARK_MARK);
    }

    protected void endFont(Editable text) {
        int len = text.length();
        Object obj = getLast(text, Font.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        where = findSpanStart(text, obj, where, len); 
        
        if (where != len) {
            Font f = (Font) obj;

            if (!TextUtils.isEmpty(f.mColor)) {
                if (f.mColor.startsWith("@")) {
                    Resources res = Resources.getSystem();
                    String name = f.mColor.substring(1);
                    int colorRes = res.getIdentifier(name, "color", "android");
                    if (colorRes != 0) {
                        ColorStateList colors = res.getColorStateList(colorRes);
                        text.setSpan(new TextAppearanceSpan(null, 0, 0, colors, null),
                                where, len,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    int c = getHtmlColor(f.mColor);
                    if (c != -1) {
                        text.setSpan(new ForegroundColorSpan(c | 0xFF000000),
                                where, len,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }

            if (f.mFace != null) {
                text.setSpan(new TypefaceSpan(f.mFace), where, len,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    protected void startA(Editable text, Attributes attributes) {
        String href = attributes.getValue("", "href");

        int len = text.length();
        text.setSpan(new Href(href), len, len, Spannable.SPAN_MARK_MARK);
    }

    protected void endA(Editable text) {
        int len = text.length();
        Object obj = getLast(text, Href.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        where = findSpanStart(text, obj, where, len); 
        
        if (where != len) {
            Href h = (Href) obj;

            if (h.mHref != null) {
                text.setSpan(new URLSpan(h.mHref), where, len,
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    protected void startScript(Editable text, String tag) { 
    	start(text, new Script());
    }
    
    protected void endScript(Editable text) {
    	end(text, Script.class, null);
    }
    
    protected void startStyle(Editable text, String tag) { 
    	start(text, new Style());
    }
    
    protected void endStyle(Editable text) {
    	end(text, Style.class, null);
    }
    
    protected void startHeader(Editable text, String tag) { 
    	start(text, new Header(tag.charAt(1) - '1'));
    }
    
    protected void endHeader(Editable text) {
        int len = text.length();
        Object obj = getLast(text, Header.class);

        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        // Back off not to change only the text, not the blank line.
        while (len > where && text.charAt(len - 1) == '\n') {
            len--;
        }

        where = findSpanStart(text, obj, where, len); 
        
        if (where != len) {
            Header h = (Header) obj;

            text.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]),
                         where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new StyleSpan(Typeface.BOLD),
                         where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override 
    public void handleStartDocument() throws SAXException {} 
    
    @Override 
	public void handleEndDocument() {} 
    
    @Override 
    public int length() { return getSpannableBuilder().length(); } 
    
    @Override 
	public char charAt(int position) { return getSpannableBuilder().charAt(position); } 
    
    @Override 
	public void append(CharSequence text) { getSpannableBuilder().append(text); } 
    
    protected static class Bold { }
    protected static class Italic { }
    protected static class Underline { }
    protected static class Big { }
    protected static class Small { }
    protected static class Monospace { }
    protected static class Blockquote { }
    protected static class Super { }
    protected static class Sub { }
    protected static class Script { }
    protected static class Style { }

    protected static class Font {
        public String mColor;
        public String mFace;

        public Font(String color, String face) {
            mColor = color;
            mFace = face;
        }
    }

    protected static class Href {
        public String mHref;

        public Href(String href) {
            mHref = href;
        }
    }

    protected static class Header {
        private int mLevel;

        public Header(int level) {
            mLevel = level;
        }
    }

    private static HashMap<String,Integer> COLORS = buildColorMap();

    private static HashMap<String,Integer> buildColorMap() {
        HashMap<String,Integer> map = new HashMap<String,Integer>();
        map.put("aqua", 0x00FFFF);
        map.put("black", 0x000000);
        map.put("blue", 0x0000FF);
        map.put("fuchsia", 0xFF00FF);
        map.put("green", 0x008000);
        map.put("grey", 0x808080);
        map.put("lime", 0x00FF00);
        map.put("maroon", 0x800000);
        map.put("navy", 0x000080);
        map.put("olive", 0x808000);
        map.put("purple", 0x800080);
        map.put("red", 0xFF0000);
        map.put("silver", 0xC0C0C0);
        map.put("teal", 0x008080);
        map.put("white", 0xFFFFFF);
        map.put("yellow", 0xFFFF00);
        return map;
    }

    /**
     * Converts an HTML color (named or numeric) to an integer RGB value.
     *
     * @param color Non-null color string.
     * @return A color value, or {@code -1} if the color string could not be interpreted.
     */
    private static int getHtmlColor(String color) {
        Integer i = COLORS.get(color.toLowerCase());
        if (i != null) {
            return i;
        } else {
            try {
                return XmlUtils.convertValueToInt(color, -1);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }
    }

}