package org.javenstudio.android.data.image;

public class ImageEvent {

	private final String mName;
	private final Throwable mException;
	
	public ImageEvent(String name) { 
		this(name, null);
	}
	
	public ImageEvent(String name, Throwable e) { 
		mName = name;
		mException = e;
		
		if (mName == null) 
			throw new NullPointerException("Name is null");
	}
	
	public final String getName() { return mName; }
	public final Throwable getException() { return mException; }
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{" + mName + "}";
	}
	
}
