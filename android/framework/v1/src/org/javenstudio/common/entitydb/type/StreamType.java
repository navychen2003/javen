package org.javenstudio.common.entitydb.type;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.javenstudio.common.util.Logger;

public class StreamType {
	private static Logger LOG = Logger.getLogger(StreamType.class);

	private final String mFieldName; 
	private InputStream mInput; 
	private String mFilePath; 
	
	public StreamType(String fieldName) {
		this(fieldName, (String)null); 
	}
	
	public StreamType(String fieldName, String filePath) {
		mFieldName = fieldName; 
		mInput = null; 
		mFilePath = filePath; 
	}
	
	public StreamType(String fieldName, InputStream input) {
		mFieldName = fieldName; 
		mInput = input; 
		mFilePath = null; 
	}
	
	public String getFieldName() { return mFieldName; } 
	public String getFilePath() { return mFilePath; } 
	public boolean isSaved() { return mFilePath != null; } 
	
	public InputStream newInputStream() throws IOException {
		try {
			if (mFilePath != null) 
				return new FileInputStream(mFilePath); 
		} catch (FileNotFoundException e) {
			// ignore 
		}
		return null; 
	}
	
	public int saveTo(String filepath) throws IOException {
		if (mInput == null) return -1; 
		
		OutputStream out = null; 
		int totalsize = 0; 
		try {
			byte[] buffer = new byte[4096]; 
			int readbytes = 0; 
			
			makeFilePath(filepath); 
			
			out = new BufferedOutputStream(new FileOutputStream(filepath)); 
			InputStream in = new BufferedInputStream(mInput); 
			
			while ((readbytes = in.read(buffer, 0, buffer.length)) >= 0) {
				if (readbytes > 0) {
					out.write(buffer, 0, readbytes); 
					totalsize += readbytes; 
				}
			}
			
			out.flush(); 
			mFilePath = filepath; 
			
			LOG.info("saved "+totalsize+" bytes stream data to "+filepath); 
			
		} catch (IOException e) { 
			totalsize = -1; 
			throw e; 
			
		} finally { 
			if (mInput != null) mInput.close(); 
			if (out != null) out.close(); 
			mInput = null; 
		}
		
		return totalsize; 
	}
	
	public static void makeFilePath(final String filepath) throws IOException { 
		if (filepath == null || filepath.length() == 0) 
			return; 
		
		if (!filepath.startsWith("/")) 
			return; 
		
		int pos = filepath.lastIndexOf('/'); 
		if (pos > 0) { 
			String path = filepath.substring(0, pos); 
			if (path != null) 
				makeDirectory(path); 
		}
	}
	
	public static void makeDirectory(final String directory) throws IOException { 
		if (directory == null || directory.length() == 0) 
			return; 
		
		File file = new File(directory); 
		if (!file.exists() && !file.mkdirs()) 
			throw new IOException("mkdirs failed: "+directory); 
	}
	
}
