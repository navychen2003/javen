package org.javenstudio.cocoka.net.http.fetch;

import org.javenstudio.cocoka.util.MimeType;

public abstract class FetchSpan extends FetchSource {

	public FetchSpan(String source) {
		super(source); 
	}
	
	public MimeType getMimeType() {
		return MimeType.TYPE_APPLICATION; 
	}
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof FetchSpan)) 
			return false; 
		
		FetchSpan other = (FetchSpan)obj; 
		
		if (!super.equals(other)) 
			return false; 

		return true; 
	}
	
}
