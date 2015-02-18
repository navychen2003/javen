package org.javenstudio.cocoka.graphics;

import android.graphics.drawable.Drawable;

public class FadeMaskDrawable extends BubbleTextDrawable {

	public FadeMaskDrawable(Drawable d) {
		super(d); 
	}
	
    public FadeMaskDrawable(Drawable d, String text) {
    	super(d, text); 
    }
    
}
