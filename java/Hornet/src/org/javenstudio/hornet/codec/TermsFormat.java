package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.codec.ITermsFormat;

public abstract class TermsFormat implements ITermsFormat {

	/** Extension of terms file */
	public static final String TERMS_EXTENSION = "tim";
	
	/** Extension of terms index file */
	public static final String TERMS_INDEX_EXTENSION = "tip";
	
	private final IIndexFormat mFormat;
	
	protected TermsFormat(IIndexFormat format) { 
		mFormat = format;
	}
	
	@Override
	public final IIndexContext getContext() { 
		return mFormat.getContext(); 
	}
	
	@Override
	public final IIndexFormat getIndexFormat() { 
		return mFormat; 
	}
	
	@Override
	public String getTermsFileName(String segment) { 
		return getContext().getSegmentFileName(segment, TERMS_EXTENSION);
	}
	
	@Override
	public String getTermsIndexFileName(String segment) { 
		return getContext().getSegmentFileName(segment, TERMS_INDEX_EXTENSION);
	}
	
	public abstract ITermsFormat.Writer createWriter(IDirectory dir, 
			ISegmentWriteState state) throws IOException;
	
}
