package org.javenstudio.cocoka.storage.fs;

import java.io.IOException; 
import java.io.InputStream;
import java.net.URI; 
import java.util.Arrays; 
import java.util.Comparator;

public class FsHelper {

	@SuppressWarnings({"unused"})
	public static IFileSystem getFileSystem(Path path) throws IOException {
		URI uri = path.toUri(); 
		if (uri == null) 
			throw new IOException("wrong path input"); 
		
		String scheme = uri.getScheme();
	    String authority = uri.getAuthority();
		
	    if (scheme == null) 
	    	scheme = "file"; 
	    
	    scheme = scheme.toLowerCase(); 
	    
	    return FileSystems.get(scheme, uri); 
	}
	
	public static IFile getFile(Path path) throws IOException {
		return getFileSystem(path).getFile(path); 
	}
	
	public static IFile getFile(String filename) throws IOException {
		return getFile(new Path(filename)); 
	}
	
	public static IFile[] sortFiles(IFile[] files) {
		if (files == null || files.length <= 1) 
			return files; 
		
		Arrays.sort(files, new Comparator<IFile>() {
			public int compare(IFile a, IFile b) {
				if (a == null && b == null) 
					return 0; 
				
				if (a == null) 
					return -1; 
				
				if (b == null) 
					return 1; 
				
				boolean aIsFolder = a.isDirectory(); 
				boolean bIsFolder = b.isDirectory(); 
				
				if (aIsFolder != bIsFolder) {
					if (aIsFolder) 
						return -1; 
					
					if (bIsFolder) 
						return 1; 
				}
				
				String aName = a.getName(); 
				String bName = b.getName(); 
				
				if (aName == null && bName == null) 
					return 0; 
				
				if (aName == null) 
					return -1; 
				
				if (bName == null) 
					return 1; 
				
				for (int i=0; i < aName.length() && i < bName.length(); i++) {
					int charA = aName.charAt(i); 
					int charB = bName.charAt(i); 
					
					charA = (charA >= 'A' && charA <= 'Z') ? (charA + 'a' - 'A') : charA; 
					charB = (charB >= 'A' && charB <= 'Z') ? (charB + 'a' - 'A') : charB; 
					
					if (charA < charB) 
						return -1; 
					else if (charA > charB) 
						return 1; 
				}
				
				int lengthA = aName.length(); 
				int lengthB = bName.length(); 
				
				if (lengthA > lengthB) 
					return 1; 
				
				if (lengthA < lengthB) 
					return -1; 
				
				return 0; 
			}
		}); 
		
		return files; 
	}
	
	public static InputStream getInputStream(IFile file) throws IOException {
		if (file == null) return null; 
		
		InputStream is = null; 
		
		try {
			IFileSystem fs = file.getFileSystem(); 
			is = fs.open(file); 
			
		} catch (IOException e) {
			is = null; 
		}
		
		return is; 
	}
	
	public static byte[] loadFile(IFile file) throws IOException {
		if (file == null) return null; 
		
		InputStream is = null; 
		
		try {
			IFileSystem fs = file.getFileSystem(); 
			is = fs.open(file); 
			
			int length = (int)file.length(); 
			byte[] buffer = new byte[length]; 
			
			int res = is.read(buffer, 0, length); 
			
			is.close(); 
			
			if (res > 0) return buffer; 
			
		} catch (IOException e) {
			try {
				if (is != null) 
					is.close(); 
			} catch (Exception ex) {
				// ignore
			}
		}
		
		return null; 
	}
}
