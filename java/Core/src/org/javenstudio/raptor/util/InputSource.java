package org.javenstudio.raptor.util;

import java.io.IOException;
import java.io.InputStream;

public interface InputSource {

	public InputStream openStream() throws IOException;
	
}
