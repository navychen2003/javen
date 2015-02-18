package org.javenstudio.mail.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import javax.net.ssl.SSLSocket;

import org.javenstudio.mail.transport.Transport;

public class MailSocket implements Transport.Socket {

	private final Socket mSocket; 
	
	public MailSocket(Socket socket) { 
		mSocket = socket; 
	}
	
	public final Socket getSocket() { 
		return mSocket; 
	}
	
	@Override 
	public final SSLSocket getSSLSocket() { 
		return mSocket instanceof SSLSocket ? (SSLSocket)mSocket : null;
	}
	
	public final InetAddress getLocalAddress() {
		return mSocket.getInetAddress(); 
	}
	
	@Override 
	public synchronized void setSoTimeout(int timeout) throws SocketException {
		mSocket.setSoTimeout(timeout); 
	}
	
	@Override 
	public synchronized void connect(SocketAddress remoteAddr, int timeout) throws IOException {
		mSocket.connect(remoteAddr, timeout); 
	}
	
	@Override 
	public synchronized void close() throws IOException {
		mSocket.close(); 
	}
	
	@Override 
	public synchronized InputStream getInputStream() throws IOException {
		return mSocket.getInputStream(); 
	}
	
	@Override 
	public synchronized OutputStream getOutputStream() throws IOException {
		return mSocket.getOutputStream(); 
	}
	
	@Override 
	public final boolean isBound() {
		return mSocket.isBound(); 
	}
	
	@Override 
	public final boolean isConnected() {
		return mSocket.isConnected(); 
	}
	
	@Override 
	public final boolean isClosed() {
		return mSocket.isClosed(); 
	}
	
	@Override
    public String toString() {
		return mSocket.toString(); 
	}
	
}
