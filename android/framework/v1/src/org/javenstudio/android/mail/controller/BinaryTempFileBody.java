package org.javenstudio.android.mail.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.javenstudio.mail.MessagingException;
import org.javenstudio.mail.OutputBody;
import org.javenstudio.mail.util.Base64;
import org.javenstudio.mail.util.Base64OutputStream;
import org.javenstudio.mail.util.IOUtils;

import org.javenstudio.android.mail.Preferences;
import org.javenstudio.common.util.Logger;

/**
 * A Body that is backed by a temp file. The Body exposes a getOutputStream method that allows
 * the user to write to the temp file. After the write the body is available via getInputStream
 * and writeTo one time. After writeTo is called, or the InputStream returned from
 * getInputStream is closed the file is deleted and the Body should be considered disposed of.
 */
public final class BinaryTempFileBody implements OutputBody {
	private static Logger LOG = Logger.getLogger(BinaryTempFileBody.class);
	
	public interface OutputBodyListener { 
		public boolean isRequestStop(); 
		public void onOutputClosed(BinaryTempFileBody body); 
		public void onWrittenBytes(BinaryTempFileBody body, long writtenBytes); 
		public void onOutputFinished(BinaryTempFileBody body, long countBytes); 
	}
	
	private final OutputBodyListener mOutputListener; 
    private File mTempDirectory = null;
    private File mFile = null;
    private boolean mOutputFinished = false; 
    
    public BinaryTempFileBody() { 
    	this(null); 
    }
    
    public BinaryTempFileBody(OutputBodyListener listener) { 
    	mOutputListener = listener; 
    }
    
    private synchronized File getTempDirectory() throws IOException {
    	if (mTempDirectory == null) { 
    		String dir = Preferences.getPreferences().getTemporaryDirectory(); 
    		if (dir != null) { 
    			File file = new File(dir); 
    			if (file.exists() && file.isDirectory()) 
    				mTempDirectory = file; 
    		}
    		if (mTempDirectory == null) 
                throw new RuntimeException("Temp directory has not been inited on BinaryTempFileBody!");
    	}
        return mTempDirectory; 
    }

    /**
     * An alternate way to put data into a BinaryTempFileBody is to simply supply an already-
     * created file.  Note that this file will be deleted after it is read.
     * @param filePath The file containing the data to be stored on disk temporarily
     */
    //protected void setFile(String filePath) {
    //    mFile = new File(filePath);
    //}

    public synchronized long getFileLength() { 
    	File file = mFile; 
    	if (file != null) 
    		return file.length(); 
    	else 
    		return 0; 
    }
    
    @Override 
    public boolean isRequestStop() { 
    	return mOutputListener != null && mOutputListener.isRequestStop(); 
    }
    
    @Override 
    public synchronized void finishOutput(long count) { 
    	mOutputFinished = (count > 0);
    	
    	OutputBodyListener listener = mOutputListener; 
    	if (listener != null) 
    		listener.onOutputFinished(this, count); 
    }
    
    public final boolean isOutputFinished() { 
    	return mOutputFinished; 
    }
    
    @Override 
    public synchronized OutputStream getOutputStream() throws IOException {
        mFile = File.createTempFile("body", null, getTempDirectory());
        mFile.deleteOnExit();
        mOutputFinished = false; 
        
        return new BinaryTempFileOutputStream(
        		new BufferedOutputStream(new FileOutputStream(mFile), 
        				Preferences.getPreferences().getTemporaryIOBufferSize()));
    }

    @Override 
    public synchronized InputStream getInputStream() throws MessagingException {
        try {
            return new BinaryTempFileBodyInputStream(
            		new BufferedInputStream(new FileInputStream(mFile), 
            				Preferences.getPreferences().getTemporaryIOBufferSize()));
        } catch (IOException ioe) {
            throw new MessagingException("Unable to open body", ioe);
        }
    }

    @Override 
    public synchronized void writeTo(OutputStream out) throws IOException, MessagingException {
        InputStream in = getInputStream();
        Base64OutputStream base64Out = new Base64OutputStream(
            out, Base64.CRLF | Base64.NO_CLOSE);
        IOUtils.copy(in, base64Out);
        base64Out.close();
        mFile.delete();
    }

    class BinaryTempFileBodyInputStream extends FilterInputStream {
        public BinaryTempFileBodyInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
        	return super.read(); 
        }
        
        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
        	return super.read(buffer, offset, count); 
        }
        
        @Override
        public void close() throws IOException {
            super.close();
            
            if (LOG.isDebugEnabled()) 
            	LOG.debug("remove temp file: "+mFile.getAbsolutePath()); 
            
            mFile.delete();
        }
    }
    
    class BinaryTempFileOutputStream extends FilterOutputStream { 
    	private long mWrittenBytes = 0; 
    	
    	public BinaryTempFileOutputStream(OutputStream out) { 
    		super(out); 
    	}
    	
    	@Override
        public void write(int oneByte) throws IOException {
    		super.write(oneByte); 
    		mWrittenBytes += 1; 
    		
    		OutputBodyListener listener = mOutputListener; 
    		if (listener != null && (mWrittenBytes % 1024) == 0) 
    			listener.onWrittenBytes(BinaryTempFileBody.this, mWrittenBytes); 
    	}
    	
    	@Override 
    	public void close() throws IOException { 
    		super.close(); 
    		
    		if (LOG.isDebugEnabled()) 
            	LOG.debug("saved "+mFile.length()+" bytes to temp file: "+mFile.getAbsolutePath()); 
    		
    		OutputBodyListener listener = mOutputListener; 
    		if (listener != null) 
    			listener.onOutputClosed(BinaryTempFileBody.this); 
    	}
    }
}