package org.javenstudio.lightning.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;

public class SocketHelper {
	private static final Logger LOG = Logger.getLogger(SocketHelper.class);

	private static final Map<Thread, List<ISocketInfo>> sThreadSockets = 
			new HashMap<Thread, List<ISocketInfo>>();
	
	static void updateSocketInfo(ISocketInfo socketInfo) { 
		if (socketInfo == null) 
			return;
		
		synchronized (sThreadSockets) { 
			final Thread thread = Thread.currentThread();
			
			List<ISocketInfo> socketList = sThreadSockets.get(thread);
			if (socketList == null) { 
				socketList = new ArrayList<ISocketInfo>();
				sThreadSockets.put(thread, socketList);
			}
			
			boolean found = false;
			for (int i=0; i < socketList.size(); ) { 
				ISocketInfo socket = socketList.get(i);
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
	
	public static ISocketInfo[] getConnectingSockets() { 
		return getConnectingSockets(Thread.currentThread());
	}
	
	public static ISocketInfo[] getConnectingSockets(final Thread thread) { 
		synchronized (sThreadSockets) { 
			List<ISocketInfo> socketList = sThreadSockets.get(thread);
			if (socketList != null) { 
				for (int i=0; i < socketList.size(); ) { 
					ISocketInfo socket = socketList.get(i);
					if (socket != null) { 
						if (socket.getConnectTime() != 0) { 
							i ++; 
							continue;
						}
					}
					socketList.remove(i);
				}
				
				return socketList.toArray(new ISocketInfo[0]);
			}
			
			return null;
		}
	}
	
	public static boolean hasConnectingSocketOverTime(final Thread thread, long maxTime) { 
		ISocketInfo[] socketInfos = getConnectingSockets(thread);
		
		if (socketInfos != null && socketInfos.length > 0) { 
			long currentTime = System.currentTimeMillis();
			
			for (int i=0; i < socketInfos.length; i++) { 
				ISocketInfo socketInfo = socketInfos[i];
				if (socketInfo != null) { 
					long connectTime = socketInfo.getConnectTime();
					if (connectTime > 0 && currentTime - connectTime > maxTime)
						return true;
				}
			}
		}
		
		return false;
	}
	
	private static ISocketFactoryCreator sCreator = null;
	
	public static synchronized void setSocketFactoryCreator(ISocketFactoryCreator creator) { 
		if (sCreator != null) 
			throw new RuntimeException("SocketFactoryCreator already set");
		if (creator != null && creator != sCreator) 
			sCreator = creator;
	}
	
	public static synchronized ISocketFactory createSocketFactory(
			int handshakeTimeoutMillis, ISocketFactoryCreator.Type type) { 
		ISocketFactoryCreator creator = sCreator;
		if (creator == null) {
			//throw new RuntimeException("SocketFactoryCreator not set");
			if (LOG.isDebugEnabled())
				LOG.debug("SocketFactoryCreator not set, use SimpleFactory");
			
			creator = new ISocketFactoryCreator() {
					@Override
					public ISocketFactory createSocketFactory(
							int handshakeTimeoutMillis, Type type) {
						return SimpleSocketFactory.getSocketFactory();
					}
				};
			
			setSocketFactoryCreator(creator);
		}
		
		return creator.createSocketFactory(handshakeTimeoutMillis, type); 
	}
	
	//public static void setHttpParamsInitializer(SimpleHttpClient.HttpParamsInitializer initializer) { 
	//	SimpleHttpClient.setHttpParamsInitializer(initializer);
	//}
	
	//public static void closeConnections() { 
	//	SimpleHttpClient.closeConnections();
	//}
	
}
