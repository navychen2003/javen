package org.javenstudio.cocoka.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

public class SimpleHttpResult implements HttpResult {

	static class SimpleHeader implements Header {
		private final org.apache.http.Header mHeader;
		
		public SimpleHeader(org.apache.http.Header header) {
			if (header == null) throw new NullPointerException();
			mHeader = header;
		}
		
		public String getName() { return mHeader.getName(); }
		public String getValue() { return mHeader.getValue(); }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{name=" + getName() 
					+ ",value=" + getValue() + "}";
		}
	}
	
	private final HttpEntity mEntity;
	private final HttpResponse mResponse;
	
	public SimpleHttpResult(HttpResponse response) {
		this(response, response != null ? response.getEntity() : null);
	}
	
	public SimpleHttpResult(HttpResponse response, HttpEntity entity) {
		if (response == null || entity == null) throw new NullPointerException();
		mEntity = entity;
		mResponse = response;
	}

	@Override
	public long getContentLength() {
		return mEntity.getContentLength();
	}

	@Override
	public String getContentType() {
		org.apache.http.Header header = mEntity.getContentType();
		return header != null ? header.getValue() : null;
	}

	@Override
	public String getContentEncoding() {
		org.apache.http.Header header = mEntity.getContentEncoding();
		return header != null ? header.getValue() : null;
	}
	
	@Override
	public Header[] getHeaders(String name) {
		org.apache.http.Header[] headers = mResponse.getHeaders(name);
		return toHeaders(headers);
	}
	
	@Override
	public Header[] getAllHeaders() {
		org.apache.http.Header[] headers = mResponse.getAllHeaders();
		return toHeaders(headers);
	}
	
	private static Header[] toHeaders(org.apache.http.Header[] headers) {
		if (headers == null || headers.length == 0)
			return null;
		
		if (headers.length == 1) {
			org.apache.http.Header header = headers[0];
			if (header != null)
				return new Header[]{ new SimpleHeader(header) };
			
			return null;
		}
		
		ArrayList<Header> list = new ArrayList<Header>();
		for (int i = 0; i < headers.length; i++) {
			org.apache.http.Header header = headers[i];
			if (header != null)
				list.add(new SimpleHeader(header));
		}
		
		return list.toArray(new Header[list.size()]);
	}
	
	@Override
	public String getContentAsString() throws IOException {
		try {
			return EntityUtils.toString(mEntity); 
		} catch (ParseException e) {
			throw new IOException(e.toString(), e);
		}
	}

	@Override
	public byte[] getContentAsBinary() throws IOException {
		try {
			return EntityUtils.toByteArray(mEntity); 
		} catch (ParseException e) {
			throw new IOException(e.toString(), e);
		}
	}

	@Override
	public InputStream getContentAsStream() throws IOException {
		try {
			return mEntity.getContent();
		} catch (IllegalStateException e) {
			throw new IOException(e.toString(), e);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() 
				+ "{contentType=" + getContentType() 
				+ ",contentEncoding=" + getContentEncoding() 
				+ ",contentLength=" + getContentLength() 
				+ "}";
	}
	
}
