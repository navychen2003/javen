package org.javenstudio.cocoka.net.http.util;

import java.util.ArrayList;

import org.javenstudio.cocoka.net.http.HttpHelper;
import org.javenstudio.common.parser.util.ApacheDirectoryParser;
import org.javenstudio.common.parser.util.DirectoryParser;

public class ApacheDirectoryUtil {
	
	public static String[] fetchDirectories(String location) {
		return parseDirectories(HttpHelper.fetchHtml(location)); 
	}
	
	public static String[] parseDirectories(String html) {
		DirectoryParser parser = new ApacheDirectoryParser(); 
		DirectoryParser.WebFile[] files = parser.parse(html); 
		
		if (files != null && files.length > 0) {
			ArrayList<String> names = new ArrayList<String>(); 
			for (int i=0; i < files.length; i++) {
				DirectoryParser.WebFile file = files[i]; 
				if (file != null && file instanceof DirectoryParser.WebDirectory) 
					names.add(file.getName()); 
			}
			return names.toArray(new String[names.size()]); 
		}
		
		return null; 
	}
	
	public static String[] fetchFiles(String location) {
		return parseFiles(HttpHelper.fetchHtml(location)); 
	}
	
	public static String[] parseFiles(String html) {
		DirectoryParser parser = new ApacheDirectoryParser(); 
		DirectoryParser.WebFile[] files = parser.parse(html); 
		
		if (files != null && files.length > 0) {
			ArrayList<String> names = new ArrayList<String>(); 
			for (int i=0; i < files.length; i++) {
				DirectoryParser.WebFile file = files[i]; 
				if (file != null && !(file instanceof DirectoryParser.WebDirectory)) 
					names.add(file.getName()); 
			}
			return names.toArray(new String[names.size()]); 
		}
		
		return null; 
	}
	
}
