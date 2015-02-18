package org.javenstudio.falcon.datum;

import java.io.IOException;
import java.io.InputStream;

public interface IScreenshot {

	public String getImageName();
	public String getImageType();
	
	public int getImageWidth();
	public int getImageHeight();
	
	public byte[] getImageBuffer();
	public InputStream openImage() throws IOException;
	
}
