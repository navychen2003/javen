package org.javenstudio.provider.account.host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.work.Work;
import org.javenstudio.common.util.Logger;
import org.javenstudio.util.StringUtils;

public class FinderHelper {
	private static final Logger LOG = Logger.getLogger(FinderHelper.class);

	public static interface IFinderListener {
		public void onHostFinding();
		public void onHostFindDone();
		public void onHostFound(String[] hostInfo);
	}
	
	private static final ArrayList<IFinderListener> sListeners = 
			new ArrayList<IFinderListener>();
	
	private static volatile DatagramSocket sClient = null;
	
	public static void addListener(IFinderListener listener) {
		if (listener == null) return;
		synchronized (sListeners) {
			for (IFinderListener l : sListeners) {
				if (l == listener) return;
			}
			sListeners.add(listener);
			if (sClient != null) listener.onHostFinding();
		}
	}
	
	public static void removeListener(IFinderListener listener) {
		if (listener == null) return;
		synchronized (sListeners) {
			sListeners.remove(listener);
		}
	}
	
	private static void removeAllListener() {
		synchronized (sListeners) {
			sListeners.clear();
		}
	}
	
	private static void onHostFinding() {
		synchronized (sListeners) {
			for (IFinderListener listener : sListeners) {
				if (listener != null) listener.onHostFinding();
			}
		}
	}
	
	private static void onHostFindDone() {
		synchronized (sListeners) {
			for (IFinderListener listener : sListeners) {
				if (listener != null) listener.onHostFindDone();
			}
			removeAllListener();
		}
	}
	
	private static void onHostFound(String[] hostInfo) {
		synchronized (sListeners) {
			for (IFinderListener listener : sListeners) {
				if (listener != null) listener.onHostFound(hostInfo);
			}
		}
	}
	
	public static void scheduleFindHost(final IFinderListener listener) {
		if (listener == null) return;
		if (sClient != null) { 
			addListener(listener);
			return;
		}
		
		try {
			ResourceHelper.getScheduler().post(new Work("FindHost") {
					@Override
					public void onRun() {
						try {
							findHost(listener);
						} catch (Throwable e) {
							if (LOG.isWarnEnabled())
								LOG.warn("findHost: error: " + e, e);
						}
					}
				});
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("findHost: error: " + e, e);
		}
	}
	
	public static void findHost(IFinderListener listener) throws IOException {
		findHost(listener, null, 0);
	}
	
	public static void findHost(IFinderListener listener, 
			String addr, int port) throws IOException {
		addListener(listener);
		if (sClient != null) return;
		
		int serverPort = port > 0 ? port : 10099;
		
		String serverAddr = addr != null && addr.length() > 0 ? 
				addr : getNetworkAddress();
		
		if (serverAddr != null && !serverAddr.equals("127.0.0.1")) {
			int pos = serverAddr.lastIndexOf('.');
			if (pos > 0) {
				String text = serverAddr.substring(0, pos);
				serverAddr = text + ".255";
			}
		}
		
		FinderHelper.FinderClient client = new FinderHelper.FinderClient() {
				@Override
				protected void onHostFound(String[] hostInfo) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("onHostFound: data=" + hostInfo);
						if (hostInfo != null) {
							for (String info : hostInfo) {
								LOG.debug("onHostFound: " + info);
							}
						}
					}
					FinderHelper.onHostFound(hostInfo);
				}
				@Override
				public void onClientCreate(DatagramSocket client) {
					if (LOG.isDebugEnabled()) LOG.debug("onHostFinding");
					sClient = client;
					FinderHelper.onHostFinding();
				}
				@Override
				public void onClientClosed(DatagramSocket client) {
					sClient = null;
				}
			};
		
		try {
			FinderHelper.startClient(client, serverAddr, serverPort);
		} finally {
			if (LOG.isDebugEnabled()) LOG.debug("onHostFindDone");
			sClient = null;
			FinderHelper.onHostFindDone();
		}
	}
	
	public static String getNetworkAddress() throws SocketException { 
		try {
			String[] addrs = getNetworkAddresses(null);
			if (addrs != null) { 
				String localIp = null;
				for (String addr : addrs) { 
					if (addr == null || addr.length() == 0) 
						continue;
					else if (addr.equals("127.0.0.1"))
						continue;
					else if (addr.startsWith("192.168."))
						localIp = addr;
					else
						return addr;
				}
				if (localIp != null && localIp.length() > 0)
					return localIp;
			}
			return "127.0.0.1";
		} catch (SocketException e) { 
			throw e;
		}
	}
	
	public static String[] getNetworkAddresses(String regexstr) 
			throws SocketException {
		try {
			ArrayList<String> ips = new ArrayList<String>(); 
			Pattern pattern = null; 
			if (regexstr != null && regexstr.length() > 0) {
				pattern = Pattern.compile(regexstr);
				if (LOG.isDebugEnabled()) 
					LOG.debug("getNetworkAddresses: compiled pattern: " + regexstr); 
			}

			Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
			while (netInterfaces.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface)netInterfaces.nextElement();
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
	        
				while (addresses.hasMoreElements()) {
					InetAddress ip = (InetAddress)addresses.nextElement();
					if (LOG.isDebugEnabled()) 
						LOG.debug("getNetworkAddresses: inetaddress: " + ip.toString()); 
	          
					//if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()) 
					String address = ip.getHostAddress();
					if (!"127.0.0.1".equals(address) && address.indexOf(":") < 0) {
						if (pattern == null || pattern.matcher(address).find()) {
							ips.add(address); 
							if (LOG.isDebugEnabled()) 
								LOG.debug("getNetworkAddresses:  found ip: "+address); 
						}
					}
				}
			}
	      
			return ips.toArray(new String[ips.size()]);
		} catch (SocketException e) {
			throw e; 
	    }
	}
	
	public static final char PACKET_SEP = '|';
	public static final String PACKET_HEAD = "ANYBOX" + PACKET_SEP;
	
	public static class FinderClient implements IClientHandler {

		@Override
		public byte[] getSendBuffer() {
			return new String(PACKET_HEAD).getBytes();
		}

		@Override
		public void onReceived(byte[] recvBuf, int offset, int length) {
			if (recvBuf != null && length > 0) {
				String data = new String(recvBuf, offset, length);
				if (LOG.isDebugEnabled())
					LOG.debug("Client: received: " + data);
				
				if (data != null && data.startsWith(PACKET_HEAD)) {
					if (LOG.isInfoEnabled())
						LOG.info("Client: received response: " + data);
					
					String[] tokens = StringUtils.split(data, PACKET_SEP);
					onHostFound(tokens);
				}
			}
		}

		protected void onHostFound(String[] hostInfo) {}
		
		@Override
		public void onClientCreate(DatagramSocket client) {
		}

		@Override
		public void onClientSend(DatagramSocket client,
				DatagramPacket sendPacket) {
		}

		@Override
		public void onClientReceive(DatagramSocket client,
				DatagramPacket recvPacket) {
		}
		
		@Override
		public void onReceiveTimeout(DatagramSocket client, 
				SocketTimeoutException e) {
		}
		
		@Override
		public boolean onClientClose(DatagramSocket client) {
			return false;
		}
		
		@Override
		public void onClientClosed(DatagramSocket client) {
		}
	}
	
	private static final int PACKET_LENGTH = 1024;
	
	public static interface IServerHandler {
		public void onServerCreate(DatagramSocket server);
		public void onServerReceive(DatagramSocket server, DatagramPacket recvPacket);
		public void onServerSend(DatagramSocket server, DatagramPacket sendPacket);
		public byte[] getSendBuffer(byte[] recvBuf, int offset, int length);
		public boolean onServerClose(DatagramSocket server);
	}
	
	public static void startServer(IServerHandler handler, int listenPort) throws IOException {
		if (handler == null) throw new NullPointerException();
		if (listenPort <= 0) throw new IllegalArgumentException("wrong listen port: " + listenPort);
		
		DatagramSocket server = new DatagramSocket(listenPort);
		handler.onServerCreate(server);
		
		while (!handler.onServerClose(server) && !server.isClosed()) {
			byte[] recvBuf = new byte[PACKET_LENGTH];
			DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
			
			handler.onServerReceive(server, recvPacket);
			server.receive(recvPacket);
			
			int port = recvPacket.getPort();
			InetAddress addr = recvPacket.getAddress();
			byte[] sendBuf = handler.getSendBuffer(recvPacket.getData(), 0, recvPacket.getLength());
			
			if (sendBuf != null) {
				DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, addr, port);
				handler.onServerSend(server, sendPacket);
				server.send(sendPacket);
				
			} else if (LOG.isDebugEnabled()) {
				LOG.debug("Server: no send data to " + addr 
						+ " port " + port + ".");
			}
		}
		
		server.close();
	}
	
	public static interface IClientHandler {
		public byte[] getSendBuffer();
		public void onReceived(byte[] recvBuf, int offset, int length);
		
		public void onClientCreate(DatagramSocket client);
		public void onClientSend(DatagramSocket client, DatagramPacket sendPacket);
		public void onClientReceive(DatagramSocket client, DatagramPacket recvPacket);
		public void onReceiveTimeout(DatagramSocket client, SocketTimeoutException e);
		public boolean onClientClose(DatagramSocket client);
		public void onClientClosed(DatagramSocket client);
	}
	
	public static void startClient(IClientHandler handler, String serverAddr, 
			int serverPort) throws IOException {
		if (handler == null || serverAddr == null) throw new NullPointerException();
		if (serverPort <= 0) throw new IllegalArgumentException("wrong server port: " + serverPort);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("startClient: serverAddr=" + serverAddr 
					+ " serverPort=" + serverPort);
		}
		
		DatagramSocket client = new DatagramSocket();
		client.setSoTimeout(10000); // 10 secs
		handler.onClientCreate(client);
		
		byte[] sendBuf = handler.getSendBuffer();
		DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, 
				InetAddress.getByName(serverAddr), serverPort);
		
		handler.onClientSend(client, sendPacket);
		client.send(sendPacket);
		
		while (!handler.onClientClose(client)) {
			byte[] recvBuf = new byte[PACKET_LENGTH];
			DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
			
			handler.onClientReceive(client, recvPacket);
			try {
				client.receive(recvPacket);
			} catch (SocketTimeoutException e) {
				if (LOG.isDebugEnabled())
					LOG.debug("startClient: receive timeout: " + e, e);
				handler.onReceiveTimeout(client, e);
				break;
			}
			
			handler.onReceived(recvPacket.getData(), 0, recvPacket.getLength());
		}
		
		client.close();
		handler.onClientClosed(client);
	}
	
	public static String toStr(Object o) { 
		if (o == null) return "";
		if (o instanceof String) return (String)o;
		if (o instanceof CharSequence) return ((CharSequence)o).toString();
		return o.toString();
	}
	
	public static String trim(String s) { 
		return StringUtils.trim(s);
	}
	
}
