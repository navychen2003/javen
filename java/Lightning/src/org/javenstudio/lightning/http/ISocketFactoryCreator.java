package org.javenstudio.lightning.http;

public interface ISocketFactoryCreator {

	public enum Type { SIMPLE, DEFAULT, SECURE, HTTP_SECURE }
	
	public ISocketFactory createSocketFactory(int handshakeTimeoutMillis, Type type); 
	
}
