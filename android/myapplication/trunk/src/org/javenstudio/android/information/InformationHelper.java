package org.javenstudio.android.information;

import android.text.Editable;
import android.text.Spanned;

import org.javenstudio.cocoka.text.Html;
import org.javenstudio.cocoka.text.HtmlToSpannedConverter;
import org.javenstudio.common.parser.util.ParseUtils;

public class InformationHelper {

	private static class TitleSpannedConverter extends HtmlToSpannedConverter { 
		public TitleSpannedConverter() { 
			super(null, null); 
		}
		
		@Override 
		protected void handleP(Editable text, boolean opening, String tag) {
			// ignore
		}
		
		@Override 
		protected void handleBr(Editable text, boolean opening, String tag) {
			// ignore
		}
	}
	
	private static class SummarySpannedConverter extends HtmlToSpannedConverter { 
		public SummarySpannedConverter() { 
			super(null, null); 
			
			getSpannableBuilder().append("\u3000\u3000"); 
		}
		
		@Override 
		protected void handleP(Editable text, boolean opening, String tag) {
			// ignore
		}
		
		@Override 
		protected void handleBr(Editable text, boolean opening, String tag) {
			// ignore
		}
	}
	
	private static class ContentSpannedConverter extends HtmlToSpannedConverter { 
		private boolean mBreakLine; 
		private boolean mIgnoreText;
		
		public ContentSpannedConverter() { 
			super(null, null); 
			
			mBreakLine = true; // first line head
			mIgnoreText = false;
		}
		
		@Override 
		protected void handleP(Editable text, boolean opening, String tag) {
			handleBr(text, opening, tag); 
		}
		
		@Override 
		protected void handleBr(Editable text, boolean opening, String tag) {
			boolean foundText = false; 
			
			int len = text.length(); 
			while ((--len) >= 0) {
				char chr = text.charAt(len); 
				if (chr == '\n') return; 
				if (chr == ' ' || chr == '\r' || chr == '\t' || 
					chr == '\u3000' || chr == '\u00A0' || (chr >= '\u2000' && chr <= '\u200A')) 
					continue; 
				
				foundText = true; 
				break; 
			}
			
			if (foundText) 
				mBreakLine = true; 
		}
		
		@Override 
		protected void startHeader(Editable text, String tag) { 
			// ignore
		}
		
		@Override 
		protected void endHeader(Editable text) {
			// ignore
		}
		
		@Override 
		protected void startScript(Editable text, String tag) { 
			mIgnoreText = true;
		}
		
		@Override 
		protected void endScript(Editable text) {
			mIgnoreText = false;
		}
		
		@Override 
		protected void startStyle(Editable text, String tag) { 
			mIgnoreText = true;
		}
		
		@Override 
		protected void endStyle(Editable text) {
			mIgnoreText = false;
		}
		
		@Override 
		public void append(CharSequence text) { 
			if (text == null || mIgnoreText) 
				return; 
			
			int len = getSpannableBuilder().length(); 
			char lastchar = len > 0 ? getSpannableBuilder().charAt(len - 1) : 0; 
			
			StringBuilder sbuf = new StringBuilder(); 
			char pchr = lastchar; 
			for (int i=0; i < text.length(); i++) { 
				char chr = text.charAt(i); 
				if (chr == '\r' || chr == '\n') continue; 
				if (chr == '\t' || chr == '\u3000' || chr == '\u00A0' || 
					(chr >= '\u2000' && chr <= '\u200A')) { 
					chr = ' '; 
					if (pchr == chr) continue; 
					if (sbuf.length() == 0) continue; 
				}
				sbuf.append(chr); 
				pchr = chr; 
			}
			
			String str = sbuf.toString(); 
			
			if (str != null && str.length() > 0) { 
				if (mBreakLine) { 
					str = ParseUtils.trim(str); 
					if (str != null && str.length() > 0) { 
						mBreakLine = false; 
						if (getSpannableBuilder().length() > 0) 
							getSpannableBuilder().append("\n\n"); 
						getSpannableBuilder().append("\u3000\u3000"); 
					}
				}
				
				if (str != null && str.length() > 0)
					getSpannableBuilder().append(str); 
			}
		}
	}
	
	public static Spanned formatTitleSpanned(String text) { 
		if (text == null) text = ""; 
		text = ParseUtils.trim(ParseUtils.removeWhiteSpaces(text)); 
		return Html.fromHtml(text, new TitleSpannedConverter()); 
	}
	
	public static Spanned formatSummarySpanned(String text) { 
		if (text == null) text = ""; 
		text = ParseUtils.trim(ParseUtils.removeWhiteSpaces(text)); 
		return Html.fromHtml(text, new SummarySpannedConverter()); 
	}
	
	public static Spanned formatContentSpanned(String text) { 
		if (text == null) text = ""; 
		text = ParseUtils.trim(ParseUtils.removeWhiteSpaces(text)); 
		return Html.fromHtml(text, new ContentSpannedConverter()); 
	}
	
}
