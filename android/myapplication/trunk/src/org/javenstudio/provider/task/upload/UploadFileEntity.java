package org.javenstudio.provider.task.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;

public class UploadFileEntity extends AbstractHttpEntity 
		implements Cloneable {

	private final UploadHandler mHandler;
	private final long mUploadId;
    private final File mFile; 
    private final long mContentLength;

    public UploadFileEntity(UploadHandler handler, long uploadId, 
    		String filepath, String contentType) throws IOException {
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
        setContentType(contentType);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return mContentLength;
    }
    
    @Override
    public InputStream getContent() throws IOException {
        return new UploadInputStream(mFile);
    }
    
    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        if (outstream == null) 
            throw new IllegalArgumentException("Output stream may not be null");
        
        InputStream instream = new UploadInputStream(mFile);
        try {
            byte[] tmp = new byte[4096];
            int len;
            while ((len = instream.read(tmp)) != -1) {
                outstream.write(tmp, 0, len);
            }
            outstream.flush();
        } finally {
            instream.close();
        }
    }

    /**
     * Tells that this entity is not streaming.
     *
     * @return <code>false</code>
     */
    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        // File instance is considered immutable
        // No need to make a copy of it
        return super.clone();
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
