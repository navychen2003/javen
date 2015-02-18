package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.codec.ITermsFormat;

public abstract class TermsWriter extends FieldsConsumer 
		implements ITermsFormat.Writer {

	private final TermsFormat mTermsFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	protected TermsWriter(TermsFormat format, IDirectory dir, String segment) { 
		mTermsFormat = format;
		mDirectory = dir;
		mSegment = segment;
	}
	
	public final TermsFormat getTermsFormat() { 
		return mTermsFormat; 
	}
	
	@Override
	public IIndexOutput createTermsOutput() throws IOException { 
		return mDirectory.createOutput(mTermsFormat.getContext(), 
				mTermsFormat.getTermsFileName(mSegment));
	}
	
	@Override
	public IIndexOutput createTermsIndexOutput() throws IOException { 
		return mDirectory.createOutput(mTermsFormat.getContext(), 
				mTermsFormat.getTermsIndexFileName(mSegment));
	}
	
}
