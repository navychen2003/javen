package org.javenstudio.falcon.user;

public interface INameData {

	public String getNameKey();
	public String getNameValue();
	public String getHostKey();
	
	public int getNameFlag();
	public String getAttr(String name);
	public String[] getAttrNames();
	
}
