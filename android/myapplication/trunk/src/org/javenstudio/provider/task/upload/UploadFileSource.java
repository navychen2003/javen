package org.javenstudio.provider.task.upload;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.javenstudio.cocoka.net.http.multipart.PartSource;

public class UploadFileSource implements PartSource {

	private final UploadHandler mHandler;
	private final long mUploadId;
    private final File mFile; 
    private final long mContentLength;
    private final String mFileName;
    
    public UploadFileSource(UploadHandler handler, long uploadId, 
    		String filepath, String filename) throws IOException {
        if (filepath == null || filepath.length() == 0) 
            throw new IllegalArgumentException("File path may not be empty");
        
        mHandler = handler;
        mUploadId = uploadId;
        mFile = new File(filepath);
        
        if (!mFile.exists()) 
        	throw new FileNotFoundException("File: " + filepath + " not found");
        
        if (!mFile.isFile())
        	throw new IllegalArgumentException(filepath + " is not a file");
        
        if (!mFile.canRead())
        	throw new IllegalArgumentException(filepath + " is not readable");
        
        mContentLength = mFile.length();
        mFileName = filename == null ? mFile.getName() : filename;
    }

    public UploadFileSource(UploadHandler handler, long uploadId, 
    		String filepath) throws IOException {
    	this(handler, uploadId, filepath, null);
    }
    
    @Override
    public long getLength() {
        if (mFile != null) 
            return mFile.length();
        else 
            return 0;
    }

    @Override
    public String getFileName() {
        return (mFileName == null) ? "noname" : mFileName;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        if (mFile != null) 
            return new UploadInputStream(mFile);
        else 
            return new ByteArrayInputStream(new byte[] {});
    }

    private void onUploadRead(long readSize) { 
    	try {
    		mHandler.onUploadFileRead(mUploadId, readSize, mContentLength);
    	} catch (Throwable e) { 
    		// ignore
    	}
    }
    
    private class UploadInputStream extends InputStream {
		private final InputStream mDelegated; 
		private long mReadSize = 0;
		
		public UploadInputStream(File file) throws IOException {
			mDelegated = new BufferedInputStream(new FileInputStream(file)); 
		}
		
		@Override
	    public int available() throws IOException {
	        return mDelegated.available();
	    }

	    @Override
	    public void close() throws IOException {
	    	mDelegated.close();
	    }
	    
	    @Override
	    public int read() throws IOException {
	        byte[] buffer = new byte[1];
	        int result = read(buffer, 0, 1);
	        return (-1 == result) ? result : buffer[0] & 0xFF;
	    }

	    @Override
	    public int read(byte[] buffer) throws IOException {
	        return read(buffer, 0, buffer.length);
	    }
	    
	    @Override
	    public int read(byte[] buffer, int offset, int count) throws IOException {
	    	int bytes = mDelegated.read(buffer, offset, count); 
	    	if (bytes > 0) mReadSize += bytes;
	    	onUploadRead(mReadSize); 
	    	return bytes; 
	    }
	    
	    @Override
	    public long skip(long n) throws IOException {
	    	return mDelegated.skip(n); 
	    }
	}
    
}
