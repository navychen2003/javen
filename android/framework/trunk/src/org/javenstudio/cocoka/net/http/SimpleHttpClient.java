package org.javenstudio.cocoka.net.http;

import java.io.IOException;

import android.content.Context;
import android.os.Looper;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.javenstudio.cocoka.net.SimpleSSLSocketFactory;
import org.javenstudio.cocoka.net.SimpleSocketFactory;
import org.javenstudio.common.util.Logger;

public final class SimpleHttpClient implements HttpClient {
	private static Logger LOG = Logger.getLogger(SimpleHttpClient.class);

    // Default connection and socket timeout of 60 seconds.  Tweak to taste.
    private static final int SOCKET_OPERATION_TIMEOUT = 20 * 1000;
	
	public static interface HttpParamsInitializer { 
		public void initHttpParams(HttpParams params);
	}
	
	private static HttpParamsInitializer mInitializer = null;
	
	public static synchronized void setHttpParamsInitializer(HttpParamsInitializer initializer) { 
		if (initializer != null && initializer != mInitializer) 
			mInitializer = initializer;
	}
	
	/** Interceptor throws an exception if the executing thread is blocked */
    private static final HttpRequestInterceptor sThreadCheckInterceptor =
            new HttpRequestInterceptor() {
        public void process(HttpRequest request, HttpContext context) {
            // Prevent the HttpRequest from being sent on the main thread
            if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper() ) {
                throw new RuntimeException("This thread forbids HTTP requests");
            }
        }
    };
    
    /**
     * Create a new HttpClient with reasonable defaults (which you can update).
     *
     * @param userAgent to report in your HTTP requests
     * @param context to use for caching SSL sessions (may be null for no caching)
     * @return AndroidHttpClient for you to use for all your requests.
     */
    public static SimpleHttpClient newInstance(String userAgent, Context context, boolean trustAll) {
        HttpParams params = new BasicHttpParams();

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Default connection and socket timeout of 30 seconds.  Tweak to taste.
        HttpConnectionParams.setConnectionTimeout(params, SOCKET_OPERATION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, 8192);

        // Don't handle redirects -- return them to the caller.  Our code
        // often wants to re-POST after a redirect, which we must do ourselves.
        HttpClientParams.setRedirecting(params, false);

        HttpParamsInitializer initializer = mInitializer;
        if (initializer != null)
        	initializer.initHttpParams(params); 
        
        // Use a session cache for SSL sockets
        //SSLSessionCache sessionCache = context == null ? null : new SSLSessionCache(context);

        // Set the specified user agent and register standard protocols.
        HttpProtocolParams.setUserAgent(params, userAgent);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http",
        		(org.apache.http.conn.scheme.SocketFactory)SimpleSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", 
        		(org.apache.http.conn.ssl.SSLSocketFactory)SimpleSSLSocketFactory.getSocketFactory(SOCKET_OPERATION_TIMEOUT, trustAll), 443));
        //schemeRegistry.register(new Scheme("https",
        //        SSLCertificateSocketFactory.getHttpSocketFactory(
        //		  SOCKET_OPERATION_TIMEOUT, sessionCache), 443));

        ClientConnectionManager manager =
                new ThreadSafeClientConnManager(params, schemeRegistry);

        // We use a factory method to modify superclass initialization
        // parameters without the funny call-a-static-method dance.
        return new SimpleHttpClient(manager, params);
    }
    
    public static void initHttpParamsDefault(HttpParams params) { 
		// Default connection and socket timeout of 20 seconds.  Tweak to taste.
        HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
        HttpConnectionParams.setSoTimeout(params, 20 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
	}
    
    /**
     * Create a new HttpClient with reasonable defaults (which you can update).
     * @param userAgent to report in your HTTP requests.
     * @return AndroidHttpClient for you to use for all your requests.
     */
    public static SimpleHttpClient newInstance(String userAgent, boolean trustAll) {
        return newInstance(userAgent, null /* session cache */, trustAll);
    }
    
    private final HttpClient delegate;

    private RuntimeException mLeakedException = new IllegalStateException(
            "SimpleHttpClient created and never closed");
    
    private SimpleHttpClient(ClientConnectionManager ccm, HttpParams params) {
        this.delegate = new DefaultHttpClient(ccm, params) {
            @Override
            protected BasicHttpProcessor createHttpProcessor() {
                // Add interceptor to prevent making requests from main thread.
                BasicHttpProcessor processor = super.createHttpProcessor();
                processor.addRequestInterceptor(sThreadCheckInterceptor);
                //processor.addRequestInterceptor(new CurlLogger());

                return processor;
            }

            @Override
            protected HttpContext createHttpContext() {
                // Same as DefaultHttpClient.createHttpContext() minus the
                // cookie store.
                HttpContext context = new BasicHttpContext();
                context.setAttribute(
                        ClientContext.AUTHSCHEME_REGISTRY,
                        getAuthSchemes());
                context.setAttribute(
                        ClientContext.COOKIESPEC_REGISTRY,
                        getCookieSpecs());
                context.setAttribute(
                        ClientContext.CREDS_PROVIDER,
                        getCredentialsProvider());
                return context;
            }
        };
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mLeakedException != null) {
        	if (LOG.isErrorEnabled())
        		LOG.error("Leak found", mLeakedException);
            mLeakedException = null;
        }
    }
    
    /**
     * Release resources associated with this client.  You must call this,
     * or significant resources (sockets and memory) may be leaked.
     */
    public void close() {
        if (mLeakedException != null) {
            getConnectionManager().shutdown();
            mLeakedException = null;
        }
    }
    
    public HttpParams getParams() {
        return delegate.getParams();
    }

    public ClientConnectionManager getConnectionManager() {
        return delegate.getConnectionManager();
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return delegate.execute(request);
    }

    public HttpResponse execute(HttpUriRequest request, HttpContext context)
            throws IOException {
        return delegate.execute(request, context);
    }

    public HttpResponse execute(HttpHost target, HttpRequest request)
            throws IOException {
        return delegate.execute(target, request);
    }

    public HttpResponse execute(HttpHost target, HttpRequest request,
            HttpContext context) throws IOException {
        return delegate.execute(target, request, context);
    }

    public <T> T execute(HttpUriRequest request, 
            ResponseHandler<? extends T> responseHandler)
            throws IOException, ClientProtocolException {
        return delegate.execute(request, responseHandler);
    }

    public <T> T execute(HttpUriRequest request,
            ResponseHandler<? extends T> responseHandler, HttpContext context)
            throws IOException, ClientProtocolException {
        return delegate.execute(request, responseHandler, context);
    }

    public <T> T execute(HttpHost target, HttpRequest request,
            ResponseHandler<? extends T> responseHandler) throws IOException,
            ClientProtocolException {
        return delegate.execute(target, request, responseHandler);
    }

    public <T> T execute(HttpHost target, HttpRequest request,
            ResponseHandler<? extends T> responseHandler, HttpContext context)
            throws IOException, ClientProtocolException {
        return delegate.execute(target, request, responseHandler, context);
    }
    
}
