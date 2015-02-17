package org.javenstudio.cocoka.net;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class SimpleSSLSocketFactory extends org.apache.http.conn.ssl.SSLSocketFactory {
	//private static final Logger LOG = Logger.getLogger(SimpleSSLSocketFactory.class);
	
	public SimpleSSLSocketFactory(KeyStore truststore)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException {
		super(truststore);
		setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	}

	public static org.apache.http.conn.ssl.SSLSocketFactory getSocketFactory(int handshakeTimeoutMillis, boolean trustAll) {
		if (trustAll) {
			//try {
			//	return new SimpleSSLSocketFactory(null);
			//} catch (Throwable e) {
			//	if (LOG.isWarnEnabled())
			//		LOG.warn("getSocketFactory: error: " + e, e);
			//}
		}
		
		org.apache.http.conn.ssl.SSLSocketFactory factory = (org.apache.http.conn.ssl.SSLSocketFactory)
				SocketFactoryCreator.Helper.createSocketFactory(handshakeTimeoutMillis, 
						trustAll ? SocketFactoryCreator.Type.HTTP_INSECURE : 
							SocketFactoryCreator.Type.HTTP_SECURE).getFactoryInstance();
		
		if (factory != null && trustAll) {
			//factory.setHostnameVerifier(
			//		org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		}
		
		return factory;
    }
	
}
