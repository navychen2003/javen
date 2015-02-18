package org.javenstudio.hornet.codec;

import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ITermPostingsFormat;
import org.javenstudio.common.indexdb.codec.ISegmentReadState;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;

public abstract class TermPostingsFormat implements ITermPostingsFormat {

	/** Extension of freq postings file */
	public static final String FREQ_EXTENSION = "frq";

	/** Extension of prox postings file */
	public static final String PROX_EXTENSION = "prx";
	
	private final IIndexFormat mFormat;
	
	protected TermPostingsFormat(IIndexFormat format) { 
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
	public String getPostingsFreqFileName(String segment) { 
		return getContext().getSegmentFileName(segment, FREQ_EXTENSION);
	}
	
	@Override
	public String getPostingsProxFileName(String segment) { 
		return getContext().getSegmentFileName(segment, PROX_EXTENSION);
	}
	
	public abstract String getTermsCodecName();
	public abstract String getFreqCodecName();
	public abstract String getProxCodecName();
	public abstract String getFormatName();
	
	public abstract ITermPostingsFormat.Writer createWriter(IDirectory dir, 
			ISegmentWriteState state) throws IOException;
	
	public abstract ITermPostingsFormat.Reader createReader(IDirectory dir, 
			ISegmentReadState state) throws IOException;
	
}
