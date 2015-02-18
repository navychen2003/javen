package org.javenstudio.falcon.datum;

import java.io.IOException;
import java.io.InputStream;

public interface IImageSource {

	public static interface Param { 
		public int getParamWidth();
		public int getParamHeight();
		public boolean getParamTrim();
	}
	
	public static interface Bitmap { 
		public String getMimeType();
		public int getWidth();
		public int getHeight();
		public InputStream openBitmap() throws IOException;
	}
	
	public Bitmap getBitmap(Param param) throws IOException;
	
}
