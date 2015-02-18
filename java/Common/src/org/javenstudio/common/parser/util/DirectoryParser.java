package org.javenstudio.common.parser.util;

public abstract class DirectoryParser {

	public static class WebDirectory extends WebFile {
		public WebDirectory(String name) {
			super(name); 
		}
	}
	
	public static class WebFile {
		private String mName = null; 
		
		public WebFile(String name) {
			mName = name; 
		}
		
		public String getName() {
			return mName; 
		}
		
		public String toString() {
			return mName; 
		}
	}
	
	public DirectoryParser() {} 
	
	public abstract WebFile[] parse(String html); 
	
	
}
