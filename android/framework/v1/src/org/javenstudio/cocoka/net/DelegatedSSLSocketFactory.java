package org.javenstudio.cocoka.net;

import java.io.IOException;

import org.javenstudio.mail.transport.Transport;

public class DelegatedSSLSocketFactory implements Transport.SocketFactory {

	public static interface SocketFactoryCreator { 
		public SecureSocketFactory createSocketFactory(int handshakeTimeoutMillis, boolean secure); 
	}
	
	private static SocketFactoryCreator sCreator = null;
	
	public static synchronized void setSocketFactoryCreator(SocketFactoryCreator creator) { 
		if (sCreator != null) 
			throw new RuntimeException("SocketFactoryCreator already set");
		if (creator != null && creator != sCreator) 
			sCreator = creator;
	}
	
	public static DelegatedSSLSocketFactory getDefault(int handshakeTimeoutMillis) {
		return new DelegatedSSLSocketFactory(handshakeTimeoutMillis, false); 
	}
	
	public static DelegatedSSLSocketFactory getInsecure(int handshakeTimeoutMillis) {
		return new DelegatedSSLSocketFactory(handshakeTimeoutMillis, true); 
	}
	
	// 2.2 and above version use this
	//private final SSLSocketFactory mFactory; 
	// 2.1 and blow version use this
	//private final SocketFactory mFactory; 
	
	private final SecureSocketFactory mFactory; 
	
	private DelegatedSSLSocketFactory(int handshakeTimeoutMillis, boolean secure) { 
		if (secure) { 
			// 2.2 and above version use this
			//mFactory = SSLCertificateSocketFactory.getInsecure(handshakeTimeoutMillis, null); 
			// 2.1 and blow version use this
			//mFactory = SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis);
		} else { 
			// 2.2 and above version use this
			//mFactory = SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis, null);
			// 2.1 and blow version use this
			//mFactory = SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis);
		}
		
		SocketFactoryCreator creator = sCreator;
		if (creator == null) 
			throw new RuntimeException("SocketFactoryCreator not set");
		
		mFactory = creator.createSocketFactory(handshakeTimeoutMillis, secure); 
	}
	
	public DelegatedSocket createSocket() throws IOException { 
		return new DelegatedSocket(mFactory.createSocket()); 
	}
	
	public DelegatedSocket createSocket(DelegatedSocket k, String host, int port, boolean autoClose) throws IOException {
		// 2.2 and above version use this
		return new DelegatedSocket(mFactory.createSocket(k.getSocket(), host, port, autoClose)); 
		// 2.1 and blow version use this
		//return new DelegatedSocket(mFactory.createSocket(host, port)); 
	}
	
	public Transport.Socket createTransportSocket() throws IOException { 
		return createSocket();
	}
	
	public Transport.Socket createTransportSocket(Transport.Socket k, String host, int port, boolean autoClose) throws IOException { 
		// 2.2 and above version use this
		return new DelegatedSocket(mFactory.createSocket(k.getSocket(), host, port, autoClose)); 
		// 2.1 and blow version use this
		//return new DelegatedSocket(mFactory.createSocket(host, port)); 
	}
	
}
