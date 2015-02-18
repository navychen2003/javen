package org.javenstudio.cocoka.graphics;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface DelegatedBitmap {

	public int getWidth(); 
	public int getHeight(); 
	public void draw(Canvas canvas, RectF bounds); 
	
}
