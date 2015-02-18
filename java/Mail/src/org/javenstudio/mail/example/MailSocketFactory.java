package org.javenstudio.mail.example;

import java.io.IOException;
import java.net.Socket;

import org.javenstudio.mail.transport.Transport;

public class MailSocketFactory implements Transport.SocketFactory {

	@Override
	public Transport.Socket createTransportSocket() throws IOException { 
		return new MailSocket(new Socket());
	}
	
	@Override
	public Transport.Socket createTransportSocket(Transport.Socket k, String host, int port, boolean autoClose) throws IOException {
		throw new IOException("not support");
	}
	
}
