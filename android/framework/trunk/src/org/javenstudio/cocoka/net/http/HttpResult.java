package org.javenstudio.cocoka.net.http;

import java.io.IOException;
import java.io.InputStream;

public interface HttpResult {

	public static interface Header {
		public String getName();
		public String getValue();
	}

	public long getContentLength();
	public String getContentType();
	public String getContentEncoding();
	
	public Header[] getHeaders(String name);
	public Header[] getAllHeaders();
	
	public String getContentAsString() throws IOException;
	public byte[] getContentAsBinary() throws IOException;
	public InputStream getContentAsStream() throws IOException;
	
}
