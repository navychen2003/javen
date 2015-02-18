package org.javenstudio.common.entitydb;

import java.io.IOException;

public interface IStreamStore {

	public String createFilePath(String filename) throws IOException;
	
}
