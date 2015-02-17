package org.javenstudio.cocoka.net;

import java.io.IOException;
import java.net.Socket;

import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;

import org.javenstudio.cocoka.android.ResourceHelper;

public interface SocketFactoryCreator {

	public enum Type { SIMPLE, DEFAULT, INSECURE, HTTP_SECURE, HTTP_INSECURE }
	
	public ISocketFactory createSocketFactory(int handshakeTimeoutMillis, Type type); 
	
	public static final class Helper {
		private static SocketFactoryCreator sCreator = null;
		
		public static synchronized void setSocketFactoryCreator(SocketFactoryCreator creator) { 
			if (sCreator != null) 
				throw new RuntimeException("SocketFactoryCreator already set");
			if (creator != null && creator != sCreator) 
				sCreator = creator;
		}
		
		public static synchronized ISocketFactory createSocketFactory(
				int handshakeTimeoutMillis, SocketFactoryCreator.Type type) { 
			SocketFactoryCreator creator = sCreator;
			if (creator == null) 
				throw new RuntimeException("SocketFactoryCreator not set");
			
			ISocketFactory factory = creator.createSocketFactory(handshakeTimeoutMillis, type); 
			if (factory != null) return factory;
			
			return createDefault(handshakeTimeoutMillis, type);
		}
		
		private static ISocketFactory createDefault(final int handshakeTimeoutMillis, 
				final SocketFactoryCreator.Type type) {
			if (type == SocketFactoryCreator.Type.SIMPLE) 
				return SimpleSocketFactory.getSocketFactory();
			
			// 2.2 and above version use this
			// Use a session cache for SSL sockets
	        final SSLSessionCache sessionCache = new SSLSessionCache(ResourceHelper.getContext());
	        
	        if (type == SocketFactoryCreator.Type.HTTP_SECURE) { 
	        	return new ISocketFactory() { 
	        		private final org.apache.http.conn.ssl.SSLSocketFactory mFactory = 
	        				SSLCertificateSocketFactory.getHttpSocketFactory(handshakeTimeoutMillis, sessionCache);
	        		public org.apache.http.conn.ssl.SSLSocketFactory getFactoryInstance() { return mFactory; }
	        	};
	        } else if (type == SocketFactoryCreator.Type.HTTP_INSECURE) {
	        	return new ISocketFactory() { 
	        		private final org.apache.http.conn.ssl.SSLSocketFactory mFactory = 
	        				SSLCertificateSocketFactory.getHttpSocketFactory(handshakeTimeoutMillis, sessionCache);
	        				//new org.apache.http.conn.ssl.SSLSocketFactory(
	        				//		SSLCertificateSocketFactory.getInsecure(handshakeTimeoutMillis, sessionCache));
	        		public org.apache.http.conn.ssl.SSLSocketFactory getFactoryInstance() { 
	        			//SSLCertificateSocketFactory factory = (SSLCertificateSocketFactory)mFactory.getSocketFactory();
	        			//factory.setTrustManagers(FakeX509TrustManager.getTrustManagers());
	        			//FakeX509TrustManager.allowAllSSL();
	        			mFactory.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	        			return mFactory; 
	        		}
	        	};
	        }
	        
			return new SecureSocketFactory() { 
					private final javax.net.ssl.SSLSocketFactory mFactory = (type == SocketFactoryCreator.Type.INSECURE) ? 
							SSLCertificateSocketFactory.getInsecure(handshakeTimeoutMillis, sessionCache) : 
								SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis, sessionCache); 
					public javax.net.ssl.SSLSocketFactory getFactoryInstance() { return mFactory; }
					public Socket createSocket() throws IOException { return mFactory.createSocket(); }
					public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException { 
						return mFactory.createSocket(socket, host, port, autoClose); 
					}
				};
				
			// 2.1 and blow version use this
			//return new SecureSocketFactory() { 
			//		private final SocketFactory mFactory = SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis); 
			//		public Socket createSocket() throws IOException { return mFactory.createSocket(); }
			//		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException { 
			//			return mFactory.createSocket(host, port); 
			//		}
			//	};
		}
	}
	
}
