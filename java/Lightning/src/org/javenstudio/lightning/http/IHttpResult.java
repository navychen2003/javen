package org.javenstudio.lightning.http;

import java.io.InputStream;
import java.net.URI;

import org.javenstudio.falcon.ErrorException;

public interface IHttpResult {
	
	public static interface IHeader {
		public String getName();
		public String getValue();
	}

	public URI getURL();
	public long getContentLength();
	public String getContentType();
	public String getContentEncoding();
	
	public IHeader[] getHeaders(String name);
	public IHeader[] getAllHeaders();
	
	public String getContentAsString() throws ErrorException;
	public byte[] getContentAsBinary() throws ErrorException;
	public InputStream getContentAsStream() throws ErrorException;
	
}
