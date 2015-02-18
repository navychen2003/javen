package org.javenstudio.falcon.setting.cluster;

public class AttachUser implements IAttachUser, Comparable<AttachUser> {

	private final String mUserKey;
	private final String mUserName;
	private final String mMailAddr;
	
	public AttachUser(String key, String name, String mailaddr) {
		if (key == null || name == null || mailaddr == null) 
			throw new NullPointerException();
		mUserKey = key;
		mUserName = name;
		mMailAddr = mailaddr;
	}
	
	public String getUserKey() { return mUserKey; }
	public String getUserName() { return mUserName; }
	public String getUserEmail() { return mMailAddr; }
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof AttachUser)) return false;
		AttachUser other = (AttachUser)obj;
		return getUserName().equals(other.getUserName());
	}
	
	@Override
	public int compareTo(AttachUser o) {
		if (o == null) return -1;
		return getUserName().compareTo(o.getUserName());
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{key=" + getUserKey() 
				+ ",name=" + getUserName() + ",mailAddr=" + getUserEmail() 
				+ "}";
	}

}
