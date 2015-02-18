package org.javenstudio.lightning.http;

import java.net.Socket;

public interface ISocketInfo {

	public String getAddressName(); 
	public Socket getSocket();
	public long getConnectTime();
	
}
