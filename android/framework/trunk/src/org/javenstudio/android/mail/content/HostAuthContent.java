package org.javenstudio.android.mail.content;

public interface HostAuthContent {

	public static final int TYPE_STORE = 1; 
	public static final int TYPE_SENDER = 2; 
	
	public static final int FLAG_SSL = 1;
    public static final int FLAG_TLS = 2;
    public static final int FLAG_AUTHENTICATE = 4;
    public static final int FLAG_TRUST_ALL_CERTIFICATES = 8;
	
    
    public String toUri(); 
    
	public long getId(); 
	public int getType(); 
	public String getProtocol(); 
	public String getAddress(); 
	public int getPort(); 
	public int getFlags(); 
	public String getLogin(); 
	public String getPassword(); 
	public String getDomain(); 
	public long getAccountKey(); 
	
	public void setType(int type); 
	public void setProtococl(String s); 
	public void setAddress(String s); 
	public void setPort(int port); 
	public void setFlags(int flags); 
	public void setLogin(String s); 
	public void setPassword(String s); 
	public void setDomain(String s); 
	
}
