package org.javenstudio.common.util;

import java.util.ArrayList;
import java.util.List;

public class DelegatedEncoder implements InputEncoder {

	private List<InputEncoder> mEncoders = new ArrayList<InputEncoder>(); 
	
	public DelegatedEncoder() { }
	
	public DelegatedEncoder addEncoder(InputEncoder encoder) {
		if (encoder != null) {
			for (InputEncoder enc : mEncoders) {
				if (enc == encoder) 
					return this; 
			}
			
			mEncoders.add(encoder); 
		}
		
		return this; 
	}
	
	public String encode(String text) {
		if (text == null || text.length() == 0) 
			return text; 
		
		String result = text; 
		
		for (InputEncoder encoder : mEncoders) {
			result = encoder.encode(result); 
		}
		
		return result; 
	}
}
