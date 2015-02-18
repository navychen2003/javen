package org.javenstudio.lightning.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public class SimpleHttpResult implements IHttpResult {
	private static final Logger LOG = Logger.getLogger(SimpleHttpResult.class);

	static class SimpleHeader implements IHeader {
		private final Header mHeader;
		
		public SimpleHeader(Header header) {
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
	private final URI mURI;
	
	public SimpleHttpResult(URI uri, HttpResponse response, HttpEntity entity) {
		if (uri == null || response == null || entity == null) 
			throw new NullPointerException();
		mEntity = entity;
		mResponse = response;
		mURI = uri;
	}

	@Override
	public URI getURL() {
		return mURI;
	}
	
	@Override
	public long getContentLength() {
		return mEntity.getContentLength();
	}

	@Override
	public String getContentType() {
		Header header = mEntity.getContentType();
		return header != null ? header.getValue() : null;
	}

	@Override
	public String getContentEncoding() {
		Header header = mEntity.getContentEncoding();
		return header != null ? header.getValue() : null;
	}
	
	@Override
	public IHeader[] getHeaders(String name) {
		Header[] headers = mResponse.getHeaders(name);
		return toHeaders(headers);
	}
	
	@Override
	public IHeader[] getAllHeaders() {
		Header[] headers = mResponse.getAllHeaders();
		return toHeaders(headers);
	}
	
	private static IHeader[] toHeaders(Header[] headers) {
		if (headers == null || headers.length == 0)
			return null;
		
		if (headers.length == 1) {
			Header header = headers[0];
			if (header != null)
				return new IHeader[]{ new SimpleHeader(header) };
			
			return null;
		}
		
		ArrayList<IHeader> list = new ArrayList<IHeader>();
		for (int i = 0; i < headers.length; i++) {
			Header header = headers[i];
			if (header != null)
				list.add(new SimpleHeader(header));
		}
		
		return list.toArray(new IHeader[list.size()]);
	}
	
	@Override
	public String getContentAsString() throws ErrorException {
		try {
			return toString(mEntity); 
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	@Override
	public byte[] getContentAsBinary() throws ErrorException {
		try {
			return toByteArray(mEntity); 
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	@Override
	public InputStream getContentAsStream() throws ErrorException {
		try {
			return mEntity.getContent();
		} catch (IllegalStateException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{url=" + mURI 
				+ ",contentType=" + getContentType() 
				+ ",contentEncoding=" + getContentEncoding() 
				+ ",contentLength=" + getContentLength() 
				+ "}";
	}
	
	public static String toString(final HttpEntity entity) 
			throws IOException, ParseException {
		return toString(entity, null);
	}
	
	public static String toString(final HttpEntity entity, final Charset defaultCharset) 
			throws IOException, ParseException {
        Args.notNull(entity, "Entity");
        final InputStream instream = entity.getContent();
        if (instream == null) {
            return null;
        }
        try {
            Args.check(entity.getContentLength() <= Integer.MAX_VALUE,
                    "HTTP entity too large to be buffered in memory");
            int i = (int)entity.getContentLength();
            if (i < 0) {
                i = 4096;
            }
            Charset charset = null;
            try {
                final ContentType contentType = ContentType.get(entity);
                if (contentType != null) {
                    charset = contentType.getCharset();
                }
            } catch (final UnsupportedCharsetException ex) {
                throw new UnsupportedEncodingException(ex.getMessage());
            }
            if (charset == null) {
                charset = defaultCharset;
            }
            if (charset == null) {
                charset = HTTP.DEF_CONTENT_CHARSET;
            }
            final Reader reader = new InputStreamReader(instream, charset);
            final CharArrayBuffer buffer = new CharArrayBuffer(i);
            final char[] tmp = new char[1024];
            int l;
            try {
	            while((l = reader.read(tmp)) != -1) {
	                buffer.append(tmp, 0, l);
	            }
            } catch (ConnectionClosedException e) {
            	if (LOG.isWarnEnabled())
            		LOG.warn("toString: error: " + e, e);
            }
            return buffer.toString();
        } finally {
            instream.close();
        }
    }
	
	public static byte[] toByteArray(final HttpEntity entity) throws IOException {
        Args.notNull(entity, "Entity");
        final InputStream instream = entity.getContent();
        if (instream == null) {
            return null;
        }
        try {
            Args.check(entity.getContentLength() <= Integer.MAX_VALUE,
                    "HTTP entity too large to be buffered in memory");
            int i = (int)entity.getContentLength();
            if (i < 0) {
                i = 4096;
            }
            final ByteArrayBuffer buffer = new ByteArrayBuffer(i);
            final byte[] tmp = new byte[4096];
            int l;
            try {
	            while((l = instream.read(tmp)) != -1) {
	                buffer.append(tmp, 0, l);
	            }
            } catch (ConnectionClosedException e) {
            	if (LOG.isWarnEnabled())
            		LOG.warn("toString: error: " + e, e);
            }
            return buffer.toByteArray();
        } finally {
            instream.close();
        }
    }
	
}
