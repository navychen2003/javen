package org.javenstudio.lightning.util;

import org.javenstudio.falcon.util.NetUtils;
import org.javenstudio.lightning.core.service.FinderHelper;
import org.javenstudio.raptor.conf.Configuration;

public class SimpleFinder extends SimpleShell {

	public static void main(String[] args) throws Exception {
		Configuration conf = loadConf().getConf();
		int serverPort = conf.getInt("finder.udp.port", 10099);
		
		String serverAddr = args != null && args.length > 0 ? 
				args[0] : NetUtils.getNetworkAddress();
		
		if (serverAddr != null && !serverAddr.equals("127.0.0.1")) {
			int pos = serverAddr.lastIndexOf('.');
			if (pos > 0) {
				String addr = serverAddr.substring(0, pos);
				serverAddr = addr + ".255";
			}
		}
		
		FinderHelper.FinderClient client = new FinderHelper.FinderClient() {
			@Override
			protected void onHostFound(String[] hostInfo) {
				System.out.println("Found Anybox Host: ");
				if (hostInfo != null) {
					for (String info : hostInfo) {
						System.out.println(info);
					}
				}
			}
		};
		
		FinderHelper.startClient(client, serverAddr, serverPort);
	}
	
}
