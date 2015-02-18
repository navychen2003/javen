package org.javenstudio.cocoka.net;

import java.io.IOException;
import java.net.Socket;

public interface SecureSocketFactory {

	public Socket createSocket() throws IOException; 
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException; 
	
}
