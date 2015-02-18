package org.javenstudio.cocoka.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import android.net.NetworkInfo;

import org.javenstudio.cocoka.net.metrics.AbstractMetrics;

public class SocketMetrics extends AbstractMetrics {
	
	public static final String ACTION_BEGIN = "begin"; 
	public static final String ACTION_END = "end"; 
	public static final String ACTION_DONE = "done"; 
	
	public static final String BIND_ACTION = "socket.bind"; 
	public static final String CONNECT_ACTION = "socket.connect"; 
	public static final String SHUTDOWNINPUT_ACTION = "socket.shutdowninput"; 
	public static final String SHUTDOWNOUTPUT_ACTION = "socket.shutdownoutput"; 
	public static final String CLOSE_ACTION = "socket.close"; 
	public static final String READ_ACTION = "socket.read"; 
	public static final String WRITE_ACTION = "socket.write"; 
	
	public static interface SocketInfo { 
		public String getAddressName(); 
		public Socket getSocket();
		public long getConnectTime();
	}
	
	public static class NetworkStatus implements OnNetworkChangeListener { 
		private int mNetworkType = -1; // unavaliable
		
		@Override
		public synchronized void onNetworkChanged(NetworkInfo activeInfo, NetworkInfo mobileInfo, NetworkInfo wifiInfo) {
			if (activeInfo != null) 
				mNetworkType = activeInfo.getType(); 
			else 
				mNetworkType = -1; 
		}
		
		public synchronized int getNetworkType() { 
			return mNetworkType; 
		}
	}
	
	private static NetworkStatus sNetworkStatus = null; 
	
	public static synchronized NetworkStatus getNetworkStatus() { 
		if (sNetworkStatus == null) 
			sNetworkStatus = new NetworkStatus(); 
		
		return sNetworkStatus; 
	}
	
	public static int getCurrentNetworkType() {
		return getNetworkStatus().getNetworkType(); 
	}
	
	private final SocketInfo mSocketInfo; 
	
	SocketMetrics(SocketInfo socketInfo) {
		mSocketInfo = socketInfo; 
	}
	
	@Override 
	public String getMetricsName() { 
		return getClass().getName() + ":" + mSocketInfo.getAddressName(); 
	}
	
	public void onBindBegin(SocketAddress localAddr) {
		setMetric(BIND_ACTION, 0, ACTION_BEGIN); 
	}
	
	public void onBindEnd(SocketAddress localAddr) {
		setMetric(BIND_ACTION, 0, ACTION_END); 
	}
	
	public void onConnectBegin(SocketAddress remoteAddr, int timeout) {
		setMetric(CONNECT_ACTION, 0, ACTION_BEGIN); 
	}
	
	public void onConnectEnd(SocketAddress remoteAddr, int timeout) {
		setMetric(CONNECT_ACTION, 0, ACTION_END); 
	}

	public void onShutdownInput() {
		setMetric(SHUTDOWNINPUT_ACTION, 0, ACTION_DONE); 
	}
	
	public void onShutdownOutput() {
		setMetric(SHUTDOWNOUTPUT_ACTION, 0, ACTION_DONE); 
	}
	
	public void onClose() {
		setMetric(CLOSE_ACTION, 0, ACTION_DONE); 
	}
	
	public void onRead(int bytes) {
		if (bytes > 0) { 
			int type = getCurrentNetworkType(); 
			incrMetric(READ_ACTION, type, (long)bytes); 
		}
	}
	
	public void onWrite(int bytes) {
		if (bytes > 0) { 
			int type = getCurrentNetworkType(); 
			incrMetric(WRITE_ACTION, type, (long)bytes); 
		}
	}
	
	public static class MetricsInputStream extends InputStream {
		private final InputStream mDelegated; 
		private final SocketMetrics mMetrics; 
		
		public MetricsInputStream(InputStream is, SocketMetrics metrics) {
			mDelegated = is; 
			mMetrics = metrics; 
		}
		
		@Override
	    public int available() throws IOException {
	        return mDelegated.available();
	    }

	    @Override
	    public void close() throws IOException {
	    	mDelegated.close();
	    }
	    
	    @Override
	    public int read() throws IOException {
	        byte[] buffer = new byte[1];
	        int result = read(buffer, 0, 1);
	        return (-1 == result) ? result : buffer[0] & 0xFF;
	    }

	    @Override
	    public int read(byte[] buffer) throws IOException {
	        return read(buffer, 0, buffer.length);
	    }
	    
	    @Override
	    public int read(byte[] buffer, int offset, int count) throws IOException {
	    	int bytes = mDelegated.read(buffer, offset, count); 
	    	mMetrics.onRead(bytes); 
	    	return bytes; 
	    }
	    
	    @Override
	    public long skip(long n) throws IOException {
	    	return mDelegated.skip(n); 
	    }
	}
	
	public static class MetricsOutputStream extends OutputStream {
		private final OutputStream mDelegated; 
		private final SocketMetrics mMetrics; 
		
		public MetricsOutputStream(OutputStream os, SocketMetrics metrics) {
			mDelegated = os; 
			mMetrics = metrics; 
		}
		
		@Override
	    public void close() throws IOException {
			mDelegated.close();
	    }

	    @Override
	    public void write(byte[] buffer) throws IOException {
	        write(buffer, 0, buffer.length);
	    }
	    
	    @Override
	    public void write(int oneByte) throws IOException {
	        byte[] buffer = new byte[1];
	        buffer[0] = (byte) (oneByte & 0xFF);

	        write(buffer, 0, 1);
	    }
	    
	    @Override
	    public void write(byte[] buffer, int offset, int count) throws IOException {
	    	mDelegated.write(buffer, offset, count); 
	    	mMetrics.onWrite(count); 
	    }
	}
	
}
