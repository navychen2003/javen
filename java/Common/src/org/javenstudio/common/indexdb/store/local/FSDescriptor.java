package org.javenstudio.common.indexdb.store.local;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FSDescriptor extends RandomAccessFile {

    // remember if the file is open, so that we don't try to close it
    // more than once
    protected volatile boolean mIsOpen;
    protected long mPosition;
    protected final long mLength;
    protected final String mName;
    
	public FSDescriptor(File file, String mode) throws IOException {
    	super(file, mode);
    	mName = file.getName();
    	mIsOpen = true;
    	mLength = length();
    }

    @Override
    public void close() throws IOException {
    	if (mIsOpen) {
    		mIsOpen = false;
    		super.close();
    	}
	}
    
    public final String getName() { return mName; }
    public final boolean isOpen() { return mIsOpen; }
    public final long getPosition() { return mPosition; }
    public final long getLength() { return mLength; }
    
    protected void setPosition(long position) { 
    	mPosition = position;
    }
    
}
