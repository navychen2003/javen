package org.javenstudio.cocoka.net;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.javenstudio.common.util.Logger;

public class FakeX509TrustManager implements X509TrustManager, HostnameVerifier {
	private static final Logger LOG = Logger.getLogger(FakeX509TrustManager.class);

	private static final FakeX509TrustManager sTrustManager = new FakeX509TrustManager();
	private static final TrustManager[] sTrustManagers = new TrustManager[] { sTrustManager };
	private static final X509Certificate[] sAcceptedIssuers = new X509Certificate[] {};
	
	public static synchronized void allowAllSSL() {
		if (LOG.isDebugEnabled()) LOG.debug("allowAllSSL");
		
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, getTrustManagers(), new SecureRandom());
			
			HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier());
			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("allowAllSSL: error: " + e, e);
		}
	}
	
	public static TrustManager[] getTrustManagers() { 
		return sTrustManagers;
	}
	
	public static HostnameVerifier getHostnameVerifier() {
		return sTrustManager;
	}
	
	@Override
	public boolean verify(String s, SSLSession sslsession) {
		if (LOG.isDebugEnabled())
			LOG.debug("verify: s=" + s + " sslsession=" + sslsession);
		return true;
	}
	
	public boolean isClientTrusted(X509Certificate[] chain) { return true; }
	public boolean isServerTrusted(X509Certificate[] chain) { return true; }
	
	@Override
	public void checkClientTrusted(X509Certificate[] ax509certificate, String s)
			throws java.security.cert.CertificateException {
		if (LOG.isDebugEnabled())
			LOG.debug("checkClientTrusted: ax509certificate=" + ax509certificate + " s=" + s);
	}
	
	@Override
	public void checkServerTrusted(X509Certificate[] ax509certificate, String s)
			throws java.security.cert.CertificateException {
		if (LOG.isDebugEnabled())
			LOG.debug("checkServerTrusted: ax509certificate=" + ax509certificate + " s=" + s);
	}
	
	@Override
	public X509Certificate[] getAcceptedIssuers() { 
		return sAcceptedIssuers; 
	}
	
}
