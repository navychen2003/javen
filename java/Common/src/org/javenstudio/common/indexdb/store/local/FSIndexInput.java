package org.javenstudio.common.indexdb.store.local;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.store.BufferedIndexInput;

abstract class FSIndexInput extends BufferedIndexInput {

	protected final FSDescriptor mFile;
	
	public FSIndexInput(IIndexContext context, FSDescriptor file) {
		super(context);
		
		mFile = file;
	}
	
	public FSIndexInput(IIndexContext context, FSDescriptor file, int bufferSize) {
		super(context, bufferSize);
		
		mFile = file;
	}
	
	@Override
	protected void toString(StringBuilder sbuf) { 
		super.toString(sbuf);
		
		sbuf.append(",name=");
		sbuf.append(mFile.getName());
	}
	
}
