package org.javenstudio.lightning.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import javax.net.ssl.SSLContext;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;
import org.javenstudio.common.util.Logger;

@SuppressWarnings("deprecation")
public final class SimpleHttpClient implements HttpClient {
	private static Logger LOG = Logger.getLogger(SimpleHttpClient.class);

    // Default connection and socket timeout of 60 seconds.  Tweak to taste.
    private static final int SOCKET_OPERATION_TIMEOUT = 30 * 1000;
	
	//public static interface HttpParamsInitializer { 
	//	public void initHttpParams(HttpParams params);
	//}
	
	//private static HttpParamsInitializer mInitializer = null;
	
	//public static synchronized void setHttpParamsInitializer(HttpParamsInitializer initializer) { 
	//	if (initializer != null && initializer != mInitializer) 
	//		mInitializer = initializer;
	//}
	
	/** Interceptor throws an exception if the executing thread is blocked */
    //private static final HttpRequestInterceptor sThreadCheckInterceptor =
    //        new HttpRequestInterceptor() {
    //    public void process(HttpRequest request, HttpContext context) {
    //        // Prevent the HttpRequest from being sent on the main thread
    //        //if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper() ) {
    //        //    throw new RuntimeException("This thread forbids HTTP requests");
    //        //}
    //    }
    //};
    
    /**
     * Create a new HttpClient with reasonable defaults (which you can update).
     *
     * @param userAgent to report in your HTTP requests
     * @param context to use for caching SSL sessions (may be null for no caching)
     * @return AndroidHttpClient for you to use for all your requests.
     */
    public static SimpleHttpClient newInstance(String userAgent, IHttpContext context) {
        //HttpParams params = new BasicHttpParams();

        // Turn off stale checking.  Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        //HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Default connection and socket timeout of 30 seconds.  Tweak to taste.
        //HttpConnectionParams.setConnectionTimeout(params, SOCKET_OPERATION_TIMEOUT);
        //HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);
        //HttpConnectionParams.setSocketBufferSize(params, 8192);

        // Don't handle redirects -- return them to the caller.  Our code
        // often wants to re-POST after a redirect, which we must do ourselves.
        //HttpClientParams.setRedirecting(params, false);

        //HttpParamsInitializer initializer = mInitializer;
        //if (initializer != null)
        //	initializer.initHttpParams(params); 
        
        // Use a session cache for SSL sockets
        //SSLSessionCache sessionCache = context == null ? null : new SSLSessionCache(context);

        // Set the specified user agent and register standard protocols.
        //HttpProtocolParams.setUserAgent(params, userAgent);
        //SchemeRegistry schemeRegistry = new SchemeRegistry();
        //schemeRegistry.register(new Scheme("http",
        //		(org.apache.http.conn.scheme.SocketFactory)SimpleSocketFactory.getSocketFactory(), 80));
        //schemeRegistry.register(new Scheme("https", 
        //		(org.apache.http.conn.ssl.SSLSocketFactory)(SocketHelper.createSocketFactory(SOCKET_OPERATION_TIMEOUT, 
        //				ISocketFactoryCreator.Type.HTTP_SECURE).getFactoryInstance()), 443));
        //schemeRegistry.register(new Scheme("https",
        //        SSLCertificateSocketFactory.getHttpSocketFactory(
        //		  SOCKET_OPERATION_TIMEOUT, sessionCache), 443));

        //ClientConnectionManager manager =
        //        new ThreadSafeClientConnManager(params, schemeRegistry);

        // We use a factory method to modify superclass initialization
        // parameters without the funny call-a-static-method dance.
        //return new SimpleHttpClient(manager);
    	
    	HttpClientConnectionManager connManager = null;
    	CookieStore cookieStore = null;
    	CredentialsProvider credentialsProvider = null;
    	RequestConfig defaultRequestConfig = null;
        
        if (context != null) { 
        	connManager = context.getConnectionManager();
        	cookieStore = context.getCookieStore();
        	credentialsProvider = context.getCredentialsProvider();
        	defaultRequestConfig = context.getDefaultRequestConfig();
        }
        
        if (connManager == null)
        	connManager = getDefaultConnectionManager();
        if (cookieStore == null) 
        	cookieStore = getDefaultCookieStore();
        if (credentialsProvider == null)
        	credentialsProvider = getDefaultCredentialsProvider();
        if (defaultRequestConfig == null)
        	defaultRequestConfig = getDefaultRequestConfig();
        
        return new SimpleHttpClient(connManager, cookieStore, 
        		credentialsProvider, defaultRequestConfig, userAgent);
    }
    
    private static HttpClientConnectionManager sConnectionManager = null;
    private static RequestConfig sRequestConfig = null;
    private static CredentialsProvider sCredentialsProvider = null;
    private static CookieStore sCookieStore = null;
    
    static synchronized CookieStore getDefaultCookieStore() { 
    	if (sCookieStore != null) return sCookieStore;
    	
    	// Use custom cookie store if necessary.
        CookieStore cookieStore = new BasicCookieStore();
    	
        sCookieStore = cookieStore;
    	return sCookieStore;
    }
    
    static synchronized CredentialsProvider getDefaultCredentialsProvider() { 
    	if (sCredentialsProvider != null) return sCredentialsProvider;
    	
    	// Use custom credentials provider if necessary.
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    	
        sCredentialsProvider = credentialsProvider;
    	return sCredentialsProvider;
    }
    
    static synchronized RequestConfig getDefaultRequestConfig() { 
    	if (sRequestConfig != null) return sRequestConfig;
    	
    	// Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setCookieSpec(CookieSpecs.BEST_MATCH)
            .setExpectContinueEnabled(true)
            .setStaleConnectionCheckEnabled(true)
            .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
            .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
            .setSocketTimeout(SOCKET_OPERATION_TIMEOUT)
            .setConnectTimeout(SOCKET_OPERATION_TIMEOUT)
            .setConnectionRequestTimeout(SOCKET_OPERATION_TIMEOUT)
            .build();
    	
        sRequestConfig = defaultRequestConfig;
    	return sRequestConfig;
    }
    
    static synchronized void closeConnections() { 
    	if (sConnectionManager == null) return;
    	
    	HttpClientConnectionManager connManager = sConnectionManager;
    	sConnectionManager = null;
    	
    	if (LOG.isDebugEnabled())
    		LOG.debug("closeConnections: connManager=" + connManager);
    	
    	connManager.shutdown();
    }
    
    static synchronized HttpClientConnectionManager getDefaultConnectionManager() { 
    	if (sConnectionManager != null) return sConnectionManager;
    	
    	// Use custom message parser / writer to customize the way HTTP
        // messages are parsed from and written out to the data stream.
        HttpMessageParserFactory<HttpResponse> responseParserFactory = 
        	new DefaultHttpResponseParserFactory() {
	            @Override
	            public HttpMessageParser<HttpResponse> create(
	            		SessionInputBuffer buffer, MessageConstraints constraints) {
	                LineParser lineParser = new BasicLineParser() {
		                    @Override
		                    public Header parseHeader(final CharArrayBuffer buffer) {
		                        try {
		                            return super.parseHeader(buffer);
		                        } catch (ParseException ex) {
		                            return new BasicHeader(buffer.toString(), null);
		                        }
		                    }
		                };
	                return new DefaultHttpResponseParser(buffer, lineParser, 
	                			DefaultHttpResponseFactory.INSTANCE, constraints) {
		                    @Override
		                    protected boolean reject(final CharArrayBuffer line, int count) {
		                        // try to ignore all garbage preceding a status line infinitely
		                        return false;
		                    }
		                };
	            }
	        };
        
    	HttpMessageWriterFactory<HttpRequest> requestWriterFactory = 
    			new DefaultHttpRequestWriterFactory();
    	
    	// Use a custom connection factory to customize the process of
        // initialization of outgoing HTTP connections. Beside standard connection
        // configuration parameters HTTP connection factory can define message
        // parser / writer routines to be employed by individual connections.
        HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = 
        		new ManagedHttpClientConnectionFactory(
        				requestWriterFactory, responseParserFactory);

        // Client HTTP connection objects when fully initialized can be bound to
        // an arbitrary network socket. The process of network socket initialization,
        // its connection to a remote address and binding to a local one is controlled
        // by a connection socket factory.

        // SSL context for secure connections can be created either based on
        // system or application specific properties.
        SSLContext sslcontext = SSLContexts.createSystemDefault();
        // Use custom hostname verifier to customize SSL hostname verification.
        X509HostnameVerifier hostnameVerifier = new BrowserCompatHostnameVerifier();

        // Create a registry of custom connection socket factories for supported
        // protocol schemes.
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.INSTANCE)
            .register("https", new SSLConnectionSocketFactory(sslcontext, hostnameVerifier))
            .build();

        // Use custom DNS resolver to override the system DNS resolution.
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
	            @Override
	            public InetAddress[] resolve(final String host) throws UnknownHostException {
	            	if (LOG.isDebugEnabled()) LOG.debug("resolve: host=" + host);
	                //if (host.equalsIgnoreCase("myhost")) {
	                //    return new InetAddress[] { InetAddress.getByAddress(new byte[] {127, 0, 0, 1}) };
	                //} else {
	                    return super.resolve(host);
	                //}
	            }
	        };
        
    	// Create a connection manager with custom configuration.
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
                socketFactoryRegistry, connFactory, dnsResolver);

        // Create socket configuration
        SocketConfig socketConfig = SocketConfig.custom()
            .setTcpNoDelay(true)
            .build();
        
        // Configure the connection manager to use socket configuration either
        // by default or for a specific host.
        connManager.setDefaultSocketConfig(socketConfig);
        //connManager.setSocketConfig(new HttpHost("somehost", 80), socketConfig);
        
    	// Create message constraints
        MessageConstraints messageConstraints = MessageConstraints.custom()
            .setMaxHeaderCount(200)
            .setMaxLineLength(2000)
            .build();
        
        // Create connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setMalformedInputAction(CodingErrorAction.IGNORE)
            .setUnmappableInputAction(CodingErrorAction.IGNORE)
            .setCharset(Consts.UTF_8)
            .setMessageConstraints(messageConstraints)
            .build();
        
        // Configure the connection manager to use connection configuration either
        // by default or for a specific host.
        connManager.setDefaultConnectionConfig(connectionConfig);
        //connManager.setConnectionConfig(new HttpHost("somehost", 80), ConnectionConfig.DEFAULT);
        
    	// Configure total max or per route limits for persistent connections
        // that can be kept in the pool or leased by the connection manager.
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(10);
        //connManager.setMaxPerRoute(new HttpRoute(new HttpHost("somehost", 80)), 20);
        
        sConnectionManager = connManager;
    	return sConnectionManager;
    }
    
    /**
     * Create a new HttpClient with reasonable defaults (which you can update).
     * @param userAgent to report in your HTTP requests.
     * @return AndroidHttpClient for you to use for all your requests.
     */
    public static SimpleHttpClient newInstance(String userAgent) {
        return newInstance(userAgent, null /* session cache */);
    }
    
    private final HttpClient delegate;

    private Exception mLeakedException = 
    		new IllegalStateException("SimpleHttpClient created and never closed");
    
    private SimpleHttpClient(HttpClientConnectionManager connManager, 
    		CookieStore cookieStore, CredentialsProvider credentialsProvider,
    		RequestConfig defaultRequestConfig, String userAgent) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("SimpleHttpClient: create, userAgent=" + userAgent);
    	
    	// Create an HttpClient with the given custom dependencies and configuration.
        CloseableHttpClient httpclient = HttpClients.custom()
            .setConnectionManager(connManager)
            .setDefaultCookieStore(cookieStore)
            .setDefaultCredentialsProvider(credentialsProvider)
            //.setProxy(new HttpHost("myproxy", 8080))
            .setDefaultRequestConfig(defaultRequestConfig)
            .setUserAgent(userAgent)
            .build();
        
        this.delegate = httpclient;
        //this.delegate = new DefaultHttpClient(ccm, params) {
        //    @Override
        //    protected BasicHttpProcessor createHttpProcessor() {
        //        // Add interceptor to prevent making requests from main thread.
        //        BasicHttpProcessor processor = super.createHttpProcessor();
        //        processor.addRequestInterceptor(sThreadCheckInterceptor);
        //        //processor.addRequestInterceptor(new CurlLogger());

        //        return processor;
        //    }

        //    @Override
        //    protected HttpContext createHttpContext() {
        //        // Same as DefaultHttpClient.createHttpContext() minus the
        //        // cookie store.
        //        HttpContext context = new BasicHttpContext();
        //        context.setAttribute(
        //                ClientContext.AUTHSCHEME_REGISTRY,
        //                getAuthSchemes());
        //        context.setAttribute(
        //                ClientContext.COOKIESPEC_REGISTRY,
        //                getCookieSpecs());
        //        context.setAttribute(
        //                ClientContext.CREDS_PROVIDER,
        //                getCredentialsProvider());
        //        return context;
        //    }
        //};
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mLeakedException != null) {
        	if (LOG.isWarnEnabled())
        		LOG.warn("finalize: Leak found", mLeakedException);
            mLeakedException = null;
        }
    }
    
    /**
     * Release resources associated with this client.  You must call this,
     * or significant resources (sockets and memory) may be leaked.
     */
    public void close() {
        if (mLeakedException != null) 
            mLeakedException = null;
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
