package org.javenstudio.falcon.util;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.util.StringUtils;

public class FsUtils {
	private static final Logger LOG = Logger.getLogger(FsUtils.class);

	public static String normalizePath(String path) {
		if (path == null) return null;
		final String p = path;
		
		if (path.startsWith("file:"))
			path = path.substring(5);
		
		if (File.separatorChar == '\\') { 
			path = path.replaceAll("\\\\.\\\\", "\\\\");
			path = path.replaceAll("/./", "\\\\");
		} else { 
			path = path.replaceAll("\\\\.\\\\", ""+File.separatorChar);
			path = path.replaceAll("/./", ""+File.separatorChar);
		}
		
	    path = path.replace('/', File.separatorChar);
	    path = path.replace('\\', File.separatorChar);
	    
	    path = Path.normalizePath(path);
		
	    StringBuffer sbuf = new StringBuffer();
	    boolean flag = path.indexOf(":/") > 0;
	    char prechr = 0;
	    
	    for (int i=0; i < path.length(); i++) { 
	    	char chr = path.charAt(i);
	    	if (chr == '\\' || chr == '/') { 
	    		if (prechr == chr) continue;
	    		if (sbuf.length() == 0) { 
	    			if (flag) continue;
	    		}
	    	}
	    	sbuf.append(chr);
	    	prechr = chr;
	    }
	    
	    path = sbuf.toString();
	    
	    if (LOG.isDebugEnabled())
			LOG.debug("normalizePath: path=" + p + " result=" + path);
	    
	    return path;
	}
	
	public static String normalizeUri(String uri) { 
		if (uri == null) return null;
		
		uri = uri.replaceAll("0:0:0:0:0:0:0:0", "127.0.0.1");
		uri = uri.replaceAll("0.0.0.0", "127.0.0.1");
		
		return uri;
	}
	
	public static boolean isLocalFs(FileSystem fs) { 
		return fs.getUri().getScheme().equalsIgnoreCase("file");
	}
	
	public static boolean isCloudFs(FileSystem fs) { 
		return fs.getUri().getScheme().equalsIgnoreCase("dfs");
	}
	
	public static String getHostName() { 
		String hostname = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			hostname = StringUtils.trim(addr.getCanonicalHostName());
		} catch (UnknownHostException e) {
			hostname = "Unknown";
		}
		return hostname;
	}
	
	public static String getFriendlyName() {
		String name = getHostName();
		if (name == null || name.equalsIgnoreCase("Unknown") || 
			name.equalsIgnoreCase("localhost") || 
			name.equalsIgnoreCase("127.0.0.1")) {
			name = StringUtils.trim(System.getProperty("os.name"));
		}
		if (name == null || name.length() == 0)
			name = "Unknown";
		return normalizeFriendlyName(name);
	}
	
	public static String normalizeFriendlyName(String name) { 
		if (name == null) name = "";
		name = StringUtils.trim(name);
		
		StringBuilder sbuf = new StringBuilder();
		char pchr = 0;
		
		for (int i=0; i < name.length(); i++) { 
			char chr = name.charAt(i);
			if (chr >= 0 && chr <= 127) {
				if ((chr >= 'a' && chr <= 'z') || (chr >= 'A' && chr <= 'Z') || 
					(chr >= '0' && chr <= '9')) { 
					sbuf.append(chr);
				} else if (chr == '.' || chr == '_' || chr == '-') { 
					sbuf.append(chr);
				} else {
					if (sbuf.length() == 0) 
						continue;
					chr = ' ';
					if (pchr != chr) 
						sbuf.append('-');
				}
			} else 
				sbuf.append(chr);
			pchr = chr;
		}
		
		return sbuf.toString();
	}
	
}
