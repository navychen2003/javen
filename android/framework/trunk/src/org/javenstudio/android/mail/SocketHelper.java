package org.javenstudio.android.mail;

import org.javenstudio.cocoka.net.DelegatedSSLSocketFactory;
import org.javenstudio.cocoka.net.DelegatedSocketFactory;
import org.javenstudio.mail.transport.Transport;

public class SocketHelper {

	private static Transport.SocketFactory sInsecureFactory = null; 
    private static Transport.SocketFactory sSecureFactory = null; 
	
	/**
     * Returns a {@link SSLSocketFactory}.  Optionally bypass all SSL certificate checks.
     *
     * @param insecure if true, bypass all SSL certificate checks
     */
	public synchronized static final Transport.SocketFactory getSSLSocketFactory(boolean insecure) {
        if (insecure) {
            if (sInsecureFactory == null) { 
                sInsecureFactory = DelegatedSSLSocketFactory.getInsecure(0);
            }
            return sInsecureFactory;
        } else {
            if (sSecureFactory == null) { 
                sSecureFactory = DelegatedSSLSocketFactory.getDefault(0);
            }
            return sSecureFactory;
        }
    }
    
    public synchronized static final Transport.SocketFactory getSocketFactory() { 
    	return DelegatedSocketFactory.getDefault(); 
    }
	
}
