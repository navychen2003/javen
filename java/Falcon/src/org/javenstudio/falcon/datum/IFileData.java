package org.javenstudio.falcon.datum;

import java.io.IOException;
import java.io.InputStream;

public interface IFileData extends IData {

	public String getFolderId();
	public InputStream open() throws IOException;
	
}
