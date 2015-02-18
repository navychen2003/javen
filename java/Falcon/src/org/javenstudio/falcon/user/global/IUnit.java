package org.javenstudio.falcon.user.global;

public interface IUnit {
	
	public static final String TYPE_GROUP = "group";
	public static final String TYPE_MEMBER = "member";

	public String getKey();
	public String getName();
	
	public String getType();
	public String getStatus();
	public String getOwner();
	
}
