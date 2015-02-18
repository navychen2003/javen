package org.javenstudio.android.information;

public class InformationPage {

	public static enum Status { 
		UNFETCH, FETCHED, ERROR
	}
	
	private final String mLocation;
	private Status mStatus = Status.UNFETCH;
	
	public InformationPage(String location) { 
		if (location == null) throw new NullPointerException();
		mLocation = location;
	}
	
	public String getLocation() { return mLocation; }
	
	public Status getStatus() { return mStatus; }
	public void setStatus(Status status) { mStatus = status; }
	
}
