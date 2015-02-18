package org.javenstudio.mail.internet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlHelper {

	// Regex that matches characters that have special meaning in HTML. '<', '>', '&' and
    // multiple continuous spaces.
    private static final Pattern PLAIN_TEXT_TO_ESCAPE = Pattern.compile("[<>&]| {2,}|\r?\n");
	
	/**
     * Escape some special character as HTML escape sequence.
     * 
     * @param text Text to be displayed using WebView.
     * @return Text correctly escaped.
     */
    public static String escapeCharacterToDisplay(String text) {
        Pattern pattern = PLAIN_TEXT_TO_ESCAPE;
        Matcher match = pattern.matcher(text);
        
        if (match.find()) {
            StringBuilder out = new StringBuilder();
            int end = 0;
            do {
                int start = match.start();
                out.append(text.substring(end, start));
                end = match.end();
                int c = text.codePointAt(start);
                if (c == ' ') {
                    // Escape successive spaces into series of "&nbsp;".
                    for (int i = 1, n = end - start; i < n; ++i) {
                        out.append("&nbsp;");
                    }
                    out.append(' ');
                } else if (c == '\r' || c == '\n') {
                    out.append("<br>");
                } else if (c == '<') {
                    out.append("&lt;");
                } else if (c == '>') {
                    out.append("&gt;");
                } else if (c == '&') {
                    out.append("&amp;");
                }
            } while (match.find());
            out.append(text.substring(end));
            text = out.toString();
        }        
        return text;
    }
    
}
