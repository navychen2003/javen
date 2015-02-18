package org.javenstudio.falcon.util;

import java.net.SocketException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.raptor.net.DNS;

public class NetUtils {

	public static String getNetworkAddress() throws ErrorException { 
		try {
			String[] addrs = DNS.getNetworkAddresses();
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
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
}
