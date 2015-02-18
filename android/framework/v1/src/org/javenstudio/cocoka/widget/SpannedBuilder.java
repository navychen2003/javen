package org.javenstudio.cocoka.widget;

import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ImageSpan;

public class SpannedBuilder {

	public static final String SPAN_OBJCHAR = "\uFFFC"; 
	
	private final SpannedEditor mEditor; 
	
	SpannedBuilder(SpannedEditor editor) {
		this.mEditor = editor; 
	}
	
	private Editable getEditable() {
		return mEditor.getText(); 
	}
	
	public String getText() {
		return getEditable().toString(); 
	}
	
	public void clear() {
		mEditor.setText(""); 
	}
	
	public void appendText(String text) {
		if (text != null) 
			getEditable().append(text); 
	}
	
	public void appendText(String text, Drawable d) {
		if (d == null) {
			appendText(text); 
			return; 
		}
		
		appendImageSpan(d, text, text); 
	}
	
	public void appendImage(Drawable d) {
		appendImage(d, null); 
	}
	
	public void appendImage(Drawable d, String src) {
		appendImageSpan(d, src, null); 
	}
	
	private void appendImageSpan(Drawable d, String src, String text) {
		if (d == null) return; 
		
		Editable builder = getEditable(); 
		if (builder != null) {
			int len = builder.length();
			
			if (text != null && text.length() > 0) 
				builder.append(text); 
			else 
				builder.append(SPAN_OBJCHAR);
			
			setImageSpan(builder, newImageSpan(d, src), len, builder.length()); 
		}
	}
	
	protected ImageSpan newImageSpan(Drawable d, String source) { 
		return new ImageSpan(d, source, ImageSpan.ALIGN_BOTTOM); 
	}
	
	protected void setImageSpan(Spannable spannable, ImageSpan span, int start, int end) {
		spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
	}
	
	public String toString() {
		return getEditable().toString(); 
	}
	
}
