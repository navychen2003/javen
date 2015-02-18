package org.javenstudio.cocoka.net;

import java.io.IOException;
import java.net.Socket;

import org.javenstudio.mail.transport.Transport;

public class DelegatedSocketFactory implements Transport.SocketFactory {

	private final static DelegatedSocketFactory sDefault = new DelegatedSocketFactory(); 
	
	public static DelegatedSocketFactory getDefault() { 
		return sDefault; 
	}
	
	private DelegatedSocketFactory() {}
	
	public DelegatedSocket createSocket() throws IOException { 
		return new DelegatedSocket(new Socket()); 
	}
	
	public Transport.Socket createTransportSocket() throws IOException { 
		return createSocket();
	}
	
	public Transport.Socket createTransportSocket(Transport.Socket k, String host, int port, boolean autoClose) throws IOException {
		throw new IOException("not support");
	}
	
}
