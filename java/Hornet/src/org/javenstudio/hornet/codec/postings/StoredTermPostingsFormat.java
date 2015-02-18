package org.javenstudio.hornet.codec.postings;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.codec.ISegmentReadState;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.hornet.codec.TermPostingsFormat;
import org.javenstudio.hornet.codec.TermPostingsReader;
import org.javenstudio.hornet.codec.TermPostingsWriter;
import org.javenstudio.hornet.codec.SegmentReadState;
import org.javenstudio.hornet.codec.SegmentWriteState;

public class StoredTermPostingsFormat extends TermPostingsFormat {

	private static final String TERMS_CODEC = "Lucene40PostingsWriterTerms";
	private static final String FRQ_CODEC = "Lucene40PostingsWriterFrq";
	private static final String PRX_CODEC = "Lucene40PostingsWriterPrx";

	private static final String FORMAT_NAME = "Lucene40";
	
	// Increment version to change it:
	static final int VERSION_START = 0;
	static final int VERSION_CURRENT = VERSION_START;
	
	public StoredTermPostingsFormat(IIndexFormat format) { 
		super(format);
	}
	
	public String getTermsCodecName() { return TERMS_CODEC; }
	public String getFreqCodecName() { return FRQ_CODEC; }
	public String getProxCodecName() { return PRX_CODEC; }
	public String getFormatName() { return FORMAT_NAME; }
	
	@Override
	public TermPostingsWriter createWriter(IDirectory dir, ISegmentWriteState state) 
			throws IOException { 
		return new StoredTermPostingsWriter(this, dir, (SegmentWriteState)state);
	}
	
	@Override
	public TermPostingsReader createReader(IDirectory dir, ISegmentReadState state) 
			throws IOException { 
		return new StoredTermPostingsReader(this, dir, (SegmentReadState)state);
	}
	
}
