package org.javenstudio.cocoka.net.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import org.javenstudio.common.util.Logger;

public abstract class SimpleHttpExecutor implements HttpExecutor {
	private static Logger LOG = Logger.getLogger(SimpleHttpExecutor.class);

	public static class SimpleProgress implements HttpTask.Progress {
		private final int mContentLength; 
		private final int mFetchedLength; 
		private final boolean mAborted; 
		
		public SimpleProgress(int contentLength, int fetchedLength) {
			this(contentLength, fetchedLength, false); 
		}
		
		public SimpleProgress(int contentLength, int fetchedLength, boolean aborted) {
			mContentLength = contentLength; 
			mFetchedLength = fetchedLength; 
			mAborted = aborted; 
		}
		
		public boolean isAborted() { 
			return mAborted; 
		}
		
		public int getContentLength() {
			return mContentLength; 
		}
		
		public int getFetchedLength() {
			return mFetchedLength; 
		}
		
		public float getProgress() {
			float finishedLen = (float)getFetchedLength(); 
			float totalLen = (float)getContentLength(); 
			if (totalLen > 0 && finishedLen >= 0) {
				return finishedLen/totalLen; 
			}
			return 0.0f; 
		}
	}
	
	public static HttpExecutorFactory getHtmlFactory() {
		return new HttpExecutorFactory() {
			public HttpExecutor createExecutor() {
				return new StringHttpExecutor(null); 
			}
		};
	}
	
	public static HttpExecutorFactory getBytesFactory() {
		return new HttpExecutorFactory() {
			public HttpExecutor createExecutor() {
				return new BytesHttpExecutor(null); 
			}
		};
	}
	
	private final ResponseChecker mResponseChecker; 
	
	public SimpleHttpExecutor(ResponseChecker checker) { 
		mResponseChecker = checker; 
	}
	
	@Override
	public HttpTask.Result execute(HttpClient client, HttpUriRequest request, HttpTask.Publisher publisher) {
		if (client == null || request == null) 
			return null; 
		
		try {
			int redirectCount = 0; 
			while (true) { 
				HttpException checkException = checkRequestBeforeExecute(client, request); 
				if (checkException != null) 
					return checkException; 
				
				HttpResponse response = client.execute(request); 
				if (checkResponseSuccess(response)) {
					return readEntity(response.getEntity(), publisher); 
					
				} else { 
					if (checkResponseRedirect(response, redirectCount)) { 
						Header header = response.getFirstHeader("Location");
		                if (header != null) {
		                	String location = header.getValue(); 
		                	LOG.info("redirect http request to "+location); 
		                	
		                	redirectCount ++; 
		                	request = createRedirectRequest(location); 
		                	if (request != null) 
		                		continue; 
		                }
					}
					
					int statusCode = response.getStatusLine().getStatusCode(); 
					String message = response.getStatusLine().getReasonPhrase(); 
					LOG.error("HTTP status error: "+statusCode+" reason: "+message); 
					
					return new HttpException(statusCode, message); 
				}
			}
			
		} catch (ClientProtocolException e) {
			LOG.error("HTTP error", e); 
			return new HttpException(HttpTask.SC_EXCEPTION, "HTTP error", e); 
			
		} catch (IOException e) {
			LOG.error("HTTP error", e); 
			return new HttpException(HttpTask.SC_EXCEPTION, "HTTP error", e); 
			
		} catch (IllegalStateException e) { 
			LOG.error("Request error", e); 
			return new HttpException(HttpTask.SC_EXCEPTION, "Request error", e); 
			
		} catch (NullPointerException e) { 
			LOG.error("Request error", e); 
			return new HttpException(HttpTask.SC_EXCEPTION, "Request error", e); 
		}
	}
	
	private HttpException checkRequestBeforeExecute(HttpClient client, HttpUriRequest request) { 
		if (client == null || request == null) 
			return null; 
		
		try { 
			ResponseChecker checker = mResponseChecker; 
			if (checker != null) 
				checker.checkRequestBeforeExecute(client, request); 
			
		} catch (HttpException ex) { 
			return ex; 
		}
		
		return null; 
	}
	
	private boolean checkResponseSuccess(HttpResponse response) { 
		if (response == null) return false; 
		
		ResponseChecker checker = mResponseChecker; 
		if (checker != null) 
			return checker.checkResponseSuccess(response); 
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT) 
			return true; 
		else
			return false; 
	}
	
	private boolean checkResponseRedirect(HttpResponse response, int redirectCount) { 
		if (response == null) return false; 
		
		ResponseChecker checker = mResponseChecker; 
		if (checker != null) 
			return checker.checkResponseRedirect(response, redirectCount); 
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307) { 
			if (redirectCount > 2) return false; 
			return true; 
			
		} else
			return false; 
	}
	
	private HttpUriRequest createRedirectRequest(String location) { 
		ResponseChecker checker = mResponseChecker; 
		if (checker != null) 
			return checker.createRedirectRequest(location); 
		
		if (location != null && location.indexOf("://") > 0) 
			return new HttpGet(location); 
		
		return null;
	}
	
	protected abstract HttpTask.Result readEntity(final HttpEntity entity, HttpTask.Publisher publisher) throws IOException, ParseException; 
	
	public static class BytesResult implements HttpTask.Result {
		public final byte[] mData; 
		
		public BytesResult(byte[] data) {
			mData = data; 
		}
		
		public Object getData() {
			return mData; 
		}
	}
	
	public static class StringResult implements HttpTask.Result {
		public final String mData; 
		public final String mContentCharset; 
		
		public StringResult(String data, String charset) {
			mData = data; 
			mContentCharset = charset; 
		}
		
		public Object getData() {
			return mData; 
		}
	}
	
	public static class BytesHttpExecutor extends SimpleHttpExecutor {
		public BytesHttpExecutor(ResponseChecker checker) { 
			super(checker); 
		}
		
		@Override 
		protected HttpTask.Result readEntity(final HttpEntity entity, HttpTask.Publisher publisher) 
				throws IOException, ParseException {
			return readByteArray(this, entity, publisher); 
		}
		
		protected HttpTask.Progress createProgress(int contentLength, int fetchedLength, boolean aborted) {
			return new SimpleProgress(contentLength, fetchedLength, aborted); 
		}
	}
	
	public static class StringHttpExecutor extends SimpleHttpExecutor {
		private String mCharsetEncoding = null; 
		
		public StringHttpExecutor(ResponseChecker checker) { 
			super(checker); 
		}
		
		public void setCharsetEncoding(String encoding) { mCharsetEncoding = encoding; }
		public String getCharsetEncoding() { return mCharsetEncoding; }
		
		@Override 
		protected HttpTask.Result readEntity(final HttpEntity entity, HttpTask.Publisher publisher) 
				throws IOException, ParseException {
			return readString(this, entity, publisher); 
		}
		
		protected HttpTask.Progress createProgress(int contentLength, int fetchedLength, boolean aborted) {
			return new SimpleProgress(contentLength, fetchedLength, aborted); 
		}
	}
	
	public static StringResult readString(final StringHttpExecutor executor, 
			final HttpEntity entity, HttpTask.Publisher publisher) 
			throws IOException, ParseException {
		return readString(executor, entity, publisher, (String)null); 
	}
	
	public static StringResult readString(final StringHttpExecutor executor, 
			final HttpEntity entity, HttpTask.Publisher publisher, final String defaultCharset) 
			throws IOException, ParseException {
		StringWriter output = new StringWriter(); 
		
		if (saveStringEntity(executor, entity, publisher, output, defaultCharset) > 0) 
			return new StringResult(output.toString(), executor.getCharsetEncoding()); 
		
		return new StringResult("", null);
	}
	
	public static int saveStringEntity(final StringHttpExecutor executor, 
			final HttpEntity entity, HttpTask.Publisher publisher, Writer output) 
			throws IOException, ParseException {
		return saveStringEntity(executor, entity, publisher, output); 
	}
	
	public static int saveStringEntity(final StringHttpExecutor executor, 
			final HttpEntity entity, HttpTask.Publisher publisher, Writer output, final String defaultCharset) 
			throws IOException, ParseException {
		if (publisher.isAborted()) { 
			publisher.publish(executor.createProgress(0, 0, true)); 
			return 0; 
		}
		
        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        
        InputStream instream = entity.getContent();
        if (instream == null) {
            return -1; 
        }
        
        if (entity.getContentLength() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        int contentLength = (int)entity.getContentLength();
        int i = contentLength; 
        if (i < 0) {
            i = 4096;
        }
        
        String charset = EntityUtils.getContentCharSet(entity);
        if (charset == null) {
            charset = defaultCharset;
        }
        if (charset == null) {
            charset = HTTP.DEFAULT_CONTENT_CHARSET;
        }
        
        if (publisher.isAborted()) { 
        	publisher.publish(executor.createProgress(contentLength, 0, true)); 
        	return 0; 
        }
        
        executor.setCharsetEncoding(charset); 
        
        Reader reader = new InputStreamReader(instream, charset);
        int l, fetchedLength = 0;
        try {
            char[] tmp = new char[1024];
            while((l = reader.read(tmp)) != -1) {
            	if (publisher.isAborted()) { 
            		publisher.publish(
	                		executor.createProgress(contentLength, fetchedLength, true)); 
            		break; 
            	}
            	if (l > 0) { 
            		output.write(tmp, 0, l); 
            		fetchedLength += l; 
	                publisher.publish(
	                		executor.createProgress(contentLength, fetchedLength, false)); 
            	}
            }
        } finally {
            reader.close();
        }
        
        return fetchedLength; 
    }
	
	public static BytesResult readByteArray(final BytesHttpExecutor executor, 
			final HttpEntity entity, HttpTask.Publisher publisher) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream(); 
		
		if (saveByteArrayEntity(executor, entity, publisher, output) > 0) 
			return new BytesResult(output.toByteArray()); 
		
		return new BytesResult(new byte[] {});
	}
	
	public static int saveByteArrayEntity(final BytesHttpExecutor executor, 
			final HttpEntity entity, HttpTask.Publisher publisher, OutputStream output) throws IOException {
		if (publisher.isAborted()) { 
			publisher.publish(executor.createProgress(0, 0, true)); 
			return 0; 
		}
		
        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        
        InputStream instream = entity.getContent();
        if (instream == null) {
            return -1; 
        }
        
        if (entity.getContentLength() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        int contentLength = (int)entity.getContentLength();
        int i = contentLength; 
        if (i < 0) {
            i = 4096;
        }
        
        if (publisher.isAborted()) { 
        	publisher.publish(executor.createProgress(contentLength, 0, true)); 
        	return 0; 
        }
        
        int n, fetchedLength = 0;
        try {
            byte[] tmp = new byte[1024];
            while ((n = instream.read(tmp)) != -1) {
            	if (publisher.isAborted()) { 
            		publisher.publish(
	                		executor.createProgress(contentLength, fetchedLength, true)); 
            		break; 
            	}
            	if (n > 0) {
            		output.write(tmp, 0, n); 
	                fetchedLength += n; 
	                publisher.publish(
	                		executor.createProgress(contentLength, fetchedLength, false)); 
            	}
            }
        } finally {
            instream.close();
        }
        
        return fetchedLength; 
    }
}
