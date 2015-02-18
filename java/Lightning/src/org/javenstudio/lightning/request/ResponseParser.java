package org.javenstudio.lightning.request;

import java.io.InputStream;
import java.io.Reader;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public abstract class ResponseParser {

	public abstract String getWriterType(); // for example: wt=XML, JSON, etc

	public abstract NamedList<Object> processResponse(InputStream body, 
			String encoding) throws ErrorException;
	
	public abstract NamedList<Object> processResponse(Reader reader) 
			throws ErrorException;
  
	/**
	 * @return the version param passed to server
	 */
	public String getVersion() {
		return "2.2";
	}
	
}
