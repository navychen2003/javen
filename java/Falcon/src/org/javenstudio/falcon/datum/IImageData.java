package org.javenstudio.falcon.datum;

import java.io.IOException;
import java.io.InputStream;

public interface IImageData extends IData, IFileData, IImageSource {

	public InputStream openImage() throws IOException;
	
}
