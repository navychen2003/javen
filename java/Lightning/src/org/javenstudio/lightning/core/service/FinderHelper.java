package org.javenstudio.lightning.core.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.lightning.core.CoreContainers;
import org.javenstudio.util.StringUtils;

public class FinderHelper {
	private static final Logger LOG = Logger.getLogger(FinderHelper.class);

	public static final char PACKET_SEP = '|';
	public static final String PACKET_HEAD = "ANYBOX" + PACKET_SEP;
	
	public static class FinderServer implements IServerHandler {

		private final CoreContainers mContainers;
		private DatagramSocket mServer = null;
		
		public FinderServer(CoreContainers containers) {
			if (containers == null) throw new NullPointerException();
			mContainers = containers;
		}
		
		public CoreContainers getContainers() { return mContainers; }
		
		@Override
		public byte[] getSendBuffer(byte[] recvBuf, int offset, int length) {
			if (recvBuf != null && length > 0) {
				String data = new String(recvBuf, offset, length);
				if (LOG.isDebugEnabled())
					LOG.debug("Server: received: " + data);
				
				if (data != null && data.startsWith(PACKET_HEAD)) {
					if (LOG.isInfoEnabled())
						LOG.info("Server: received request: " + data);
					
					StringBuilder sbuf = new StringBuilder();
					sbuf.append(PACKET_HEAD);
					
					IHostNode host = getContainers().getCluster().getHostSelf();
					if (host != null) {
						String clusterDomain = host.getClusterDomain();
						if (clusterDomain == null || clusterDomain.length() == 0)
							clusterDomain = host.getHostName() + "/" + host.getHostAddress();
						
						sbuf.append(toStr(host.getClusterId()));
						sbuf.append(PACKET_SEP);
						sbuf.append(toStr(clusterDomain));
						sbuf.append(PACKET_SEP);
						sbuf.append(toStr(host.getHostAddress()));
						sbuf.append(PACKET_SEP);
						sbuf.append(Integer.toString(host.getHttpPort()));
						sbuf.append(PACKET_SEP);
						sbuf.append(Integer.toString(host.getHttpsPort()));
						sbuf.append(PACKET_SEP);
					}
					
					return sbuf.toString().getBytes();
				}
			}
			return null;
		}

		public void start(int port) throws IOException {
			startServer(this, port);
		}
		
		public void close() {
			DatagramSocket server = mServer;
			if (server != null) server.close();
		}
		
		@Override
		public boolean onServerClose(DatagramSocket server) {
			return false;
		}
		
		@Override
		public void onServerCreate(DatagramSocket server) {
			mServer = server;
		}

		@Override
		public void onServerReceive(DatagramSocket server, 
				DatagramPacket recvPacket) {
		}

		@Override
		public void onServerSend(DatagramSocket server, 
				DatagramPacket sendPacket) {
		}
	}
	
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
