package org.javenstudio.cocoka.net.http.fetch;

public class FetchSource {

	protected final String mSource; 
	
	public FetchSource(String source) {
		mSource = source; 
	}
	
	public String getSource() { return mSource; } 
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof FetchSource)) 
			return false; 
		
		FetchSource other = (FetchSource)obj; 
		
		if (!mSource.equals(other.mSource)) 
			return false; 

		return true; 
	}
	
}
