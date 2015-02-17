package org.javenstudio.mail.example;

import java.io.IOException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.javenstudio.mail.transport.Transport;

public class MailSSLSocketFactory implements Transport.SocketFactory {

	private SocketFactory mFactory = SSLSocketFactory.getDefault();
	
	@Override
	public Transport.Socket createTransportSocket() throws IOException { 
		return new MailSocket(mFactory.createSocket());
	}
	
	@Override
	public Transport.Socket createTransportSocket(Transport.Socket k, String host, int port, boolean autoClose) throws IOException {
		return new MailSocket(((SSLSocketFactory)mFactory).createSocket(k.getSocket(), host, port, autoClose));
	}
	
}
