package org.javenstudio.falcon.setting.cluster;

public class AttachHostUser implements IAttachUser, Comparable<AttachHostUser> {

	private final IHostUserData mUser;
	
	public AttachHostUser(IHostUserData user) {
		if (user == null) throw new NullPointerException();
		mUser = user;
	}
	
	public IHostUserData getUserData() { return mUser; }
	public IHostInfo getHostNode() { return getUserData().getHostNode(); }
	
	public String getUserKey() { return getUserData().getUserKey(); }
	public String getUserName() { return getUserData().getUserName(); }
	public String getUserEmail() { return getUserData().getUserEmail(); }
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof AttachHostUser)) return false;
		AttachHostUser other = (AttachHostUser)obj;
		return getUserName().equals(other.getUserName());
	}
	
	@Override
	public int compareTo(AttachHostUser o) {
		if (o == null) return -1;
		return getUserName().compareTo(o.getUserName());
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{key=" + getUserKey() 
				+ ",name=" + getUserName() + ",mailAddr=" + getUserEmail() 
				+ ",hostkey=" + getHostNode().getHostKey() + "}";
	}

}
