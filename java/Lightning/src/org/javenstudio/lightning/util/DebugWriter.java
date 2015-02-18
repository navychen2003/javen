package org.javenstudio.lightning.util;

import java.io.IOException;
import java.io.Writer;

import org.javenstudio.common.util.Logger;

public class DebugWriter extends Writer {
	private static final Logger LOG = Logger.getLogger(DebugWriter.class);

	private final Writer mWriter;
	
	public DebugWriter(Writer writer) { 
		mWriter = writer;
	}
	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug(">>>>> " + new String(cbuf, off, len));
		
		mWriter.write(cbuf, off, len);
	}

	@Override
	public void flush() throws IOException {
		mWriter.flush();
	}

	@Override
	public void close() throws IOException {
		mWriter.close();
	}

}
