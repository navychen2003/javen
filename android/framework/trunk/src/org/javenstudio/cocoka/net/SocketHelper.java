package org.javenstudio.cocoka.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.cocoka.net.http.SimpleHttpClient;

public class SocketHelper {

	private static final Map<Thread, List<SocketMetrics.SocketInfo>> sThreadSockets = 
			new HashMap<Thread, List<SocketMetrics.SocketInfo>>();
	
	static void updateSocketInfo(SocketMetrics.SocketInfo socketInfo) { 
		if (socketInfo == null) 
			return;
		
		synchronized (sThreadSockets) { 
			final Thread thread = Thread.currentThread();
			
			List<SocketMetrics.SocketInfo> socketList = sThreadSockets.get(thread);
			if (socketList == null) { 
				socketList = new ArrayList<SocketMetrics.SocketInfo>();
				sThreadSockets.put(thread, socketList);
			}
			
			boolean found = false;
			for (int i=0; i < socketList.size(); ) { 
				SocketMetrics.SocketInfo socket = socketList.get(i);
				if (socket != null) { 
					if (socket == socketInfo) 
						found = true;
					if (socket.getConnectTime() != 0) { 
						i ++; 
						continue;
					}
				}
				socketList.remove(i);
			}
			
			if (!found && socketInfo.getConnectTime() != 0) 
				socketList.add(socketInfo);
		}
	}
	
	public static SocketMetrics.SocketInfo[] getConnectingSockets() { 
		return getConnectingSockets(Thread.currentThread());
	}
	
	public static SocketMetrics.SocketInfo[] getConnectingSockets(final Thread thread) { 
		synchronized (sThreadSockets) { 
			List<SocketMetrics.SocketInfo> socketList = sThreadSockets.get(thread);
			if (socketList != null) { 
				for (int i=0; i < socketList.size(); ) { 
					SocketMetrics.SocketInfo socket = socketList.get(i);
					if (socket != null) { 
						if (socket.getConnectTime() != 0) { 
							i ++; 
							continue;
						}
					}
					socketList.remove(i);
				}
				
				return socketList.toArray(new SocketMetrics.SocketInfo[0]);
			}
			
			return null;
		}
	}
	
	public static boolean hasConnectingSocketOverTime(final Thread thread, long maxTime) { 
		SocketMetrics.SocketInfo[] socketInfos = getConnectingSockets(thread);
		
		if (socketInfos != null && socketInfos.length > 0) { 
			long currentTime = System.currentTimeMillis();
			
			for (int i=0; i < socketInfos.length; i++) { 
				SocketMetrics.SocketInfo socketInfo = socketInfos[i];
				if (socketInfo != null) { 
					long connectTime = socketInfo.getConnectTime();
					if (connectTime > 0 && currentTime - connectTime > maxTime)
						return true;
				}
			}
		}
		
		return false;
	}
	
	public static void setHttpParamsInitializer(SimpleHttpClient.HttpParamsInitializer initializer) { 
		SimpleHttpClient.setHttpParamsInitializer(initializer);
	}
	
}
