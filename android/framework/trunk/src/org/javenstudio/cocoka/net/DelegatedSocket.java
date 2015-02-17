package org.javenstudio.cocoka.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;

import org.javenstudio.mail.transport.Transport;

public class DelegatedSocket implements Transport.Socket, SocketMetrics.SocketInfo {

	private final Socket mSocket; 
	private final SocketMetrics mMetrics; 
	private String mAddressName = null; 
	private long mConnectTime = -1;
	
	public DelegatedSocket(Socket socket) { 
		mSocket = socket; 
		mMetrics = new SocketMetrics(this); 
		SocketHelper.updateSocketInfo(this);
	}
	
	@Override 
	public final String getAddressName() {
		return mAddressName; 
	}
	
	@Override 
	public long getConnectTime() { 
		return mConnectTime;
	}
	
	@Override
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
		mAddressName = SimpleSocket.toAddressName(remoteAddr); 
		
		if (SimpleSocket.isIPv4Address(mAddressName))
			mMetrics.onConnectBegin(remoteAddr, timeout); 
		mConnectTime = System.currentTimeMillis();
		
		mSocket.connect(remoteAddr, timeout); 
		
		mConnectTime = 0;
		mMetrics.onConnectEnd(remoteAddr, timeout); 
		
		SocketHelper.updateSocketInfo(this);
	}
	
	@Override 
	public synchronized void close() throws IOException {
		mSocket.close(); 
		mMetrics.onClose(); 
		mConnectTime = 0;
		SocketHelper.updateSocketInfo(this);
	}
	
	@Override 
	public synchronized InputStream getInputStream() throws IOException {
		return new SocketMetrics.MetricsInputStream(mSocket.getInputStream(), mMetrics); 
	}
	
	@Override 
	public synchronized OutputStream getOutputStream() throws IOException {
		return new SocketMetrics.MetricsOutputStream(mSocket.getOutputStream(), mMetrics); 
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
