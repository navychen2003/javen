package org.javenstudio.common.util;

import java.util.ArrayList;
import java.util.List;

public class DelegatedDecoder implements InputDecoder {

	private List<InputDecoder> mDecoders = new ArrayList<InputDecoder>(); 
	
	public DelegatedDecoder() { }
	
	public DelegatedDecoder addDecoder(InputDecoder decoder) {
		if (decoder != null) {
			for (InputDecoder dec : mDecoders) {
				if (dec == decoder) 
					return this; 
			}
			
			mDecoders.add(decoder); 
		}
		
		return this; 
	}
	
	public String decode(String text) {
		if (text == null || text.length() == 0) 
			return text; 
		
		String result = text; 
		
		for (InputDecoder decoder : mDecoders) {
			result = decoder.decode(result); 
		}
		
		return result; 
	}
}
