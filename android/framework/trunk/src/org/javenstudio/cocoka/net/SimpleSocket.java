package org.javenstudio.cocoka.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.UnknownHostException;

public final class SimpleSocket extends Socket implements SocketMetrics.SocketInfo {

	private final SocketMetrics mMetrics; 
	private String mAddressName = null; 
	private long mConnectTime = -1;
	
	public SimpleSocket() {
		super(); 
		mMetrics = new SocketMetrics(this); 
		SocketHelper.updateSocketInfo(this);
	}
	
	public SimpleSocket(Proxy proxy) {
		super(proxy); 
		mMetrics = new SocketMetrics(this); 
	}
	
	public SimpleSocket(String dstName, int dstPort) 
			throws UnknownHostException, IOException {
		super(dstName, dstPort); 
		mMetrics = new SocketMetrics(this); 
	}
	
	public SimpleSocket(String dstName, int dstPort, 
			InetAddress localAddress, int localPort) throws IOException {
		super(dstName, dstPort, localAddress, localPort); 
		mMetrics = new SocketMetrics(this); 
	}
	
	@Deprecated
    public SimpleSocket(String hostName, int port, 
    		boolean streaming) throws IOException {
		super(hostName, port, streaming); 
		mMetrics = new SocketMetrics(this); 
	}
	
	public SimpleSocket(InetAddress dstAddress, int dstPort) throws IOException {
		super(dstAddress, dstPort); 
		mMetrics = new SocketMetrics(this); 
	}
	
	public SimpleSocket(InetAddress dstAddress, int dstPort, 
			InetAddress localAddress, int localPort) throws IOException {
		super(dstAddress, dstPort, localAddress, localPort); 
		mMetrics = new SocketMetrics(this); 
	}
	
	@Deprecated
    public SimpleSocket(InetAddress addr, int port, 
    		boolean streaming) throws IOException {
		super(addr, port, streaming); 
		mMetrics = new SocketMetrics(this); 
	}
	
	protected SimpleSocket(SocketImpl anImpl) throws SocketException {
		super(anImpl); 
		mMetrics = new SocketMetrics(this); 
	}
	
	@Override 
	public synchronized void close() throws IOException {
		super.close(); 
		mMetrics.onClose(); 
		mConnectTime = 0;
		SocketHelper.updateSocketInfo(this);
	}
	
	@Override 
	public InputStream getInputStream() throws IOException {
		return new SocketMetrics.MetricsInputStream(super.getInputStream(), mMetrics); 
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		return new SocketMetrics.MetricsOutputStream(super.getOutputStream(), mMetrics); 
	}
	
	@Override 
	public void shutdownInput() throws IOException {
		super.shutdownInput(); 
		mMetrics.onShutdownInput(); 
	}
	
	@Override 
	public void shutdownOutput() throws IOException {
		super.shutdownOutput(); 
		mMetrics.onShutdownOutput(); 
	}
	
	@Override 
	public void bind(SocketAddress localAddr) throws IOException {
		mMetrics.onBindBegin(localAddr); 
		super.bind(localAddr); 
		mMetrics.onBindEnd(localAddr); 
	}
	
	@Override
	public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
		mAddressName = toAddressName(remoteAddr); 
		
		if (isIPv4Address(mAddressName))
			mMetrics.onConnectBegin(remoteAddr, timeout); 
		mConnectTime = System.currentTimeMillis();
		
		super.connect(remoteAddr, timeout); 
		
		mConnectTime = 0;
		mMetrics.onConnectEnd(remoteAddr, timeout); 
		
		SocketHelper.updateSocketInfo(this);
	}
	
	@Override 
	public String getAddressName() {
		return mAddressName; 
	}
	
	@Override 
	public Socket getSocket() { 
		return this;
	}
	
	@Override 
	public long getConnectTime() { 
		return mConnectTime;
	}
	
	public static String toAddressName(SocketAddress addr) {
		if (addr == null) return null; 
		if (addr instanceof InetSocketAddress) {
			InetSocketAddress ia = (InetSocketAddress)addr; 
			InetAddress address = ia.getAddress(); 
			if (address != null) 
				return address.getHostAddress() + ":" + ia.getPort(); 
		}
		return addr.toString(); 
	}
	
	public static boolean isIPv4Address(String address) { 
		if (address != null) { 
			int pos = address.indexOf(':');
			if (pos > 0) { 
				String ipaddress = address.substring(0, pos);
				if (ipaddress != null) { 
					String[] ips = ipaddress.split(".");
					if (ips != null && ips.length == 4) { 
						try { 
							int ip1 = Integer.valueOf(ips[0]).intValue();
							int ip2 = Integer.valueOf(ips[1]).intValue();
							int ip3 = Integer.valueOf(ips[2]).intValue();
							int ip4 = Integer.valueOf(ips[3]).intValue();
							
							if ((ip1 >= 0 && ip1 <= 255) && (ip2 >= 0 && ip2 <= 255) && 
								(ip3 >= 0 && ip3 <= 255) && (ip4 >= 0 && ip4 <= 255)) { 
								return true;
							}
						} catch (Exception e) { 
							
						}
					}
				}
			}
		}
		
		return false;
	}
	
}
